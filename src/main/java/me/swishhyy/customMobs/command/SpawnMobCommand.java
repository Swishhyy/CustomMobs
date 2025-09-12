package me.swishhyy.customMobs.command;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import org.bukkit.Location;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import me.swishhyy.customMobs.CustomMobs;
import me.swishhyy.customMobs.mob.MobManager;

public class SpawnMobCommand implements CommandExecutor, TabCompleter {
    private final MobManager mobManager;
    private final CustomMobs plugin;

    public SpawnMobCommand(CustomMobs plugin, MobManager mobManager) {
        this.plugin = plugin;
        this.mobManager = mobManager;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (args.length == 1 && args[0].equalsIgnoreCase("reload")) {
            if (!sender.hasPermission("custommobs.admin")) {
                sender.sendMessage("No permission.");
                return true;
            }
            plugin.reloadConfig();
            mobManager.reloadAll();
            if (plugin.getAutoUpdate() != null) plugin.getAutoUpdate().startOrSchedule();
            sender.sendMessage("CustomMobs: Reload complete. Loaded " + mobManager.getMobIds().size() + " mobs.");
            return true;
        }

        if (args.length == 1 && args[0].equalsIgnoreCase("update")) {
            if (!sender.hasPermission("custommobs.admin")) {
                sender.sendMessage("No permission.");
                return true;
            }
            if (plugin.getAutoUpdate() == null) {
                sender.sendMessage("Auto-update module not available.");
                return true;
            }
            plugin.getAutoUpdate().manualCheck(sender);
            return true;
        }

        if (!(sender instanceof Player)) {
            sender.sendMessage("Only players can use this command.");
            return true;
        }
        if (!sender.hasPermission("custommobs.admin")) {
            sender.sendMessage("No permission.");
            return true;
        }
        if (args.length < 1) {
            sender.sendMessage("Usage: /" + label + " <mobId>|reload");
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

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> out = new ArrayList<>();
        if (args.length == 1) {
            String partial = args[0].toLowerCase(Locale.ROOT);
            if ("reload".startsWith(partial) && sender.hasPermission("custommobs.admin")) {
                out.add("reload");
            }
            if ("update".startsWith(partial) && sender.hasPermission("custommobs.admin")) out.add("update");
            for (String id : mobManager.getMobIds()) {
                if (id.startsWith(partial)) out.add(id);
            }
        }
        return out;
    }
}
