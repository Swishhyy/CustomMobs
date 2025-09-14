package me.swishhyy.customMobs.mob;

import java.util.concurrent.ThreadLocalRandom;

/** Optional money drop specification for MoneyFromMobs integration. */
public final class MoneySpec {
    private final double chancePercent; // 0-100
    private final double minAmount;
    private final double maxAmount;
    private final int minDrops;
    private final int maxDrops;

    public MoneySpec(double chancePercent, double minAmount, double maxAmount, int minDrops, int maxDrops) {
        this.chancePercent = Math.max(0, Math.min(100, chancePercent));
        this.minAmount = Math.max(0, minAmount);
        this.maxAmount = Math.max(this.minAmount, maxAmount);
        this.minDrops = Math.max(1, minDrops);
        this.maxDrops = Math.max(this.minDrops, maxDrops);
    }

    public double chancePercent() { return chancePercent; }
    public double randomAmount() {
        if (minAmount == maxAmount) return minAmount;
        return ThreadLocalRandom.current().nextDouble(minAmount, maxAmount + 1e-9);
    }
    public int randomDrops() {
        return minDrops == maxDrops ? minDrops : ThreadLocalRandom.current().nextInt(minDrops, maxDrops + 1);
    }
}

