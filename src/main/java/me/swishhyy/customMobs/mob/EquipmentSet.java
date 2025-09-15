package me.swishhyy.customMobs.mob;

/** Represents a full armor set specification; token may be e.g. IRON_ARMOR, GOLD_ARMOR, DIAMOND_ARMOR, LEATHER_ARMOR, CHAINMAIL_ARMOR, NETHERITE_ARMOR or a concrete armor piece like DIAMOND_HELMET. */
public final class EquipmentSet {
    private final String token; // upper-case input token
    private final double chance; // 0..1 equip chance
    private final double dropChance; // 0..1 per piece
    public EquipmentSet(String token, double chance, double dropChance) {
        this.token = token.toUpperCase();
        this.chance = clamp01(chance);
        this.dropChance = clamp01(dropChance);
    }
    private double clamp01(double v){ return v<0?0:(v>1?1:v); }
    public String token(){ return token; }
    public double chance(){ return chance; }
    public double dropChance(){ return dropChance; }
}

