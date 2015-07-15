package com.wimbli.WorldBorder;

import com.mojang.realmsclient.gui.ChatFormatting;
import com.wimbli.WorldBorder.forge.Configuration;
import com.wimbli.WorldBorder.forge.Particles;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.world.WorldServer;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.Logger;

import java.io.File;
import java.text.DecimalFormat;
import java.util.*;
import java.util.Map.Entry;


public class Config
{
	// private stuff used within this class
	private static WorldBorder plugin;
    private static Configuration cfgMain = null;
    private static Configuration cfgBorders = null;
	private static Logger wbLog = null;
	public static volatile DecimalFormat coord = new DecimalFormat("0.0");
	private static int borderTask = -1;
	public static volatile WorldFillTask fillTask = null;
	public static volatile WorldTrimTask trimTask = null;
	private static Runtime rt = Runtime.getRuntime();

	// actual configuration values which can be changed
	private static boolean shapeRound = true;
	private static Map<String, BorderData> borders = Collections.synchronizedMap(new LinkedHashMap<String, BorderData>());
	private static Set<UUID> bypassPlayers = Collections.synchronizedSet(new LinkedHashSet<UUID>());
	private static String message;		// raw message without color code formatting
	private static String messageFmt;	// message with color code formatting ("&" changed to funky sort-of-double-dollar-sign for legitimate color/formatting codes)
	private static String messageClean;	// message cleaned of formatting codes
	private static boolean DEBUG = false;
	private static float knockBack = 3.0F;
	private static int timerTicks = 20;
	private static boolean whooshEffect = true;
	private static boolean portalRedirection = true;
	private static boolean dynmapEnable = true;
	private static String dynmapMessage;
	private static int remountDelayTicks = 0;
	private static boolean killPlayer = false;
	private static boolean denyEnderpearl = false;
	private static int fillAutosaveFrequency = 30;
	private static int fillMemoryTolerance = 500;
	private static boolean preventBlockPlace = false;
	private static boolean preventMobSpawn = false;

	// for monitoring plugin efficiency
//	public static long timeUsed = 0;

	public static long Now()
	{
		return System.currentTimeMillis();
	}


	public static void setBorder(String world, BorderData border, boolean logIt)
	{
		borders.put(world, border);
		if (logIt)
			log("Border set. " + BorderDescription(world));
		save(true);
		DynMapFeatures.showBorder(world, border);
	}
	public static void setBorder(String world, BorderData border)
	{
		setBorder(world, border, true);
	}

	public static void setBorder(String world, int radiusX, int radiusZ, double x, double z, Boolean shapeRound)
	{
		BorderData old = Border(world);
		boolean oldWrap = (old != null) && old.getWrapping();
		setBorder(world, new BorderData(x, z, radiusX, radiusZ, shapeRound, oldWrap), true);
	}
	public static void setBorder(String world, int radiusX, int radiusZ, double x, double z)
	{
		BorderData old = Border(world);
		Boolean oldShape = (old == null) ? null : old.getShape();
		boolean oldWrap = (old != null) && old.getWrapping();
		setBorder(world, new BorderData(x, z, radiusX, radiusZ, oldShape, oldWrap), true);
	}


