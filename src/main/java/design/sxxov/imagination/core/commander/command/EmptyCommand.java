package design.sxxov.imagination.core.commander.command;

import java.util.List;

import org.bukkit.entity.Player;

import design.sxxov.imagination.Imagination;
import design.sxxov.imagination.core.commander.CommanderReplyBuilder;

public class EmptyCommand extends Command {
	@Override
	public boolean run(
		Player player, 
		org.bukkit.command.Command command, 
		String label, 
		String[] args,
		Imagination ctx) {
		if (Command.isImagining(player)) {
			new CommanderReplyBuilder(player)
				.info("You are already imagining (:")
				.build()
				.send();

			return true;
		}

		List<String> targetWorldNames = Command.getTargetWorldNames(player);

		if (targetWorldNames.size() > 1) {
			new CommanderReplyBuilder(player)
				.info("There are multiple imaginations in this world")
				.info(Command.getFormattedCommandListReply(player, targetWorldNames))
				.build()
				.send();

			return true;
		}

		return new ToCommand().run(
			player,
			command,
			label,
			new String[] { targetWorldNames.get(0) },
			ctx
		);
	}

	@Override
	public String getTrigger() {
		return null;
	}
}
