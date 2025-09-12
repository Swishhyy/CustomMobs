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
    private final String displayName; // optional
    private final List<Ability> onSpawn;
    private final List<Ability> onHit;
    private final List<Ability> onDamaged;
    private final Map<SkillTrigger, List<SkillNode>> skills; // new skill system
    private final Map<DamageCause, Double> damageMultipliers; // cause -> multiplier
    private final List<DropSpec> drops; // custom drops

    public MobDefinition(String id, String type, double health, double attack, String displayName,
                         List<Ability> onSpawn, List<Ability> onHit, List<Ability> onDamaged,
                         Map<SkillTrigger, List<SkillNode>> skills,
                         Map<DamageCause, Double> damageMultipliers,
                         List<DropSpec> drops) {
        this.id = id;
        this.type = type;
        this.health = health;
        this.attack = attack;
        this.displayName = displayName;
        this.onSpawn = Collections.unmodifiableList(onSpawn);
        this.onHit = Collections.unmodifiableList(onHit);
        this.onDamaged = Collections.unmodifiableList(onDamaged);
        this.skills = skills == null ? Collections.emptyMap() : Collections.unmodifiableMap(skills);
        this.damageMultipliers = damageMultipliers == null ? java.util.Collections.emptyMap() : java.util.Collections.unmodifiableMap(damageMultipliers);
        this.drops = drops == null ? java.util.Collections.emptyList() : java.util.Collections.unmodifiableList(drops);
    }

    public String id() { return id; }
    public String type() { return type; }
    public double health() { return health; }
    public double attack() { return attack; }
    public String displayName() { return displayName; }
    public List<Ability> onSpawn() { return onSpawn; }
    public List<Ability> onHit() { return onHit; }
    public List<Ability> onDamaged() { return onDamaged; }
    public Map<SkillTrigger, List<SkillNode>> skills() { return skills; }
    public Map<DamageCause, Double> damageMultipliers() { return damageMultipliers; }
    public List<DropSpec> drops() { return drops; }
}
