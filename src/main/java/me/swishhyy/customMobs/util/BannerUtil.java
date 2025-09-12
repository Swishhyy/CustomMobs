package me.swishhyy.customMobs.util;

import org.bukkit.Bukkit;
import org.bukkit.plugin.Plugin;

import java.util.logging.Logger;

/**
 * Prints a colored ASCII art banner on plugin enable.
 */
public final class BannerUtil {
    private BannerUtil() {}

    public static void printStartupBanner(Plugin plugin) {
        Logger log = plugin.getLogger();
        String version = plugin.getDescription().getVersion();
        String mcVersion = Bukkit.getServer().getVersion();
        log.info(Colors.PURPLE + "============================= CM =============================" + Colors.RESET);
        log.info(Colors.CYAN +   "   ______   __  ___" + Colors.RESET);
        log.info(Colors.CYAN +   "  / ____/  /  |/  /  (C)" + Colors.RESET);
        log.info(Colors.CYAN +   " / /      / /|_/ /" + Colors.RESET);
        log.info(Colors.CYAN +   "/ /___   / /  / /   (M)" + Colors.RESET);
        log.info(Colors.CYAN +   "\\____/  /_/  /_/" + Colors.RESET);
        log.info(Colors.PURPLE + "   CM - Authored by Swishhyy" + Colors.RESET);
        log.info(Colors.GREEN +  "   Version: " + version + " | Server: " + mcVersion + Colors.RESET);
        log.info(Colors.PURPLE + "==============================================================" + Colors.RESET);
    }
}
