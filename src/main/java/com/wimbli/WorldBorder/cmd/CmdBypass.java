package com.wimbli.WorldBorder.cmd;

import java.util.List;
import java.util.UUID;

import com.mojang.authlib.GameProfile;
import com.wimbli.WorldBorder.forge.Util;
import net.minecraft.command.ICommandSender;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.server.MinecraftServer;
import org.bukkit.Bukkit;
import org.bukkit.command.*;
import org.bukkit.entity.Player;

import com.wimbli.WorldBorder.*;
import com.wimbli.WorldBorder.UUID.UUIDFetcher;


public class CmdBypass extends WBCmd
{
	public CmdBypass()
	{
		name = permission = "bypass";
		minParams = 0;
		maxParams = 2;

		addCmdExample(nameEmphasized() + "{player} [on|off] - let player go beyond border.");
		helpText = "If [player] isn't specified, command sender is used. If [on|off] isn't specified, the value will " +
			"be toggled. Once bypass is enabled, the player will not be stopped by any borders until bypass is " +
			"disabled for them again. Use the " + commandEmphasized("bypasslist") + C_DESC + "command to list all " +
			"players with bypass enabled.";
	}

	@Override
	public void cmdStatus(ICommandSender sender)
	{
		if (!(sender instanceof EntityPlayerMP))
			return;

		boolean bypass = Config.isPlayerBypassing(((EntityPlayerMP)sender).getUniqueID());
		Util.chat(sender, C_HEAD + "Border bypass is currently " + enabledColored(bypass) + C_HEAD + " for you.");
	}

	@Override
	public void execute(final ICommandSender sender, final EntityPlayerMP player, final List<String> params, String worldName)
	{
		if (player == null && params.isEmpty())
		{
			sendErrorAndHelp(sender, "When running this command from console, you must specify a player.");
			return;
		}

		WorldBorder.scheduler.scheduleSyncDelayedTask(WorldBorder.plugin, new Runnable()
		{
			@Override
			public void run()
			{
                assert player != null;
                final String sPlayer = (params.isEmpty()) ? player.getDisplayName() : params.get(0);
				UUID uPlayer = (params.isEmpty()) ? player.getUniqueID() : null;

				if (uPlayer == null)
				{
                    GameProfile p = WorldBorder.server.func_152358_ax().func_152655_a(sPlayer);
					if (p != null)
					{
						uPlayer = p.getId();
					}
					else
					{
						// only do UUID lookup using Mojang server if specified player isn't online
						try
						{
							uPlayer = UUIDFetcher.getUUIDOf(sPlayer);
						}
						catch (Exception ex)
						{
							sendErrorAndHelp(sender, "Failed to look up UUID for the player name you specified. " + ex.getLocalizedMessage());
							return;
						}
					}
				}
				if (uPlayer == null)
				{
					sendErrorAndHelp(sender, "Failed to look up UUID for the player name you specified; null value returned.");
					return;
				}

				boolean bypassing = !Config.isPlayerBypassing(uPlayer);
				if (params.size() > 1) bypassing = strAsBool(params.get(1));

				Config.setPlayerBypass(uPlayer, bypassing);

				EntityPlayerMP target = WorldBorder.server.getConfigurationManager().func_152612_a(sPlayer);
				if ( target != null )
					Util.chat(target, "Border bypass is now " + enabledColored(bypassing) + ".");

				Config.log(
                    "Border bypass for player \"" + sPlayer + "\" is "
                    + (bypassing ? "enabled" : "disabled")
                    + (" at the command of player \"" + player.getDisplayName() + "\"") + "."
                );

				if (player != target)
					Util.chat(sender, "Border bypass for player \"" + sPlayer + "\" is " + enabledColored(bypassing) + ".");
			}
		});
	}
}
