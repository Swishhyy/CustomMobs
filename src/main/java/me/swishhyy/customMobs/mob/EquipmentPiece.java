package me.swishhyy.customMobs.mob;

import org.bukkit.Material;

public final class EquipmentPiece {
    private final Material material;
    private final double chance;      // 0..1
    private final double dropChance;  // 0..1
    private final String enchantFile; // relative path in enchants/ or absolute inside plugin data folder

    public EquipmentPiece(Material material, double chance, double dropChance) {
        this(material, chance, dropChance, null);
    }
    public EquipmentPiece(Material material, double chance, double dropChance, String enchantFile) {
        this.material = material;
        this.chance = clamp01(chance);
        this.dropChance = clamp01(dropChance);
        this.enchantFile = enchantFile;
    }
    private double clamp01(double v) { return v < 0 ? 0 : (v > 1 ? 1 : v); }
    public Material material() { return material; }
    public double chance() { return chance; }
    public double dropChance() { return dropChance; }
    public String enchantFile() { return enchantFile; }
}
