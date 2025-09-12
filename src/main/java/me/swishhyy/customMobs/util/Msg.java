package me.swishhyy.customMobs.util;

import org.bukkit.command.CommandSender;

public final class Msg {
    private Msg() {}
    public static final String PREFIX = CM.CHAT_PREFIX; // includes color formatting already

    public static void send(CommandSender sender, String message) {
        sender.sendMessage(PREFIX + message);
    }

    public static boolean isAdmin(CommandSender sender) {
        return sender.hasPermission(CM.PERM_ADMIN);
    }
}

