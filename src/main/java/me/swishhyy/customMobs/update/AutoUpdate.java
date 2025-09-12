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

import me.swishhyy.customMobs.util.CM;
import me.swishhyy.customMobs.util.Msg;
import me.swishhyy.customMobs.util.Colors;

public class AutoUpdate {
    private final JavaPlugin plugin;
    private final Logger log;
    private static final String REPO = "Swishhyy/CustomMobs";
    private static final long CHECK_INTERVAL_TICKS = 60L * 60L * 20L; // 60 minutes

    private final AtomicBoolean performingImmediate = new AtomicBoolean(false);
    private BukkitTask repeatingTask;
    private String lastDownloadedVersion = null;

    private static final String PREFIX = CM.LOG_PREFIX;

    public AutoUpdate(JavaPlugin plugin) { this.plugin = plugin; this.log = plugin.getLogger(); }

    public void startOrSchedule() {
        if (!CM.isAutoUpdateEnabled(plugin)) { cancelRepeating(); log.fine("Auto-update disabled."); return; }
        if (repeatingTask == null || repeatingTask.isCancelled()) {
            repeatingTask = Bukkit.getScheduler().runTaskTimerAsynchronously(plugin, this::runCheckSafe, 20L, CHECK_INTERVAL_TICKS);
            log.info("Auto-update enabled (interval 60m).");
        }
        triggerImmediateCheck();
    }

    public void shutdown() { cancelRepeating(); }

    private void cancelRepeating() { if (repeatingTask != null) { repeatingTask.cancel(); repeatingTask = null; } }

