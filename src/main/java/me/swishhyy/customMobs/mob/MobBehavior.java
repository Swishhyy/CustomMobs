package me.swishhyy.customMobs.mob;

/** Behavioral classification for a custom mob. */
public enum MobBehavior {
    PASSIVE,
    NEUTRAL,
    HOSTILE;

    public static MobBehavior fromString(String s, MobBehavior def) {
        if (s == null) return def;
        try { return MobBehavior.valueOf(s.trim().toUpperCase()); } catch (IllegalArgumentException ex) { return def; }
    }
}

