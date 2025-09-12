package me.swishhyy.customMobs.mob;

import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import java.util.concurrent.ThreadLocalRandom;

public final class DropSpec {
    private final Material material;
    private final int min;
    private final int max;
    private final double chance; // 0..1

    public DropSpec(Material material, int min, int max, double chance) {
        this.material = material;
        this.min = Math.max(0, min);
        this.max = Math.max(this.min, max);
        this.chance = Math.max(0, Math.min(1, chance));
    }

    public ItemStack createStack() {
        if (material == null) return null;
        int amt = min == max ? min : ThreadLocalRandom.current().nextInt(min, max + 1);
        if (amt <= 0) return null;
        return new ItemStack(material, Math.min(amt, material.getMaxStackSize()));
    }

    public boolean roll() { return Math.random() <= chance; }
}

