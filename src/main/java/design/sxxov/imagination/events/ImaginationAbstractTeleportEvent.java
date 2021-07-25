package design.sxxov.imagination.events;

import com.onarandombox.MultiverseCore.api.MultiverseWorld;

import org.bukkit.entity.Player;

import design.sxxov.imagination.core.multiverser.Multiverser;

public abstract class ImaginationAbstractTeleportEvent {
	private Player player;
	private MultiverseWorld startingWorld;
	private MultiverseWorld destinationWorld;

	public ImaginationAbstractTeleportEvent(Player player, MultiverseWorld destinationWorld) {
		this.player = player;
		this.startingWorld = Multiverser.getMVWorld(this.player.getWorld());
		this.destinationWorld = destinationWorld;
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
