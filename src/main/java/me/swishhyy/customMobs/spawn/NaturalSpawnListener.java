package me.swishhyy.customMobs.spawn;

import me.swishhyy.customMobs.mob.MobDefinition;
import me.swishhyy.customMobs.mob.MobManager;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.CreatureSpawnEvent;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.Chunk;
import org.bukkit.block.Biome;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ThreadLocalRandom;

public class NaturalSpawnListener implements Listener {
    private final MobManager mobManager;

    public NaturalSpawnListener(MobManager mobManager) { this.mobManager = mobManager; }

    private static String biomeKey(Biome b) {
        try { return b.getKey().getKey().toUpperCase(Locale.ROOT); } catch (Throwable t) { return b.toString().toUpperCase(Locale.ROOT); }
    }

    @EventHandler
    public void onCreatureSpawn(CreatureSpawnEvent e) {
        if (e.getSpawnReason() != CreatureSpawnEvent.SpawnReason.NATURAL) return;
        Location loc = e.getLocation(); World world = loc.getWorld(); if (world == null) return;
        String biome = biomeKey(world.getBiome(loc));
        int globalChunkCap = mobManager.getPlugin().getConfig().getInt("natural-global.chunk-cap", 0);
        if (globalChunkCap > 0 && countNaturalCustomInChunk(loc.getChunk()) >= globalChunkCap) return;
        List<Candidate> candidates = new ArrayList<>(); double total = 0;
        for (MobDefinition def : mobManager.getAll()) {
            if (!def.naturalEnabled()) continue;
            if (!def.naturalBiomes().isEmpty() && !def.naturalBiomes().contains(biome)) continue;
            if (def.naturalChance() > 0 && ThreadLocalRandom.current().nextDouble() > def.naturalChance()) continue;
            Integer capChunk = def.naturalCapChunk();
            if (capChunk != null && capChunk > 0 && countSpecificNaturalInChunk(loc.getChunk(), def.id()) >= capChunk) continue;
            double w = Math.max(0, def.naturalWeight()); if (w <= 0) continue;
            candidates.add(new Candidate(def, w)); total += w;
        }
        if (candidates.isEmpty()) return;
        double r = ThreadLocalRandom.current().nextDouble() * total; double acc = 0; MobDefinition chosen = candidates.get(candidates.size()-1).def;
        for (Candidate c : candidates) { acc += c.weight; if (r <= acc) { chosen = c.def; break; } }
        if (chosen.naturalReplace()) e.setCancelled(true);
        mobManager.spawnNatural(chosen.id(), loc);
    }
    private int countNaturalCustomInChunk(Chunk chunk) { int c = 0; for (Entity e : chunk.getEntities()) if (e instanceof LivingEntity le) if (mobManager.getDefinitionFromEntity(le)!=null && mobManager.isNatural(le)) c++; return c; }
    private int countSpecificNaturalInChunk(Chunk chunk, String id) { int c = 0; for (Entity e : chunk.getEntities()) if (e instanceof LivingEntity le) { var def = mobManager.getDefinitionFromEntity(le); if (def!=null && def.id().equalsIgnoreCase(id) && mobManager.isNatural(le)) c++; } return c; }
    private record Candidate(MobDefinition def, double weight) {}
}
