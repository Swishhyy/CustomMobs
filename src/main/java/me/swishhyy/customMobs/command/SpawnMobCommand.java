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

    private void sendRootUsage(CommandSender sender) {
        Msg.send(sender, "§7Usage: §f/cm reload | update | updatebeta | spawn <mobId>");
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (args.length == 0) { sendRootUsage(sender); return true; }
        String sub = args[0].toLowerCase(Locale.ROOT);

        switch (sub) {
            case "reload" -> {
                if (noPerm(sender)) return true;
                plugin.fullReload(sender);
                return true;
            }
            case "update" -> {
                if (noPerm(sender)) return true;
                if (plugin.getAutoUpdate() == null) { Msg.send(sender, "§cAuto-update module not available."); return true; }
                plugin.getAutoUpdate().manualCheck(sender);
                return true;
            }
            case "updatebeta" -> {
                if (noPerm(sender)) return true;
                if (plugin.getAutoUpdate() == null) { Msg.send(sender, "§cAuto-update module not available."); return true; }
                plugin.getAutoUpdate().manualCheckBeta(sender);
                return true;
            }
            case "spawn" -> {
                if (!(sender instanceof Player)) { Msg.send(sender, "§cOnly players can spawn mobs."); return true; }
                if (noPerm(sender)) return true;
                if (args.length < 2) { Msg.send(sender, "§cUsage: §f/cm spawn <mobId>"); return true; }
                String mobId = args[1];
                Player p = (Player) sender;
                Location loc = p.getLocation();
                if (mobManager.spawnResolved(mobId, loc) != null) {
                    Msg.send(sender, "§aSpawned mob: §f" + mobId);
                } else {
                    Msg.send(sender, "§cUnknown mob: §f" + mobId);
                }
                return true;
            }
            default -> {
                sendRootUsage(sender);
                return true;
            }
        }
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> out = new ArrayList<>();
        if (args.length == 1) {
            String partial = args[0].toLowerCase(Locale.ROOT);
            if ("reload".startsWith(partial) && sender.hasPermission(CM.PERM_ADMIN)) out.add("reload");
            if ("update".startsWith(partial) && sender.hasPermission(CM.PERM_ADMIN)) out.add("update");
            if ("updatebeta".startsWith(partial) && sender.hasPermission(CM.PERM_ADMIN)) out.add("updatebeta");
            if ("spawn".startsWith(partial) && sender.hasPermission(CM.PERM_ADMIN)) out.add("spawn");
        } else if (args.length == 2 && args[0].equalsIgnoreCase("spawn")) {
            String partial = args[1].toLowerCase(Locale.ROOT);
            for (String id : mobManager.getMobIds()) if (id.startsWith(partial)) out.add(id);
        }
        return out;
    }
}
