package com.wimbli.WorldBorder;

import com.mojang.realmsclient.gui.ChatFormatting;
import com.wimbli.WorldBorder.forge.Configuration;
import com.wimbli.WorldBorder.task.BorderCheckTask;
import com.wimbli.WorldBorder.task.WorldFillTask;
import org.apache.logging.log4j.Level;

import java.io.File;
import java.text.DecimalFormat;
import java.util.*;
import java.util.Map.Entry;

/**
 * Static class for holding, loading and saving global and per-border data
 */
public class Config
{
    public static final DecimalFormat COORD_FORMAT = new DecimalFormat("0.0");

    private static final String MAIN_CAT = "general";
    private static final String FILL_CAT = "fillTask";

    // TODO: move these elsewhere?
    public static BorderCheckTask borderTask = null;

    private static File          configDir;
    private static Configuration cfgMain;
    private static Configuration cfgBorders;

    // actual configuration values which can be changed
    private static Map<String, BorderData> borders = Collections.synchronizedMap(new LinkedHashMap<String, BorderData>());

    /** Knockback message without formatting */
    private static String    message;
    /** Knockback message with formatting */
    private static String    messageFmt;
    private static String    dynmapMessage;
    private static Set<UUID> bypassPlayers     = Collections.synchronizedSet(new LinkedHashSet<UUID>());
    private static boolean   debugMode         = false;
    private static boolean   shapeRound        = true;
	private static float     knockBack         = 3.0F;
	private static int       timerTicks        = 20;
	private static boolean   whooshEffect      = true;
	private static boolean   dynmapEnable      = true;
    private static boolean   remount           = true;
    private static boolean   killPlayer        = false;
    private static boolean   portalRedirection = true;
    private static boolean   denyEnderpearl    = false;
	private static boolean   preventBlockPlace = false;
    private static boolean   preventMobSpawn   = false;

    private static int fillAutosaveFrequency = 30;
    private static int fillMemoryTolerance   = 500;

    public static void setupConfigDir(File globalDir)
    {
        configDir = new File(globalDir, WorldBorder.MODID);

        if ( !configDir.exists() && configDir.mkdirs() )
            log("Created config directory for the first time");
    }

	public static void setBorder(String world, BorderData border, boolean logIt)
	{
		borders.put(world, border);
		if (logIt)
			log("Border set. " + BorderDescription(world));
		save(true);
		DynMapFeatures.showBorder(world, border);
	}

	public static void setBorder(String world, int radiusX, int radiusZ, double x, double z)
	{
		BorderData old = Border(world);
		Boolean oldShape = (old == null) ? null : old.getShape();
		boolean oldWrap = (old != null) && old.getWrapping();
		setBorder(world, new BorderData(x, z, radiusX, radiusZ, oldShape, oldWrap), true);
	}

	// set border based on corner coordinates
	public static void setBorderCorners(String world, double x1, double z1, double x2, double z2, Boolean shapeRound, boolean wrap)
	{
		double radiusX = Math.abs(x1 - x2) / 2;
		double radiusZ = Math.abs(z1 - z2) / 2;
		double x = ((x1 < x2) ? x1 : x2) + radiusX;
		double z = ((z1 < z2) ? z1 : z2) + radiusZ;
		setBorder(world, new BorderData(x, z, (int) Math.round(radiusX), (int) Math.round(radiusZ), shapeRound, wrap), true);
	}

	public static void setBorderCorners(String world, double x1, double z1, double x2, double z2)
	{
		BorderData old = Border(world);
		Boolean oldShape = (old == null) ? null : old.getShape();
		boolean oldWrap = (old != null) && old.getWrapping();
		setBorderCorners(world, x1, z1, x2, z2, oldShape, oldWrap);
	}

	public static void removeBorder(String world)
	{
		borders.remove(world);
		log("Removed border for world \"" + world + "\".");
		save(true);
		DynMapFeatures.removeBorder(world);
	}

	public static void removeAllBorders()
	{
		borders.clear();
		log("Removed all borders for all worlds.");
		save(true);
		DynMapFeatures.removeAllBorders();
	}

	public static String BorderDescription(String world)
	{
		BorderData border = borders.get(world);

        return border == null
            ? "No border was found for the world \"" + world + "\"."
            : "World \"" + world + "\" has border " + border.toString();
	}

