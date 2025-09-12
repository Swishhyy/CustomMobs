package me.swishhyy.customMobs;

import org.bukkit.command.CommandSender;
import org.bukkit.plugin.java.JavaPlugin;

import me.swishhyy.customMobs.mob.MobManager;
import me.swishhyy.customMobs.listener.MobListener;
import me.swishhyy.customMobs.spawn.NaturalSpawnListener;
import me.swishhyy.customMobs.command.SpawnMobCommand;
import me.swishhyy.customMobs.update.AutoUpdate;
import me.swishhyy.customMobs.util.BannerUtil;
import me.swishhyy.customMobs.faction.FactionManager;
import me.swishhyy.customMobs.util.Msg;

public final class CustomMobs extends JavaPlugin {

    private MobManager mobManager;
    private AutoUpdate autoUpdate;
    private FactionManager factionManager;

    @Override
    public void onEnable() {
        BannerUtil.printStartupBanner(this);
        saveDefaultConfig();
        factionManager = new FactionManager(this);
        mobManager = new MobManager(this);
        mobManager.reloadAll();
        autoUpdate = new AutoUpdate(this);
        autoUpdate.startOrSchedule();
        getServer().getPluginManager().registerEvents(new MobListener(mobManager, factionManager), this);
        getServer().getPluginManager().registerEvents(new NaturalSpawnListener(mobManager), this);
        if (getCommand("cmspawn") != null) {
            SpawnMobCommand cmd = new SpawnMobCommand(this, mobManager);
            getCommand("cmspawn").setExecutor(cmd);
            getCommand("cmspawn").setTabCompleter(cmd);
        }
    }

    @Override
    public void onDisable() { if (autoUpdate != null) autoUpdate.shutdown(); }

    // Unified full reload invoked by /cmspawn reload
    public void fullReload(CommandSender sender) {
        long start = System.currentTimeMillis();
        reloadConfig();
        if (factionManager != null) factionManager.reload();
        if (mobManager != null) mobManager.reloadAll();
        if (autoUpdate != null) autoUpdate.startOrSchedule();
        long took = System.currentTimeMillis() - start;
        if (sender != null) {
            Msg.send(sender, "§aReload complete. §7(\u00A7f" + mobManager.getMobIds().size() + " mobs, " + took + "ms)§r");
        } else {
            getLogger().info("Reload complete (" + mobManager.getMobIds().size() + " mobs, " + took + "ms)");
        }
    }

    public AutoUpdate getAutoUpdate() { return autoUpdate; }
    public MobManager getMobManager() { return mobManager; }
    public FactionManager getFactionManager() { return factionManager; }
}