	// backwards-compatible methods from before elliptical/rectangular shapes were supported
	public static void setBorder(String world, int radius, double x, double z, Boolean shapeRound)
	{
		setBorder(world, new BorderData(x, z, radius, radius, shapeRound), true);
	}
	public static void setBorder(String world, int radius, double x, double z)
	{
		setBorder(world, radius, radius, x, z);
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
	public static void setBorderCorners(String world, double x1, double z1, double x2, double z2, Boolean shapeRound)
	{
		setBorderCorners(world, x1, z1, x2, z2, shapeRound, false);
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
		if (border == null)
			return "No border was found for the world \"" + world + "\".";
		else
			return "World \"" + world + "\" has border " + border.toString();
	}

	public static Set<String> BorderDescriptions()
	{
		Set<String> output = new HashSet<String>();

		for(String worldName : borders.keySet())
		{
			output.add(BorderDescription(worldName));
		}

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
		messageClean = stripAmpColors(msg);
	}

	public static String Message()
	{
		return messageFmt;
	}
	public static String MessageRaw()
	{
		return message;
	}
	public static String MessageClean()
	{
		return messageClean;
	}

	public static void setShape(boolean round)
	{
		shapeRound = round;
		log("Set default border shape to " + (ShapeName()) + ".");
		save(true);
		DynMapFeatures.showAllBorders();
	}

	public static boolean ShapeRound()
	{
		return shapeRound;
	}

	public static String ShapeName()
	{
		return ShapeName(shapeRound);
	}
	public static String ShapeName(Boolean round)
	{
		if (round == null)
			return "default";
		return round ? "elliptic/round" : "rectangular/square";
	}

	public static void setDebug(boolean debugMode)
	{
		DEBUG = debugMode;
		log("Debug mode " + (DEBUG ? "enabled" : "disabled") + ".");
		save(true);
	}

	public static boolean Debug()
	{
		return DEBUG;
	}

	public static void setWhooshEffect(boolean enable)
	{
		whooshEffect = enable;
		log("\"Whoosh\" knockback effect " + (enable ? "enabled" : "disabled") + ".");
		save(true);
	}

	public static boolean whooshEffect()
	{
		return whooshEffect;
	}

	public static void showWhooshEffect(EntityPlayerMP player)
	{
		if (!whooshEffect())
			return;

        WorldServer world = player.getServerForPlayer();
        Particles.emitEnder(world, player.posX, player.posY, player.posZ);
        Particles.emitSmoke(world, player.posX, player.posY, player.posZ);
        world.playSoundAtEntity(player, "mob.ghast.fireball", 1.0F, 1.0F);
	}
	
	public static void setPreventBlockPlace(boolean enable)
	{
		if (preventBlockPlace != enable)
			WorldBorder.plugin.enableBlockPlaceListener(enable);

		preventBlockPlace = enable;
		log("prevent block place " + (enable ? "enabled" : "disabled") + ".");
		save(true);
	}
	
	public static void setPreventMobSpawn(boolean enable)
	{
		if (preventMobSpawn != enable)
			WorldBorder.plugin.enableMobSpawnListener(enable);

		preventMobSpawn = enable;
		log("prevent mob spawn " + (enable ? "enabled" : "disabled") + ".");
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

	public static boolean getIfPlayerKill()
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

	public static boolean portalRedirection()
	{
		return portalRedirection;
	}

	public static void setKnockBack(float numBlocks)
	{
		knockBack = numBlocks;
		log("Knockback set to " + knockBack + " blocks inside the border.");
		save(true);
	}

	public static double KnockBack()
	{
		return knockBack;
	}

	public static void setTimerTicks(int ticks)
	{
		timerTicks = ticks;
		log("Timer delay set to " + timerTicks + " tick(s). That is roughly " + (timerTicks * 50) + "ms / " + (((double)timerTicks * 50.0) / 1000.0) + " seconds.");
		StartBorderTimer();
		save(true);
	}

	public static int TimerTicks()
	{
		return timerTicks;
	}

	public static void setRemountTicks(int ticks)
	{
		remountDelayTicks = ticks;
		if (remountDelayTicks == 0)
			log("Remount delay set to 0. Players will be left dismounted when knocked back from the border while on a vehicle.");
		else
		{
			log("Remount delay set to " + remountDelayTicks + " tick(s). That is roughly " + (remountDelayTicks * 50) + "ms / " + (((double)remountDelayTicks * 50.0) / 1000.0) + " seconds.");
			if (ticks < 10)
				logWarn("setting the remount delay to less than 10 (and greater than 0) is not recommended. This can lead to nasty client glitches.");
		}
		save(true);
	}

	public static int RemountTicks()
	{
		return remountDelayTicks;
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

	public static int FillAutosaveFrequency()
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

	public static boolean DynmapBorderEnabled()
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

	public static String DynmapMessage()
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
		ArrayList<String> strings = new ArrayList<String>();

		for (UUID uuid : bypassPlayers)
			strings.add(uuid.toString());

		return strings.toArray(new String[ strings.size() ]);
	}


	public static boolean isBorderTimerRunning()
    {
        return borderTask != -1
            && (WorldBorder.scheduler.isQueued(borderTask) || WorldBorder.scheduler.isCurrentlyRunning(borderTask));
    }

	public static void StartBorderTimer()
	{
		StopBorderTimer();

		borderTask = WorldBorder.scheduler.scheduleSyncRepeatingTask(new BorderCheckTask(), timerTicks, timerTicks);

		if (borderTask == -1)
			logWarn("Failed to start timed border-checking task! This will prevent the plugin from working. Try restarting Bukkit.");

		logConfig("Border-checking timed task started.");
	}

	public static void StopBorderTimer()
	{
		if (borderTask == -1) return;

        WorldBorder.scheduler.cancelTask(borderTask);
		borderTask = -1;
		logConfig("Border-checking timed task stopped.");
	}

	public static void StopFillTask()
	{
		if (fillTask != null && fillTask.valid())
			fillTask.cancel();
	}

	public static void StoreFillTask()
	{
		save(false, true);
	}
	public static void UnStoreFillTask()
	{
		save(false);
	}

	public static void RestoreFillTask(String world, int fillDistance, int chunksPerRun, int tickFrequency, int x, int z, int length, int total, boolean forceLoad)
	{
		fillTask = new WorldFillTask(WorldBorder.server, null, world, fillDistance, chunksPerRun, tickFrequency, forceLoad);
		if (fillTask.valid())
		{
			fillTask.continueProgress(x, z, length, total);
			int task = WorldBorder.scheduler.scheduleSyncRepeatingTask(fillTask, 20, tickFrequency);
			fillTask.setTaskID(task);
		}
	}


	public static void StopTrimTask()
	{
		if (trimTask != null && trimTask.valid())
			trimTask.cancel();
	}


	public static int AvailableMemory()
	{
		return (int)((rt.maxMemory() - rt.totalMemory() + rt.freeMemory()) / 1048576);  // 1024*1024 = 1048576 (bytes in 1 MB)
	}

	public static boolean AvailableMemoryTooLow()
	{
		return AvailableMemory() < fillMemoryTolerance;
	}


	public static String replaceAmpColors (String message)
	{
        return message.replaceAll("(?i)&([a-fk-or0-9])", ChatFormatting.PREFIX_CODE + "$1");
    }

    // adapted from code posted by Sleaker
	public static String stripAmpColors (String message)
	{
		return message.replaceAll("(?i)&([a-fk-or0-9])", "");
	}


	public static void log(Level lvl, String text)
	{
		wbLog.log(lvl, text);
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
	{	// load config from file
		wbLog = WorldBorder.LOGGER;

        if (cfgMain == null)
            cfgMain = new Configuration( new File(WorldBorder.configDir, "main.cfg") );
        else cfgMain.load();

        if (cfgBorders == null)
            cfgBorders = new Configuration( new File(WorldBorder.configDir, "borders.cfg") );
        else cfgBorders.load();

		int cfgVersion = cfgMain.getInt("cfg-version", currentCfgVersion);

		String msg = cfgMain.getString("message", "");
		shapeRound = cfgMain.getBoolean("round-border", true);
		DEBUG = cfgMain.getBoolean("debug-mode", false);
		whooshEffect = cfgMain.getBoolean("whoosh-effect", true);
		portalRedirection = cfgMain.getBoolean("portal-redirection", true);
		knockBack = cfgMain.getFloat("knock-back-dist", 3.0F);
		timerTicks = cfgMain.getInt("timer-delay-ticks", 20);
		remountDelayTicks = cfgMain.getInt("remount-delay-ticks", 0);
		dynmapEnable = cfgMain.getBoolean("dynmap-border-enabled", true);
		dynmapMessage = cfgMain.getString("dynmap-border-message", "The border of the world.");
		logConfig("Using " + (ShapeName()) + " border, knockback of " + knockBack + " blocks, and timer delay of " + timerTicks + ".");
		killPlayer = cfgMain.getBoolean("player-killed-bad-spawn", false);
		denyEnderpearl = cfgMain.getBoolean("deny-enderpearl", true);
		fillAutosaveFrequency = cfgMain.getInt("fill-autosave-frequency", 30);
		importBypassStringList(cfgMain.getStringList("bypass-list-uuids"));
		fillMemoryTolerance = cfgMain.getInt("fill-memory-tolerance", 500);
		preventBlockPlace = cfgMain.getBoolean("prevent-block-place", true);
		preventMobSpawn = cfgMain.getBoolean("prevent-mob-spawn", true);

		StartBorderTimer();

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
		if ( cfgMain.hasCategory("fillTask") )
		{
            String worldName = cfgMain.get("fillTask", "world", "").getString();
			int fillDistance = cfgMain.get("fillTask", "fillDistance", 176).getInt();
			int chunksPerRun = cfgMain.get("fillTask", "chunksPerRun", 5).getInt();
			int tickFrequency = cfgMain.get("fillTask", "tickFrequency", 20).getInt();
			int fillX = cfgMain.get("fillTask", "x", 0).getInt();
			int fillZ = cfgMain.get("fillTask", "z", 0).getInt();
			int fillLength = cfgMain.get("fillTask", "length", 0).getInt();
			int fillTotal = cfgMain.get("fillTask", "total", 0).getInt();
			boolean forceLoad = cfgMain.get("fillTask", "forceLoad", false).getBoolean();
			RestoreFillTask(worldName, fillDistance, chunksPerRun, tickFrequency, fillX, fillZ, fillLength, fillTotal, forceLoad);
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

        String GENERAL = Configuration.GENERAL;
		cfgMain.set(GENERAL, "cfg-version", currentCfgVersion);
		cfgMain.set(GENERAL, "message", message);
		cfgMain.set(GENERAL, "round-border", shapeRound);
		cfgMain.set(GENERAL, "debug-mode", DEBUG);
		cfgMain.set(GENERAL, "whoosh-effect", whooshEffect);
		cfgMain.set(GENERAL, "portal-redirection", portalRedirection);
		cfgMain.set(GENERAL, "knock-back-dist", knockBack);
		cfgMain.set(GENERAL, "timer-delay-ticks", timerTicks);
		cfgMain.set(GENERAL, "remount-delay-ticks", remountDelayTicks);
		cfgMain.set(GENERAL, "dynmap-border-enabled", dynmapEnable);
		cfgMain.set(GENERAL, "dynmap-border-message", dynmapMessage);
		cfgMain.set(GENERAL, "player-killed-bad-spawn", killPlayer);
		cfgMain.set(GENERAL, "deny-enderpearl", denyEnderpearl);
		cfgMain.set(GENERAL, "fill-autosave-frequency", fillAutosaveFrequency);
		cfgMain.set(GENERAL, "bypass-list-uuids", exportBypassStringList());
		cfgMain.set(GENERAL, "fill-memory-tolerance", fillMemoryTolerance);
		cfgMain.set(GENERAL, "prevent-block-place", preventBlockPlace);
		cfgMain.set(GENERAL, "prevent-mob-spawn", preventMobSpawn);

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

		if (storeFillTask && fillTask != null && fillTask.valid())
		{
			cfgMain.set("fillTask","world", fillTask.refWorld());
			cfgMain.set("fillTask","fillDistance", fillTask.refFillDistance());
			cfgMain.set("fillTask","chunksPerRun", fillTask.refChunksPerRun());
			cfgMain.set("fillTask","tickFrequency", fillTask.refTickFrequency());
			cfgMain.set("fillTask","x", fillTask.refX());
			cfgMain.set("fillTask","z", fillTask.refZ());
			cfgMain.set("fillTask","length", fillTask.refLength());
			cfgMain.set("fillTask","total", fillTask.refTotal());
			cfgMain.set("fillTask","forceLoad", fillTask.refForceLoad());
		}
		else
			cfgMain.removeCategory("fillTask");

		cfgMain.save();
        cfgBorders.save();

		if (logIt)
			logConfig("Configuration saved.");
	}
}
