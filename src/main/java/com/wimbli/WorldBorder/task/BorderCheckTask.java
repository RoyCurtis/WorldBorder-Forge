package com.wimbli.WorldBorder.task;

import com.wimbli.WorldBorder.BorderData;
import com.wimbli.WorldBorder.Config;
import com.wimbli.WorldBorder.WorldBorder;
import com.wimbli.WorldBorder.forge.Location;
import com.wimbli.WorldBorder.forge.Util;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLiving;
import net.minecraft.entity.item.EntityBoat;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.world.WorldServer;

import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;


public class BorderCheckTask implements Runnable
{
	@Override
	public void run()
	{
		// if knockback is set to 0, simply return
		if (Config.KnockBack() == 0.0)
			return;

		for (Object o : WorldBorder.server.getConfigurationManager().playerEntityList)
		{
			EntityPlayerMP player = (EntityPlayerMP) o;
			checkPlayer(player, null, false, true);
		}
	}

	// track players who are being handled (moved back inside the border) already; needed since Bukkit is sometimes sending teleport events with the old (now incorrect) location still indicated, which can lead to a loop when we then teleport them thinking they're outside the border, triggering event again, etc.
	private static Set<String> handlingPlayers = Collections.synchronizedSet(new LinkedHashSet<String>());

	// set targetLoc only if not current player location; set returnLocationOnly to true to have new Location returned if they need to be moved to one, instead of directly handling it
	public static Location checkPlayer(EntityPlayerMP player, Location targetLoc, boolean returnLocationOnly, boolean notify)
	{
		if (player == null) return null;

		Location loc = (targetLoc == null) ? new Location(player) : targetLoc;

		WorldServer world = loc.world;
		if (world == null) return null;
		BorderData border = Config.Border( Util.getWorldName(world) );
		if (border == null) return null;

		if (border.insideBorder(loc.posX, loc.posZ, Config.ShapeRound()))
			return null;

		// if player is in bypass list (from bypass command), allow them beyond border; also ignore players currently being handled already
		if (Config.isPlayerBypassing(player.getUniqueID()) || handlingPlayers.contains(player.getDisplayName().toLowerCase()))
			return null;

		// tag this player as being handled so we can't get stuck in a loop due to Bukkit currently sometimes repeatedly providing incorrect location through teleport event
		handlingPlayers.add(player.getDisplayName().toLowerCase());

		Location newLoc = newLocation(player, loc, border, notify);
		boolean handlingVehicle = false;

		/*
		 * since we need to forcibly eject players who are inside vehicles, that fires a teleport event (go figure) and
		 * so would effectively double trigger for us, so we need to handle it here to prevent sending two messages and
		 * two log entries etc.
		 * after players are ejected we can wait a few ticks (long enough for their client to receive new entity location)
		 * and then set them as passenger of the vehicle again
		 */
		if (player.isRiding())
		{
			Entity ride = player.ridingEntity;
			player.mountEntity(null);
			if (ride != null)
			{	// vehicles need to be offset vertically and have velocity stopped
				double vertOffset = (ride instanceof EntityLiving) ? 0 : ride.posY - loc.posY;
				Location rideLoc = new Location(newLoc);
				rideLoc.posY = newLoc.posY + vertOffset;
				if (Config.Debug())
					Config.logWarn("Player was riding a \"" + ride.toString() + "\".");
				if (ride instanceof EntityBoat)
				{	// boats currently glitch on client when teleported, so crappy workaround is to remove it and spawn a new one
					ride.setDead();
					ride = new EntityBoat(world, rideLoc.posX, rideLoc.posY, rideLoc.posZ);
                    world.spawnEntityInWorld(ride);
				}
				else
				{
					ride.setVelocity(0, 0, 0);
                    ride.setPositionAndRotation(rideLoc.posX, rideLoc.posY, rideLoc.posZ, rideLoc.pitch, rideLoc.yaw);
				}

				if (Config.RemountTicks() > 0)
				{
					setPassengerDelayed(ride, player, player.getDisplayName(), Config.RemountTicks());
					handlingVehicle = true;
				}
			}
		}

		// check if player has something (a pet, maybe?) riding them; only possible through odd plugins.
		// it can prevent all teleportation of the player completely, so it's very much not good and needs handling
		if (player.riddenByEntity != null)
		{
			Entity rider = player.riddenByEntity;
			rider.mountEntity(null);
            rider.setPositionAndRotation(newLoc.posX, newLoc.posY, newLoc.posZ, newLoc.pitch, newLoc.yaw);
            Util.chat(player, "Your passenger has been ejected.");
			if (Config.Debug())
				Config.logWarn("Player had a passenger riding on them: " + rider.getCommandSenderName());
		}

		// give some particle and sound effects where the player was beyond the border, if "whoosh effect" is enabled
		Config.showWhooshEffect(player);

		if (!returnLocationOnly)
            player.setPositionAndUpdate(newLoc.posX, newLoc.posY, newLoc.posZ);

		if (!handlingVehicle)
			handlingPlayers.remove(player.getDisplayName().toLowerCase());

		if (returnLocationOnly)
			return newLoc;

		return null;
	}
	public static Location checkPlayer(EntityPlayerMP player, Location targetLoc, boolean returnLocationOnly)
	{
		return checkPlayer(player, targetLoc, returnLocationOnly, true);
	}

	private static Location newLocation(EntityPlayerMP player, Location loc, BorderData border, boolean notify)
	{
		if (Config.Debug())
		{
			Config.logWarn(
                (notify ? "Border crossing" : "Check was run")
                + " in \"" + Util.getWorldName(loc.world)
                + "\". Border " + border.toString());
			Config.logWarn(
                "Player position X: " + Config.coord.format(loc.posX)
                + " Y: " + Config.coord.format(loc.posY)
                + " Z: " + Config.coord.format(loc.posZ));
		}

		Location newLoc = border.correctedPosition(loc, Config.ShapeRound(), player.capabilities.isFlying);

		// it's remotely possible (such as in the Nether) a suitable location isn't available, in which case...
		if (newLoc == null)
		{
			if (Config.Debug())
				Config.logWarn("Target new location unviable, using spawn or killing player.");
			if (Config.getIfPlayerKill())
			{
				player.setHealth(0.0F);
				return null;
			}
			newLoc = new Location( (WorldServer) player.worldObj );
		}

		if (Config.Debug())
			Config.logWarn(
                "New position in world \"" + Util.getWorldName( newLoc.world )
                + "\" at X: " + Config.coord.format(newLoc.posX)
                + " Y: " + Config.coord.format(newLoc.posY)
                + " Z: " + Config.coord.format(newLoc.posZ));

		if (notify)
			Util.chat(player, Config.Message());

		return newLoc;
	}

	private static void setPassengerDelayed(final Entity vehicle, final EntityPlayerMP player, final String playerName, long delay)
	{
        WorldBorder.scheduler.scheduleSyncDelayedTask(new Runnable()
        {
            @Override
            public void run()
            {
                handlingPlayers.remove(playerName.toLowerCase());
                if (vehicle == null || player == null) return;

                player.mountEntity(vehicle);
            }
        }, delay);
	}
}
