package com.wimbli.WorldBorder.listener;

import com.wimbli.WorldBorder.BorderCheck;
import com.wimbli.WorldBorder.Config;
import com.wimbli.WorldBorder.forge.Location;
import com.wimbli.WorldBorder.forge.Log;
import cpw.mods.fml.common.eventhandler.EventPriority;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.world.chunk.Chunk;
import net.minecraftforge.event.entity.living.EnderTeleportEvent;
import net.minecraftforge.event.world.ChunkEvent;

public class EnderPearlListener
{
    @SubscribeEvent(priority = EventPriority.LOWEST)
    public void onPlayerPearl(EnderTeleportEvent event)
    {
        if ( !(event.entityLiving instanceof EntityPlayerMP) )
            return;

        if ( Config.getKnockBack() == 0.0 || !Config.getDenyEnderpearl() )
            return;

        EntityPlayerMP player = (EntityPlayerMP) event.entityLiving;
        Log.trace( "Caught pearl teleport event by %s", player.getDisplayName() );

        Location target = new Location(event, player);
        Location newLoc = BorderCheck.checkPlayer(player, target, true, true);

        if (newLoc != null)
        {
            event.setCanceled(true);
            event.targetX = newLoc.posX;
            event.targetY = newLoc.posY;
            event.targetZ = newLoc.posZ;
        }
    }
}
