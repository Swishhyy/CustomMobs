package me.swishhyy.customMobs;

import org.bukkit.plugin.java.JavaPlugin;

import me.swishhyy.customMobs.mob.MobManager;
import me.swishhyy.customMobs.listener.MobListener;
import me.swishhyy.customMobs.command.SpawnMobCommand;

public final class CustomMobs extends JavaPlugin {

    private MobManager mobManager;

    @Override
    public void onEnable() {
        mobManager = new MobManager(this);
        mobManager.reloadAll();
        getServer().getPluginManager().registerEvents(new MobListener(mobManager), this);
        if (getCommand("cmspawn") != null) {
            SpawnMobCommand cmd = new SpawnMobCommand(mobManager);
            getCommand("cmspawn").setExecutor(cmd);
            getCommand("cmspawn").setTabCompleter(cmd);
        }
    }

    @Override
    public void onDisable() {
        // Plugin shutdown logic
    }
}
