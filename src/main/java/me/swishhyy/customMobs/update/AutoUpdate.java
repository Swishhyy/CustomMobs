package me.swishhyy.customMobs.update;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Logger;

/**
 * Simple auto-updater.
 * Controlled only by config key: auto-update.enabled
 * All other parameters are fixed constants below.
 *
 * Logic:
 * - On start, if enabled, run an async check.
 * - Schedule repeating task at fixed interval (hard-coded) while enabled.
 * - Compare current plugin version to latest release tag (semantic compare if possible).
 * - If newer, download specified asset to plugins/update/CustomMobs.jar (Paper/Spigot will swap on restart).
 */
public class AutoUpdate {
    private final JavaPlugin plugin;
    private final Logger log;

    // Minimal required constants
    private static final String REPO = "Swishhyy/CustomMobs"; // GitHub owner/repo
    private static final String ASSET_PREFIX = "CustomMobs-"; // Release asset name starts with this
    private static final String ASSET_SUFFIX = ".jar";        // And ends with this
    private static final long CHECK_INTERVAL_TICKS = 60L * 60L * 20L; // 60 minutes

    private final AtomicBoolean performingImmediate = new AtomicBoolean(false);
    private BukkitTask repeatingTask;
    private String lastDownloadedVersion = null;

    private static final String ANSI_RESET = "\u001B[0m";
    private static final String ANSI_GREEN = "\u001B[32m";
    private static final String ANSI_CYAN = "\u001B[36m";
    private static final String ANSI_YELLOW = "\u001B[33m";
    private static final String ANSI_RED = "\u001B[31m";
    private static final String ANSI_MAGENTA = "\u001B[35m";
    private static final String PREFIX = "[CustomMobs] ";

    public AutoUpdate(JavaPlugin plugin) {
        this.plugin = plugin;
        this.log = plugin.getLogger();
    }

    public void startOrSchedule() {
        if (!plugin.getConfig().getBoolean("auto-update.enabled", false)) {
            cancelRepeating();
            log.fine("Auto-update disabled.");
            return;
        }
        if (repeatingTask == null || repeatingTask.isCancelled()) {
            repeatingTask = Bukkit.getScheduler().runTaskTimerAsynchronously(plugin, this::runCheckSafe, 20L, CHECK_INTERVAL_TICKS);
            log.info("Auto-update enabled (interval 60m).");
        }
        triggerImmediateCheck();
    }

    public void shutdown() {
        cancelRepeating();
    }

    private void cancelRepeating() {
        if (repeatingTask != null) {
            repeatingTask.cancel();
            repeatingTask = null;
        }
    }

