package com.wimbli.WorldBorder;

import cpw.mods.fml.common.FMLCommonHandler;
import cpw.mods.fml.common.Mod;
import cpw.mods.fml.common.event.FMLPreInitializationEvent;
import cpw.mods.fml.common.event.FMLServerStartingEvent;
import cpw.mods.fml.common.event.FMLServerStoppingEvent;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import net.minecraft.server.MinecraftServer;
import net.minecraftforge.common.MinecraftForge;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Main class and mod definition of WorldBorder-Forge. Holds static references to its
 * singleton instance and management objects. Should only ever be created by Forge.
 */
@Mod(
    modid   = WorldBorder.MODID,
    name    = WorldBorder.MODID,
    version = WorldBorder.VERSION,

    acceptableRemoteVersions = "*",
    acceptableSaveVersions   = ""
)
public class WorldBorder
{
    public static final String VERSION = "1.8.4";
    public static final String MODID   = "WorldBorder";
    public static final Logger LOGGER  = LogManager.getFormatterLogger(MODID);

    /** Singleton instance of WorldBorder, created by Forge */
    public static WorldBorder     INSTANCE = null;
    /** Shortcut reference to vanilla server instance */
    public static MinecraftServer SERVER   = null;
    /** Singleton instance of WorldBorder's command handler */
    public static WBCommand       COMMAND  = null;
    /** Singleton instance of WorldBorder's event handler */
    public static WBListener      LISTENER = null;

    private BlockPlaceListener blockPlaceListener = null;
    private MobSpawnListener   mobSpawnListener   = null;

    /**
     * Given WorldBorder's dependency on dedicated server classes and is designed for
     * use in multiplayer environments, we don't load anything on the client
     */
    @Mod.EventHandler
    @SideOnly(Side.CLIENT)
    public void clientPreInit(FMLPreInitializationEvent event)
    {
        LOGGER.error("This mod is intended only for use on servers");
        LOGGER.error("Please consider removing this mod from your installation");
    }

    @Mod.EventHandler
    @SideOnly(Side.SERVER)
    public void serverPreInit(FMLPreInitializationEvent event)
    {
        Config.setupConfigDir(event.getModConfigurationDirectory());
    }

    @Mod.EventHandler
    @SideOnly(Side.SERVER)
    public void serverStart(FMLServerStartingEvent event)
    {
        if (INSTANCE  == null) INSTANCE = this;
        if (SERVER    == null) SERVER   = event.getServer();
        if (COMMAND   == null) COMMAND  = new WBCommand();
        if (LISTENER  == null) LISTENER = new WBListener();

        // Load (or create new) config files
        Config.load(false);

        // our one real command, though it does also have aliases "wb" and "worldborder"
        event.registerServerCommand(COMMAND);

        // keep an eye on teleports, to redirect them to a spot inside the border if necessary
        FMLCommonHandler.instance().bus().register(LISTENER);
        MinecraftForge.EVENT_BUS.register(LISTENER);

        if (Config.preventBlockPlace())
            enableBlockPlaceListener(true);

        if (Config.preventMobSpawn())
            enableMobSpawnListener(true);

        // integrate with DynMap if it becomes available
        DynMapFeatures.registerListener();
    }

    @Mod.EventHandler
    @SideOnly(Side.SERVER)
    public void serverStop(FMLServerStoppingEvent event)
    {
        DynMapFeatures.removeAllBorders();
        Config.storeFillTask();
    }

    // for other plugins to hook into
    // TODO: use IMC for this?
    @SideOnly(Side.SERVER)
    public BorderData getWorldBorder(String worldName)
    {
        return Config.Border(worldName);
    }

    @SideOnly(Side.SERVER)
    public void enableBlockPlaceListener(boolean enable)
    {
        if (enable)
            MinecraftForge.EVENT_BUS.register(this.blockPlaceListener = new BlockPlaceListener());
        else if (blockPlaceListener != null)
            MinecraftForge.EVENT_BUS.unregister(this.blockPlaceListener);
    }

    @SideOnly(Side.SERVER)
    public void enableMobSpawnListener(boolean enable)
    {
        if (enable)
            MinecraftForge.EVENT_BUS.register(this.mobSpawnListener = new MobSpawnListener());
        else if (mobSpawnListener != null)
            MinecraftForge.EVENT_BUS.unregister(this.mobSpawnListener);
    }
}
