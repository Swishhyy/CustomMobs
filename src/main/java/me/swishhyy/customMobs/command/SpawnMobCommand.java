package me.swishhyy.customMobs.command;

import org.bukkit.Location;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import me.swishhyy.customMobs.mob.MobManager;

public class SpawnMobCommand implements CommandExecutor {
    private final MobManager mobManager;

    public SpawnMobCommand(MobManager mobManager) {
        this.mobManager = mobManager;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("Only players can use this command.");
            return true;
        }
        if (args.length < 1) {
            sender.sendMessage("Usage: /" + label + " <mobId>");
            return true;
        }
        Player p = (Player) sender;
        Location loc = p.getLocation();
        String query = String.join(" ", args);
        if (mobManager.spawnResolved(query, loc) != null) {
            sender.sendMessage("Spawned mob: " + query);
        } else {
            sender.sendMessage("Unknown mob: " + query + " (try id or display name)");
        }
        return true;
    }
}
