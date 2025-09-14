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
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Logger;

import me.swishhyy.customMobs.util.CM;
import me.swishhyy.customMobs.util.Msg;
import me.swishhyy.customMobs.util.Colors;

/** Handles auto and manual update checks (with interactive prompt). */
public class AutoUpdate {
    private final JavaPlugin plugin;
    private final Logger log;
    private static final String REPO = "Swishhyy/CustomMobs";
    private static final long CHECK_INTERVAL_TICKS = 60L * 60L * 20L; // 60 minutes

    private final AtomicBoolean performingImmediate = new AtomicBoolean(false);
    private BukkitTask repeatingTask;
    private String lastDownloadedVersion = null;
    private final Map<UUID, PendingPrompt> pendingPrompts = new ConcurrentHashMap<>();

    private static final String PREFIX = CM.LOG_PREFIX;

    public AutoUpdate(JavaPlugin plugin) { this.plugin = plugin; this.log = plugin.getLogger(); }

    // Scheduling ------------------------------------------------------------
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

    // Manual interactive check ---------------------------------------------
    public void manualCheck(org.bukkit.command.CommandSender sender) {
        if (!CM.isAutoUpdateEnabled(plugin)) { Msg.send(sender, "§cAuto-update disabled in config."); return; }
        boolean allowPrerelease = plugin.getConfig().getBoolean("auto-update.beta", false);
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            try {
                String currentVersion = plugin.getDescription().getVersion();
                ReleaseInfo latest = fetchDesiredRelease(allowPrerelease);
                if (latest == null) { Msg.send(sender, "§eNo release info available."); return; }
                if (!isNewer(latest.tagName, currentVersion)) { Msg.send(sender, "§aUp to date (§f" + currentVersion + "§a)."); return; }
                if (!(sender instanceof org.bukkit.entity.Player player)) {
                    Msg.send(sender, "§bUpdate §f" + latest.tagName + " §bavailable. Downloading (console)." );
                    if (latest.assetDownloadUrl != null) downloadAsset(latest.assetDownloadUrl, currentVersion, latest.tagName);
                    return;
                }
                PendingPrompt prompt = new PendingPrompt(player.getUniqueId(), latest, allowPrerelease);
                pendingPrompts.put(player.getUniqueId(), prompt);
                String base = "§eNew Update Found: §f" + latest.tagName + " §7(current §f" + currentVersion + "§7)";
                if (allowPrerelease) {
                    Msg.send(player, base);
                    Msg.send(player, "§eType §aYes§e to download this version, §bStable§e for latest stable, or §cNo§e to cancel.");
                } else {
                    Msg.send(player, base);
                    Msg.send(player, "§eWould you like to update now? Type §aYes§e or §cNo§e.");
                }
            } catch (Exception ex) { Msg.send(sender, "§cUpdate check failed: " + ex.getMessage()); }
        });
    }

    // Automatic background check (silent download) -------------------------
    private void runCheck() throws IOException {
        if (!CM.isAutoUpdateEnabled(plugin)) return;
        boolean allowPrerelease = plugin.getConfig().getBoolean("auto-update.beta", false);
        String currentVersion = plugin.getDescription().getVersion();
        ReleaseInfo latest = fetchDesiredRelease(allowPrerelease);
        if (latest == null) { log.info(Colors.YELLOW + PREFIX + "No release info received." + Colors.RESET); return; }
        if (!isNewer(latest.tagName, currentVersion)) { log.info(Colors.GREEN + PREFIX + "Up to date (" + currentVersion + ")." + Colors.RESET); return; }
        if (latest.assetDownloadUrl == null) { log.warning(Colors.RED + PREFIX + "No .jar asset in release " + latest.tagName + "." + Colors.RESET); return; }
        log.info(Colors.CYAN + PREFIX + "Update found: current=" + currentVersion + " latest=" + latest.tagName + " -> downloading..." + Colors.RESET);
        downloadAsset(latest.assetDownloadUrl, currentVersion, latest.tagName);
    }

    // GitHub API helpers ----------------------------------------------------
    private ReleaseInfo fetchDesiredRelease(boolean allowPrerelease) throws IOException {
        if (!allowPrerelease) return fetchLatestRelease();
        HttpURLConnection conn = (HttpURLConnection) new URL("https://api.github.com/repos/" + REPO + "/releases").openConnection();
        conn.setRequestProperty("Accept", "application/vnd.github+json");
        conn.setRequestProperty("User-Agent", "CustomMobs-Updater");
        if (conn.getResponseCode() != 200) return null;
        String json; try (InputStream in = conn.getInputStream()) { json = new String(in.readAllBytes(), StandardCharsets.UTF_8); }
        int idx = 0; ReleaseInfo chosen = null;
        while (true) {
            int objStart = json.indexOf('{', idx); if (objStart < 0) break;
            int objEnd = json.indexOf("}\n", objStart); if (objEnd < 0) objEnd = json.indexOf('}', objStart); if (objEnd < 0) break;
            String obj = json.substring(objStart, objEnd + 1);
            if (obj.contains("\"draft\":true")) { idx = objEnd + 1; continue; }
            String tag = extract(obj, "tag_name"); if (tag == null) { idx = objEnd + 1; continue; }
            ReleaseInfo info = new ReleaseInfo();
            info.tagName = tag.startsWith("v") ? tag.substring(1) : tag;
            // asset
            String assetUrl = null; int aIdx = obj.indexOf("assets");
            if (aIdx >= 0) {
                int aStart = obj.indexOf('[', aIdx), aEnd = obj.indexOf(']', aStart);
                if (aStart > 0 && aEnd > aStart) {
                    String arr = obj.substring(aStart, aEnd + 1);
                    int p = 0; while (true) { int n = arr.indexOf("\"name\"", p); if (n < 0) break; String name = extractFrom(arr, "name", n); if (name != null && name.toLowerCase().endsWith(".jar")) { String sub = arr.substring(n); String urlMatch = extract(sub, "browser_download_url"); if (urlMatch != null) { assetUrl = urlMatch; break; } } p = n + 1; }
                }
            }
            info.assetDownloadUrl = assetUrl; chosen = info; break; // first non-draft entry
        }
        return chosen;
    }

    private void downloadAsset(String url, String oldVersion, String newVersion) throws IOException {
        if (newVersion != null && newVersion.equalsIgnoreCase(lastDownloadedVersion)) { log.info(Colors.YELLOW + PREFIX + "Version " + newVersion + " already downloaded earlier." + Colors.RESET); return; }
        File updateDir = new File(plugin.getDataFolder().getParentFile(), "update"); if (!updateDir.exists()) updateDir.mkdirs();
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

    private boolean isNewer(String remote, String local) {
        if (remote.equals(local)) return false;
        if (remote.matches("[0-9.]+") && local.matches("[0-9.]+")) {
            String[] r = remote.split("\\."); String[] l = local.split("\\.");
            int len = Math.max(r.length, l.length);
            for (int i = 0; i < len; i++) {
                int rv = i < r.length ? parseIntSafe(r[i]) : 0;
                int lv = i < l.length ? parseIntSafe(l[i]) : 0;
                if (rv != lv) return rv > lv;
            }
            return false;
        }
        return true;
    }
    private int parseIntSafe(String s) { try { return Integer.parseInt(s); } catch (NumberFormatException e) { return 0; } }
    private String getCurrentPluginJarName() { try { String path = plugin.getClass().getProtectionDomain().getCodeSource().getLocation().getPath(); if (path == null) return null; int slash = Math.max(path.lastIndexOf('/'), path.lastIndexOf('\\')); if (slash >= 0) path = path.substring(slash + 1); return path.isEmpty() ? null : path; } catch (Exception e) { return null; } }

    private ReleaseInfo fetchLatestRelease() throws IOException {
        HttpURLConnection conn = (HttpURLConnection) new URL("https://api.github.com/repos/" + REPO + "/releases/latest").openConnection();
        conn.setRequestProperty("Accept", "application/vnd.github+json");
        conn.setRequestProperty("User-Agent", "CustomMobs-Updater");
        if (conn.getResponseCode() != 200) return null;
        String json; try (InputStream in = conn.getInputStream()) { json = new String(in.readAllBytes(), StandardCharsets.UTF_8); }
        String tag = extract(json, "tag_name"); if (tag == null) return null;
        String assetUrl = null; int assetsIndex = json.indexOf("\"assets\"");
        if (assetsIndex >= 0) {
            int arrayStart = json.indexOf('[', assetsIndex), arrayEnd = json.indexOf(']', arrayStart);
            if (arrayStart > 0 && arrayEnd > arrayStart) {
                String arr = json.substring(arrayStart, arrayEnd + 1);
                int pos = 0; while (true) { int n = arr.indexOf("\"name\"", pos); if (n < 0) break; String name = extractFrom(arr, "name", n); if (name != null && name.toLowerCase().endsWith(".jar")) { String sub = arr.substring(n); String urlMatch = extract(sub, "browser_download_url"); if (urlMatch != null) { assetUrl = urlMatch; break; } } pos = n + 1; }
            }
        }
        ReleaseInfo info = new ReleaseInfo(); info.tagName = tag.startsWith("v") ? tag.substring(1) : tag; info.assetDownloadUrl = assetUrl; return info;
    }

    private String extract(String json, String key) { int idx = json.indexOf('"' + key + '"'); return idx < 0 ? null : extractFrom(json, key, idx); }
    private String extractFrom(String json, String key, int startIdx) { int colon = json.indexOf(':', startIdx); if (colon < 0) return null; int q1 = json.indexOf('"', colon); if (q1 < 0) return null; int q2 = json.indexOf('"', q1 + 1); if (q2 < 0) return null; return json.substring(q1 + 1, q2); }

    // Chat prompt handling --------------------------------------------------
    public boolean handleChatResponse(org.bukkit.entity.Player player, String messageRaw) {
        PendingPrompt prompt = pendingPrompts.get(player.getUniqueId());
        if (prompt == null) return false;
        String msg = messageRaw.trim().toLowerCase(java.util.Locale.ROOT);
        if (msg.equals("yes") || msg.equals("y")) {
            pendingPrompts.remove(player.getUniqueId());
            Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
                try {
                    if (prompt.info.assetDownloadUrl == null) { Msg.send(player, "§cNo jar asset in release."); return; }
                    Msg.send(player, "§bDownloading update §f" + prompt.info.tagName + "§b...");
                    String current = plugin.getDescription().getVersion();
                    downloadAsset(prompt.info.assetDownloadUrl, current, prompt.info.tagName);
                } catch (Exception ex) { Msg.send(player, "§cDownload failed: " + ex.getMessage()); }
            });
            return true;
        }
        if (msg.equals("stable") && prompt.allowPrerelease) {
            pendingPrompts.remove(player.getUniqueId());
            Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
                try {
                    ReleaseInfo stable = fetchLatestRelease();
                    if (stable == null) { Msg.send(player, "§cCould not fetch stable release."); return; }
                    String current = plugin.getDescription().getVersion();
                    if (!isNewer(stable.tagName, current)) { Msg.send(player, "§eAlready at latest stable (§f" + stable.tagName + "§e)." ); return; }
                    if (stable.assetDownloadUrl == null) { Msg.send(player, "§cStable release missing jar asset."); return; }
                    Msg.send(player, "§bDownloading stable update §f" + stable.tagName + "§b...");
                    downloadAsset(stable.assetDownloadUrl, current, stable.tagName);
                } catch (Exception ex) { Msg.send(player, "§cStable download failed: " + ex.getMessage()); }
            });
            return true;
        }
        if (msg.equals("no") || msg.equals("n")) {
            pendingPrompts.remove(player.getUniqueId());
            Msg.send(player, "§7Update canceled.");
            return true;
        }
        // remind
        if (prompt.allowPrerelease) Msg.send(player, "§ePlease type §aYes§e, §bStable§e, or §cNo§e."); else Msg.send(player, "§ePlease type §aYes§e or §cNo§e.");
        return true; // consume chat even if invalid to avoid chat spam
    }

    // Data classes ----------------------------------------------------------
    private static class PendingPrompt { final UUID playerId; final ReleaseInfo info; final boolean allowPrerelease; PendingPrompt(UUID id, ReleaseInfo info, boolean allow) { this.playerId = id; this.info = info; this.allowPrerelease = allow; } }
    private static class ReleaseInfo { String tagName; String assetDownloadUrl; }
}
