package me.swishhyy.customMobs.skills;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.LivingEntity;

public final class SkillConditionRegistry {
    public static final SkillConditionRegistry INSTANCE = new SkillConditionRegistry();
    private final Map<String, Function<ConfigurationSection, SkillCondition>> factories = new HashMap<>();

    private SkillConditionRegistry() { registerDefaults(); }

    private void registerDefaults() {
        register("ALWAYS", sec -> ctx -> true);
        register("CHANCE", sec -> {
            double chance = Math.max(0, Math.min(1, sec.getDouble("value", sec.getDouble("chance", 0.5))));
            return ctx -> Math.random() <= chance;
        });
        register("CASTER_HEALTH_BELOW", sec -> {
            double pct = sec.getDouble("value", 0.5);
            return ctx -> ctx.caster() != null && (ctx.caster().getHealth() / ctx.caster().getMaxHealth()) < pct;
        });
        register("TARGET_HEALTH_BELOW", sec -> {
            double pct = sec.getDouble("value", 0.5);
            return ctx -> ctx.target() != null && (ctx.target().getHealth() / ctx.target().getMaxHealth()) < pct;
        });
        register("TARGET_IS_PLAYER", sec -> ctx -> ctx.target() instanceof org.bukkit.entity.Player);
        register("CASTER_HAS_TARGET", sec -> ctx -> ctx.target() != null);
    }

    public void register(String type, Function<ConfigurationSection, SkillCondition> factory) {
        factories.put(type.toUpperCase(), factory);
    }

    public SkillCondition create(ConfigurationSection sec) {
        if (sec == null) return null;
        String type = sec.getString("type", sec.getName());
        Function<ConfigurationSection, SkillCondition> f = factories.get(type.toUpperCase());
        return f == null ? null : f.apply(sec);
    }
}

