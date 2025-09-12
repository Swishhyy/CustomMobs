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
import me.swishhyy.customMobs.util.CM;
import me.swishhyy.customMobs.util.Msg;

public class SpawnMobCommand implements CommandExecutor, TabCompleter {
    private final MobManager mobManager;
    private final CustomMobs plugin;

    public SpawnMobCommand(CustomMobs plugin, MobManager mobManager) {
        this.plugin = plugin;
        this.mobManager = mobManager;
    }

    private boolean noPerm(CommandSender sender) {
        if (Msg.isAdmin(sender)) return false;
        Msg.send(sender, "§cNo permission.");
        return true;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (args.length == 1 && args[0].equalsIgnoreCase("reload")) {
            if (noPerm(sender)) return true;
            plugin.reloadConfig();
            mobManager.reloadAll();
            if (plugin.getAutoUpdate() != null) plugin.getAutoUpdate().startOrSchedule();
            Msg.send(sender, "§aReload complete. Loaded §f" + mobManager.getMobIds().size() + "§a mobs.");
            return true;
        }

        if (args.length == 1 && args[0].equalsIgnoreCase("update")) {
            if (noPerm(sender)) return true;
            if (plugin.getAutoUpdate() == null) { Msg.send(sender, "§cAuto-update module not available."); return true; }
            plugin.getAutoUpdate().manualCheck(sender);
            return true;
        }

        if (!(sender instanceof Player)) { Msg.send(sender, "§cOnly players can use this command."); return true; }
        if (noPerm(sender)) return true;
        if (args.length < 1) { Msg.send(sender, "§7Usage: /" + label + " <mobId>|reload|update"); return true; }
        Player p = (Player) sender;
        Location loc = p.getLocation();
        String query = String.join(" ", args);
        if (mobManager.spawnResolved(query, loc) != null) {
            Msg.send(sender, "§aSpawned mob: §f" + query);
        } else {
            Msg.send(sender, "§cUnknown mob: §f" + query + "§7 (try id or display name)");
        }
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> out = new ArrayList<>();
        if (args.length == 1) {
            String partial = args[0].toLowerCase(Locale.ROOT);
            if ("reload".startsWith(partial) && sender.hasPermission(CM.PERM_ADMIN)) out.add("reload");
            if ("update".startsWith(partial) && sender.hasPermission(CM.PERM_ADMIN)) out.add("update");
            for (String id : mobManager.getMobIds()) if (id.startsWith(partial)) out.add(id);
        }
        return out;
    }
}
