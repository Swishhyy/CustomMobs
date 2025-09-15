package me.swishhyy.customMobs.mob;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
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
import org.bukkit.inventory.EntityEquipment;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.configuration.InvalidConfigurationException;
import java.io.IOException;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;

import me.swishhyy.customMobs.abilities.Ability;
import me.swishhyy.customMobs.abilities.AbilityRegistry;
import me.swishhyy.customMobs.abilities.AbilityContext;
import me.swishhyy.customMobs.skills.*;
import me.swishhyy.customMobs.faction.FactionManager;

public class MobManager {
    private final JavaPlugin plugin;
    private final Map<String, MobDefinition> definitions = new HashMap<>();
    private final NamespacedKey mobIdKey;
    private final NamespacedKey naturalKey;
    private final FactionManager factionManager;

    public MobManager(JavaPlugin plugin, FactionManager factionManager) {
        this.plugin = plugin;
        this.factionManager = factionManager;
        this.mobIdKey = new NamespacedKey(plugin, "mob-id");
        this.naturalKey = new NamespacedKey(plugin, "natural");
        ensureDefaultConfigs();
    }

    private void ensureDefaultConfigs() {
        // Copy example resources to data folder on first run
        copyResource("custom/zombie2.yaml");
        copyResource("vanilla/zombie.yaml");
        copyResource("enchants/example_sword.yml");
        copyResource("enchants/zombie_sword_enchants.yml");
        copyResource("enchants/zombie_armor_enchants.yml");
    }

