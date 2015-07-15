package com.wimbli.WorldBorder.cmd;

import com.wimbli.WorldBorder.Config;
import com.wimbli.WorldBorder.UUID.NameFetcher;
import com.wimbli.WorldBorder.forge.Util;
import net.minecraft.command.ICommandSender;
import net.minecraft.entity.player.EntityPlayerMP;

import java.util.List;
import java.util.Map;
import java.util.UUID;


public class CmdBypasslist extends WBCmd
{
	public CmdBypasslist()
	{
		name = permission = "bypasslist";
		minParams = maxParams = 0;

		addCmdExample(nameEmphasized() + "- list players with border bypass enabled.");
		helpText = "The bypass list will persist between server restarts, and applies to all worlds. Use the " +
			commandEmphasized("bypass") + C_DESC + "command to add or remove players.";
	}

	@Override
	public void execute(final ICommandSender sender, EntityPlayerMP player, List<String> params, String worldName)
	{
		final UUID[] uuids = Config.getPlayerBypassList();
		if (uuids.length == 0)
		{
			Util.chat(sender, "Players with border bypass enabled: <none>");
			return;
		}

        try
        {
            NameFetcher fetcher = new NameFetcher(uuids);
            Map<UUID, String> names = fetcher.call();
            String nameString = names.values().toString();

            Util.chat(sender, "Players with border bypass enabled: " + nameString.substring(1, nameString.length() - 1));
        }
        catch (Exception ex)
        {
            sendErrorAndHelp(sender, "Failed to look up names for the UUIDs in the border bypass list. " + ex.getLocalizedMessage());
        }
	}
}
