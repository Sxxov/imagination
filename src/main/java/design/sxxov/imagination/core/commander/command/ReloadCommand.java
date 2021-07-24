package design.sxxov.imagination.core.commander.command;

import org.bukkit.entity.Player;

import design.sxxov.imagination.Imagination;
import design.sxxov.imagination.core.commander.CommanderReplyBuilder;
import design.sxxov.imagination.core.configurator.Configurator;

public class ReloadCommand extends Command {
	@Override
	public boolean run(Player player, org.bukkit.command.Command command, String label, String[] args, Imagination ctx) {
		Configurator.reload(ctx);

		ctx.destroy();
		ctx.configure();

		new CommanderReplyBuilder(player)
			.info("Successfully reloaded")
			.build()
			.send();

		return true;
	}

	@Override
	public String getTrigger() {
		return "reload";
	}
}