	public static Set<String> BorderDescriptions()
	{
		Set<String> output = new HashSet<String>();

		for (String worldName : borders.keySet())
			output.add(BorderDescription(worldName));

		return output;
	}

	public static BorderData Border(String world)
	{
		return borders.get(world);
	}

	public static Map<String, BorderData> getBorders()
	{
		return new LinkedHashMap<String, BorderData>(borders);
	}

	public static void setMessage(String msg)
	{
		updateMessage(msg);
		save(true);
	}

	public static void updateMessage(String msg)
	{
		message = msg;
		messageFmt = replaceAmpColors(msg);
	}

	public static String getMessage()
	{
		return messageFmt;
	}

	public static String getMessageRaw()
	{
		return message;
	}

	public static void setShape(boolean round)
	{
		shapeRound = round;
		log("Set default border shape to " + (getShapeName()) + ".");
		save(true);
		DynMapFeatures.showAllBorders();
	}

	public static boolean getShapeRound()
	{
		return shapeRound;
	}

	public static String getShapeName()
	{
		return getShapeName(shapeRound);
	}

	public static String getShapeName(Boolean round)
	{
		if (round == null)
			return "default";
		return round ? "elliptic/round" : "rectangular/square";
	}

	public static void setDebugMode(boolean mode)
	{
		debugMode = mode;
		log("Debug mode " + (debugMode ? "enabled" : "disabled") + ".");
		save(true);
	}

	public static boolean isDebugMode()
	{
		return debugMode;
	}

	public static void setWhooshEffect(boolean enable)
	{
		whooshEffect = enable;
		log("\"Whoosh\" knockback effect " + (enable ? "enabled" : "disabled") + ".");
		save(true);
	}

	public static boolean doWhooshEffect()
	{
		return whooshEffect;
	}

	public static void setPreventBlockPlace(boolean enable)
	{
		if (preventBlockPlace != enable)
			WorldBorder.INSTANCE.enableBlockPlaceListener(enable);

		preventBlockPlace = enable;
		log("Prevent block place " + (enable ? "enabled" : "disabled") + ".");
		save(true);
	}

	public static void setPreventMobSpawn(boolean enable)
	{
		if (preventMobSpawn != enable)
			WorldBorder.INSTANCE.enableMobSpawnListener(enable);

		preventMobSpawn = enable;
		log("Prevent mob spawn " + (enable ? "enabled" : "disabled") + ".");
		save(true);
	}

	public static boolean preventBlockPlace()
	{
		return preventBlockPlace;
	}

	public static boolean preventMobSpawn()
	{
		return preventMobSpawn;
	}

	public static boolean doPlayerKill()
	{
		return killPlayer;
	}

	public static boolean getDenyEnderpearl()
	{
		return denyEnderpearl;
	}

	public static void setDenyEnderpearl(boolean enable)
	{
		denyEnderpearl = enable;
		log("Direct cancellation of ender pearls thrown past the border " + (enable ? "enabled" : "disabled") + ".");
		save(true);
	}

	public static void setPortalRedirection(boolean enable)
	{
		portalRedirection = enable;
		log("Portal redirection " + (enable ? "enabled" : "disabled") + ".");
		save(true);
	}

	public static boolean doPortalRedirection()
	{
		return portalRedirection;
	}

	public static void setKnockBack(float numBlocks)
	{
		knockBack = numBlocks;
		log("Knockback set to " + knockBack + " blocks inside the border.");
		save(true);
	}

	public static double getKnockBack()
	{
		return knockBack;
	}

	public static void setTimerTicks(int ticks)
	{
		timerTicks = ticks;
		log("Timer delay set to " + timerTicks + " tick(s). That is roughly " + (timerTicks * 50) + "ms / " + (((double)timerTicks * 50.0) / 1000.0) + " seconds.");
		startBorderTimer();
		save(true);
	}

	public static int getTimerTicks()
	{
		return timerTicks;
	}

	public static void setRemount(boolean enable)
	{
		remount = enable;
		if (remount)
			log("Remount is now enabled. Players will be remounted on their vehicle when knocked back");
		else
            log("Remount is now disabled. Players will be left dismounted when knocked back from the border while on a vehicle.");

		save(true);
	}

	public static boolean getRemount()
	{
		return remount;
	}

