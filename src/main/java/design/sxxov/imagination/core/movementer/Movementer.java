package design.sxxov.imagination.core.movementer;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.plugin.Plugin;

import design.sxxov.imagination.core.commander.CommanderReply;
import design.sxxov.imagination.core.commander.CommanderReplyBuilder;
import design.sxxov.imagination.events.ImaginationEnterEvent;
import design.sxxov.imagination.events.ImaginationExitEvent;

public class Movementer implements Listener {
	private final class MovementerOnPlayerMoveListener implements Listener {
		@EventHandler
		public void onPlayerMove(PlayerMoveEvent event) {
			Movementer me = Movementer.this;

			if (!event.getPlayer().getWorld().getName().equals(me.enabledWorldName)) {
				me.unregister();

				return;
			}
	
			Location from = event.getFrom();
			Location to = event.getTo();
			int distanceFromToToSource = me.getDistanceFromSource(to);
	
			if (distanceFromToToSource >= me.radius) {
				to.setX(from.getX());
				to.setY(from.getY());
				to.setZ(from.getZ());

				if (!me.isPlayerAtEdge) {
					me.isPlayerAtEdge = true;
					me.atEdgeReply.send();
				}
			} else {
				if (me.isPlayerAtEdge) {
					me.isPlayerAtEdge = false;
				}
			}
		}
	}

	Plugin ctx;
	Player player;
	private String enabledWorldName;
	private int[] sourceLocation;
	private int radius;
	private boolean isPlayerAtEdge;
	private final Listener listener = new MovementerOnPlayerMoveListener();
	private final CommanderReply atEdgeReply;

	public Movementer(
		Plugin ctx,
		Player player,
		String enabledWorldName,
		int radius
	) {
		this.ctx = ctx;
		this.player = player;
		this.enabledWorldName = enabledWorldName;
		this.radius = radius;
		this.atEdgeReply = new CommanderReplyBuilder(player)
			.info("Hmm...")
			.info("You can't seem to imagine any further.")
			.build();

		Bukkit.getServer().getPluginManager().registerEvents(this, this.ctx);
	}

	@EventHandler
	public void onImaginationEnterEvent(ImaginationEnterEvent event) {
		Player player = event.getPlayer();

		if (player.getUniqueId() != this.player.getUniqueId()) {
			return;
		}

		if (!player.getWorld().getName().equals(this.enabledWorldName)) {
			return;
		}

		Location location = event.getPlayer().getLocation();
		this.sourceLocation = new int[] {
			(int) location.getX(),
			(int) location.getY(),
			(int) location.getZ(),
		};

		this.register();
	}

	@EventHandler
	public void onImaginationExitEvent(ImaginationExitEvent event) {
		Player player = event.getPlayer();
		
		if (player.getUniqueId() != this.player.getUniqueId()) {
			return;
		}

		this.unregister();

		if (this.sourceLocation == null) {
			return;
		}

		player.teleport(
			new Location(
				player.getWorld(),
				(double) this.sourceLocation[0],
				(double) this.sourceLocation[1],
				(double) this.sourceLocation[2]
			)
		);
	}

	public void destroy() {
		this.unregister();
		HandlerList.unregisterAll(this);
	}

	public void register() {
		Bukkit.getServer().getPluginManager().registerEvents(this.listener, this.ctx);
	}

	public void unregister() {
		HandlerList.unregisterAll(this.listener);
	}

	// private void setPlayerSpeed(float speed) {
	// 	this.player.setFlySpeed(speed);
	// 	this.player.setWalkSpeed(speed);
	// }

	// private float getSpeed(int distanceFromSource) {
	// 	return Math.max(
	// 		Math.min(
	// 			(
	// 				this.minimumSpeed 
	// 				+ (
	// 					1f - (
	// 						Math.max(
	// 							distanceFromSource - this.graceRadius,
	// 							0
	// 						)
	// 						/ (float) this.fallOffDistance
	// 					)
	// 				)
	// 			)
	// 			/ (1f + this.minimumSpeed),
	// 			1f
	// 		),
	// 		0
	// 	);
	// }

	private int getDistanceFromSource(Location location) {
		int[] targetLocation = new int[] {
			location.getBlockX(),
			location.getBlockY(),
			location.getBlockZ()
		};

		return (int) Math.sqrt(
			Math.pow(targetLocation[0] - this.sourceLocation[0], 2) 
			+ Math.pow(targetLocation[1] - this.sourceLocation[1], 2) 
			+ Math.pow(targetLocation[2] - this.sourceLocation[2], 2)
		);
	}
}
