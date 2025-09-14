package me.swishhyy.customMobs.mob;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;
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
import org.bukkit.Material;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;
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
    private final NamespacedKey naturalKey;

    public MobManager(JavaPlugin plugin) {
        this.plugin = plugin;
        this.mobIdKey = new NamespacedKey(plugin, "mob-id");
        this.naturalKey = new NamespacedKey(plugin, "natural");
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
        try {
            String type = cfg.getString("type", "ZOMBIE");
            double health = safePrimitive(cfg, "health", 20.0);
            double attack = safePrimitive(cfg, "attack", 0.0);
            Double speed = safeWrapper(cfg, "speed");
            Double followRange = cfg.isSet("follow_range") ? safeWrapper(cfg, "follow_range") : (cfg.isSet("followRange") ? safeWrapper(cfg, "followRange") : null);
            Double armor = safeWrapper(cfg, "armor");
            Double armorTough = cfg.isSet("armor_toughness") ? safeWrapper(cfg, "armor_toughness") : (cfg.isSet("armorToughness") ? safeWrapper(cfg, "armorToughness") : null);
            Double kbResist = cfg.isSet("knockback_resist") ? safeWrapper(cfg, "knockback_resist") : (cfg.isSet("knockbackResist") ? safeWrapper(cfg, "knockbackResist") : null);
            String displayName = cfg.getString("display", null);
            String faction = cfg.getString("faction", null); // default handled later
            boolean passiveAI = cfg.getBoolean("passive_ai", false);
            int minLevel = (int) safePrimitive(cfg, "min_level", 1.0);
            int maxLevel = (int) safePrimitive(cfg, "max_level", minLevel);
            double healthPerLevel = safePrimitive(cfg, "health_per_level", 0.0);
            double attackPerLevel = safePrimitive(cfg, "attack_per_level", 0.0);
            // natural spawn section
            boolean natEnabled = false; double natChance = 0.0; boolean natReplace = false; double natWeight = 1.0; java.util.Set<String> natBiomes = new HashSet<>();
            Integer natCapChunk = null;
            ConfigurationSection natSec = cfg.getConfigurationSection("natural");
            if (natSec != null) {
                natEnabled = natSec.getBoolean("enabled", false);
                natChance = natSec.contains("chance") ? safePrimitive(natSec, "chance", 0.0) : 0.0;
                natReplace = natSec.getBoolean("replace", false);
                natWeight = natSec.contains("weight") ? safePrimitive(natSec, "weight", 1.0) : 1.0;
                if (natSec.isInt("cap_chunk")) natCapChunk = natSec.getInt("cap_chunk");
                for (String b : natSec.getStringList("biomes")) natBiomes.add(b.toUpperCase(java.util.Locale.ROOT));
            }
            // existing lists
            List<Ability> onSpawn = new ArrayList<>();
            List<Ability> onHit = new ArrayList<>();
            List<Ability> onDamaged = new ArrayList<>();
            Map<SkillTrigger, List<SkillNode>> skills = new HashMap<>();
            Map<DamageCause, Double> damageMods = new HashMap<>();
            List<DropSpec> drops = new ArrayList<>();
            MoneySpec money = null;
            // abilities
            ConfigurationSection abilities = cfg.getConfigurationSection("abilities");
            if (abilities != null) { loadAbilityList(abilities, "onSpawn", onSpawn); loadAbilityList(abilities, "onHit", onHit); loadAbilityList(abilities, "onDamaged", onDamaged); }
            // skills
            ConfigurationSection skillsSec = cfg.getConfigurationSection("skills");
            if (skillsSec != null) {
                for (String trigKey : skillsSec.getKeys(false)) {
                    SkillTrigger trigger = null; try { trigger = SkillTrigger.valueOf(trigKey.toUpperCase()); } catch (IllegalArgumentException ignored) {}
                    if (trigger == null) continue; ConfigurationSection listSec = skillsSec.getConfigurationSection(trigKey); if (listSec == null) continue; List<SkillNode> nodes = new ArrayList<>();
                    for (String nodeKey : listSec.getKeys(false)) { ConfigurationSection nodeSec = listSec.getConfigurationSection(nodeKey); if (nodeSec == null) continue; SkillAction action = SkillActionRegistry.INSTANCE.create(nodeSec); if (action == null) continue; Targeter targeter = null; ConfigurationSection targeterSec = nodeSec.getConfigurationSection("targeter"); if (targeterSec != null) targeter = Targeter.REGISTRY.create(targeterSec); List<SkillCondition> conds = new ArrayList<>(); ConfigurationSection condSec = nodeSec.getConfigurationSection("conditions"); if (condSec != null) for (String cKey : condSec.getKeys(false)) { ConfigurationSection one = condSec.getConfigurationSection(cKey); if (one == null) continue; SkillCondition sc = SkillConditionRegistry.INSTANCE.create(one); if (sc != null) conds.add(sc); } long cooldownMs = 0L; if (nodeSec.contains("cooldownMs")) cooldownMs = nodeSec.getLong("cooldownMs"); else if (nodeSec.contains("cooldown")) cooldownMs = nodeSec.getLong("cooldown"); else if (nodeSec.contains("cooldownSeconds")) cooldownMs = nodeSec.getLong("cooldownSeconds") * 1000L; nodes.add(new SkillNode(nodeKey, action, targeter, conds, cooldownMs)); }
                    if (!nodes.isEmpty()) skills.put(trigger, nodes);
                }
            }
            // damage modifiers
            ConfigurationSection dmgSec = cfg.getConfigurationSection("damage_modifiers");
            if (dmgSec != null) for (String causeKey : dmgSec.getKeys(false)) { try { DamageCause cause = DamageCause.valueOf(causeKey.toUpperCase()); double mult = safePrimitive(dmgSec, causeKey, 1.0); damageMods.put(cause, mult); } catch (IllegalArgumentException ignored) {} }
            // drops
            ConfigurationSection dropsSec = cfg.getConfigurationSection("drops");
            if (dropsSec != null) for (String dk : dropsSec.getKeys(false)) { ConfigurationSection d = dropsSec.getConfigurationSection(dk); if (d == null) continue; String matName = d.getString("type", d.getString("material", "STONE")); Material mat = null; try { mat = Material.valueOf(matName.toUpperCase()); } catch (IllegalArgumentException ignored) {} int min = d.getInt("min", 1); int max = d.getInt("max", min); double chance = safePrimitive(d, "chance", 1.0); if (mat != null && chance > 0) drops.add(new DropSpec(mat, min, max, chance)); }
            // money (MoneyFromMobs integration)
            ConfigurationSection moneySec = cfg.getConfigurationSection("money");
            if (moneySec != null) {
                double chance = safePrimitive(moneySec, "chance", 100.0); // percent
                double minAmt = safePrimitive(moneySec, "min", 0.0);
                double maxAmt = safePrimitive(moneySec, "max", minAmt);
                int minDrops = 1; int maxDrops = 1;
                if (moneySec.isSet("drops")) {
                    if (moneySec.isInt("drops")) { minDrops = maxDrops = moneySec.getInt("drops"); }
                    else {
                        String ds = moneySec.getString("drops");
                        if (ds != null) {
                            String[] parts = ds.split("-", 2);
                            try { if (parts.length == 2) { minDrops = Integer.parseInt(parts[0].trim()); maxDrops = Integer.parseInt(parts[1].trim()); } }
                            catch (NumberFormatException ignored) {}
                        }
                    }
                }
                if (moneySec.isInt("minDrops")) minDrops = moneySec.getInt("minDrops");
                if (moneySec.isInt("maxDrops")) maxDrops = Math.max(minDrops, moneySec.getInt("maxDrops"));
                if (chance > 0 && maxAmt >= 0) money = new MoneySpec(chance, minAmt, maxAmt, Math.max(1, minDrops), Math.max(minDrops, maxDrops));
            }

            MobDefinition def = new MobDefinition(id, type, health, attack, speed, followRange, armor, armorTough, kbResist,
                    displayName, faction, passiveAI, minLevel, maxLevel, healthPerLevel, attackPerLevel,
                    onSpawn, onHit, onDamaged, skills, damageMods, drops, money,
                    natEnabled, natChance, natBiomes, natReplace, natWeight,
                    natCapChunk);
            definitions.put(id.toLowerCase(Locale.ROOT), def);
        } catch (Throwable t) {
            plugin.getLogger().severe("Mob definition failed for id='" + id + "': " + t.getClass().getSimpleName() + ": " + t.getMessage());
        }
    }

    private double safePrimitive(ConfigurationSection sec, String path, double def) {
        try { return sec.getDouble(path); } catch (Exception ignored) { return def; }
    }
    private Double safeWrapper(ConfigurationSection sec, String path) {
        if (!sec.isSet(path)) return null;
        try { return sec.getDouble(path); } catch (Exception ignored) { return null; }
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
        try { type = EntityType.valueOf(def.type().toUpperCase()); } catch (IllegalArgumentException ex) { plugin.getLogger().warning("Unknown entity type for mob '" + id + "': " + def.type()); return null; }
        if (loc.getWorld() == null) return null;
        LivingEntity ent = (LivingEntity) loc.getWorld().spawnEntity(loc, type);
        if (def.displayName() != null) { Component name = LegacyComponentSerializer.legacyAmpersand().deserialize(def.displayName()); ent.customName(name); ent.setCustomNameVisible(true); }
        ent.getPersistentDataContainer().set(mobIdKey, PersistentDataType.STRING, def.id());
        // Determine level using ThreadLocalRandom
        int levelRange = Math.max(0, def.maxLevel() - def.minLevel());
        int level = def.minLevel() + (levelRange == 0 ? 0 : ThreadLocalRandom.current().nextInt(levelRange + 1));
        NamespacedKey levelKey = new NamespacedKey(plugin, "mob-level");
        ent.getPersistentDataContainer().set(levelKey, PersistentDataType.INTEGER, level);

        double scaledHealth = def.health() + (Math.max(0, level - 1) * def.healthPerLevel());
        double scaledAttack = def.attack() + (Math.max(0, level - 1) * def.attackPerLevel());

        AttributeInstance maxHealth = ent.getAttribute(Attribute.MAX_HEALTH);
        if (maxHealth != null) { maxHealth.setBaseValue(scaledHealth); ent.setHealth(Math.min(scaledHealth, maxHealth.getBaseValue())); }
        if (scaledAttack > 0) { AttributeInstance attackAttr = ent.getAttribute(Attribute.ATTACK_DAMAGE); if (attackAttr != null) attackAttr.setBaseValue(scaledAttack); }
        if (def.speed() != null) { AttributeInstance attr = ent.getAttribute(Attribute.MOVEMENT_SPEED); if (attr != null) attr.setBaseValue(def.speed()); }
        if (def.followRange() != null) { AttributeInstance attr = ent.getAttribute(Attribute.FOLLOW_RANGE); if (attr != null) attr.setBaseValue(def.followRange()); }
        if (def.armor() != null) { AttributeInstance attr = ent.getAttribute(Attribute.ARMOR); if (attr != null) attr.setBaseValue(def.armor()); }
        if (def.armorToughness() != null) { AttributeInstance attr = ent.getAttribute(Attribute.ARMOR_TOUGHNESS); if (attr != null) attr.setBaseValue(def.armorToughness()); }
        if (def.knockbackResist() != null) { AttributeInstance attr = ent.getAttribute(Attribute.KNOCKBACK_RESISTANCE); if (attr != null) attr.setBaseValue(def.knockbackResist()); }
        // Execute onSpawn abilities
        def.onSpawn().forEach(a -> a.execute(new AbilityContext(ent, null, null)));
        def.skills().getOrDefault(SkillTrigger.ON_SPAWN, java.util.Collections.emptyList()).forEach(n -> n.execute(new SkillContext(ent, null, null, def)));
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
    public java.util.Collection<MobDefinition> getAll() { return java.util.Collections.unmodifiableCollection(definitions.values()); }

    public MobDefinition getDefinitionFromEntity(LivingEntity ent) {
        if (ent == null) return null;
        String id = ent.getPersistentDataContainer().get(mobIdKey, PersistentDataType.STRING);
        return id == null ? null : get(id);
    }

    public JavaPlugin getPlugin() { return plugin; }
    public NamespacedKey getNaturalKey() { return naturalKey; }

    public LivingEntity spawnNatural(String id, org.bukkit.Location loc) {
        LivingEntity ent = spawn(id, loc);
        if (ent != null) {
            ent.getPersistentDataContainer().set(naturalKey, PersistentDataType.BYTE, (byte)1);
        }
        return ent;
    }
    public boolean isNatural(LivingEntity ent) {
        return ent.getPersistentDataContainer().has(naturalKey, PersistentDataType.BYTE);
    }
}
