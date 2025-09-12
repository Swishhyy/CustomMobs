package me.swishhyy.customMobs.spawn;

import me.swishhyy.customMobs.mob.MobDefinition;
import me.swishhyy.customMobs.mob.MobManager;
import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.CreatureSpawnEvent;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ThreadLocalRandom;

public class NaturalSpawnListener implements Listener {
    private final MobManager mobManager;

    public NaturalSpawnListener(MobManager mobManager) { this.mobManager = mobManager; }

    @EventHandler
    public void onCreatureSpawn(CreatureSpawnEvent e) {
        if (e.getSpawnReason() != CreatureSpawnEvent.SpawnReason.NATURAL) return;
        Location loc = e.getLocation();
        World world = loc.getWorld();
        if (world == null) return;
        String biome = world.getBiome(loc).name().toUpperCase(Locale.ROOT);
        int globalChunkCap = mobManager.plugin.getConfig().getInt("natural-global.chunk-cap", 0);
        int globalBiomeCap = mobManager.plugin.getConfig().getInt("natural-global.biome-cap", 0);
        if (globalChunkCap > 0 && countCustomInChunk(loc.getChunk()) >= globalChunkCap) return;
        if (globalBiomeCap > 0 && countCustomInBiome(world, biome) >= globalBiomeCap) return;
        List<Candidate> candidates = new ArrayList<>(); double total = 0;
        for (MobDefinition def : mobManager.getAll()) {
            if (!def.naturalEnabled()) continue;
            if (!def.naturalBiomes().isEmpty() && !def.naturalBiomes().contains(biome)) continue;
            if (def.naturalChance() > 0 && ThreadLocalRandom.current().nextDouble() > def.naturalChance()) continue;
            // per-mob chunk cap
            Integer capChunk = def.naturalCapChunk();
            if (capChunk != null && capChunk > 0 && countSpecificInChunk(loc.getChunk(), def.id()) >= capChunk) continue;
            // per-mob biome cap
            Integer capBiome = def.naturalCapBiome();
            if (capBiome != null && capBiome > 0 && countSpecificInBiome(world, biome, def.id()) >= capBiome) continue;
            double w = Math.max(0, def.naturalWeight()); if (w <= 0) continue; candidates.add(new Candidate(def, w)); total += w;
        }
        if (candidates.isEmpty()) return;
        double r = ThreadLocalRandom.current().nextDouble() * total; MobDefinition chosen = candidates.get(candidates.size()-1).def; double acc = 0; for (Candidate c : candidates) { acc += c.weight; if (r <= acc) { chosen = c.def; break; } }
        if (chosen.naturalReplace()) e.setCancelled(true);
        mobManager.spawn(chosen.id(), loc);
    }

    private int countCustomInChunk(Chunk chunk) { int c=0; for (Entity e:chunk.getEntities()) if (e instanceof LivingEntity le && mobManager.getDefinitionFromEntity(le)!=null) c++; return c; }
    private int countSpecificInChunk(Chunk chunk, String id) { int c=0; for (Entity e:chunk.getEntities()) if (e instanceof LivingEntity le) { var d=mobManager.getDefinitionFromEntity(le); if (d!=null && d.id().equalsIgnoreCase(id)) c++; } return c; }
    private int countCustomInBiome(World world, String biome) { int c=0; for (Entity e: world.getEntities()) if (e instanceof LivingEntity le) { if (!world.getBiome(le.getLocation()).name().equalsIgnoreCase(biome)) continue; if (mobManager.getDefinitionFromEntity(le)!=null) c++; } return c; }
    private int countSpecificInBiome(World world, String biome, String id) { int c=0; for (Entity e: world.getEntities()) if (e instanceof LivingEntity le) { if (!world.getBiome(le.getLocation()).name().equalsIgnoreCase(biome)) continue; var d=mobManager.getDefinitionFromEntity(le); if (d!=null && d.id().equalsIgnoreCase(id)) c++; } return c; }
    private record Candidate(MobDefinition def, double weight) {}
}
