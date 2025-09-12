package me.swishhyy.customMobs.mob;

import java.util.Collections;
import java.util.List;
import me.swishhyy.customMobs.abilities.Ability;

public final class MobDefinition {
    private final String id;
    private final String type;
    private final double health;
    private final double attack;
    private final String displayName; // optional
    private final List<Ability> onSpawn;
    private final List<Ability> onHit;
    private final List<Ability> onDamaged;

    public MobDefinition(String id, String type, double health, double attack, String displayName,
                         List<Ability> onSpawn, List<Ability> onHit, List<Ability> onDamaged) {
        this.id = id;
        this.type = type;
        this.health = health;
        this.attack = attack;
        this.displayName = displayName;
        this.onSpawn = Collections.unmodifiableList(onSpawn);
        this.onHit = Collections.unmodifiableList(onHit);
        this.onDamaged = Collections.unmodifiableList(onDamaged);
    }

    public String id() { return id; }
    public String type() { return type; }
    public double health() { return health; }
    public double attack() { return attack; }
    public String displayName() { return displayName; }
    public List<Ability> onSpawn() { return onSpawn; }
    public List<Ability> onHit() { return onHit; }
    public List<Ability> onDamaged() { return onDamaged; }
}
