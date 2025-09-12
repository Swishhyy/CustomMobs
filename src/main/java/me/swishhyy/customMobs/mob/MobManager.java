package me.swishhyy.customMobs.mob;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Locale;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;

import org.bukkit.NamespacedKey;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;

import me.swishhyy.customMobs.abilities.Ability;
import me.swishhyy.customMobs.abilities.AbilityRegistry;
import me.swishhyy.customMobs.abilities.AbilityContext;

public class MobManager {
    private final JavaPlugin plugin;
    private final AbilityRegistry abilityRegistry = new AbilityRegistry();
    private final Map<String, MobDefinition> definitions = new HashMap<>();
    private final NamespacedKey mobIdKey;

    public MobManager(JavaPlugin plugin) {
        this.plugin = plugin;
        this.mobIdKey = new NamespacedKey(plugin, "mob-id");
        ensureDefaultConfigs();
    }

    private void ensureDefaultConfigs() {
        // Copy example resources to data folder on first run
        copyResource("custom/zombie2.yaml");
        copyResource("vanilla/zombie.yaml");
    }

    private void copyResource(String path) {
        File out = new File(plugin.getDataFolder(), path);
        if (out.exists()) return;
        if (!out.getParentFile().exists()) out.getParentFile().mkdirs();
        plugin.saveResource(path, false);
    }

    public void reloadAll() {
        definitions.clear();
        loadMobConfigs("custom");
        loadMobConfigs("vanilla");
    }

    public void loadMobConfigs(String type) {
        File folder = new File(plugin.getDataFolder(), type);
        if (!folder.exists()) {
            folder.mkdirs();
        }
        File[] files = folder.listFiles((dir, name) -> name.endsWith(".yml") || name.endsWith(".yaml"));
        if (files == null) return;
        for (File file : files) {
            try {
                FileConfiguration cfg = YamlConfiguration.loadConfiguration(file);
                parseDefinition(file.getName().replaceFirst("\\.ya?ml$", ""), cfg);
            } catch (Exception e) {
                plugin.getLogger().log(Level.SEVERE, "Failed to parse mob config " + file.getName(), e);
            }
        }
        plugin.getLogger().info("Loaded " + definitions.size() + " mob definitions.");
    }

    private void parseDefinition(String id, FileConfiguration cfg) throws IOException {
        MobDefinition def = new MobDefinition();
        def.id = id;
        def.type = cfg.getString("type", "ZOMBIE");
        def.health = cfg.getDouble("health", 20.0);
        def.attack = cfg.getDouble("attack", 0.0);
        def.displayName = cfg.getString("display", null);

        ConfigurationSection abilities = cfg.getConfigurationSection("abilities");
        if (abilities != null) {
            // MythicMobs-like: triggers sections
            loadAbilityList(abilities, "onSpawn", def.onSpawn);
            loadAbilityList(abilities, "onHit", def.onHit);
            loadAbilityList(abilities, "onDamaged", def.onDamaged);
        }

        definitions.put(id.toLowerCase(), def);
    }

    private void loadAbilityList(ConfigurationSection parent, String key, List<Ability> out) {
        ConfigurationSection sec = parent.getConfigurationSection(key);
        if (sec == null) return;
        for (String child : sec.getKeys(false)) {
            ConfigurationSection abilitySec = sec.getConfigurationSection(child);
            if (abilitySec == null) continue;
            Ability a = abilityRegistry.create(abilitySec);
            if (a != null) {
                out.add(a);
            } else {
                String t = abilitySec.getString("type", "<missing>");
                plugin.getLogger().warning("Unknown ability type '" + t + "' in section " + parent.getCurrentPath() + "." + key + "." + child);
            }
        }
    }

    public MobDefinition get(String id) {
        return definitions.get(id.toLowerCase());
    }

    public LivingEntity spawn(String id, org.bukkit.Location loc) {
        MobDefinition def = get(id);
        if (def == null) return null;
        EntityType type;
        try {
            type = EntityType.valueOf(def.type.toUpperCase());
        } catch (IllegalArgumentException ex) {
            plugin.getLogger().warning("Unknown entity type for mob '" + id + "': " + def.type);
            return null;
        }
        if (loc.getWorld() == null) return null;
        LivingEntity ent = (LivingEntity) loc.getWorld().spawnEntity(loc, type);
        if (def.displayName != null) {
            Component name = LegacyComponentSerializer.legacyAmpersand().deserialize(def.displayName);
            ent.customName(name);
            ent.setCustomNameVisible(true);
        }
        ent.getPersistentDataContainer().set(mobIdKey, PersistentDataType.STRING, def.id);
        AttributeInstance maxHealth = ent.getAttribute(Attribute.MAX_HEALTH);
        if (maxHealth != null) {
            maxHealth.setBaseValue(def.health);
            ent.setHealth(Math.min(def.health, maxHealth.getBaseValue()));
        }
        if (def.attack > 0) {
            AttributeInstance attackAttr = ent.getAttribute(Attribute.ATTACK_DAMAGE);
            if (attackAttr != null) attackAttr.setBaseValue(def.attack);
        }
        // Execute onSpawn abilities
        def.onSpawn.forEach(a -> a.execute(new AbilityContext(ent, null, null)));
        return ent;
    }

    private String normalizeDisplay(String s) {
        Component c = LegacyComponentSerializer.legacyAmpersand().deserialize(s);
        String plain = PlainTextComponentSerializer.plainText().serialize(c);
        return plain.toLowerCase(Locale.ROOT).trim();
    }

    public MobDefinition resolve(String query) {
        if (query == null) return null;
        String q = query.toLowerCase(Locale.ROOT).trim();
        MobDefinition byId = definitions.get(q);
        if (byId != null) return byId;
        for (MobDefinition def : definitions.values()) {
            if (def.displayName == null) continue;
            if (normalizeDisplay(def.displayName).equals(q)) return def;
        }
        return null;
    }

    public LivingEntity spawnResolved(String query, org.bukkit.Location loc) {
        MobDefinition def = resolve(query);
        if (def == null) return null;
        return spawn(def.id, loc);
    }

    public void handleHit(LivingEntity caster, LivingEntity target, org.bukkit.event.Event evt) {
        String id = caster.getPersistentDataContainer().get(mobIdKey, PersistentDataType.STRING);
        if (id == null) return;
        MobDefinition def = get(id);
        if (def == null) return;
        def.onHit.forEach(a -> a.execute(new AbilityContext(caster, target, evt)));
    }

    public void handleDamaged(LivingEntity caster, LivingEntity damager, org.bukkit.event.Event evt) {
        String id = caster.getPersistentDataContainer().get(mobIdKey, PersistentDataType.STRING);
        if (id == null) return;
        MobDefinition def = get(id);
        if (def == null) return;
        def.onDamaged.forEach(a -> a.execute(new AbilityContext(caster, damager, evt)));
    }

    public java.util.Set<String> getMobIds() {
        return java.util.Collections.unmodifiableSet(definitions.keySet());
    }
}
