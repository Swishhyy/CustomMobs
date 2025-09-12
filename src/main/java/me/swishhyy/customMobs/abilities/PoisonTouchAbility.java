package me.swishhyy.customMobs.abilities;

import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

public class PoisonTouchAbility implements Ability {
    private final int durationTicks;
    private final int amplifier;

    public PoisonTouchAbility(int durationTicks, int amplifier) {
        this.durationTicks = durationTicks;
        this.amplifier = amplifier;
    }

    @Override
    public String getType() { return "POISON_TOUCH"; }

    @Override
    public void execute(AbilityContext ctx) {
        if (ctx.getTarget() != null) {
            ctx.getTarget().addPotionEffect(new PotionEffect(PotionEffectType.POISON, durationTicks, amplifier));
        }
    }
}