    private void triggerImmediateCheck() { if (!performingImmediate.compareAndSet(false, true)) return; Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> { try { runCheckSafe(); } finally { performingImmediate.set(false); } }); }

    private void runCheckSafe() { try { log.info(Colors.CYAN + PREFIX + "Checking for updates..." + Colors.RESET); runCheck(); } catch (Exception ex) { log.warning(Colors.RED + PREFIX + "Auto-update check failed: " + ex.getMessage() + Colors.RESET); } }

    public void manualCheck(org.bukkit.command.CommandSender sender) {
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            if (!CM.isAutoUpdateEnabled(plugin)) { Msg.send(sender, "§cAuto-update disabled in config."); return; }
            try {
                log.info(Colors.CYAN + PREFIX + "Manual update check triggered by " + sender.getName() + "..." + Colors.RESET);
                String currentVersion = plugin.getDescription().getVersion();
                ReleaseInfo latest = fetchLatestRelease();
                if (latest == null) { Msg.send(sender, "§eNo release info available."); log.info(Colors.YELLOW + PREFIX + "No release info from GitHub API." + Colors.RESET); return; }
                if (!isNewer(latest.tagName, currentVersion)) {
                    if (latest.tagName.equalsIgnoreCase(lastDownloadedVersion)) {
                        Msg.send(sender, "§eLatest version §a" + latest.tagName + " §ealready downloaded. Restart to apply.");
                        log.info(Colors.YELLOW + PREFIX + "Already downloaded " + latest.tagName + " (awaiting restart)." + Colors.RESET);
                    } else {
                        Msg.send(sender, "§aUp to date (§f" + currentVersion + "§a).");
                        log.info(Colors.GREEN + PREFIX + "Up to date (" + currentVersion + ")." + Colors.RESET);
                    }
                    return;
                }
                if (latest.assetDownloadUrl == null) { Msg.send(sender, "§cNo .jar asset found in latest release."); log.warning(Colors.RED + PREFIX + "No jar asset in release " + latest.tagName + "." + Colors.RESET); return; }
                Msg.send(sender, "§bDownloading §f" + latest.tagName + "§b...");
                log.info(Colors.CYAN + PREFIX + "Update found: current=" + currentVersion + " latest=" + latest.tagName + " -> downloading..." + Colors.RESET);
                downloadAsset(latest.assetDownloadUrl, currentVersion, latest.tagName);
                Msg.send(sender, "§aDownload complete. Restart or reload to apply.");
            } catch (Exception ex) { Msg.send(sender, "§cUpdate check failed: " + ex.getMessage()); log.warning(Colors.RED + PREFIX + "Manual update check failed: " + ex.getMessage() + Colors.RESET); }
        });
    }

    private void runCheck() throws IOException {
        if (!CM.isAutoUpdateEnabled(plugin)) return;
        String currentVersion = plugin.getDescription().getVersion();
        ReleaseInfo latest = fetchLatestRelease();
        if (latest == null) { log.info(Colors.YELLOW + PREFIX + "No release info received." + Colors.RESET); return; }
        if (!isNewer(latest.tagName, currentVersion)) { log.info(Colors.GREEN + PREFIX + "Up to date (" + currentVersion + ")." + Colors.RESET); return; }
        if (latest.assetDownloadUrl == null) { log.warning(Colors.RED + PREFIX + "No .jar asset in release " + latest.tagName + "." + Colors.RESET); return; }
        log.info(Colors.CYAN + PREFIX + "Update found: current=" + currentVersion + " latest=" + latest.tagName + " -> downloading..." + Colors.RESET);
        downloadAsset(latest.assetDownloadUrl, currentVersion, latest.tagName);
    }

    private void downloadAsset(String url, String oldVersion, String newVersion) throws IOException {
        if (newVersion != null && newVersion.equalsIgnoreCase(lastDownloadedVersion)) { log.info(Colors.YELLOW + PREFIX + "Version " + newVersion + " already downloaded earlier." + Colors.RESET); return; }
        File updateDir = new File(plugin.getDataFolder().getParentFile(), "update");
        if (!updateDir.exists()) updateDir.mkdirs();
        String targetFileName = getCurrentPluginJarName();
        File outFile = new File(updateDir, targetFileName == null ? "CustomMobs.jar" : targetFileName);
        HttpURLConnection conn = (HttpURLConnection) new URL(url).openConnection();
        conn.setRequestProperty("User-Agent", "CustomMobs-Updater");
        if (conn.getResponseCode() != 200) return;
        try (InputStream in = new BufferedInputStream(conn.getInputStream()); FileOutputStream out = new FileOutputStream(outFile)) { byte[] buf = new byte[8192]; int r; while ((r = in.read(buf)) != -1) out.write(buf, 0, r); }
        lastDownloadedVersion = newVersion;
        log.info(Colors.GREEN + PREFIX + "Download finished for version " + newVersion + ". Stored for next restart." + Colors.RESET);
        String notify = "§aCustomMobs has been installed to §f" + newVersion + " §afrom §f" + oldVersion + "§a, please restart or reload the server for it to work properly";
        for (Player pl : Bukkit.getOnlinePlayers()) if (Msg.isAdmin(pl)) Msg.send(pl, notify);
    }

    private boolean isNewer(String remote, String local) { if (remote.equals(local)) return false; if (remote.matches("[0-9.]+") && local.matches("[0-9.]+")) { String[] r = remote.split("\\."); String[] l = local.split("\\."); int len = Math.max(r.length, l.length); for (int i = 0; i < len; i++) { int rv = i < r.length ? parseIntSafe(r[i]) : 0; int lv = i < l.length ? parseIntSafe(l[i]) : 0; if (rv != lv) return rv > lv; } return false; } return !remote.equals(local); }

    private int parseIntSafe(String s) { try { return Integer.parseInt(s); } catch (NumberFormatException e) { return 0; } }

    private String getCurrentPluginJarName() { try { String path = plugin.getClass().getProtectionDomain().getCodeSource().getLocation().getPath(); if (path == null) return null; int slash = Math.max(path.lastIndexOf('/'), path.lastIndexOf('\\')); if (slash >= 0) path = path.substring(slash + 1); return path.isEmpty() ? null : path; } catch (Exception e) { return null; } }

    private ReleaseInfo fetchLatestRelease() throws IOException {
        HttpURLConnection conn = (HttpURLConnection) new URL("https://api.github.com/repos/" + REPO + "/releases/latest").openConnection();
        conn.setRequestProperty("Accept", "application/vnd.github+json");
        conn.setRequestProperty("User-Agent", "CustomMobs-Updater");
        if (conn.getResponseCode() != 200) return null;
        String json; try (InputStream in = conn.getInputStream()) { json = new String(in.readAllBytes(), StandardCharsets.UTF_8); }
        String tag = extract(json, "tag_name"); if (tag == null) return null;
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
                    String name = extractFrom(assetsArray, "name", nameIdx);
                    if (name != null && name.toLowerCase().endsWith(".jar")) {
                        String sub = assetsArray.substring(nameIdx);
                        String urlMatch = extract(sub, "browser_download_url");
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

    private String extract(String json, String key) { int idx = json.indexOf('"' + key + '"'); return idx < 0 ? null : extractFrom(json, key, idx); }
    private String extractFrom(String json, String key, int startIdx) { int colon = json.indexOf(':', startIdx); if (colon < 0) return null; int q1 = json.indexOf('"', colon); if (q1 < 0) return null; int q2 = json.indexOf('"', q1 + 1); if (q2 < 0) return null; return json.substring(q1 + 1, q2); }

    private static class ReleaseInfo { String tagName; String assetDownloadUrl; }
}
