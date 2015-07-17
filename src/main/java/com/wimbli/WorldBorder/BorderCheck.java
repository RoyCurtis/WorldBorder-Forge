package com.wimbli.WorldBorder;

import com.wimbli.WorldBorder.forge.Location;
import com.wimbli.WorldBorder.forge.Particles;
import com.wimbli.WorldBorder.forge.Util;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLiving;
import net.minecraft.entity.item.EntityBoat;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.world.WorldServer;

import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;

/**
 * Static utility class that holds logic for border and player checking
 */
public class BorderCheck
{
    // track players who are being handled (moved back inside the border) already
    // needed since Bukkit is sometimes sending teleport events with the old (now incorrect) location still indicated,
    // which can lead to a loop when we then teleport them thinking they're outside the border, triggering event again, etc.
    // TODO: check if unneeded
    private static Set<String> handlingPlayers = Collections.synchronizedSet(new LinkedHashSet<String>());

    // set targetLoc only if not current player location
    // set returnLocationOnly to true to have new Location returned if they need to be moved to one, instead of directly handling it
    public static Location checkPlayer(EntityPlayerMP player, Location targetLoc, boolean returnLocationOnly, boolean notify)
    {
        if (player == null) return null;

        Location loc = (targetLoc == null) ? new Location(player) : targetLoc;

        WorldServer world = loc.world;
        if (world == null) return null;
        BorderData border = Config.Border(Util.getWorldName(world));
        if (border == null) return null;

        if (border.insideBorder(loc.posX, loc.posZ, Config.getShapeRound())) return null;

        // if player is in bypass list (from bypass command), allow them beyond border; also ignore players currently being handled already
        if (Config.isPlayerBypassing(player.getUniqueID()) || handlingPlayers.contains(player.getDisplayName().toLowerCase()))
            return null;

        // tag this player as being handled so we can't get stuck in a loop due to Bukkit currently sometimes repeatedly providing incorrect location through teleport event
        handlingPlayers.add(player.getDisplayName().toLowerCase());

        Location newLoc = newLocation(player, loc, border, notify);

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
            {    // vehicles need to be offset vertically and have velocity stopped
                double vertOffset = (ride instanceof EntityLiving) ? 0 : ride.posY - loc.posY;
                Location rideLoc = new Location(newLoc);
                rideLoc.posY = newLoc.posY + vertOffset;
                if (Config.isDebugMode()) Config.logWarn("Player was riding a \"" + ride.toString() + "\".");
                if (ride instanceof EntityBoat)
                {    // boats currently glitch on client when teleported, so crappy workaround is to remove it and spawn a new one
                    ride.setDead();
                    ride = new EntityBoat(world, rideLoc.posX, rideLoc.posY, rideLoc.posZ);
                    world.spawnEntityInWorld(ride);
                }
                else
                    ride.setPositionAndRotation(rideLoc.posX, rideLoc.posY, rideLoc.posZ, rideLoc.pitch, rideLoc.yaw);

                if ( Config.getRemount() )
                    player.mountEntity(ride);
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
            if (Config.isDebugMode())
                Config.logWarn("Player had a passenger riding on them: " + rider.getCommandSenderName());
        }

        // give some particle and sound effects where the player was beyond the border, if "whoosh effect" is enabled
        if (Config.doWhooshEffect()) Particles.showWhooshEffect(player);

        if (!returnLocationOnly) player.setPositionAndUpdate(newLoc.posX, newLoc.posY, newLoc.posZ);

        handlingPlayers.remove(player.getDisplayName().toLowerCase());

        if (returnLocationOnly) return newLoc;

        return null;
    }

    private static Location newLocation(EntityPlayerMP player, Location loc, BorderData border, boolean notify)
    {
        if (Config.isDebugMode())
        {
            Config.logWarn((notify ? "Border crossing" : "Check was run") + " in \"" + Util.getWorldName(loc.world) + "\". Border " + border.toString());
            Config.logWarn("Player position X: " + Config.COORD_FORMAT.format(loc.posX) + " Y: " + Config.COORD_FORMAT.format(loc.posY) + " Z: " + Config.COORD_FORMAT.format(loc.posZ));
        }

        Location newLoc = border.correctedPosition(loc, Config.getShapeRound(), player.capabilities.isFlying);

        // it's remotely possible (such as in the Nether) a suitable location isn't available, in which case...
        if (newLoc == null)
        {
            if (Config.isDebugMode()) Config.logWarn("Target new location unviable, using spawn or killing player.");
            if (Config.doPlayerKill())
            {
                player.setHealth(0.0F);
                return null;
            }
            newLoc = new Location((WorldServer) player.worldObj);
        }

        if (Config.isDebugMode())
            Config.logWarn("New position in world \"" + Util.getWorldName(newLoc.world) + "\" at X: " + Config.COORD_FORMAT.format(newLoc.posX) + " Y: " + Config.COORD_FORMAT.format(newLoc.posY) + " Z: " + Config.COORD_FORMAT.format(newLoc.posZ));

        if (notify) Util.chat(player, Config.getMessage());

        return newLoc;
    }
}
