package com.wimbli.WorldBorder.forge;

import com.wimbli.WorldBorder.WorldBorder;
import net.minecraft.block.Block;
import net.minecraft.command.ICommandSender;
import net.minecraft.server.dedicated.DedicatedServer;
import net.minecraft.util.ChatComponentText;
import net.minecraft.util.StatCollector;
import net.minecraft.world.MinecraftException;
import net.minecraft.world.World;
import net.minecraft.world.WorldProvider;
import net.minecraft.world.WorldServer;
import net.minecraftforge.common.DimensionManager;

/**
 * Static class for utility functions (syntactic sugar) to help transition from Bukkit
 * to Forge conventions
 * TODO: Move all world stuff to its own class
 */
public class Util
{
    /**
     * Generates a name from a given world.
     *
     * Instead of using internal folder names, this uses the Dynmap method of using the
     * dimension number with the 'DIM' prefix. This improves compatibility with Dynmap
     * and makes world handling provider-agnostic.
     */
    public static String getWorldName(World world)
    {
        // Dimension 0 will always use the name configured in server.properties
        return (world.provider.dimensionId == 0)
            ? world.getWorldInfo().getWorldName()
            : "DIM" + world.provider.dimensionId;
    }

    /**
     * Performs a case-sensitive search for a loaded world by a given name.
     *
     * First, it tries to match the name with dimension 0 (overworld), then it tries to
     * match from the world's save folder name (e.g. DIM_MYST10) and then finally the
     * Dynmap compatible identifier (e.g. DIM10)
     *
     * @param name Name of world to find
     * @return World if found, else null
     */
    public static WorldServer getWorld(String name)
    {
        if ( name == null || name.isEmpty() )
            throw new IllegalArgumentException("World name cannot be empty or null");

        for ( WorldServer world : DimensionManager.getWorlds() )
        {
            String dimName    = "DIM" + world.provider.dimensionId;
            String saveFolder = world.provider.getSaveFolder();

            if (world.provider.dimensionId == 0)
            {   // Special case for dimension 0 (overworld)
                if ( WorldBorder.SERVER.getFolderName().equals(name) )
                    return world;
            }
            else if ( saveFolder.equals(name) || dimName.equals(name) )
                return world;
        }

        return null;
    }

    /**
     * For debugging only; bad design, uses internal Forge APIs
     * TODO: remove when done
     */
    public static void getWorlds()
    {
        Log.debug("## Loaded worlds:");
        for ( WorldServer world : DimensionManager.getWorlds() )
        {
            WorldProvider provider = world.provider;
            Log.debug(
                "Found loaded dimension #%d `%s` by provider %s, save: %s",
                provider.dimensionId,
                provider.getDimensionName(),
                provider.getClass().getSimpleName(),
                provider.getSaveFolder()
            );
        }
    }

    /** Safely saves a given world to disk */
    public static void saveWorld(WorldServer world)
    {
        try
        {
            Boolean saveFlag  = world.levelSaving;
            world.levelSaving = false;
            world.saveAllChunks(true, null);
            world.levelSaving = saveFlag;
        }
        catch (MinecraftException e)
        {
            e.printStackTrace();
        }
    }

    /** Gets the ID of the block type of the given block position in a world */
    public static int getBlockID(World world, int x, int y, int z)
    {
        return Block.getIdFromBlock( world.getBlock(x, y, z) );
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

    public static String replaceAmpColors(String message)
    {
        return message.replaceAll("(?i)&([a-fk-or0-9])", "\u00a7$1");
    }

    public static String removeFormatting(String message)
    {
        return message.replaceAll("(?i)\u00a7[a-fk-or0-9]", "");
    }

    /** Shortcut for java.lang.System.currentTimeMillis */
    public static long now()
    {
        return System.currentTimeMillis();
    }
}