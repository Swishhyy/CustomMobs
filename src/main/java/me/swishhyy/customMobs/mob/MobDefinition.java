package me.swishhyy.customMobs.mob;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import me.swishhyy.customMobs.abilities.Ability;
import me.swishhyy.customMobs.skills.SkillTrigger;
import me.swishhyy.customMobs.skills.SkillNode;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;

public final class MobDefinition {
    private final String id;
    private final String type;
    private final double health;
    private final double attack;
    private final Double speed; // nullable
    private final Double followRange;
    private final Double armor;
    private final Double armorToughness;
    private final Double knockbackResist;
    private final String displayName; // optional
    private final String faction; // optional
    private final boolean passiveAI;
    private final int minLevel;
    private final int maxLevel;
    private final double healthPerLevel;
    private final double attackPerLevel;

    private final List<Ability> onSpawn;
    private final List<Ability> onHit;
    private final List<Ability> onDamaged;
    private final Map<SkillTrigger, List<SkillNode>> skills; // new skill system
    private final Map<DamageCause, Double> damageMultipliers; // cause -> multiplier
    private final List<DropSpec> drops; // custom drops
    private final MoneySpec money; // optional MoneyFromMobs spec

    private final boolean naturalEnabled;
    private final double naturalChance;
    private final java.util.Set<String> naturalBiomes; // upper-case biome names
    private final boolean naturalReplace;
    private final double naturalWeight;
    private final Integer naturalCapChunk; // removed naturalCapBiome

    public MobDefinition(String id, String type, double health, double attack,
                         Double speed, Double followRange, Double armor, Double armorToughness, Double knockbackResist,
                         String displayName, String faction, boolean passiveAI,
                         int minLevel, int maxLevel, double healthPerLevel, double attackPerLevel,
                         List<Ability> onSpawn, List<Ability> onHit, List<Ability> onDamaged,
                         Map<SkillTrigger, List<SkillNode>> skills,
                         Map<DamageCause, Double> damageMultipliers,
                         List<DropSpec> drops,
                         MoneySpec money,
                         boolean naturalEnabled, double naturalChance, java.util.Set<String> naturalBiomes,
                         boolean naturalReplace, double naturalWeight,
                         Integer naturalCapChunk) {
        this.id = id;
        this.type = type;
        this.health = health;
        this.attack = attack;
        this.speed = speed;
        this.followRange = followRange;
        this.armor = armor;
        this.armorToughness = armorToughness;
        this.knockbackResist = knockbackResist;
        this.displayName = displayName;
        this.faction = faction;
        this.passiveAI = passiveAI;
        this.minLevel = minLevel;
        this.maxLevel = Math.max(minLevel, maxLevel);
        this.healthPerLevel = healthPerLevel;
        this.attackPerLevel = attackPerLevel;
        this.onSpawn = Collections.unmodifiableList(onSpawn);
        this.onHit = Collections.unmodifiableList(onHit);
        this.onDamaged = Collections.unmodifiableList(onDamaged);
        this.skills = skills == null ? Collections.emptyMap() : Collections.unmodifiableMap(skills);
        this.damageMultipliers = damageMultipliers == null ? Collections.emptyMap() : Collections.unmodifiableMap(damageMultipliers);
        this.drops = drops == null ? Collections.emptyList() : Collections.unmodifiableList(drops);
        this.money = money;
        this.naturalEnabled = naturalEnabled;
        this.naturalChance = naturalChance;
        this.naturalBiomes = naturalBiomes == null ? java.util.Collections.emptySet() : java.util.Collections.unmodifiableSet(naturalBiomes);
        this.naturalReplace = naturalReplace;
        this.naturalWeight = naturalWeight;
        this.naturalCapChunk = naturalCapChunk;
    }

    public String id() { return id; }
    public String type() { return type; }
    public double health() { return health; }
    public double attack() { return attack; }
    public Double speed() { return speed; }
    public Double followRange() { return followRange; }
    public Double armor() { return armor; }
    public Double armorToughness() { return armorToughness; }
    public Double knockbackResist() { return knockbackResist; }
    public String displayName() { return displayName; }
    public String faction() { return faction; }
    public boolean passiveAI() { return passiveAI; }
    public int minLevel() { return minLevel; }
    public int maxLevel() { return maxLevel; }
    public double healthPerLevel() { return healthPerLevel; }
    public double attackPerLevel() { return attackPerLevel; }

    public List<Ability> onSpawn() { return onSpawn; }
    public List<Ability> onHit() { return onHit; }
    public List<Ability> onDamaged() { return onDamaged; }
    public Map<SkillTrigger, List<SkillNode>> skills() { return skills; }
    public Map<DamageCause, Double> damageMultipliers() { return damageMultipliers; }
    public List<DropSpec> drops() { return drops; }
    public MoneySpec money() { return money; }
    public boolean naturalEnabled() { return naturalEnabled; }
    public double naturalChance() { return naturalChance; }
    public java.util.Set<String> naturalBiomes() { return naturalBiomes; }
    public boolean naturalReplace() { return naturalReplace; }
    public double naturalWeight() { return naturalWeight; }
    public Integer naturalCapChunk() { return naturalCapChunk; }
}
