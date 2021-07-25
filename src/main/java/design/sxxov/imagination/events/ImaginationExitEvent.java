package design.sxxov.imagination.events;

import com.onarandombox.MultiverseCore.api.MultiverseWorld;

import org.bukkit.entity.Player;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;

public class ImaginationExitEvent extends ImaginationAbstractEntranceEvent {
	private static final HandlerList handlers = new HandlerList();

	public ImaginationExitEvent(Player player, MultiverseWorld targetWorld) {
		super(player, targetWorld);
	}

	public MultiverseWorld getTargetWorld() {
		return super.getStartingWorld();
	}

	public MultiverseWorld getSourceWorld() {
		return this.getDestinationWorld();
	}

	@Override
	public @NotNull HandlerList getHandlers() {
		return ImaginationExitEvent.handlers;
	}

	public static HandlerList getHandlerList() {
		return ImaginationExitEvent.handlers;
	}
}
