package me.swishhyy.customMobs.faction;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.util.*;

import me.swishhyy.customMobs.mob.MobBehavior;

public final class FactionManager {
    private final JavaPlugin plugin;
    private final Map<String, FactionRelation> relations = new HashMap<>();
    private File factionsFile;

    public FactionManager(JavaPlugin plugin) { this.plugin = plugin; init(); }

    private void init() {
        factionsFile = new File(plugin.getDataFolder(), "factions.yml");
        if (!factionsFile.exists()) {
            plugin.saveResource("factions.yml", false);
        }
        reload();
    }

    public void reload() {
        relations.clear();
        if (factionsFile == null) return;
        FileConfiguration cfg = YamlConfiguration.loadConfiguration(factionsFile);
        for (String fac : cfg.getKeys(false)) {
            if (!(cfg.get(fac) instanceof org.bukkit.configuration.ConfigurationSection sec)) continue;
            Set<String> allies = new HashSet<>(sec.getStringList("allies"));
            Set<String> hostiles = new HashSet<>(sec.getStringList("hostiles"));
            allies.add(fac); // self ally
            String beh = sec.getString("default_behavior", null);
            MobBehavior defaultBehavior = MobBehavior.fromString(beh, null);
            relations.put(fac.toLowerCase(Locale.ROOT), new FactionRelation(toLower(allies), toLower(hostiles), defaultBehavior));
        }
    }

    private Set<String> toLower(Set<String> in) { Set<String> out = new HashSet<>(); for (String s: in) out.add(s.toLowerCase(Locale.ROOT)); return out; }

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

    public MobBehavior getDefaultBehavior(String faction) {
        if (faction == null) return null;
        FactionRelation rel = relations.get(faction.toLowerCase(Locale.ROOT));
        return rel == null ? null : rel.defaultBehavior;
    }

    private record FactionRelation(Set<String> allies, Set<String> hostiles, MobBehavior defaultBehavior) {}
}