	public static void setFillAutosaveFrequency(int seconds)
	{
		fillAutosaveFrequency = seconds;
		if (fillAutosaveFrequency == 0)
			log("World autosave frequency during Fill process set to 0, disabling it. Note that much progress can be lost this way if there is a bug or crash in the world generation process from Bukkit or any world generation plugin you use.");
		else
			log("World autosave frequency during Fill process set to " + fillAutosaveFrequency + " seconds (rounded to a multiple of 5). New chunks generated by the Fill process will be forcibly saved to disk this often to prevent loss of progress due to bugs or crashes in the world generation process.");
		save(true);
	}

	public static int getFillAutosaveFrequency()
	{
		return fillAutosaveFrequency;
	}


	public static void setDynmapBorderEnabled(boolean enable)
	{
		dynmapEnable = enable;
		log("DynMap border display is now " + (enable ? "enabled" : "disabled") + ".");
		save(true);
		DynMapFeatures.showAllBorders();
	}

	public static boolean isDynmapBorderEnabled()
	{
		return dynmapEnable;
	}

	public static void setDynmapMessage(String msg)
	{
		dynmapMessage = msg;
		log("DynMap border label is now set to: " + msg);
		save(true);
		DynMapFeatures.showAllBorders();
	}

	public static String getDynmapMessage()
	{
		return dynmapMessage;
	}

	public static void setPlayerBypass(UUID player, boolean bypass)
	{
		if (bypass)
			bypassPlayers.add(player);
		else
			bypassPlayers.remove(player);
		save(true);
	}

	public static boolean isPlayerBypassing(UUID player)
	{
		return bypassPlayers.contains(player);
	}

	public static UUID[] getPlayerBypassList()
	{
		return (UUID[]) bypassPlayers.toArray();
	}

	// for converting bypass UUID list to/from String list, for storage in config
	private static void importBypassStringList(String[] strings)
	{
		for (String string : strings)
		{
			bypassPlayers.add(UUID.fromString(string));
		}
	}

	private static String[] exportBypassStringList()
	{
		ArrayList<String> strings = new ArrayList<>();

		for (UUID uuid : bypassPlayers)
			strings.add( uuid.toString() );

		return strings.toArray(new String[strings.size()]);
	}

	public static void startBorderTimer()
	{
		if (borderTask == null)
            borderTask = new BorderCheckTask();

        borderTask.setRunning(true);
		logConfig("Border-checking timed task started.");
	}

	public static void stopBorderTimer()
	{
        if (borderTask == null)
            return;

        borderTask.setRunning(false);
        logConfig("Border-checking timed task stopped.");
	}

	public static void storeFillTask()
	{
		save(false, true);
	}

	public static void deleteFillTask()
	{
		save(false);
	}

	public static void restoreFillTask(String world, int fillDistance, int chunksPerRun, int tickFrequency, int x, int z, int length, int total, boolean forceLoad)
	{
        if (WorldFillTask.getInstance() != null)
            throw new IllegalArgumentException("Tried to restore fill task when one is already running");

        try
        {
            WorldFillTask task = new WorldFillTask(WorldBorder.SERVER, world, forceLoad, fillDistance, chunksPerRun, tickFrequency);
            task.startFrom(x, z, length, total);
        }
		catch (Exception e)
        {
            logWarn("Could not resume fill task: " + e.getMessage());
        }
	}

	public static long getAvailableMemory()
	{
        Runtime rt = Runtime.getRuntime();
        // 1024*1024 = 1048576 (bytes in 1 MB)
		return (rt.maxMemory() - rt.totalMemory() + rt.freeMemory()) / 1048576;
	}

	public static boolean isAvailableMemoryTooLow()
	{
		return getAvailableMemory() < fillMemoryTolerance;
	}

	public static String replaceAmpColors (String message)
	{
        return message.replaceAll("(?i)&([a-fk-or0-9])", ChatFormatting.PREFIX_CODE + "$1");
    }

	public static void log(Level lvl, String text)
	{
		WorldBorder.LOGGER.log(lvl, text);
	}
	public static void log(String text)
	{
		log(Level.INFO, text);
	}
	public static void logWarn(String text)
	{
		log(Level.WARN, text);
	}
	public static void logConfig(String text)
	{
		log(Level.INFO, "[CONFIG] " + text);
	}

	private static final int currentCfgVersion = 11;

