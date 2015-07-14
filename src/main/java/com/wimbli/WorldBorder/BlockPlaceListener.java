package com.wimbli.WorldBorder;

import com.wimbli.WorldBorder.forge.Util;
import cpw.mods.fml.common.eventhandler.Event;
import cpw.mods.fml.common.eventhandler.EventPriority;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import net.minecraft.world.World;
import net.minecraftforge.event.world.BlockEvent;

public class BlockPlaceListener
{
	@SubscribeEvent(priority = EventPriority.LOWEST)
	public void onBlockPlace(BlockEvent.PlaceEvent event)
	{
		World world = event.world;
		if (world == null) return;
		BorderData border = Config.Border( Util.getWorldName(world) );
		if (border == null) return;
		
		if (!border.insideBorder(event.x, event.z, Config.ShapeRound()))
		{
			event.setResult(Event.Result.DENY);
		}
	}
}
