package me.swishhyy.customMobs.faction;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.*;

public final class FactionManager {
    private final JavaPlugin plugin;
    private final Map<String, FactionRelation> relations = new HashMap<>();

    public FactionManager(JavaPlugin plugin) { this.plugin = plugin; reload(); }

    public void reload() {
        relations.clear();
        ConfigurationSection root = plugin.getConfig().getConfigurationSection("factions");
        if (root == null) return;
        for (String fac : root.getKeys(false)) {
            ConfigurationSection sec = root.getConfigurationSection(fac);
            if (sec == null) continue;
            Set<String> allies = new HashSet<>(sec.getStringList("allies"));
            Set<String> hostiles = new HashSet<>(sec.getStringList("hostiles"));
            // Always allied to itself unless explicitly hostile
            allies.add(fac);
            relations.put(fac.toLowerCase(Locale.ROOT), new FactionRelation(allies, hostiles));
        }
    }

    public boolean areAllied(String a, String b) {
        if (a == null || b == null) return false;
        if (a.equalsIgnoreCase(b)) return true;
        FactionRelation ra = relations.get(a.toLowerCase(Locale.ROOT));
        if (ra != null && ra.allies.contains(b.toLowerCase(Locale.ROOT))) return true;
        FactionRelation rb = relations.get(b.toLowerCase(Locale.ROOT));
        return rb != null && rb.allies.contains(a.toLowerCase(Locale.ROOT));
    }

    public boolean areHostile(String a, String b) {
        if (a == null || b == null) return false;
        FactionRelation ra = relations.get(a.toLowerCase(Locale.ROOT));
        if (ra != null && ra.hostiles.contains(b.toLowerCase(Locale.ROOT))) return true;
        FactionRelation rb = relations.get(b.toLowerCase(Locale.ROOT));
        return rb != null && rb.hostiles.contains(a.toLowerCase(Locale.ROOT));
    }

    private record FactionRelation(Set<String> allies, Set<String> hostiles) {}
}

