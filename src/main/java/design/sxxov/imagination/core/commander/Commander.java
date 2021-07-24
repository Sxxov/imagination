package design.sxxov.imagination.core.commander;

import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import design.sxxov.imagination.Imagination;
import design.sxxov.imagination.core.commander.command.Command;
import design.sxxov.imagination.core.commander.command.EmptyCommand;
import design.sxxov.imagination.core.commander.command.RealityCommand;
import design.sxxov.imagination.core.commander.command.ReloadCommand;
import design.sxxov.imagination.core.commander.command.ToCommand;
import design.sxxov.imagination.core.commander.command.UnknownCommand;

import java.rmi.UnexpectedException;
import java.util.Arrays;

public class Commander {
	Imagination ctx;

	public Commander(Imagination ctx) {
		this.ctx = ctx;
	}

	public boolean onCommand(CommandSender sender, org.bukkit.command.Command command, String label, String[] args) throws UnexpectedException {
        if (!(sender instanceof Player)) {
            sender.sendMessage("Imagination can only be accessed by a player");

            return true;
        }

        Player player = (Player) sender;
		
		switch (command.getName()) {
			case "imagination":
				return onImagine(
					player,
					command,
					label,
					args
				);
			case "reality":
				String[] argsPadded = new String[args.length + 1];
				argsPadded[0] = "reality";

				System.arraycopy(
					args,
					0,
					argsPadded,
					1,
					args.length
				);
				
				return onImagine(
					player,
					command,
					label,
					argsPadded
				);
			default:
				throw new UnexpectedException("The server gave the plugin an unknown command(" + command.getName() + ") to handle");
		}
	}

	private boolean onImagine(Player player, org.bukkit.command.Command command, String label, String[] args) {
		if (args.length == 0) {
			return new EmptyCommand().run(player, command, label, args, ctx);
		}

		String[] restArgs = Arrays.copyOfRange(args, 1, args.length);
		
		
		Command[] commands = new Command[] {
			new ReloadCommand(),
			new ToCommand(),
			new RealityCommand(),
		};

		for (Command subCommand : commands) {
			if (subCommand.getTrigger().equals(args[0])) {
				return subCommand.run(player, command, label, restArgs, ctx);
			}
		}

		return new UnknownCommand().run(player, command, label, restArgs, ctx);
	}
}
