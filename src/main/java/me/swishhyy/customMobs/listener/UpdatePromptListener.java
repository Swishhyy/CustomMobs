package me.swishhyy.customMobs.listener;

import me.swishhyy.customMobs.CustomMobs;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;

public class UpdatePromptListener implements Listener {
    private final CustomMobs plugin;
    public UpdatePromptListener(CustomMobs plugin) { this.plugin = plugin; }

    @SuppressWarnings("deprecation")
    @EventHandler
    public void onChat(AsyncPlayerChatEvent e) {
        if (plugin.handleUpdatePromptChat(e.getPlayer(), e.getMessage())) {
            e.setCancelled(true);
        }
    }
}
