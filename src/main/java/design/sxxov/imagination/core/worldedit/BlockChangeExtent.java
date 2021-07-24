package design.sxxov.imagination.core.worldedit;

import java.util.UUID;

import javax.annotation.Nullable;

import org.bukkit.Bukkit;

import com.onarandombox.MultiverseCore.api.MultiverseWorld;
import com.sk89q.worldedit.extension.platform.Actor;
import com.sk89q.worldedit.extent.AbstractDelegateExtent;
import com.sk89q.worldedit.extent.Extent;

import org.bukkit.entity.Player;

import design.sxxov.imagination.Imagination;

public abstract class BlockChangeExtent extends AbstractDelegateExtent {
	private @Nullable Player player;
	private MultiverseWorld sourceWorld;
	private MultiverseWorld targetWorld;
	private boolean isEnabled = false;

	protected BlockChangeExtent(Extent extent, Actor actor, MultiverseWorld sourceWorld, MultiverseWorld targetWorld) {
		super(extent);

		this.player = BlockChangeExtent.getPlayer(actor);
		this.sourceWorld = sourceWorld;
		this.targetWorld = targetWorld;

		if (player == null) {
			Imagination.logger.warning("Failed to get the player who executed the WorldEdit command");
			Imagination.logger.warning("Applying everywhere");

			this.isEnabled = true;

			return;
		}

		if (!player.getWorld().getName().equals(this.getEnabledWorld().getName())) {
			return;
		}

		this.isEnabled = true;
	}

	protected MultiverseWorld getSourceWorld() {
		return this.sourceWorld;
	}

	protected MultiverseWorld getTargetWorld() {
		return this.targetWorld;
	}

	protected boolean getIsEnabled() {
		return this.isEnabled;
	}

	protected abstract MultiverseWorld getEnabledWorld();

	protected Player getPlayer() {
		return this.player;
	}

	protected static @Nullable Player getPlayer(Actor actor) {
		Player player = null;

		if (actor == null
			|| !actor.isPlayer()) {
			// Imagination.logger.warning("You seem to be running WorldEdit in the console, this is unsupported");
			// Imagination.logger.warning("The changes won't be mirrored into the imagination");

			return null;
		}

		UUID uuid = actor.getUniqueId();

		for (Object serverPlayer : Bukkit.getServer().getOnlinePlayers().toArray()) {
			if (serverPlayer instanceof Player
				&& ((Player) serverPlayer).getUniqueId().equals(uuid)) {
				player = (Player) serverPlayer;

				break;
			}
		}

		if (player == null) {
			// Imagination.logger.warning("Failed to get the world that the player is WorldEdit in");
			// Imagination.logger.warning("The changes won't be mirrored into the imagination");

			return null;
		}

		return player;
	}
}
