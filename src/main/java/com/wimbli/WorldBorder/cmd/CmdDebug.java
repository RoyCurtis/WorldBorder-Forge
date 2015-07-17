package com.wimbli.WorldBorder.cmd;

import com.wimbli.WorldBorder.Config;
import com.wimbli.WorldBorder.forge.Util;
import net.minecraft.command.ICommandSender;
import net.minecraft.entity.player.EntityPlayerMP;

import java.util.List;


public class CmdDebug extends WBCmd
{
    public CmdDebug()
    {
        name = permission = "debug";
        minParams = maxParams = 1;

        addCmdExample(nameEmphasized() + "<on|off> - turn console debug output on or off.");
        helpText = "Default value: off. Debug mode will show some extra debugging data in the server console/log when " +
            "players are knocked back from the border or are teleported.";
    }

    @Override
    public void cmdStatus(ICommandSender sender)
    {
        Util.chat(sender, C_HEAD + "Debug mode is " + enabledColored(Config.isDebugMode()) + C_HEAD + ".");
    }

    @Override
    public void execute(ICommandSender sender, EntityPlayerMP player, List<String> params, String worldName)
    {
        Config.setDebugMode(strAsBool(params.get(0)));

        if (player != null)
        {
            Config.log((Config.isDebugMode() ? "Enabled" : "Disabled") + " debug output at the command of player \"" + player.getDisplayName() + "\".");
            cmdStatus(sender);
        }
    }
}
