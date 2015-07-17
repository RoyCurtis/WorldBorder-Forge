package com.wimbli.WorldBorder;

import com.wimbli.WorldBorder.forge.Util;
import cpw.mods.fml.common.eventhandler.Event;
import cpw.mods.fml.common.eventhandler.EventPriority;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import net.minecraft.world.World;
import net.minecraftforge.event.entity.living.LivingSpawnEvent;


public class MobSpawnListener
{
    @SubscribeEvent(priority = EventPriority.LOWEST)
    public void onCreatureSpawn(LivingSpawnEvent.CheckSpawn event)
    {
        World world = event.entity.worldObj;
        if (world == null) return;
        BorderData border = Config.Border( Util.getWorldName(world) );
        if (border == null) return;

        if (!border.insideBorder(event.entity.posX, event.entity.posZ, Config.getShapeRound()))
        {
            event.setResult(Event.Result.DENY);
        }
    }
}
