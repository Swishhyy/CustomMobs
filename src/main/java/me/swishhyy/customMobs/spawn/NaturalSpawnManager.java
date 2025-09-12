package me.swishhyy.customMobs.spawn;

import me.swishhyy.customMobs.mob.MobManager;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.event.entity.CreatureSpawnEvent;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.*;
import java.util.concurrent.ThreadLocalRandom;
import java.util.logging.Logger;

public class NaturalSpawnManager {
    private final JavaPlugin plugin;
    private final MobManager mobManager;
    private final Logger log;

    private boolean enabled;
    private boolean replaceVanilla;
    private double globalChance;
    private int maxNearbyRadius;
    private int maxNearbyCount;
    private Map<String, String> biomePools = new HashMap<>(); // biome -> pool id
    private Map<String, List<WeightedMob>> pools = new HashMap<>(); // pool id -> weighted list

    public NaturalSpawnManager(JavaPlugin plugin, MobManager mobManager) {
        this.plugin = plugin;
        this.mobManager = mobManager;
        this.log = plugin.getLogger();
        reloadConfig();
    }

    public void reloadConfig() {
        biomePools.clear();
        pools.clear();
        ConfigurationSection root = plugin.getConfig().getConfigurationSection("natural-spawn");
        if (root == null) { enabled = false; return; }
        enabled = root.getBoolean("enabled", false);
        replaceVanilla = root.getBoolean("replace-vanilla", false);
        globalChance = clamp01(root.getDouble("global-chance", 0.05));
        maxNearbyRadius = root.getInt("max-nearby-radius", 32);
        maxNearbyCount = root.getInt("max-nearby-count", 40);
        ConfigurationSection biomeSec = root.getConfigurationSection("biome-pools");
        if (biomeSec != null) {
            for (String biome : biomeSec.getKeys(false)) {
                biomePools.put(biome.toUpperCase(Locale.ROOT), biomeSec.getString(biome, "default"));
            }
        }
        ConfigurationSection poolsSec = root.getConfigurationSection("pools");
        if (poolsSec != null) {
            for (String poolId : poolsSec.getKeys(false)) {
                ConfigurationSection pSec = poolsSec.getConfigurationSection(poolId);
                if (pSec == null) continue;
                List<String> mobLines = pSec.getStringList("mobs");
                List<WeightedMob> list = new ArrayList<>();
                for (String line : mobLines) {
                    String[] parts = line.split(":");
                    if (parts.length == 0) continue;
                    String mobId = parts[0].trim();
                    double weight = 1.0;
                    if (parts.length > 1) {
                        try { weight = Double.parseDouble(parts[1]); } catch (NumberFormatException ignored) {}
                    }
                    if (weight <= 0) continue;
                    list.add(new WeightedMob(mobId, weight));
                }
                if (!list.isEmpty()) pools.put(poolId.toLowerCase(Locale.ROOT), list);
            }
        }
        debug("Natural spawn config loaded: enabled=" + enabled + ", pools=" + pools.size());
    }

    private double clamp01(double v) { return v < 0 ? 0 : Math.min(1.0, v); }

    public void handle(CreatureSpawnEvent event) {
        if (!enabled) return;
        if (event.getSpawnReason() != CreatureSpawnEvent.SpawnReason.NATURAL) return;
        if (ThreadLocalRandom.current().nextDouble() > globalChance) return; // chance gate
        Location loc = event.getLocation();
        World world = loc.getWorld();
        if (world == null) return;
        String biomeName = world.getBiome(loc).name().toUpperCase(Locale.ROOT);
        String pool = biomePools.getOrDefault(biomeName, "default");
        List<WeightedMob> list = pools.get(pool.toLowerCase(Locale.ROOT));
        if (list == null || list.isEmpty()) return;
        if (isOverpopulated(loc)) return;
        String mobId = pick(list);
        if (mobId == null) return;
        if (replaceVanilla) event.setCancelled(true);
        if (mobManager.spawn(mobId, loc) == null) {
            debug("Failed to spawn custom mob id=" + mobId + " (not found?)");
        } else {
            debug("Spawned custom mob '" + mobId + "' at " + loc.getBlockX() + "," + loc.getBlockY() + "," + loc.getBlockZ());
        }
    }

    private boolean isOverpopulated(Location loc) {
        int count = 0;
        int r2 = maxNearbyRadius * maxNearbyRadius;
        for (Entity e : Objects.requireNonNull(loc.getWorld()).getNearbyEntities(loc, maxNearbyRadius, maxNearbyRadius, maxNearbyRadius)) {
            if (e instanceof LivingEntity) {
                if (e.getLocation().distanceSquared(loc) <= r2) {
                    count++;
                    if (count >= maxNearbyCount) return true;
                }
            }
        }
        return false;
    }

    private String pick(List<WeightedMob> list) {
        double total = 0;
        for (WeightedMob wm : list) total += wm.weight;
        double r = ThreadLocalRandom.current().nextDouble() * total;
        double cumulative = 0;
        for (WeightedMob wm : list) {
            cumulative += wm.weight;
            if (r <= cumulative) return wm.mobId;
        }
        return list.get(list.size() - 1).mobId; // fallback
    }

    private void debug(String msg) {
        if (plugin.getConfig().getBoolean("Debug.enabled", false)) log.info("[NaturalSpawn] " + msg);
    }

    private static final class WeightedMob {
        final String mobId; final double weight;
        WeightedMob(String mobId, double weight) { this.mobId = mobId; this.weight = weight; }
    }
}

