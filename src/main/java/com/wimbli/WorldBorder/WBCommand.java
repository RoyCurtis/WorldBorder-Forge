package com.wimbli.WorldBorder;

import com.wimbli.WorldBorder.cmd.*;
import com.wimbli.WorldBorder.forge.Util;
import net.minecraft.command.ICommand;
import net.minecraft.command.ICommandSender;
import net.minecraft.entity.player.EntityPlayerMP;

import java.util.*;


public class WBCommand implements ICommand
{
    static final String NAME    = "wborder";
    static final List   ALIASES = Arrays.asList("wb", "worldborder");

	// map of all sub-commands with the command name (string) for quick reference
	public Map<String, WBCmd> subCommands = new LinkedHashMap<String, WBCmd>();
	// ref. list of the commands which can have a world name in front of the command itself (ex. /wb _world_ radius 100)
	private Set<String> subCommandsWithWorldNames = new LinkedHashSet<String>();

	// constructor
	public WBCommand ()
	{
		addCmd(new CmdHelp());			// 1 example
		addCmd(new CmdSet());			// 4 examples for player, 3 for console
		addCmd(new CmdSetcorners());	// 1
		addCmd(new CmdRadius());		// 1
		addCmd(new CmdList());			// 1
		//----- 8 per page of examples
		addCmd(new CmdShape());			// 2
		addCmd(new CmdClear());			// 2
		addCmd(new CmdFill());			// 1
		addCmd(new CmdTrim());			// 1
		addCmd(new CmdBypass());		// 1
		addCmd(new CmdBypasslist());	// 1
		//-----
		addCmd(new CmdKnockback());		// 1
		addCmd(new CmdWrap());			// 1
		addCmd(new CmdWhoosh());		// 1
		addCmd(new CmdGetmsg());		// 1
		addCmd(new CmdSetmsg());		// 1
		addCmd(new CmdWshape());		// 3
		//-----
		addCmd(new CmdPreventPlace());	// 1
		addCmd(new CmdPreventSpawn());	// 1
		addCmd(new CmdDelay());			// 1
		addCmd(new CmdDynmap());		// 1
		addCmd(new CmdDynmapmsg());		// 1
		addCmd(new CmdRemount());		// 1
		addCmd(new CmdFillautosave());	// 1
		addCmd(new CmdPortal());		// 1
		//-----
		addCmd(new CmdDenypearl());		// 1
		addCmd(new CmdReload());		// 1
		addCmd(new CmdDebug());			// 1

		// this is the default command, which shows command example pages; should be last just in case
		addCmd(new CmdCommands());
	}


	private void addCmd(WBCmd cmd)
	{
		subCommands.put(cmd.name, cmd);
		if (cmd.hasWorldNameInput)
			subCommandsWithWorldNames.add(cmd.name);
	}

