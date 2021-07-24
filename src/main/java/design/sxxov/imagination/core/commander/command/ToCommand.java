package design.sxxov.imagination.core.commander.command;

import java.util.List;

import com.onarandombox.MultiverseCore.api.MultiverseWorld;

import org.bukkit.entity.Player;

import design.sxxov.imagination.Imagination;
import design.sxxov.imagination.core.commander.CommanderReplyBuilder;
import design.sxxov.imagination.core.multiverser.Multiverser;

public class ToCommand extends Command {
	@Override
	public boolean run(
		Player player, 
		org.bukkit.command.Command command, 
		String label, 
		String[] args,
		Imagination ctx
	) {
		if (Command.isImagining(player)) {
			new CommanderReplyBuilder(player)
				.info("You are already imagining (:")
				.build()
				.send();

			return true;
		}

		List<String> targetWorldNames = Command.getTargetWorldNames(player);

		if (args.length == 0) {
			new CommanderReplyBuilder(player)
				.error("You must specify where &nto")
				.info(Command.getFormattedCommandListReply(player, targetWorldNames))
				.build()
				.send();
		
			return true;
		}

		if (args[0].equals("reality")) {
			return new RealityCommand().run(player, command, label, args, ctx);
		}

		MultiverseWorld world = Multiverser.getMVWorld(args[0], true);

		if (world == null) {
			new CommanderReplyBuilder(player)
				.error("Invalid world")
				.info(Command.getFormattedCommandListReply(player, targetWorldNames))
				.build()
				.send();

			return true;
		}

		new CommanderReplyBuilder(player)
				.info("Imagining " + world.getAlias() + "…")
				.build()
				.send();

		Command.teleportToWorld(player, world);

		return true;
	}

	@Override
	public String getTrigger() {
		return "to";
	}
}
