package me.swishhyy.customMobs;

import org.bukkit.plugin.java.JavaPlugin;


import me.swishhyy.customMobs.mob.MobManager;
import me.swishhyy.customMobs.listener.MobListener;
import me.swishhyy.customMobs.command.SpawnMobCommand;

public final class CustomMobs extends JavaPlugin {

    private MobManager mobManager;

    @Override
    public void onEnable() {
        // Plugin startup logic
        mobManager = new MobManager(this);
        mobManager.loadMobConfigs("custom");
        mobManager.loadMobConfigs("vanilla");
        // Wire listeners and commands
        getServer().getPluginManager().registerEvents(new MobListener(mobManager), this);
        if (getCommand("cmspawn") != null) {
            getCommand("cmspawn").setExecutor(new SpawnMobCommand(mobManager));
        }
    }

    @Override
    public void onDisable() {
        // Plugin shutdown logic
    }
}
