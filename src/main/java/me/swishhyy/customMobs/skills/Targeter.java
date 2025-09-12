package me.swishhyy.customMobs.skills;

import java.util.*;
import java.util.function.Function;
import org.bukkit.Location;
import org.bukkit.entity.LivingEntity;
import org.bukkit.configuration.ConfigurationSection;

public interface Targeter {
    java.util.Collection<LivingEntity> resolve(SkillContext ctx);

    // Registry (lightweight)
    TargeterRegistry REGISTRY = new TargeterRegistry();

    class TargeterRegistry {
        private final Map<String, Function<ConfigurationSection, Targeter>> factories = new HashMap<>();
        public TargeterRegistry() { registerDefaults(); }
        private void registerDefaults() {
            register("SELF", sec -> ctx -> ctx.caster() == null ? Collections.emptyList() : Collections.singletonList(ctx.caster()));
            register("TARGET", sec -> ctx -> {
                LivingEntity t = ctx.target();
                if (t != null) return Collections.singletonList(t);
                return ctx.caster() == null ? Collections.emptyList() : Collections.singletonList(ctx.caster());
            });
            register("NEARBY", sec -> ctx -> {
                double radius = sec.getDouble("radius", 5.0);
                boolean playersOnly = sec.getBoolean("playersOnly", false);
                LivingEntity center = ctx.caster();
                if (center == null || center.getWorld() == null) return Collections.emptyList();
                Location loc = center.getLocation();
                double r2 = radius * radius;
                List<LivingEntity> out = new ArrayList<>();
                for (LivingEntity le : loc.getWorld().getLivingEntities()) {
                    if (le == center) continue;
                    if (playersOnly && !(le instanceof org.bukkit.entity.Player)) continue;
                    if (le.getLocation().distanceSquared(loc) <= r2) out.add(le);
                }
                return out;
            });
        }
        public void register(String type, Function<ConfigurationSection, Targeter> factory) { factories.put(type.toUpperCase(), factory); }
        public Targeter create(ConfigurationSection sec) {
            if (sec == null) return null; String type = sec.getString("type", sec.getName());
            Function<ConfigurationSection, Targeter> f = factories.get(type.toUpperCase());
            return f == null ? null : f.apply(sec);
        }
    }
}
