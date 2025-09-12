package me.swishhyy.customMobs.skills;

import org.bukkit.entity.LivingEntity;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/** Runtime tracking for per-entity skill cooldowns. */
public final class SkillRuntime {
    private static final Map<UUID, Map<String, Long>> LAST_EXEC = new ConcurrentHashMap<>();

    private SkillRuntime() {}

    /** Attempt to acquire cooldown slot. Returns true if execution permitted. */
    public static boolean tryAcquire(LivingEntity entity, String skillName, long cooldownMillis) {
        if (entity == null || skillName == null || cooldownMillis <= 0) return true; // treat as no cooldown
        long now = System.currentTimeMillis();
        Map<String, Long> map = LAST_EXEC.computeIfAbsent(entity.getUniqueId(), k -> new ConcurrentHashMap<>());
        Long last = map.get(skillName);
        if (last != null && now - last < cooldownMillis) return false;
        map.put(skillName, now);
        return true;
    }

    /** Clear all cooldowns for an entity (e.g. on death or reload). */
    public static void clear(LivingEntity entity) {
        if (entity != null) LAST_EXEC.remove(entity.getUniqueId());
    }
}

