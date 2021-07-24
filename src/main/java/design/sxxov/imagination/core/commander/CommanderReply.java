package design.sxxov.imagination.core.commander;

import java.util.ArrayList;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

public class CommanderReply {
	private Player player;
	private String message;
	private static ArrayList<CommanderReply> scheduledSingletonReplies = new ArrayList<>();

	public CommanderReply(Player player, String message) {
		this.player = player;
		this.message = message;
	}

	public void send() {
		if (this.player == null) {
			return;
		}

		this.player.sendMessage(this.message);
	}

	public void scheduleNextTick(Plugin ctx) {
		Bukkit.getScheduler().runTaskAsynchronously(ctx, new Runnable() {
			@Override
			public void run() {
				CommanderReply.this.send();
			}
		});
	}

	public void scheduleNextTickSingleton(Plugin ctx) {
		if (CommanderReply.scheduledSingletonReplies.contains(this)) {
			return;
		}

		CommanderReply.scheduledSingletonReplies.add(this);
		Bukkit.getScheduler().runTaskAsynchronously(ctx, new Runnable() {
			@Override
			public void run() {
				CommanderReply.this.send();
				CommanderReply.scheduledSingletonReplies.remove(CommanderReply.this);
			}
		});
	}

	public String getMessage() {
		return this.message;
	}
}
