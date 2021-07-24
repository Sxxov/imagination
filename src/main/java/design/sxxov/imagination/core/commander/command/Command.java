package design.sxxov.imagination.core.commander.command;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;

import com.onarandombox.MultiverseCore.api.MultiverseWorld;

import org.bukkit.Location;
import org.bukkit.entity.Player;

import design.sxxov.imagination.Imagination;
import design.sxxov.imagination.core.commander.CommanderReply;
import design.sxxov.imagination.core.commander.CommanderReplyBuilder;
import design.sxxov.imagination.core.configurator.Configurator;
import design.sxxov.imagination.core.multiverser.Multiverser;

public abstract class Command {
	private static WeakHashMap<Player, String> playerToWorldName = new WeakHashMap<>();
	private static WeakHashMap<Player, Boolean> playerToIsImagining = new WeakHashMap<>();
	private static WeakHashMap<Player, CommanderReply> playerToBlockCancelledReply = new WeakHashMap<>();

	public abstract boolean run(Player player, org.bukkit.command.Command command, String label, String[] args, Imagination ctx);

	public abstract String getTrigger();

	protected static String getSourceWorldName(Player player) {
		if (!Command.isImagining(player)) {
			return player.getWorld().getName();
		}

		for (Map.Entry<String, List<String>> entry : Configurator.imaginations.entrySet()) {
			for (String worldName : entry.getValue()) {
				if (worldName.equals(player.getWorld().getName())) {
					return entry.getKey();
				}
			}
		}

		throw new IllegalStateException("No source world found for world(" + player.getWorld().getName() + ")");
	}

	protected static List<String> getTargetWorldNames(Player player) {
		if (Command.isImagining(player)) {
			ArrayList<String> targetWorldNames = new ArrayList<>();
			targetWorldNames.add(player.getWorld().getName());

			return targetWorldNames;
		}

		return Configurator.imaginations.get(
            Multiverser.getMVWorld(player.getWorld()).getName()
        );
	}

	protected static boolean isImagining(Player player) {
		String cachedWorldName = Command.playerToWorldName.get(player);
		String playerWorldName = player.getWorld().getName();

		if (cachedWorldName != null
			&& playerWorldName.equals(cachedWorldName)) {
			return playerToIsImagining.get(player);
		}

		Command.playerToWorldName.put(player, playerWorldName);

        for (Map.Entry<String, List<String>> entry : Configurator.imaginations.entrySet()) {
            String sourceWorldName = entry.getKey();
            
            if (sourceWorldName.equals(playerWorldName)) {
				Command.playerToIsImagining.put(player, false);

                return false;
            }

            for (String targetWorldName : entry.getValue()) {
                if (targetWorldName.equals(playerWorldName)) {
					Command.playerToIsImagining.put(player, true);

                    return true;
                }
            }
        }

		Command.playerToIsImagining.put(player, false);

        return false;
    }

	
	protected static String getWorldCommandChoices(List<String> targetWorldNames) {
		int i = 0;
		String aliases = "";

		for (String world : targetWorldNames) {
			String alias = Multiverser.getMVWorld(world).getAlias();

			if (alias.contains(" ")) aliases += "\"";
			aliases += alias;
			if (alias.contains(" ")) aliases += "\"";
			if (i++ < targetWorldNames.size() - 1) aliases += "|";
		}

		return aliases;
	}

	protected static String getWorldCommandList(List<String> targetWorldNames) {
		int i = 0;
		String commandList = "";

		for (String world : targetWorldNames) {
			String alias = Multiverser.getMVWorld(world).getAlias();

			commandList += ++i + ". " + alias + "\n";
		}

		return commandList;
	}
	
	protected static CommanderReply getFormattedCommandListReply(Player player, List<String> targetWorldNames) {
		return Command.getFormattedCommandListReply(player, targetWorldNames, false);
	}

	protected static CommanderReply getFormattedCommandListReply(Player player, List<String> targetWorldNames, boolean replyBuilderFormat) {		
		return new CommanderReplyBuilder(player)
			.info("The imaginations in this world are:")
			.n()
			.info(Command.getWorldCommandList(targetWorldNames))
			.n()
			.info(
				"Try doing /imagine to "
				+ (targetWorldNames.size() == 1 ? "" : "<")
				+ Command.getWorldCommandChoices(targetWorldNames)
				+ (targetWorldNames.size() == 1 ? "" : ">"))
			.build(replyBuilderFormat);
	}

	protected static void teleportToWorld(Player player, MultiverseWorld world) {
		Location location = player.getLocation().clone();
		location.setWorld(world.getCBWorld());

		player.teleport(location);
	}

	public static CommanderReply getBlockCancelledReply(Player player) {
		CommanderReply reply = Command.playerToBlockCancelledReply.get(player);

		if (reply != null) {
			return reply;
		}

		CommanderReply newReply = new CommanderReplyBuilder(player)
			.error("You may not distort reality")
			.build();

		Command.playerToBlockCancelledReply.put(player, newReply);
			
		return newReply;
	}
}
