package me.swishhyy.customMobs;

import org.bukkit.plugin.java.JavaPlugin;

import me.swishhyy.customMobs.mob.MobManager;
import me.swishhyy.customMobs.listener.MobListener;
import me.swishhyy.customMobs.spawn.NaturalSpawnListener;
import me.swishhyy.customMobs.command.SpawnMobCommand;
import me.swishhyy.customMobs.update.AutoUpdate;
import me.swishhyy.customMobs.util.BannerUtil;

public final class CustomMobs extends JavaPlugin {

    private MobManager mobManager;
    private AutoUpdate autoUpdate;

    @Override
    public void onEnable() {
        BannerUtil.printStartupBanner(this);
        saveDefaultConfig();
        mobManager = new MobManager(this);
        mobManager.reloadAll();
        autoUpdate = new AutoUpdate(this);
        autoUpdate.startOrSchedule();
        getServer().getPluginManager().registerEvents(new MobListener(mobManager), this);
        getServer().getPluginManager().registerEvents(new NaturalSpawnListener(mobManager), this);
        if (getCommand("cmspawn") != null) {
            SpawnMobCommand cmd = new SpawnMobCommand(this, mobManager);
            getCommand("cmspawn").setExecutor(cmd);
            getCommand("cmspawn").setTabCompleter(cmd);
        }
    }

    @Override
    public void onDisable() {
        if (autoUpdate != null) autoUpdate.shutdown();
    }

    public AutoUpdate getAutoUpdate() { return autoUpdate; }
    public MobManager getMobManager() { return mobManager; }
}
