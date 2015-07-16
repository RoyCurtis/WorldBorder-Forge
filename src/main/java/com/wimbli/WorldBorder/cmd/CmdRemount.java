package com.wimbli.WorldBorder.cmd;

import com.wimbli.WorldBorder.Config;
import com.wimbli.WorldBorder.forge.Util;
import net.minecraft.command.ICommandSender;
import net.minecraft.entity.player.EntityPlayerMP;

import java.util.List;


public class CmdRemount extends WBCmd
{
	public CmdRemount()
	{
		name = permission = "remount";
		minParams = maxParams = 1;

		addCmdExample(nameEmphasized() + "<amount> - player remount delay after knockback.");
		helpText = "Default value: 0 (disabled). If set higher than 0, WorldBorder will attempt to re-mount players who " +
			"are knocked back from the border while riding something after this many server ticks. This setting can " +
			"cause really nasty glitches if enabled and set too low due to CraftBukkit teleportation problems.";
	}

	@Override
	public void cmdStatus(ICommandSender sender)
	{
		int delay = Config.getRemountTicks();
		if (delay == 0)
			Util.chat(sender, C_HEAD + "Remount delay set to 0. Players will be left dismounted when knocked back from the border while on a vehicle.");
		else
		{
			Util.chat(sender, C_HEAD + "Remount delay set to " + delay + " tick(s). That is roughly " + (delay * 50) + "ms / " + (((double) delay * 50.0) / 1000.0) + " seconds. Setting to 0 would disable remounting.");
			if (delay < 10)
				Util.chat(sender, C_ERR + "WARNING:" + C_DESC + " setting this to less than 10 (and greater than 0) is not recommended. This can lead to nasty client glitches.");
		}
	}

	@Override
	public void execute(ICommandSender sender, EntityPlayerMP player, List<String> params, String worldName)
	{
		int delay = 0;
		try
		{
			delay = Integer.parseInt(params.get(0));
			if (delay < 0)
				throw new NumberFormatException();
		}
		catch(NumberFormatException ex)
		{
			sendErrorAndHelp(sender, "The remount delay must be an integer of 0 or higher. Setting to 0 will disable remounting.");
			return;
		}

		Config.setRemountTicks(delay);

		if (player != null)
		cmdStatus(sender);
	}
}
