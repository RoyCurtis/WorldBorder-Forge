package com.wimbli.WorldBorder;

import com.wimbli.WorldBorder.forge.Scheduler;
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

import java.io.File;


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

    public static volatile MinecraftServer server = null;
    public static volatile Scheduler scheduler = null;
    public static volatile WorldBorder plugin = null;
    public static volatile WBCommand wbCommand = null;
    public static volatile WBListener wbListener = null;
	private BlockPlaceListener blockPlaceListener = null;
	private MobSpawnListener mobSpawnListener = null;
    private File configDir;

    @Mod.EventHandler
    @SideOnly(Side.CLIENT)
    public void clientPreInit(FMLPreInitializationEvent event)
    {
        LOGGER.error("This mod is intended only for use on servers.");
        LOGGER.error("Please consider removing this mod from your installation.");
    }

    @Mod.EventHandler
    @SideOnly(Side.SERVER)
    public void serverPreInit(FMLPreInitializationEvent event)
    {
        configDir = event.getModConfigurationDirectory();

        if ( !configDir.exists() && configDir.mkdirs() )
            LOGGER.info("Created config directory for the first time");
    }

    @Mod.EventHandler
    @SideOnly(Side.SERVER)
    public void serverStart(FMLServerStartingEvent event)
	{
        if (server == null)
            server = MinecraftServer.getServer();
        if (scheduler == null)
            scheduler = new Scheduler();
		if (plugin == null)
			plugin = this;
        if (wbCommand == null)
            wbCommand = new WBCommand();
        if (wbListener == null)
            wbListener = new WBListener();

		// Load (or create new) config files
		Config.load(this, configDir, false);

		// our one real command, though it does also have aliases "wb" and "worldborder"
        event.registerServerCommand(wbCommand);

        // register scheduler as tick handler
        FMLCommonHandler.instance().bus().register(scheduler);

		// keep an eye on teleports, to redirect them to a spot inside the border if necessary
        FMLCommonHandler.instance().bus().register(wbListener);
        MinecraftForge.EVENT_BUS.register(wbListener);
		
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
        DynMapFeatures.unregisterListener();
		Config.StopBorderTimer();
		Config.StoreFillTask();
		Config.StopFillTask();
	}

	// for other plugins to hook into
	public BorderData getWorldBorder(String worldName)
	{
		return Config.Border(worldName);
	}

	public void enableBlockPlaceListener(boolean enable)
	{
		if (enable)
            MinecraftForge.EVENT_BUS.register(this.blockPlaceListener = new BlockPlaceListener());
		else if (blockPlaceListener != null)
			blockPlaceListener.unregister();
	}

	public void enableMobSpawnListener(boolean enable)
	{
		if (enable)
            MinecraftForge.EVENT_BUS.register(this.mobSpawnListener = new MobSpawnListener());
		else if (mobSpawnListener != null)
			mobSpawnListener.unregister();
	}
}
