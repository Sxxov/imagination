package design.sxxov.imagination.events;

import com.onarandombox.MultiverseCore.api.MultiverseWorld;

import org.bukkit.entity.Player;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;

public class ImaginationEnterEvent extends ImaginationAbstractEntranceEvent {
	private static final HandlerList handlers = new HandlerList();

	public ImaginationEnterEvent(Player player, MultiverseWorld sourceWorld) {
		super(player, sourceWorld);
	}

	public MultiverseWorld getTargetWorld() {
		return super.getDestinationWorld();
	}

	public MultiverseWorld getSourceWorld() {
		return this.getStartingWorld();
	}

	@Override
	public @NotNull HandlerList getHandlers() {
		return ImaginationEnterEvent.handlers;
	}

	public static HandlerList getHandlerList() {
		return ImaginationEnterEvent.handlers;
	}
}
