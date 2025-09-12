package me.swishhyy.customMobs.util;

import org.bukkit.Bukkit;
import org.bukkit.plugin.Plugin;

import java.util.logging.Logger;

/**
 * Prints a colored ASCII art banner on plugin enable.
 */
public final class BannerUtil {
    private static final String ANSI_RESET = "\u001B[0m";
    private static final String ANSI_CYAN = "\u001B[36m";
    private static final String ANSI_PURPLE = "\u001B[35m";
    private static final String ANSI_GREEN = "\u001B[32m";

    private BannerUtil() {}

    public static void printStartupBanner(Plugin plugin) {
        Logger log = plugin.getLogger();
        String version = plugin.getDescription().getVersion();
        String mcVersion = Bukkit.getServer().getVersion();
        log.info(ANSI_PURPLE + "============================= CM =============================" + ANSI_RESET);
        log.info(ANSI_CYAN +   "   ______   __  ___" + ANSI_RESET);
        log.info(ANSI_CYAN +   "  / ____/  /  |/  /  (C)" + ANSI_RESET);
        log.info(ANSI_CYAN +   " / /      / /|_/ /" + ANSI_RESET);
        log.info(ANSI_CYAN +   "/ /___   / /  / /   (M)" + ANSI_RESET);
        log.info(ANSI_CYAN +   "\\____/  /_/  /_/" + ANSI_RESET);
        log.info(ANSI_PURPLE + "   CM - Authored by Swishhyy" + ANSI_RESET);
        log.info(ANSI_GREEN +  "   Version: " + version + " | Server: " + mcVersion + ANSI_RESET);
        log.info(ANSI_PURPLE + "==============================================================" + ANSI_RESET);
    }
}
