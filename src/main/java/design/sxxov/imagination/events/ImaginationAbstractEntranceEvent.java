package design.sxxov.imagination.events;

import com.onarandombox.MultiverseCore.api.MultiverseWorld;

import org.bukkit.entity.Player;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;

public abstract class ImaginationAbstractEntranceEvent extends ImaginationTeleportEvent {
	public ImaginationAbstractEntranceEvent(Player player, MultiverseWorld destinationWorld) {
		super(player, destinationWorld);
	}

	public abstract MultiverseWorld getTargetWorld();

	public abstract MultiverseWorld getSourceWorld();

	public abstract @NotNull HandlerList getHandlers();
}
