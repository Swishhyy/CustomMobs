package me.swishhyy.customMobs.util;

import org.bukkit.plugin.java.JavaPlugin;

public final class CM {
    private CM() {}
    public static final String PERM_ADMIN = "custommobs.admin";
    public static final String CHAT_PREFIX = "§8[§aCustomMobs§8] ";
    public static final String LOG_PREFIX = "[CustomMobs] ";
    public static final String CFG_AUTO_UPDATE = "auto-update.enabled";

    public static boolean isAutoUpdateEnabled(JavaPlugin plugin) {
        return plugin.getConfig().getBoolean(CFG_AUTO_UPDATE, false);
    }
}
