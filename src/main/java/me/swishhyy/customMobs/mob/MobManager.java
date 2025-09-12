package me.swishhyy.customMobs.mob;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
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

import me.swishhyy.customMobs.abilities.Ability;
import me.swishhyy.customMobs.abilities.AbilityRegistry;
import me.swishhyy.customMobs.abilities.AbilityContext;
import me.swishhyy.customMobs.skills.*;

public class MobManager {
    private final JavaPlugin plugin;
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
        if (!folder.exists()) folder.mkdirs();
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

    private void parseDefinition(String id, FileConfiguration cfg) {
        String type = cfg.getString("type", "ZOMBIE");
        double health = cfg.getDouble("health", 20.0);
        double attack = cfg.getDouble("attack", 0.0);
        String displayName = cfg.getString("display", null);

        List<Ability> onSpawn = new ArrayList<>();
        List<Ability> onHit = new ArrayList<>();
        List<Ability> onDamaged = new ArrayList<>();
        Map<SkillTrigger, List<SkillNode>> skills = new HashMap<>();

        ConfigurationSection abilities = cfg.getConfigurationSection("abilities");
        if (abilities != null) {
            // MythicMobs-like: triggers sections
            loadAbilityList(abilities, "onSpawn", onSpawn);
            loadAbilityList(abilities, "onHit", onHit);
            loadAbilityList(abilities, "onDamaged", onDamaged);
        }

        ConfigurationSection skillsSec = cfg.getConfigurationSection("skills");
        if (skillsSec != null) {
            for (String trigKey : skillsSec.getKeys(false)) {
                SkillTrigger trigger = null; try { trigger = SkillTrigger.valueOf(trigKey.toUpperCase()); } catch (IllegalArgumentException ignored) {}
                if (trigger == null) continue;
                ConfigurationSection listSec = skillsSec.getConfigurationSection(trigKey); if (listSec == null) continue;
                List<SkillNode> nodes = new ArrayList<>();
                for (String nodeKey : listSec.getKeys(false)) {
                    ConfigurationSection nodeSec = listSec.getConfigurationSection(nodeKey); if (nodeSec == null) continue;
                    SkillAction action = SkillActionRegistry.INSTANCE.create(nodeSec); if (action == null) continue;
                    Targeter targeter = null;
                    ConfigurationSection targeterSec = nodeSec.getConfigurationSection("targeter");
                    if (targeterSec != null) targeter = Targeter.REGISTRY.create(targeterSec);
                    List<SkillCondition> conds = new ArrayList<>();
                    ConfigurationSection condSec = nodeSec.getConfigurationSection("conditions");
                    if (condSec != null) {
                        for (String cKey : condSec.getKeys(false)) {
                            ConfigurationSection one = condSec.getConfigurationSection(cKey); if (one == null) continue;
                            SkillCondition sc = SkillConditionRegistry.INSTANCE.create(one); if (sc != null) conds.add(sc);
                        }
                    }
                    nodes.add(new SkillNode(nodeKey, action, targeter, conds));
                }
                if (!nodes.isEmpty()) skills.put(trigger, nodes);
            }
        }

        MobDefinition def = new MobDefinition(id, type, health, attack, displayName, onSpawn, onHit, onDamaged, skills);
        definitions.put(id.toLowerCase(Locale.ROOT), def);
    }

    private void loadAbilityList(ConfigurationSection parent, String key, List<Ability> out) {
        ConfigurationSection sec = parent.getConfigurationSection(key);
        if (sec == null) return;
        for (String child : sec.getKeys(false)) {
            ConfigurationSection abilitySec = sec.getConfigurationSection(child);
            if (abilitySec == null) continue;
            Ability a = AbilityRegistry.INSTANCE.create(abilitySec);
            if (a != null) out.add(a); else plugin.getLogger().warning("Unknown ability type in section " + parent.getCurrentPath() + "." + key + "." + child);
        }
    }

    public MobDefinition get(String id) { return id == null ? null : definitions.get(id.toLowerCase(Locale.ROOT)); }

    public LivingEntity spawn(String id, org.bukkit.Location loc) {
        MobDefinition def = get(id);
        if (def == null) return null;
        EntityType type;
        try { type = EntityType.valueOf(def.type().toUpperCase()); }
        catch (IllegalArgumentException ex) { plugin.getLogger().warning("Unknown entity type for mob '" + id + "': " + def.type()); return null; }
        if (loc.getWorld() == null) return null;
        LivingEntity ent = (LivingEntity) loc.getWorld().spawnEntity(loc, type);
        if (def.displayName() != null) {
            Component name = LegacyComponentSerializer.legacyAmpersand().deserialize(def.displayName());
            ent.customName(name);
            ent.setCustomNameVisible(true);
        }
        ent.getPersistentDataContainer().set(mobIdKey, PersistentDataType.STRING, def.id());
        AttributeInstance maxHealth = ent.getAttribute(Attribute.MAX_HEALTH);
        if (maxHealth != null) {
            maxHealth.setBaseValue(def.health());
            ent.setHealth(Math.min(def.health(), maxHealth.getBaseValue()));
        }
        if (def.attack() > 0) {
            AttributeInstance attackAttr = ent.getAttribute(Attribute.ATTACK_DAMAGE);
            if (attackAttr != null) attackAttr.setBaseValue(def.attack());
        }
        // Execute onSpawn abilities
        def.onSpawn().forEach(a -> a.execute(new AbilityContext(ent, null, null)));
        // Execute new skill system ON_SPAWN
        def.skills().getOrDefault(SkillTrigger.ON_SPAWN, java.util.Collections.emptyList())
            .forEach(n -> n.execute(new SkillContext(ent, null, null, def)));
        return ent;
    }

    public LivingEntity spawnResolved(String query, org.bukkit.Location loc) { return spawn(query, loc); }

    public void handleHit(LivingEntity caster, LivingEntity target, org.bukkit.event.Event evt) {
        String id = caster.getPersistentDataContainer().get(mobIdKey, PersistentDataType.STRING); if (id == null) return;
        MobDefinition def = get(id); if (def == null) return;
        def.onHit().forEach(a -> a.execute(new AbilityContext(caster, target, evt)));
        def.skills().getOrDefault(SkillTrigger.ON_HIT, java.util.Collections.emptyList())
            .forEach(n -> n.execute(new SkillContext(caster, target, evt, def)));
    }

    public void handleDamaged(LivingEntity caster, LivingEntity damager, org.bukkit.event.Event evt) {
        String id = caster.getPersistentDataContainer().get(mobIdKey, PersistentDataType.STRING); if (id == null) return;
        MobDefinition def = get(id); if (def == null) return;
        def.onDamaged().forEach(a -> a.execute(new AbilityContext(caster, damager, evt)));
        def.skills().getOrDefault(SkillTrigger.ON_DAMAGED, java.util.Collections.emptyList())
            .forEach(n -> n.execute(new SkillContext(caster, damager, evt, def)));
    }

    public java.util.Set<String> getMobIds() { return java.util.Collections.unmodifiableSet(definitions.keySet()); }
}
