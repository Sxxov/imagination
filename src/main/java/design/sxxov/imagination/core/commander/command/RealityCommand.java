package design.sxxov.imagination.core.commander.command;

import com.onarandombox.MultiverseCore.api.MultiverseWorld;

import org.bukkit.entity.Player;

import design.sxxov.imagination.Imagination;
import design.sxxov.imagination.core.commander.CommanderReplyBuilder;
import design.sxxov.imagination.core.multiverser.Multiverser;

public class RealityCommand extends Command {

	@Override
	public boolean run(
		Player player,
		org.bukkit.command.Command command,
		String label,
		String[] args,
		Imagination ctx
	) {
		if (!Command.isImagining(player)) {
			new CommanderReplyBuilder(player)
				.info("You are already in reality (:")
				.build()
				.send();

			return true;
		}

		MultiverseWorld world = Multiverser.getMVWorld(Command.getSourceWorldName(player), true);

		new CommanderReplyBuilder(player)
			.info("Snapping back to reality…")
			.build()
			.send();
			
		Command.teleportToWorld(player, world);

		return true;
	}

	@Override
	public String getTrigger() {
		return "reality";
	}
	
}