	@Override
	public void processCommand(ICommandSender sender, String[] split)
	{
		EntityPlayerMP player = sender instanceof EntityPlayerMP
			? (EntityPlayerMP) sender
			: null;

		// if world name is passed inside quotation marks, handle that, and get List<String> instead of String[]
		List<String> params = concatenateQuotedWorldName(split);

		String worldName = null;
		// is second parameter the command and first parameter a world name? definitely world name if it was in quotation marks
		if (wasWorldQuotation || (params.size() > 1 && !subCommands.containsKey(params.get(0)) && subCommandsWithWorldNames.contains(params.get(1))))
			worldName = params.get(0);

		// no command specified? show command examples / help
		if (params.isEmpty())
			params.add(0, "commands");

		// determined the command name
		String cmdName = (worldName == null) ? params.get(0).toLowerCase() : params.get(1).toLowerCase();

		// remove command name and (if there) world name from front of param array
		params.remove(0);
		if (worldName != null)
			params.remove(0);

		// make sure command is recognized, default to showing command examples / help if not; also check for specified page number
		if (!subCommands.containsKey(cmdName))
		{
			int page = (player == null) ? 0 : 1;
			try
			{
				page = Integer.parseInt(cmdName);
			}
			catch(NumberFormatException ignored)
			{
                Util.chat(sender, WBCmd.C_ERR + "Command not recognized. Showing command list.");
			}
			cmdName = "commands";
			params.add(0, Integer.toString(page));
		}

		WBCmd subCommand = subCommands.get(cmdName);

		// if command requires world name when run by console, make sure that's in place
		if (player == null && subCommand.hasWorldNameInput && subCommand.consoleRequiresWorldName && worldName == null)
		{
			Util.chat(sender, WBCmd.C_ERR + "This command requires a world to be specified if run by the console.");
			subCommand.sendCmdHelp(sender);
			return;
		}

		// make sure valid number of parameters has been provided
		if (params.size() < subCommand.minParams || params.size() > subCommand.maxParams)
		{
			if (subCommand.maxParams == 0)
                Util.chat(sender, WBCmd.C_ERR + "This command does not accept any parameters.");
			else
				Util.chat(sender, WBCmd.C_ERR + "You have not provided a valid number of parameters.");
			subCommand.sendCmdHelp(sender);
			return;
		}

		// execute command
		subCommand.execute(sender, player, params, worldName);
	}

	private boolean wasWorldQuotation = false;

	// if world name is surrounded by quotation marks, combine it down and flag wasWorldQuotation if it's first param.
	// also return List<String> instead of input primitive String[]
	private List<String> concatenateQuotedWorldName(String[] split)
	{
		wasWorldQuotation = false;
		List<String> args = new ArrayList<String>(Arrays.asList(split));

		int startIndex = -1;
		for (int i = 0; i < args.size(); i++)
		{
			if (args.get(i).startsWith("\""))
			{
				startIndex = i;
				break;
			}
		}
		if (startIndex == -1)
			return args;

		if (args.get(startIndex).endsWith("\""))
		{
			args.set(startIndex, args.get(startIndex).substring(1, args.get(startIndex).length() - 1));
			if (startIndex == 0)
				wasWorldQuotation = true;
		}
		else
		{
			List<String> concat = new ArrayList<String>(args);
			Iterator<String> concatI = concat.iterator();

			// skip past any parameters in front of the one we're starting on
			for (int i = 1; i < startIndex + 1; i++)
			{
				concatI.next();
			}

			StringBuilder quote = new StringBuilder(concatI.next());
			while (concatI.hasNext())
			{
				String next = concatI.next();
				concatI.remove();
				quote.append(" ");
				quote.append(next);
				if (next.endsWith("\""))
				{
					concat.set(startIndex, quote.substring(1, quote.length() - 1));
					args = concat;
					if (startIndex == 0)
						wasWorldQuotation = true;
					break;
				}
			}
		}
		return args;
	}

	public Set<String> getCommandNames()
	{
		// using TreeSet to sort alphabetically
		Set<String> commands = new TreeSet<String>(subCommands.keySet());
		// removing default "commands" command as it's not normally shown or run like other commands
		commands.remove("commands");
		return commands;
	}

    // Begin Forge implementation

	@Override
	public String getCommandName()
	{
		return NAME;
	}

	@Override
	public String getCommandUsage(ICommandSender sender)
	{
		return "/wborder help [n]";
	}

	@Override
	public List getCommandAliases()
	{
		return ALIASES;
	}

	@Override
	public boolean canCommandSenderUseCommand(ICommandSender sender)
	{
		return true;
	}

	@Override
	public List addTabCompletionOptions(ICommandSender sender, String[] args)
	{
		return args.length > 0
            ? Collections.emptyList()
            : new ArrayList<String>( getCommandNames() );
	}

	@Override
	public boolean isUsernameIndex(String[] sender, int idx)
	{
		return true;
	}

    @Override
    public int compareTo(Object o)
    {
        ICommand command = (ICommand) o;
        return command.getCommandName().compareTo( getCommandName() );
    }
}