	public static void load(boolean logIt)
	{
        if (cfgMain == null)
            cfgMain = new Configuration( new File(configDir, "main.cfg") );
        else cfgMain.load();

        if (cfgBorders == null)
            cfgBorders = new Configuration( new File(configDir, "borders.cfg") );
        else cfgBorders.load();

		int cfgVersion = cfgMain.getInt(MAIN_CAT, "cfg-version", currentCfgVersion);

		String msg = cfgMain.getString(MAIN_CAT, "message", "");
        importBypassStringList(cfgMain.getStringList(MAIN_CAT, "bypass-list-uuids"));
        debugMode         = cfgMain.getBoolean(MAIN_CAT, "debug-mode", false);
        shapeRound        = cfgMain.getBoolean(MAIN_CAT, "round-border", true);
        whooshEffect      = cfgMain.getBoolean(MAIN_CAT, "whoosh-effect", true);
        portalRedirection = cfgMain.getBoolean(MAIN_CAT, "portal-redirection", true);
        knockBack         = cfgMain.getFloat(MAIN_CAT, "knock-back-dist", 3.0F);
        timerTicks        = cfgMain.getInt(MAIN_CAT, "timer-delay-ticks", 20);
        remount           = cfgMain.getBoolean(MAIN_CAT, "remount-on-knockback", true);
        dynmapEnable      = cfgMain.getBoolean(MAIN_CAT, "dynmap-border-enabled", true);
        dynmapMessage     = cfgMain.getString(MAIN_CAT, "dynmap-border-message", "The border of the world.");
        killPlayer        = cfgMain.getBoolean(MAIN_CAT, "player-killed-bad-spawn", false);
        denyEnderpearl    = cfgMain.getBoolean(MAIN_CAT, "deny-enderpearl", true);
        preventBlockPlace = cfgMain.getBoolean(MAIN_CAT, "prevent-block-place", true);
		preventMobSpawn   = cfgMain.getBoolean(MAIN_CAT, "prevent-mob-spawn", true);

        fillAutosaveFrequency = cfgMain.getInt(MAIN_CAT, "fill-autosave-frequency", 30);
        fillMemoryTolerance   = cfgMain.getInt(MAIN_CAT, "fill-memory-tolerance", 500);

        logConfig(
            "Using " + (getShapeName()) + " border, knockback of "
            + knockBack + " blocks, and timer delay of " + timerTicks + "."
        );

        // TODO: move to server setup
        startBorderTimer();

		borders.clear();

		// if empty border message, assume no config
		if (msg == null || msg.isEmpty())
		{	// store defaults
			logConfig("Configuration not present, creating new file.");
			msg = "&cYou have reached the edge of this world.";
			updateMessage(msg);
			save(false);
			return;
		}
		// otherwise just set border message
		else
			updateMessage(msg);

        Set<String> worldNames = cfgBorders.getCategoryNames();

        for(String worldName : worldNames)
        {
            // backwards compatibility for config from before elliptical/rectangular borders were supported
            if (cfgBorders.hasKey(worldName, "radius") && !cfgBorders.hasKey(worldName, "radiusX"))
            {
                int radius = cfgBorders.get(worldName, "radius", 0).getInt();
                cfgBorders.set(worldName, "radiusX", radius);
                cfgBorders.set(worldName, "radiusZ", radius);
            }

            // TODO: make overrideShape nullable again, because WB uses null to determine if
            // override is in effect
            boolean overrideShape = cfgBorders.get(worldName, "shape-round", false).getBoolean();
            boolean wrap = cfgBorders.get(worldName, "wrapping", false).getBoolean();
            BorderData border = new BorderData(
                cfgBorders.get(worldName, "x", 0.0D).getDouble(), cfgBorders.get(worldName, "z", 0.0D).getDouble(),
                cfgBorders.get(worldName, "radiusX", 0).getInt(), cfgBorders.get(worldName, "radiusZ", 0).getInt(),
                overrideShape, wrap
            );
            borders.put(worldName, border);
            logConfig(BorderDescription(worldName));
        }

		// if we have an unfinished fill task stored from a previous run, load it up
		if ( cfgMain.hasCategory(FILL_CAT) )
		{
            // TODO: make these get from category object to provoke NPE
            String  worldName = cfgMain.get(FILL_CAT, "world", "").getString();
            boolean forceLoad = cfgMain.get(FILL_CAT, "forceLoad", false).getBoolean();

            int fillDistance  = cfgMain.get(FILL_CAT, "fillDistance", 176).getInt();
			int chunksPerRun  = cfgMain.get(FILL_CAT, "chunksPerRun", 5).getInt();
			int tickFrequency = cfgMain.get(FILL_CAT, "tickFrequency", 20).getInt();
			int fillX         = cfgMain.get(FILL_CAT, "x", 0).getInt();
			int fillZ         = cfgMain.get(FILL_CAT, "z", 0).getInt();
			int fillLength    = cfgMain.get(FILL_CAT, "length", 0).getInt();
			int fillTotal     = cfgMain.get(FILL_CAT, "total", 0).getInt();

			restoreFillTask(worldName, fillDistance, chunksPerRun, tickFrequency, fillX, fillZ, fillLength, fillTotal, forceLoad);
			save(false);
		}

		if (logIt)
			logConfig("Configuration loaded.");

		if (cfgVersion < currentCfgVersion) save(false);
	}

