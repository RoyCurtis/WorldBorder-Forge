package com.wimbli.WorldBorder;


import com.wimbli.WorldBorder.forge.Location;
import com.wimbli.WorldBorder.task.BorderCheckTask;
import cpw.mods.fml.common.eventhandler.EventPriority;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.PlayerEvent;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraftforge.event.entity.living.EnderTeleportEvent;
import net.minecraftforge.event.world.ChunkEvent;

public class WBListener
{
	@SubscribeEvent(priority = EventPriority.LOWEST)
	public void onPlayerPearl(EnderTeleportEvent event)
	{
        if ( !(event.entityLiving instanceof EntityPlayerMP) )
            return;

		// if knockback is set to 0, simply return
		if (Config.getKnockBack() == 0.0)
			return;

		if (Config.isDebugMode())
			Config.log("Teleport cause: Enderpearl");

        EntityPlayerMP player = (EntityPlayerMP) event.entityLiving;

        Location target = new Location(event, player);
		Location newLoc = BorderCheckTask.checkPlayer(player, target, true, true);

		if (newLoc != null)
		{
			if( Config.getDenyEnderpearl() )
			{
				event.setCanceled(true);
				return;
			}

            event.targetX = newLoc.posX;
            event.targetY = newLoc.posY;
            event.targetZ = newLoc.posZ;
		}
	}

    // FORGE: Forge is missing a general teleport event. Revisit this later if security
    // holes arise
//	@SubscribeEvent(priority = EventPriority.LOWEST)
//	public void onPlayerTeleport(PlayerTeleportEvent event)
//	{
//		// if knockback is set to 0, simply return
//		if (Config.getKnockBack() == 0.0)
//			return;
//
//		if (Config.Debug())
//			Config.log("Teleport cause: " + event.getCause().toString());
//
//		Location newLoc = BorderCheckTask.checkPlayer(event.getPlayer(), event.getTo(), true, true);
//		if (newLoc != null)
//		{
//			if(event.getCause() == PlayerTeleportEvent.TeleportCause.ENDER_PEARL && Config.getDenyEnderpearl())
//			{
//				event.setCancelled(true);
//				return;
//			}
//
//			event.setTo(newLoc);
//		}
//	}

    // FORGE: This is a very hacky attempt at emulating teleport event. May not work as
    // intended.
    @SubscribeEvent(priority = EventPriority.LOWEST)
    public void onPlayerRespawn(PlayerEvent.PlayerRespawnEvent event)
    {
        // if knockback is set to 0, simply return
        if (Config.getKnockBack() == 0.0)
            return;

        if (Config.isDebugMode())
            Config.log("Teleport cause: Respawn");

        EntityPlayerMP player = (EntityPlayerMP) event.player;

        Location target = new Location(event.player);
        Location newLoc = BorderCheckTask.checkPlayer(player, target, true, true);

        if (newLoc != null)
            event.player.setPositionAndUpdate(newLoc.posX, newLoc.posY, newLoc.posZ);
    }

    // FORGE: This is a very hacky attempt at emulating portal event. May not work as
    // intended.
	@SubscribeEvent(priority = EventPriority.LOWEST)
	public void onPlayerPortal(PlayerEvent.PlayerChangedDimensionEvent event)
	{
		// if knockback is set to 0, or portal redirection is disabled, simply return
		if (Config.getKnockBack() == 0.0 || !Config.doPortalRedirection())
			return;

        if (Config.isDebugMode())
            Config.log("Teleport cause: Dimension change");

        EntityPlayerMP player = (EntityPlayerMP) event.player;

        Location target = new Location(event.player);
		Location newLoc = BorderCheckTask.checkPlayer(player, target, true, false);

		if (newLoc != null)
			event.player.setPositionAndUpdate(newLoc.posX, newLoc.posY, newLoc.posZ);
	}

    @SubscribeEvent(priority = EventPriority.LOWEST)
	public void onChunkLoad(ChunkEvent.Load event)
	{
/*		// tested, found to spam pretty rapidly as client repeatedly requests the same chunks since they're not being sent
		// definitely too spammy at only 16 blocks outside border
		// potentially useful at standard 208 block padding as it was triggering only occasionally while trying to get out all along edge of round border, though sometimes up to 3 triggers within a second corresponding to 3 adjacent chunks
		// would of course need to be further worked on to have it only affect chunks outside a border, along with an option somewhere to disable it or even set specified distance outside border for it to take effect; maybe  send client chunk composed entirely of air to shut it up

		// method to prevent new chunks from being generated, core method courtesy of code from NoNewChunk plugin (http://dev.bukkit.org/bukkit-plugins/nonewchunk/)
		if(event.isNewChunk())
		{
			Chunk chunk = event.getChunk();
			chunk.unload(false, false);
			Config.logWarn("New chunk generation has been prevented at X " + chunk.getX() + ", Z " + chunk.getZ());
		}
*/
		// make sure our border monitoring task is still running like it should
		if (Config.isBorderTimerRunning()) return;

		Config.logWarn("Border-checking task was not running! Something on your server apparently killed it. It will now be restarted.");
		Config.startBorderTimer();
	}
}
