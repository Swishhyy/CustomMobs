package me.swishhyy.customMobs.skills;

import java.util.HashMap;
import java.util.Map;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.LivingEntity;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.Particle;
import org.bukkit.Location;

public final class SkillActionRegistry {
    public static final SkillActionRegistry INSTANCE = new SkillActionRegistry();
    private final Map<String, Function<ConfigurationSection, SkillAction>> factories = new HashMap<>();

    private SkillActionRegistry() { registerDefaults(); }

    private void registerDefaults() {
        register("DAMAGE", sec -> ctx -> {
            double amount = sec.getDouble("amount", 1.0);
            LivingEntity target = ctx.target() != null ? ctx.target() : ctx.caster();
            if (target != null && !target.isDead()) target.damage(amount, ctx.caster());
        });
        register("POTION", sec -> ctx -> {
            String effectName = sec.getString("effect", "POISON").toUpperCase();
            int duration = sec.getInt("duration", 100);
            int amplifier = sec.getInt("amplifier", 0);
            LivingEntity target = ctx.target() != null ? ctx.target() : ctx.caster();
            PotionEffectType type = PotionEffectType.getByName(effectName);
            if (type != null && target != null) target.addPotionEffect(new PotionEffect(type, duration, amplifier));
        });
        register("MESSAGE", sec -> ctx -> {
            String msg = sec.getString("text", "Skill Triggered");
            if (ctx.caster() != null && msg != null) ctx.caster().sendMessage(msg.replace('&', '§'));
        });
        register("PARTICLE", sec -> ctx -> {
            String particleName = sec.getString("particle", "FLAME").toUpperCase();
            int count = sec.getInt("count", 8);
            double off = sec.getDouble("offset", 0.25);
            double speed = sec.getDouble("speed", 0.0);
            Location loc = ctx.target() != null ? ctx.target().getLocation() : (ctx.caster() != null ? ctx.caster().getLocation() : null);
            if (loc == null || loc.getWorld() == null) return;
            try {
                Particle p = Particle.valueOf(particleName);
                loc.getWorld().spawnParticle(p, loc, count, off, off, off, speed);
            } catch (IllegalArgumentException ignored) {}
        });
        register("CHAIN", sec -> {
            ConfigurationSection stepsSec = sec.getConfigurationSection("steps");
            List<SkillAction> steps = new ArrayList<>();
            if (stepsSec != null) {
                for (String k : stepsSec.getKeys(false)) {
                    ConfigurationSection child = stepsSec.getConfigurationSection(k);
                    if (child == null) continue;
                    SkillAction a = create(child);
                    if (a != null) steps.add(a);
                }
            }
            return ctx -> { for (SkillAction a : steps) a.execute(ctx); };
        });
    }

    public void register(String type, Function<ConfigurationSection, SkillAction> factory) {
        factories.put(type.toUpperCase(), factory);
    }

    public SkillAction create(ConfigurationSection section) {
        if (section == null) return null;
        String type = section.getString("type");
        if (type == null) return null;
        Function<ConfigurationSection, SkillAction> f = factories.get(type.toUpperCase());
        return f == null ? null : f.apply(section);
    }
}
