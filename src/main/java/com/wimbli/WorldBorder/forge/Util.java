package com.wimbli.WorldBorder.forge;

import net.minecraft.command.ICommandSender;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.ChatComponentText;
import net.minecraft.util.IChatComponent;
import net.minecraft.util.StatCollector;
import net.minecraft.world.World;
import net.minecraft.world.WorldServer;

/**
 * Static class for utility functions (syntactic sugar) to help transition from Bukkit
 * to Forge conventions
 */
public class Util
{
    public static String worldName(World world)
    {
        return world.provider.getSaveFolder();
    }

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
     * Broadcasts an auto. translated, formatted encapsulated message to all players
     * @param server Server instance to broadcast to
     * @param msg String or language key to broadcast
     * @param parts Optional objects to add to formattable message
     */
    public static void broadcast(MinecraftServer server, String msg, Object... parts)
    {
        server.getConfigurationManager()
                .sendChatMsg( prepareText(msg, parts) );
    }

    /**
     * Sends an automatically translated, formatted & encapsulated message to a player
     * @param sender Target to send message to
     * @param msg String or language key to broadcast
     * @param parts Optional objects to add to formattable message
     */
    public static void chat(ICommandSender sender, String msg, Object... parts)
    {
        sender.addChatMessage( prepareText(msg, parts) );
    }

    private static IChatComponent prepareText(String msg, Object... parts)
    {
        String translated = translate(msg);
        String formatted  = String.format(translated, parts);

        return new ChatComponentText(formatted);
    }
}