package design.sxxov.imagination.events;

import com.onarandombox.MultiverseCore.api.MultiverseWorld;

import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;

import design.sxxov.imagination.core.multiverser.Multiverser;

public class ImaginationTeleportEvent extends Event {
	private static final HandlerList handlers = new HandlerList();
	private Player player;
	private MultiverseWorld startingWorld;
	private MultiverseWorld destinationWorld;

	public ImaginationTeleportEvent(Player player, MultiverseWorld destinationWorld) {
		super();

		this.player = player;
		this.startingWorld = Multiverser.getMVWorld(this.player.getWorld());
		this.destinationWorld = destinationWorld;
	}

	@Override
	public @NotNull HandlerList getHandlers() {
		return ImaginationTeleportEvent.handlers;
	}

	public static HandlerList getHandlerList() {
		return ImaginationTeleportEvent.handlers;
	}

	public Player getPlayer() {
		return this.player;
	}

	public MultiverseWorld getStartingWorld() {
		return this.startingWorld;
	}

	public MultiverseWorld getDestinationWorld() {
		return this.destinationWorld;
	}
}
