package com.wimbli.WorldBorder.forge;

import net.minecraft.command.ICommandSender;
import net.minecraft.server.dedicated.DedicatedServer;
import net.minecraft.util.ChatComponentText;
import net.minecraft.util.StatCollector;

/** Static utility class for shortcut methods to help transition from Bukkit to Forge */
public class Util
{
    /**
     * Attempts to a translate a given string/key using the local language, and then
     * using the fallback language.
     * @param msg String or language key to translate
     * @return Translated or same string
     */
    public static String translate(String msg)
    {
        return StatCollector.canTranslate(msg)
            ? StatCollector.translateToLocal(msg)
            : StatCollector.translateToFallback(msg);
    }

    /**
     * Sends an automatically translated & encapsulated message to a player
     * @param sender Target to send message to
     * @param msg String or language key to broadcast
     */
    public static void chat(ICommandSender sender, String msg)
    {
        String translated = translate(msg);

        // Consoles require ANSI coloring for formatting
        if (sender instanceof DedicatedServer)
            translated = removeFormatting(translated);

        sender.addChatMessage( new ChatComponentText(translated) );
    }

    /** Replaces Bukkit-convention amp format tokens with vanilla ones */
    public static String replaceAmpColors(String message)
    {
        return message.replaceAll("(?i)&([a-fk-or0-9])", "§$1");
    }

    /** Strips vanilla formatting from a string */
    public static String removeFormatting(String message)
    {
        return message.replaceAll("(?i)§[a-fk-or0-9]", "");
    }

    /** Shortcut for java.lang.System.currentTimeMillis */
    public static long now()
    {
        return System.currentTimeMillis();
    }
}