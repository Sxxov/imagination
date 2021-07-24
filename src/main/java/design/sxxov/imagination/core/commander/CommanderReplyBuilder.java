package design.sxxov.imagination.core.commander;

import java.util.ArrayList;

import org.bukkit.entity.Player;

public class CommanderReplyBuilder {
	private Player player;
	private ArrayList<String> messages = new ArrayList<>();

	public CommanderReplyBuilder(Player player) {
		this.player = player;
	}

	public CommanderReplyBuilder n() {
		messages.add("");

		return this;
	}

	public CommanderReplyBuilder error(CommanderReply reply) {
		return this.error(reply.getMessage());
	}

	public CommanderReplyBuilder error(String message) {
		messages.add(CommanderReplyBuilder.colourize(message, 'c'));

		return this;
	}

	public CommanderReplyBuilder warning(CommanderReply reply) {
		return this.warning(reply.getMessage());
	}

	public CommanderReplyBuilder warning(String message) {
		messages.add(CommanderReplyBuilder.colourize(message, 'e'));

		return this;
	}

	public CommanderReplyBuilder info(CommanderReply reply) {
		return this.info(reply.getMessage());
	}

	public CommanderReplyBuilder info(String message) {
		messages.add(CommanderReplyBuilder.colourize(message, 'f'));

		return this;
	}

	public CommanderReply build() {
		return this.build(true);
	}

	public CommanderReply build(boolean format) {
		String replyMessage = "";

		if (format) replyMessage += "\n";
		if (format) replyMessage += "§7§l[§8§lImagination§7§l]\n";
		
		for (String message : this.messages) {
			for (String line : message.split("\\n")) {
				if (format) replyMessage += "    ";
				replyMessage += line.replace("&(?=(\\d|[abcdefklmnor]))", "§");
				replyMessage += "\n";
			}
		}

		if (format) replyMessage += "\n";

		return new CommanderReply(this.player, replyMessage);
	}

	private static String colourize(String string, char colour) {
		String[] lines = string.split("\\n");
		String colourized = "";

		for (int i = 0, l = lines.length; i < l; ++i) {
			colourized += "§" + colour + lines[i];
			if (i != l - 1) colourized += "\n";
		}
		
		return colourized;
	}
}
