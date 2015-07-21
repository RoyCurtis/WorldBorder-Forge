package com.wimbli.WorldBorder;

import com.wimbli.WorldBorder.forge.Worlds;
import cpw.mods.fml.common.eventhandler.EventPriority;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import net.minecraft.world.World;
import net.minecraftforge.event.world.BlockEvent;

public class BlockPlaceListener
{
    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public void onBlockPlace(BlockEvent.PlaceEvent event)
    {
        if ( isInsideBorder(event.world, event.x, event.z) )
            return;

        event.setResult(BlockEvent.Result.DENY);
        event.setCanceled(true);
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public void onMultiBlockPlace(BlockEvent.MultiPlaceEvent event)
    {
        if ( isInsideBorder(event.world, event.x, event.z) )
            return;

        event.setResult(BlockEvent.Result.DENY);
        event.setCanceled(true);
    }

    private boolean isInsideBorder(World world, int x, int z)
    {
        BorderData border = Config.Border( Worlds.getWorldName(world) );

        return border == null
            || border.insideBorder( x, z, Config.getShapeRound() );
    }
}