    private void copyResource(String path) {
        File out = new File(plugin.getDataFolder(), path);
        if (out.exists()) return;
        if (plugin.getResource(path) == null) { // resource not bundled
            plugin.getLogger().warning("Resource not found in jar (skipping): " + path);
            return;
        }
        if (!out.getParentFile().exists()) out.getParentFile().mkdirs();
        try {
            plugin.saveResource(path, false);
        } catch (IllegalArgumentException ex) {
            plugin.getLogger().warning("Failed to save resource '" + path + "': " + ex.getMessage());
        }
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
            // attributes override block
            ConfigurationSection attrSec = cfg.getConfigurationSection("attributes");
            AttributeRanges ranges = null;
            if (attrSec != null) {
                // parse scalar overrides first (already present code below remains)
                // collect ranges where a value is in form "min-max"
                double[] hRange = parseNumericRange(attrSec, "max_health_range");
                double[] aRange = parseNumericRange(attrSec, "attack_damage_range");
                double[] sRange = parseNumericRange(attrSec, "movement_speed_range");
                double[] fRange = parseNumericRange(attrSec, "follow_range_range");
                double[] arRange = parseNumericRange(attrSec, "armor_range");
                double[] atRange = parseNumericRange(attrSec, "armor_toughness_range");
                double[] kbRange = parseNumericRange(attrSec, "knockback_resistance_range");
                if (hRange!=null||aRange!=null||sRange!=null||fRange!=null||arRange!=null||atRange!=null||kbRange!=null) {
                    ranges = new AttributeRanges(hRange,aRange,sRange,fRange,arRange,atRange,kbRange);
                }
            }
            // attributes override values
            if (attrSec != null) {
                if (attrSec.isSet("max_health")) health = safePrimitive(attrSec, "max_health", health);
                if (attrSec.isSet("attack_damage")) attack = safePrimitive(attrSec, "attack_damage", attack);
                if (attrSec.isSet("movement_speed")) speed = safePrimitive(attrSec, "movement_speed", speed == null ? 0.0 : speed);
                if (attrSec.isSet("follow_range")) followRange = safePrimitive(attrSec, "follow_range", followRange == null ? 0.0 : followRange);
                if (attrSec.isSet("armor")) armor = safePrimitive(attrSec, "armor", armor == null ? 0.0 : armor);
                if (attrSec.isSet("armor_toughness")) armorTough = safePrimitive(attrSec, "armor_toughness", armorTough == null ? 0.0 : armorTough);
                if (attrSec.isSet("knockback_resistance")) kbResist = safePrimitive(attrSec, "knockback_resistance", kbResist == null ? 0.0 : kbResist);
            }
            String displayName = cfg.getString("display", null);
            boolean passiveAI = cfg.getBoolean("passive_ai", false);
            // Accept faction/behavior either at root or nested in attributes section for user flexibility
            String factionRoot = cfg.getString("faction", null);
            String behaviorRoot = cfg.getString("behavior", null);
            // (attrSec may be null at this point; if so these remain null)
            if (factionRoot == null && attrSec != null) factionRoot = attrSec.getString("faction", null);
            if (behaviorRoot == null && attrSec != null) behaviorRoot = attrSec.getString("behavior", null);
            int minLevel = (int) safePrimitive(cfg, "min_level", 1.0);
            int maxLevel = (int) safePrimitive(cfg, "max_level", minLevel);
            double healthPerLevel = safePrimitive(cfg, "health_per_level", 0.0);
            double attackPerLevel = safePrimitive(cfg, "attack_per_level", 0.0);
            boolean natEnabled = false; double natChance = 0.0; boolean natReplace = false; double natWeight = 1.0; java.util.Set<String> natBiomes = new HashSet<>(); Integer natCapChunk = null;
            ConfigurationSection natSec = cfg.getConfigurationSection("natural");
            if (natSec != null) {
                natEnabled = natSec.getBoolean("enabled", false);
                natChance = natSec.contains("chance") ? safePrimitive(natSec, "chance", 0.0) : 0.0;
                natReplace = natSec.getBoolean("replace", false);
                natWeight = natSec.contains("weight") ? safePrimitive(natSec, "weight", 1.0) : 1.0;
                if (natSec.isInt("cap_chunk")) natCapChunk = natSec.getInt("cap_chunk");
                for (String b : natSec.getStringList("biomes")) natBiomes.add(b.toUpperCase(java.util.Locale.ROOT));
            }
            List<Ability> onSpawn = new ArrayList<>(); List<Ability> onHit = new ArrayList<>(); List<Ability> onDamaged = new ArrayList<>(); Map<SkillTrigger, List<SkillNode>> skills = new HashMap<>(); Map<DamageCause, Double> damageMods = new HashMap<>(); List<DropSpec> drops = new ArrayList<>();
            ConfigurationSection abilities = cfg.getConfigurationSection("abilities"); if (abilities != null) { loadAbilityList(abilities, "onSpawn", onSpawn); loadAbilityList(abilities, "onHit", onHit); loadAbilityList(abilities, "onDamaged", onDamaged); }
            ConfigurationSection skillsSec = cfg.getConfigurationSection("skills"); if (skillsSec != null) { for (String trigKey : skillsSec.getKeys(false)) { SkillTrigger trigger = null; try { trigger = SkillTrigger.valueOf(trigKey.toUpperCase()); } catch (IllegalArgumentException ignored) {} if (trigger == null) continue; ConfigurationSection listSec = skillsSec.getConfigurationSection(trigKey); if (listSec == null) continue; List<SkillNode> nodes = new ArrayList<>(); for (String nodeKey : listSec.getKeys(false)) { ConfigurationSection nodeSec = listSec.getConfigurationSection(nodeKey); if (nodeSec == null) continue; SkillAction action = SkillActionRegistry.INSTANCE.create(nodeSec); if (action == null) continue; Targeter targeter = null; ConfigurationSection targeterSec = nodeSec.getConfigurationSection("targeter"); if (targeterSec != null) targeter = Targeter.REGISTRY.create(targeterSec); List<SkillCondition> conds = new ArrayList<>(); ConfigurationSection condSec = nodeSec.getConfigurationSection("conditions"); if (condSec != null) for (String cKey : condSec.getKeys(false)) { ConfigurationSection one = condSec.getConfigurationSection(cKey); if (one == null) continue; SkillCondition sc = SkillConditionRegistry.INSTANCE.create(one); if (sc != null) conds.add(sc); } long cooldownMs = 0L; if (nodeSec.contains("cooldownMs")) cooldownMs = nodeSec.getLong("cooldownMs"); else if (nodeSec.contains("cooldown")) cooldownMs = nodeSec.getLong("cooldown"); else if (nodeSec.contains("cooldownSeconds")) cooldownMs = nodeSec.getLong("cooldownSeconds") * 1000L; nodes.add(new SkillNode(nodeKey, action, targeter, conds, cooldownMs)); } if (!nodes.isEmpty()) skills.put(trigger, nodes); } }
            ConfigurationSection dmgSec = cfg.getConfigurationSection("damage_modifiers"); if (dmgSec != null) for (String causeKey : dmgSec.getKeys(false)) { try { DamageCause cause = DamageCause.valueOf(causeKey.toUpperCase()); double mult = safePrimitive(dmgSec, causeKey, 1.0); damageMods.put(cause, mult); } catch (IllegalArgumentException ignored) {} }
            ConfigurationSection dropsSec = cfg.getConfigurationSection("drops"); if (dropsSec != null) for (String dk : dropsSec.getKeys(false)) { ConfigurationSection d = dropsSec.getConfigurationSection(dk); if (d == null) continue; String matName = d.getString("type", d.getString("material", "STONE")); Material mat = null; try { mat = Material.valueOf(matName.toUpperCase()); } catch (IllegalArgumentException ignored) {} int min = d.getInt("min", 1); int max = d.getInt("max", min); double chance = safePrimitive(d, "chance", 1.0); if (mat != null && chance > 0) drops.add(new DropSpec(mat, min, max, chance)); }
            // Equipment parsing
            EquipmentSet eqSet = null; Map<EquipmentSlot, EquipmentPiece> pieces = new HashMap<>();
            ConfigurationSection eqSec = cfg.getConfigurationSection("equipment");
            if (eqSec != null) {
                ConfigurationSection setSec = eqSec.getConfigurationSection("set");
                if (setSec != null) {
                    String token = setSec.getString("token", setSec.getString("material", null));
                    if (token != null) {
                        double chance = parseChance(setSec.get("chance"), 0.0);
                        double dropC = parseChance(setSec.get("dropChance"), 0.0);
                        if (chance > 0) eqSet = new EquipmentSet(token.toUpperCase(Locale.ROOT), chance, dropC);
                    }
                }
                ConfigurationSection pcsSec = eqSec.getConfigurationSection("pieces");
                if (pcsSec != null) {
                    for (String key : pcsSec.getKeys(false)) {
                        ConfigurationSection one = pcsSec.getConfigurationSection(key);
                        if (one == null) continue;
                        String typeName = one.getString("type", one.getString("material", null));
                        if (typeName == null) continue;
                        try {
                            Material mat = Material.valueOf(typeName.toUpperCase());
                            double chance = parseChance(one.get("chance"), 0.0);
                            double dropChance = parseChance(one.get("dropChance"), 0.0);
                            String enchantFile = one.getString("enchantFile", null);
                            if (chance <= 0) continue;
                            EquipmentSlot slot = mapSlot(key);
                            if (slot != null) pieces.put(slot, new EquipmentPiece(mat, chance, dropChance, enchantFile));
                        } catch (IllegalArgumentException ignored) {}
                    }
                }
            }

            // Behavior (PASSIVE | NEUTRAL | HOSTILE)
            MobBehavior behavior;
            String faction = factionRoot;
            if (behaviorRoot != null) {
                behavior = MobBehavior.fromString(behaviorRoot, MobBehavior.HOSTILE);
            } else {
                behavior = factionManager != null ? factionManager.getDefaultBehavior(faction) : null;
                if (behavior == null) behavior = MobBehavior.HOSTILE;
            }

            MobDefinition def = new MobDefinition(id, type, health, attack, speed, followRange, armor, armorTough, kbResist,
                    displayName, faction, passiveAI, minLevel, maxLevel, healthPerLevel, attackPerLevel,
                    onSpawn, onHit, onDamaged, skills, damageMods, drops,
                    eqSet, pieces,
                    ranges,
                    behavior,
                    natEnabled, natChance, natBiomes, natReplace, natWeight,
                    natCapChunk);
            definitions.put(id.toLowerCase(Locale.ROOT), def);
        } catch (Throwable t) {
            plugin.getLogger().severe("Mob definition failed for id='" + id + "': " + t.getClass().getSimpleName() + ": " + t.getMessage());
        }
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
        int levelRange = Math.max(0, def.maxLevel() - def.minLevel());
        int level = def.minLevel() + (levelRange == 0 ? 0 : ThreadLocalRandom.current().nextInt(levelRange + 1));
        NamespacedKey levelKey = new NamespacedKey(plugin, "mob-level");
        ent.getPersistentDataContainer().set(levelKey, PersistentDataType.INTEGER, level);
        double scaledHealth = def.health() + (Math.max(0, level - 1) * def.healthPerLevel());
        double scaledAttack = def.attack() + (Math.max(0, level - 1) * def.attackPerLevel());
        Double spd = def.speed();
        Double fol = def.followRange();
        Double arm = def.armor();
        Double armT = def.armorToughness();
        Double kb = def.knockbackResist();
        AttributeRanges ranges = def.attributeRanges();
        if (ranges != null && ranges.any()) {
            scaledHealth = ranges.rollHealth(scaledHealth);
            scaledAttack = ranges.rollAttack(scaledAttack);
            spd = ranges.rollSpeed(spd);
            fol = ranges.rollFollow(fol);
            arm = ranges.rollArmor(arm);
            armT = ranges.rollArmorTough(armT);
            kb = ranges.rollKnockback(kb);
        }
        AttributeInstance maxHealth = ent.getAttribute(Attribute.MAX_HEALTH);
        if (maxHealth != null) { maxHealth.setBaseValue(scaledHealth); ent.setHealth(Math.min(scaledHealth, maxHealth.getBaseValue())); }
        if (scaledAttack > 0) { AttributeInstance attackAttr = ent.getAttribute(Attribute.ATTACK_DAMAGE); if (attackAttr != null) attackAttr.setBaseValue(scaledAttack); }
        if (spd != null) { AttributeInstance attr = ent.getAttribute(Attribute.MOVEMENT_SPEED); if (attr != null) attr.setBaseValue(spd); }
        if (fol != null) { AttributeInstance attr = ent.getAttribute(Attribute.FOLLOW_RANGE); if (attr != null) attr.setBaseValue(fol); }
        if (arm != null) { AttributeInstance attr = ent.getAttribute(Attribute.ARMOR); if (attr != null) attr.setBaseValue(arm); }
        if (armT != null) { AttributeInstance attr = ent.getAttribute(Attribute.ARMOR_TOUGHNESS); if (attr != null) attr.setBaseValue(armT); }
        if (kb != null) { AttributeInstance attr = ent.getAttribute(Attribute.KNOCKBACK_RESISTANCE); if (attr != null) attr.setBaseValue(kb); }
        applyEquipment(def, ent);
        applyPieceEnchantments(def, ent);
        def.onSpawn().forEach(a -> a.execute(new AbilityContext(ent, null, null)));
        def.skills().getOrDefault(SkillTrigger.ON_SPAWN, java.util.Collections.emptyList()).forEach(n -> n.execute(new SkillContext(ent, null, null, def)));
        return ent;
    }

    private void applyEquipment(MobDefinition def, LivingEntity ent) {
        EntityEquipment eq = ent.getEquipment(); if (eq == null) return;
        ThreadLocalRandom r = ThreadLocalRandom.current();
        // full set token (e.g., IRON_ARMOR)
        if (def.equipmentSet() != null && r.nextDouble() <= def.equipmentSet().chance()) {
            giveFullSet(eq, def.equipmentSet());
        }
        // individual pieces override
        for (var entry : def.equipmentPieces().entrySet()) {
            EquipmentSlot slot = entry.getKey(); EquipmentPiece piece = entry.getValue();
            if (r.nextDouble() > piece.chance()) continue;
            ItemStack stack = new ItemStack(piece.material());
            switch (slot) {
                case HEAD -> eq.setHelmet(stack);
                case CHEST -> eq.setChestplate(stack);
                case LEGS -> eq.setLeggings(stack);
                case FEET -> eq.setBoots(stack);
                case HAND -> eq.setItemInMainHand(stack);
                case OFF_HAND -> eq.setItemInOffHand(stack);
                default -> {}
            }
            float dc = (float) piece.dropChance();
            switch (slot) {
                case HEAD -> eq.setHelmetDropChance(dc);
                case CHEST -> eq.setChestplateDropChance(dc);
                case LEGS -> eq.setLeggingsDropChance(dc);
                case FEET -> eq.setBootsDropChance(dc);
                case HAND -> eq.setItemInMainHandDropChance(dc);
                case OFF_HAND -> eq.setItemInOffHandDropChance(dc);
                default -> {}
            }
        }
    }

    private void giveFullSet(EntityEquipment eq, EquipmentSet set) {
        String token = set.token();
        // Allow tokens: <PREFIX>_ARMOR or concrete helmet piece
        List<Material> pieces = new ArrayList<>();
        if (token.endsWith("_ARMOR")) {
            String prefix = token.substring(0, token.length() - 6); // remove _ARMOR
            String helm = armorName(prefix, "HELMET");
            String chest = armorName(prefix, "CHESTPLATE");
            String legs = armorName(prefix, "LEGGINGS");
            String boots = armorName(prefix, "BOOTS");
            tryAdd(pieces, helm); tryAdd(pieces, chest); tryAdd(pieces, legs); tryAdd(pieces, boots);
        } else if (token.endsWith("HELMET")) {
            // derive set from helmet piece name
            String prefix = token.substring(0, token.length() - 6); // remove HELMET
            tryAdd(pieces, token);
            tryAdd(pieces, armorName(prefix, "CHESTPLATE"));
            tryAdd(pieces, armorName(prefix, "LEGGINGS"));
            tryAdd(pieces, armorName(prefix, "BOOTS"));
        }
        if (pieces.isEmpty()) return;
        float dc = (float) set.dropChance();
        for (Material m : pieces) {
            if (m.name().endsWith("HELMET")) { eq.setHelmet(new ItemStack(m)); eq.setHelmetDropChance(dc); }
            else if (m.name().endsWith("CHESTPLATE")) { eq.setChestplate(new ItemStack(m)); eq.setChestplateDropChance(dc); }
            else if (m.name().endsWith("LEGGINGS")) { eq.setLeggings(new ItemStack(m)); eq.setLeggingsDropChance(dc); }
            else if (m.name().endsWith("BOOTS")) { eq.setBoots(new ItemStack(m)); eq.setBootsDropChance(dc); }
        }
    }

    private void tryAdd(List<Material> list, String matName) {
        try { list.add(Material.valueOf(matName)); } catch (IllegalArgumentException ignored) {}
    }

    private String armorName(String prefix, String part) {
        // GOLD armor items use GOLDEN_ prefix.
        if (prefix.equals("GOLD")) prefix = "GOLDEN";
        return prefix + "_" + part;
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

    private void applyPieceEnchantments(MobDefinition def, LivingEntity ent) {
        EntityEquipment eq = ent.getEquipment(); if (eq == null) return;
        for (var entry: def.equipmentPieces().entrySet()) {
            EquipmentPiece piece = entry.getValue();
            if (piece.enchantFile() == null) continue;
            ItemStack stack = switch (entry.getKey()) {
                case HEAD -> eq.getHelmet();
                case CHEST -> eq.getChestplate();
                case LEGS -> eq.getLeggings();
                case FEET -> eq.getBoots();
                case HAND -> eq.getItemInMainHand();
                case OFF_HAND -> eq.getItemInOffHand();
                default -> null;
            };
            if (stack == null) continue;
            applyEnchantFile(stack, piece.enchantFile());
        }
    }

    private void applyEnchantFile(ItemStack item, String fileName) {
        File file = new File(plugin.getDataFolder(), "enchants/" + fileName);
        if (!file.exists()) {
            try { plugin.saveResource("enchants/" + fileName, false); } catch (IllegalArgumentException ignored) {}
        }
        if (!file.exists()) return;
        FileConfiguration yml = new YamlConfiguration();
        try { yml.load(file); } catch (IOException | InvalidConfigurationException e) { return; }

        // Optional config section for limits and groups
        ConfigurationSection configSec = yml.getConfigurationSection("config");
        int maxEnchants = configSec != null ? configSec.getInt("max_enchants", 0) : 0; // 0 = unlimited
        ConfigurationSection groupsSec = configSec != null ? configSec.getConfigurationSection("groups") : null;

        Set<String> appliedNames = new HashSet<>(); // store normalized names (namespace:key)
        int appliedCount = 0;

        // Process weighted exclusive groups first
        if (groupsSec != null) {
            for (String groupName : groupsSec.getKeys(false)) {
                if (maxEnchants > 0 && appliedCount >= maxEnchants) break;
                ConfigurationSection group = groupsSec.getConfigurationSection(groupName);
                if (group == null) continue;
                int picks = Math.max(1, group.getInt("pick", 1));
                ConfigurationSection options = group.getConfigurationSection("options");
                if (options == null) continue;
                // Build list of applicable options
                List<EnchantOption> opts = new ArrayList<>();
                int totalWeight = 0;
                for (String optKey : options.getKeys(false)) {
                    ConfigurationSection optSec = options.getConfigurationSection(optKey);
                    if (optSec == null) continue;
                    Enchantment ench = resolveEnchantKey(optKey);
                    if (ench == null) continue;
                    if (!ench.canEnchantItem(item)) continue; // skip incompatible
                    int weight = Math.max(1, optSec.getInt("weight", 1));
                    int level; int min = optSec.getInt("min", -1); int max = optSec.getInt("max", -1);
                    if (optSec.isSet("level")) level = optSec.getInt("level", 1);
                    else if (min >= 0 && max >= 0) {
                        if (max < min) max = min; level = min == max ? min : ThreadLocalRandom.current().nextInt(min, max + 1);
                    } else level = 1;
                    double chance = parseChance(optSec.get("chance"), 1.0); // optional per option
                    opts.add(new EnchantOption(ench, level, weight, chance));
                    totalWeight += weight;
                }
                if (opts.isEmpty()) continue;
                // Perform picks (no replacement)
                for (int p = 0; p < picks && !opts.isEmpty(); p++) {
                    if (maxEnchants > 0 && appliedCount >= maxEnchants) break;
                    int roll = ThreadLocalRandom.current().nextInt(totalWeight);
                    EnchantOption chosen = null; int cumulative = 0; int index = -1;
                    for (int i = 0; i < opts.size(); i++) {
                        EnchantOption eo = opts.get(i);
                        cumulative += eo.weight;
                        if (roll < cumulative) { chosen = eo; index = i; break; }
                    }
                    if (chosen == null) break;
                    // Chance gate
                    if (Math.random() <= chosen.chance) {
                        int finalLevel = Math.max(1, Math.min(chosen.level, chosen.ench.getMaxLevel()));
                        try { item.addUnsafeEnchantment(chosen.ench, finalLevel); appliedNames.add(normalizeEnchant(chosen.ench)); appliedCount++; } catch (Exception ignored) {}
                    }
                    // remove chosen (no replacement for exclusivity)
                    if (index >= 0) {
                        totalWeight -= chosen.weight;
                        opts.remove(index);
                    }
                    if (totalWeight <= 0) break;
                }
            }
        }

        // Standard enchantments section (backward compatible)
        ConfigurationSection sec = yml.getConfigurationSection("enchantments");
        if (sec == null) return; // nothing else
        List<String> keys = new ArrayList<>(sec.getKeys(false));
        // Shuffle for randomness
        java.util.Collections.shuffle(keys);
        for (String k : keys) {
            if (maxEnchants > 0 && appliedCount >= maxEnchants) break;
            ConfigurationSection es = sec.getConfigurationSection(k); if (es == null) continue;
            Enchantment ench = resolveEnchantKey(k);
            if (ench == null) continue;
            String norm = normalizeEnchant(ench);
            if (appliedNames.contains(norm)) continue; // skip duplicates from groups
            if (!ench.canEnchantItem(item)) continue;
            double chance = parseChance(es.get("chance"), 1.0);
            if (Math.random() > chance) continue;
            int level;
            if (es.isSet("level")) level = es.getInt("level", 1);
            else {
                int min = es.getInt("min", 1); int max = es.getInt("max", min);
                if (max < min) max = min;
                level = min == max ? min : ThreadLocalRandom.current().nextInt(min, max + 1);
            }
            level = Math.max(1, Math.min(level, ench.getMaxLevel()));
            try { item.addUnsafeEnchantment(ench, level); appliedNames.add(norm); appliedCount++; } catch (Exception ignored) {}
        }
    }

    private Enchantment resolveEnchantKey(String key) {
        if (key == null) return null;
        String lower = key.toLowerCase(Locale.ROOT).trim();
        Enchantment ench = null;
        // Try namespaced form first
        if (lower.contains(":")) {
            NamespacedKey nk = NamespacedKey.fromString(lower);
            if (nk != null) ench = Enchantment.getByKey(nk);
        }
        if (ench == null) {
            NamespacedKey nk = NamespacedKey.minecraft(lower);
            ench = Enchantment.getByKey(nk);
        }
        if (ench == null) { // fallback for older style names
            ench = Enchantment.getByName(lower.toUpperCase(Locale.ROOT));
        }
        return ench;
    }

    private String normalizeEnchant(Enchantment ench) {
        if (ench == null) return "";
        NamespacedKey k = ench.getKey();
        return (k == null ? ench.getClass().getSimpleName() : k.toString()).toLowerCase(Locale.ROOT);
    }

    // --- Missing helper methods restored ---
    private double[] parseNumericRange(ConfigurationSection sec, String key) {
        if (sec == null || !sec.isSet(key)) return null;
        String raw = sec.getString(key, null);
        if (raw == null || !raw.contains("-")) return null;
        String[] parts = raw.split("-", 2);
        try {
            double min = Double.parseDouble(parts[0].trim());
            double max = Double.parseDouble(parts[1].trim());
            if (max < min) { double tmp = min; min = max; max = tmp; }
            return new double[]{min, max};
        } catch (NumberFormatException ex) { return null; }
    }

    private double parseChance(Object raw, double def) {
        if (raw == null) return def;
        double v;
        if (raw instanceof Number n) v = n.doubleValue();
        else { try { v = Double.parseDouble(raw.toString()); } catch (NumberFormatException e) { return def; } }
        if (v > 1) v /= 100.0; // treat >1 as percent
        if (v < 0) v = 0; if (v > 1) v = 1;
        return v;
    }

    private EquipmentSlot mapSlot(String key) {
        if (key == null) return null;
        key = key.toLowerCase(Locale.ROOT);
        return switch (key) {
            case "helmet", "head" -> EquipmentSlot.HEAD;
            case "chest", "chestplate" -> EquipmentSlot.CHEST;
            case "legs", "leggings" -> EquipmentSlot.LEGS;
            case "boots", "feet" -> EquipmentSlot.FEET;
            case "mainhand", "hand", "weapon" -> EquipmentSlot.HAND;
            case "offhand", "shield" -> EquipmentSlot.OFF_HAND;
            default -> null;
        };
    }

    private double safePrimitive(ConfigurationSection sec, String path, double def) {
        try { return sec.getDouble(path); } catch (Exception ignored) { return def; }
    }
    private Double safeWrapper(ConfigurationSection sec, String path) {
        if (sec == null || !sec.isSet(path)) return null;
        try { return sec.getDouble(path); } catch (Exception ignored) { return null; }
    }
    // --- end helper methods ---

    private static final class EnchantOption { final Enchantment ench; final int level; final int weight; final double chance; EnchantOption(Enchantment ench,int level,int weight,double chance){ this.ench=ench; this.level=level; this.weight=weight; this.chance=chance; } }
}