    private void triggerImmediateCheck() {
        if (!performingImmediate.compareAndSet(false, true)) return;
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            try { runCheckSafe(); } finally { performingImmediate.set(false); }
        });
    }

    private void bannerLine(String line) { log.info(ANSI_MAGENTA + line + ANSI_RESET); }

    private void printUpdateBanner(String context) {
        bannerLine("//====================================================\\");
        bannerLine("   _____          _                 __  __       _     ");
        bannerLine("  / ____|        | |               |  \\|  |     | |    ");
        bannerLine(" | |    _   _ ___| |_ ___ _ __ ___ | |\\ | | ___ | |__  ");
        bannerLine(" | |   | | | / __| __/ _ \\ '_ ` _ \\| | \\| |/ _ \\ '_ \\ ");
        bannerLine(" | |___| |_| \\__ \\ ||  __/ | | | | | |\\  |  __/ |_) | ");
        bannerLine("  \\_____\\__,_|___/\\__\\___|_| |_| |_| |_|_|\\___|_.__/  ");
        bannerLine("        Authored by Swishhyy | " + context);
        bannerLine("\\====================================================//");
    }

    private void runCheckSafe() {
        try {
            printUpdateBanner("Auto Check");
            log.info(ANSI_CYAN + PREFIX + "Checking for updates..." + ANSI_RESET);
            runCheck();
        } catch (Exception ex) {
            log.warning(ANSI_RED + PREFIX + "Auto-update check failed: " + ex.getMessage() + ANSI_RESET);
        }
    }

    public void manualCheck(org.bukkit.command.CommandSender sender) {
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            if (!plugin.getConfig().getBoolean("auto-update.enabled", false)) {
                sender.sendMessage("§8[§aCustomMobs§8] §cAuto-update disabled in config.");
                return;
            }
            try {
                printUpdateBanner("Manual Trigger: " + sender.getName());
                log.info(ANSI_CYAN + PREFIX + "Checking for updates..." + ANSI_RESET);
                String currentVersion = plugin.getDescription().getVersion();
                ReleaseInfo latest = fetchLatestRelease();
                if (latest == null) { sender.sendMessage("§8[§aCustomMobs§8] §eNo release info available."); log.info(ANSI_YELLOW + PREFIX + "No release info from GitHub API." + ANSI_RESET); return; }
                if (!isNewer(latest.tagName, currentVersion)) {
                    if (lastDownloadedVersion != null && lastDownloadedVersion.equalsIgnoreCase(latest.tagName)) {
                        sender.sendMessage("§8[§aCustomMobs§8] §eLatest version §a" + latest.tagName + " §ealready downloaded. Restart to apply.");
                        log.info(ANSI_YELLOW + PREFIX + "Latest version " + latest.tagName + " already downloaded. Awaiting restart/reload." + ANSI_RESET);
                    } else {
                        sender.sendMessage("§8[§aCustomMobs§8] §aUp to date (§f" + currentVersion + "§a).");
                        log.info(ANSI_GREEN + PREFIX + "Up to date (" + currentVersion + ")." + ANSI_RESET);
                    }
                    return;
                }
                if (latest.assetDownloadUrl == null) { sender.sendMessage("§8[§aCustomMobs§8] §cMatching asset not found."); log.warning(ANSI_RED + PREFIX + "Matching asset not found in release " + latest.tagName + "." + ANSI_RESET); return; }
                sender.sendMessage("§8[§aCustomMobs§8] §bDownloading §f" + latest.tagName + "§b...");
                log.info(ANSI_CYAN + PREFIX + "Update found: current=" + currentVersion + " latest=" + latest.tagName + " -> downloading..." + ANSI_RESET);
                downloadAsset(latest.assetDownloadUrl, currentVersion, latest.tagName);
                sender.sendMessage("§8[§aCustomMobs§8] §aDownload complete. Restart or reload to apply.");
            } catch (Exception ex) {
                sender.sendMessage("§8[§aCustomMobs§8] §cUpdate check failed: " + ex.getMessage());
                log.warning(ANSI_RED + PREFIX + "Manual update check failed: " + ex.getMessage() + ANSI_RESET);
            }
        });
    }

    private void runCheck() throws IOException {
        if (!plugin.getConfig().getBoolean("auto-update.enabled", false)) return;
        String currentVersion = plugin.getDescription().getVersion();
        ReleaseInfo latest = fetchLatestRelease();
        if (latest == null) { log.info(ANSI_YELLOW + PREFIX + "No release info received." + ANSI_RESET); return; }
        if (!isNewer(latest.tagName, currentVersion)) { log.info(ANSI_GREEN + PREFIX + "Up to date (" + currentVersion + ")." + ANSI_RESET); return; }
        if (latest.assetDownloadUrl == null) { log.warning(ANSI_RED + PREFIX + "Matching asset missing in release " + latest.tagName + "." + ANSI_RESET); return; }
        log.info(ANSI_CYAN + PREFIX + "Update found: current=" + currentVersion + " latest=" + latest.tagName + " -> downloading..." + ANSI_RESET);
        downloadAsset(latest.assetDownloadUrl, currentVersion, latest.tagName);
    }

    private void downloadAsset(String url, String oldVersion, String newVersion) throws IOException {
        if (newVersion != null && newVersion.equalsIgnoreCase(lastDownloadedVersion)) { log.info(ANSI_YELLOW + PREFIX + "Version " + newVersion + " already downloaded earlier." + ANSI_RESET); return; }
        File updateDir = new File(plugin.getDataFolder().getParentFile(), "update");
        if (!updateDir.exists()) updateDir.mkdirs();
        String targetFileName = getCurrentPluginJarName();
        File outFile = new File(updateDir, targetFileName == null ? "CustomMobs.jar" : targetFileName);
        HttpURLConnection conn = (HttpURLConnection) new URL(url).openConnection();
        conn.setRequestProperty("User-Agent", "CustomMobs-Updater");
        if (conn.getResponseCode() != 200) return;
        try (InputStream in = new BufferedInputStream(conn.getInputStream());
             FileOutputStream out = new FileOutputStream(outFile)) {
            byte[] buf = new byte[8192]; int r; while ((r = in.read(buf)) != -1) out.write(buf, 0, r);
        }
        lastDownloadedVersion = newVersion;
        log.info(ANSI_GREEN + PREFIX + "Download finished for version " + newVersion + ". Stored for next restart." + ANSI_RESET);
        String msg = "§8[§aCustomMobs§8] §aCustomMobs has been installed to §f" + newVersion + " §afrom §f" + oldVersion + "§a, please restart or reload the server for it to work properly";
        for (Player pl : Bukkit.getOnlinePlayers()) if (pl.hasPermission("custommobs.admin")) pl.sendMessage(msg);
    }

    private boolean isNewer(String remote, String local) {
        if (remote.equals(local)) return false;
        if (remote.matches("[0-9.]+") && local.matches("[0-9.]+")) {
            String[] r = remote.split("\\.");
            String[] l = local.split("\\.");
            int len = Math.max(r.length, l.length);
            for (int i = 0; i < len; i++) {
                int rv = i < r.length ? parseIntSafe(r[i]) : 0;
                int lv = i < l.length ? parseIntSafe(l[i]) : 0;
                if (rv != lv) return rv > lv;
            }
            return false;
        }
        return !remote.equals(local);
    }

    private int parseIntSafe(String s) { try { return Integer.parseInt(s); } catch (NumberFormatException e) { return 0; } }

    private String extractJsonString(String json, String key) {
        int idx = json.indexOf('"' + key + '"');
        if (idx < 0) return null;
        return extractJsonStringFrom(json, key, idx);
    }

    private String extractJsonStringFrom(String json, String key, int startIdx) {
        int colon = json.indexOf(':', startIdx); if (colon < 0) return null;
        int firstQuote = json.indexOf('"', colon); if (firstQuote < 0) return null;
        int secondQuote = json.indexOf('"', firstQuote + 1); if (secondQuote < 0) return null;
        return json.substring(firstQuote + 1, secondQuote);
    }

    private String getCurrentPluginJarName() {
        try {
            String path = plugin.getClass().getProtectionDomain().getCodeSource().getLocation().getPath();
            if (path == null) return null;
            int slash = Math.max(path.lastIndexOf('/'), path.lastIndexOf('\\'));
            if (slash >= 0) path = path.substring(slash + 1);
            return path.isEmpty() ? null : path;
        } catch (Exception e) {
            return null;
        }
    }

    private ReleaseInfo fetchLatestRelease() throws IOException {
        HttpURLConnection conn = (HttpURLConnection) new URL("https://api.github.com/repos/" + REPO + "/releases/latest").openConnection();
        conn.setRequestProperty("Accept", "application/vnd.github+json");
        conn.setRequestProperty("User-Agent", "CustomMobs-Updater");
        if (conn.getResponseCode() != 200) return null;
        String json;
        try (InputStream in = conn.getInputStream()) {
            json = new String(in.readAllBytes(), StandardCharsets.UTF_8);
        }
        String tag = extractJsonString(json, "tag_name");
        if (tag == null) return null;
        String assetUrl = null;
        int assetsIndex = json.indexOf("\"assets\"");
        if (assetsIndex >= 0) {
            int arrayStart = json.indexOf('[', assetsIndex);
            int arrayEnd = json.indexOf(']', arrayStart);
            if (arrayStart > 0 && arrayEnd > arrayStart) {
                String assetsArray = json.substring(arrayStart, arrayEnd + 1);
                int pos = 0;
                while (true) {
                    int nameIdx = assetsArray.indexOf("\"name\"", pos);
                    if (nameIdx < 0) break;
                    String name = extractJsonStringFrom(assetsArray, "name", nameIdx);
                    if (name != null && name.startsWith(ASSET_PREFIX) && name.endsWith(ASSET_SUFFIX)) {
                        String sub = assetsArray.substring(nameIdx);
                        String urlMatch = extractJsonString(sub, "browser_download_url");
                        if (urlMatch != null) { assetUrl = urlMatch; break; }
                    }
                    pos = nameIdx + 1;
                }
            }
        }
        ReleaseInfo info = new ReleaseInfo();
        info.tagName = tag.startsWith("v") ? tag.substring(1) : tag;
        info.assetDownloadUrl = assetUrl;
        return info;
    }

    private static class ReleaseInfo { String tagName; String assetDownloadUrl; }
}
