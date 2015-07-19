package com.wimbli.WorldBorder.cmd;

import com.wimbli.WorldBorder.Config;
import com.wimbli.WorldBorder.Log;
import com.wimbli.WorldBorder.forge.Util;
import net.minecraft.command.ICommandSender;
import net.minecraft.entity.player.EntityPlayerMP;

import java.util.List;


public class CmdPortal extends WBCmd
{
    public CmdPortal()
    {
        name = permission = "portal";
        minParams = maxParams = 1;

        addCmdExample(nameEmphasized() + "<on|off> - turn portal redirection on or off.");
        helpText = "Default value: on. This feature monitors new portal creation and changes the target new portal " +
            "location if it is outside of the border. Try disabling this if you have problems with other plugins " +
            "related to portals.";
    }

    @Override
    public void cmdStatus(ICommandSender sender)
    {
        Util.chat(sender, C_HEAD + "Portal redirection is " + enabledColored(Config.doPortalRedirection()) + C_HEAD + ".");
    }

    @Override
    public void execute(ICommandSender sender, EntityPlayerMP player, List<String> params, String worldName)
    {
        Config.setPortalRedirection(strAsBool(params.get(0)));

        if (player != null)
        {
            Log.info((Config.doPortalRedirection() ? "Enabled" : "Disabled") + " portal redirection at the command of player \"" + player.getDisplayName() + "\".");
            cmdStatus(sender);
        }
    }
}
