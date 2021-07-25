package design.sxxov.imagination.core.commander.command;

import com.onarandombox.MultiverseCore.api.MultiverseWorld;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import design.sxxov.imagination.Imagination;
import design.sxxov.imagination.core.commander.CommanderReplyBuilder;
import design.sxxov.imagination.core.multiverser.Multiverser;
import design.sxxov.imagination.events.ImaginationExitEvent;

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

		MultiverseWorld sourceWorld = Multiverser.getMVWorld(Command.getSourceWorldName(player), true);
		MultiverseWorld targetWorld = Multiverser.getMVWorld(player.getWorld());

		new CommanderReplyBuilder(player)
			.info("Snapping back to reality…")
			.build()
			.send();
			
		Command.teleportToWorld(player, sourceWorld);

		ImaginationExitEvent event = new ImaginationExitEvent(player, targetWorld);
		Bukkit.getServer().getPluginManager().callEvent(event);

		return true;
	}

	@Override
	public String getTrigger() {
		return "reality";
	}
	
}