	public static void save(boolean logIt)
	{
		save(logIt, false);
	}
	public static void save(boolean logIt, boolean storeFillTask)
	{	// save config to file
		if (cfgMain == null) return;

		cfgMain.set(MAIN_CAT, "cfg-version", currentCfgVersion);
		cfgMain.set(MAIN_CAT, "message", message);
		cfgMain.set(MAIN_CAT, "round-border", shapeRound);
		cfgMain.set(MAIN_CAT, "debug-mode", debugMode);
		cfgMain.set(MAIN_CAT, "whoosh-effect", whooshEffect);
		cfgMain.set(MAIN_CAT, "portal-redirection", portalRedirection);
		cfgMain.set(MAIN_CAT, "knock-back-dist", knockBack);
		cfgMain.set(MAIN_CAT, "timer-delay-ticks", timerTicks);
		cfgMain.set(MAIN_CAT, "remount-on-knockback", remount);
		cfgMain.set(MAIN_CAT, "dynmap-border-enabled", dynmapEnable);
		cfgMain.set(MAIN_CAT, "dynmap-border-message", dynmapMessage);
		cfgMain.set(MAIN_CAT, "player-killed-bad-spawn", killPlayer);
		cfgMain.set(MAIN_CAT, "deny-enderpearl", denyEnderpearl);
		cfgMain.set(MAIN_CAT, "fill-autosave-frequency", fillAutosaveFrequency);
		cfgMain.set(MAIN_CAT, "bypass-list-uuids", exportBypassStringList());
		cfgMain.set(MAIN_CAT, "fill-memory-tolerance", fillMemoryTolerance);
		cfgMain.set(MAIN_CAT, "prevent-block-place", preventBlockPlace);
		cfgMain.set(MAIN_CAT, "prevent-mob-spawn", preventMobSpawn);

		cfgBorders.clear();
		for(Entry<String, BorderData> stringBorderDataEntry : borders.entrySet())
		{
            String name = stringBorderDataEntry.getKey().replace(".", "<");
			BorderData bord = stringBorderDataEntry.getValue();

			cfgBorders.set(name, "x", bord.getX());
			cfgBorders.set(name, "z", bord.getZ());
			cfgBorders.set(name, "radiusX", bord.getRadiusX());
			cfgBorders.set(name, "radiusZ", bord.getRadiusZ());
			cfgBorders.set(name, "wrapping", bord.getWrapping());

			if (bord.getShape() != null)
                cfgBorders.set(name, "shape-round", bord.getShape());
		}

        WorldFillTask fillTask = WorldFillTask.getInstance();
		if (storeFillTask && fillTask != null)
		{
			cfgMain.set(FILL_CAT, "world", fillTask.getWorld());
			cfgMain.set(FILL_CAT, "fillDistance", fillTask.getFillDistance());
			cfgMain.set(FILL_CAT, "chunksPerRun", fillTask.getChunksPerRun());
			cfgMain.set(FILL_CAT, "tickFrequency", fillTask.getTickFrequency());
			cfgMain.set(FILL_CAT, "x", fillTask.getRefX());
			cfgMain.set(FILL_CAT, "z", fillTask.getRefZ());
			cfgMain.set(FILL_CAT, "length", fillTask.getRefLength());
			cfgMain.set(FILL_CAT, "total", fillTask.getRefTotal());
			cfgMain.set(FILL_CAT, "forceLoad", fillTask.getForceLoad());
		}
		else
			cfgMain.removeCategory("fillTask");

		cfgMain.save();
        cfgBorders.save();

		if (logIt)
			logConfig("Configuration saved.");
	}


}
