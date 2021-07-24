package design.sxxov.imagination.core.commander.command;

import org.bukkit.entity.Player;

import design.sxxov.imagination.Imagination;
import design.sxxov.imagination.core.commander.CommanderReplyBuilder;

public class UnknownCommand extends Command {

	@Override
	public boolean run(Player player, org.bukkit.command.Command command, String label, String[] args, Imagination ctx) {
		new CommanderReplyBuilder(player)
			.error("Unknown command ):")
			.n()
			.info("Try doing &n/help imagination")
			.build()
			.send();

		return true;
	}

	@Override
	public String getTrigger() {
		return null;
	}
	
}
