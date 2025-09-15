package me.swishhyy.customMobs.mob;

import java.util.concurrent.ThreadLocalRandom;

/** Holds optional min/max ranges for randomized mob base attributes. */
public final class AttributeRanges {
    private final double[] health; // null or {min,max}
    private final double[] attack;
    private final double[] speed;
    private final double[] follow;
    private final double[] armor;
    private final double[] armorTough;
    private final double[] knockback;

    public AttributeRanges(double[] health, double[] attack, double[] speed, double[] follow,
                           double[] armor, double[] armorTough, double[] knockback) {
        this.health = health; this.attack = attack; this.speed = speed; this.follow = follow;
        this.armor = armor; this.armorTough = armorTough; this.knockback = knockback;
    }
    private double roll(double base, double[] range) {
        if (range == null) return base;
        if (range[0] == range[1]) return range[0];
        return ThreadLocalRandom.current().nextDouble(range[0], range[1]);
    }
    public double rollHealth(double base) { return roll(base, health); }
    public double rollAttack(double base) { return roll(base, attack); }
    public Double rollSpeed(Double base) { return speed == null ? base : roll(base == null ? speed[0] : base, speed); }
    public Double rollFollow(Double base) { return follow == null ? base : roll(base == null ? follow[0] : base, follow); }
    public Double rollArmor(Double base) { return armor == null ? base : roll(base == null ? armor[0] : base, armor); }
    public Double rollArmorTough(Double base) { return armorTough == null ? base : roll(base == null ? armorTough[0] : base, armorTough); }
    public Double rollKnockback(Double base) { return knockback == null ? base : roll(base == null ? knockback[0] : base, knockback); }

    public boolean any() { return health!=null||attack!=null||speed!=null||follow!=null||armor!=null||armorTough!=null||knockback!=null; }
}

