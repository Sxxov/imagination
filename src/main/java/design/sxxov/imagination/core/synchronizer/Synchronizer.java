package design.sxxov.imagination.core.synchronizer;

import java.util.HashMap;

import javax.annotation.Nullable;

import com.onarandombox.MultiverseCore.api.MultiverseWorld;
import com.sk89q.worldedit.EditSession.Stage;
import com.sk89q.worldedit.WorldEdit;
import com.sk89q.worldedit.event.extent.EditSessionEvent;
import com.sk89q.worldedit.util.eventbus.Subscribe;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.event.Cancellable;
import org.bukkit.event.EventHandler;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockBurnEvent;
import org.bukkit.event.block.BlockCookEvent;
import org.bukkit.event.block.BlockDamageEvent;
import org.bukkit.event.block.BlockDispenseEvent;
import org.bukkit.event.block.BlockExplodeEvent;
import org.bukkit.event.block.BlockFadeEvent;
import org.bukkit.event.block.BlockFertilizeEvent;
import org.bukkit.event.block.BlockFromToEvent;
import org.bukkit.event.block.BlockGrowEvent;
import org.bukkit.event.block.BlockIgniteEvent;
import org.bukkit.event.block.BlockPistonExtendEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.block.CauldronLevelChangeEvent;
import org.bukkit.event.block.FluidLevelChangeEvent;
import org.bukkit.event.block.LeavesDecayEvent;
import org.bukkit.event.block.MoistureChangeEvent;
import org.bukkit.event.block.SignChangeEvent;
import org.bukkit.event.block.SpongeAbsorbEvent;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.event.inventory.BrewEvent;
import org.bukkit.event.inventory.BrewingStandFuelEvent;
import org.bukkit.event.inventory.FurnaceBurnEvent;
import org.bukkit.event.world.ChunkLoadEvent;
import org.bukkit.event.world.ChunkUnloadEvent;
import org.bukkit.plugin.Plugin;

import design.sxxov.imagination.Imagination;
import design.sxxov.imagination.core.commander.command.Command;
import design.sxxov.imagination.core.worldedit.CancelBlockChangeExtent;
import design.sxxov.imagination.core.worldedit.SynchronizeBlockChangeExtent;

public class Synchronizer implements Listener {
	@Nullable WorldEdit worldEdit;
	private MultiverseWorld sourceWorld;
	private MultiverseWorld targetWorld;
	private Block airBlock;
	private SynchronizerChunkManager chunkManager;
	private HashMap<Long, SynchronizerChunk> borrowedChunks = new HashMap<>();
	private Plugin ctx;

	public Synchronizer(Plugin ctx, MultiverseWorld sourceWorld, MultiverseWorld targetWorld) {
		this.ctx = ctx;
		this.sourceWorld = sourceWorld;
		this.targetWorld = targetWorld;
		this.airBlock = this.targetWorld.getCBWorld().getBlockAt(0, 255, 0);
		this.chunkManager = new SynchronizerChunkManager(ctx, targetWorld.getName());

		if (Bukkit.getServer().getPluginManager().getPlugin("WorldEdit") != null) {
			this.worldEdit = WorldEdit.getInstance();
			this.worldEdit.getEventBus().register(this);
			Imagination.logger.info("Registered WorldEdit EditSessionEvent");
		}

		Bukkit.getServer().getPluginManager().registerEvents(this, ctx);
	}

	public void destroy() {
		HandlerList.unregisterAll(this);
	}

	public void flush() {
		this.chunkManager.flush();
	}

	public void synchronizeBlock(Block sourceBlock) {
		Block targetBlock = new Location(
			this.targetWorld.getCBWorld(), 
			sourceBlock.getX(),
			sourceBlock.getY(),
			sourceBlock.getZ()
		)
			.getBlock();

		targetBlock.setBlockData(sourceBlock.getBlockData());
	}

	public void whitelistBlock(Block targetBlock) {
		long id = SynchronizerChunk.getId(targetBlock.getChunk());

		this.chunkManager.get(id).blocks.add(new int[] {
			targetBlock.getX(),
			targetBlock.getY(),
			targetBlock.getZ()
		});
		this.chunkManager.dirty(id);
	}

	public void blacklistBlock(Block targetBlock) {
		long id = SynchronizerChunk.getId(targetBlock.getChunk());

		this.chunkManager.get(id).blocks.removeIf(
			(coords) -> (
				coords[0] == targetBlock.getX() 
				&& coords[1] == targetBlock.getY()
				&& coords[2] == targetBlock.getZ()
			)
		);
		this.chunkManager.dirty(id);
	}

	@Subscribe
	public void onEditSession(EditSessionEvent event) {
		if (event.getStage() == Stage.BEFORE_CHANGE) {
			event.setExtent(
				new CancelBlockChangeExtent(
					new SynchronizeBlockChangeExtent(
						event.getExtent(), 
						event.getActor(),
						this.sourceWorld,
						this.targetWorld
					),
					event.getActor(),
					this.sourceWorld,
					this.targetWorld,
					this,
					this.ctx
				)
			);
		}
	}

	private void onBlockGeneric(Cancellable event, Block block) {
		if (this.shouldBeSynchronized(block)) {
			this.synchronizeBlock(block);
		}

		boolean shouldBeCancelled = this.shouldBeCancelled(block);

		if (shouldBeCancelled) {
			event.setCancelled(true);
		}
	}

	private void onBlocksGeneric(Cancellable event, Iterable<Block> blocks) {
		for (Block block : blocks) {
			if (this.shouldBeSynchronized(block)) {
				this.synchronizeBlock(block);
			}
	
			if (this.shouldBeCancelled(block)) {
				event.setCancelled(true);

				return;
			}
		}
	}
	
	// @EventHandler
	// public void onBlockPhysics(BlockPhysicsEvent event) {
	// 	this.onBlockGeneric(event, event.getBlock());
	// }
	
	@EventHandler
	public void onBlockPlace(BlockPlaceEvent event) {
		Block block = event.getBlock();

		if (this.shouldBeSynchronized(block)) {
			this.synchronizeBlock(block);
		}

		if (this.shouldBeWhitelisted(block)) {
			this.whitelistBlock(block);
		}

		if (this.shouldBeBlacklisted(block)) {
			this.blacklistBlock(block);
		}
	}

	@EventHandler
	public void onBrew(BrewEvent event) {
		this.onBlockGeneric(event, event.getBlock());
	}

	@EventHandler
	public void onBrewingStandFuel(BrewingStandFuelEvent event) {
		this.onBlockGeneric(event, event.getBlock());
	}

	@EventHandler
	public void onCauldronLevelChange(CauldronLevelChangeEvent event) {
		this.onBlockGeneric(event, event.getBlock());
	}

	@EventHandler
	public void onFluidLevelChange(FluidLevelChangeEvent event) {
		this.onBlockGeneric(event, event.getBlock());
	}

	@EventHandler
	public void onFurnaceBurn(FurnaceBurnEvent event) {
		this.onBlockGeneric(event, event.getBlock());
	}

	@EventHandler
	public void onLeavesDecay(LeavesDecayEvent event) {
		this.onBlockGeneric(event, event.getBlock());
	}

	@EventHandler
	public void onMoistureChange(MoistureChangeEvent event) {
		this.onBlockGeneric(event, event.getBlock());
	}

	@EventHandler
	public void onSignChange(SignChangeEvent event) {
		this.onBlockGeneric(event, event.getBlock());
	}

	@EventHandler
	public void onSpongeAbsorb(SpongeAbsorbEvent event) {
		this.onBlockGeneric(event, event.getBlock());
	}

	@EventHandler
	public void onBlockIgnite(BlockIgniteEvent event) {
		this.onBlockGeneric(event, event.getBlock());
	}

	@EventHandler
	public void onBlockGrow(BlockGrowEvent event) {
		this.onBlockGeneric(event, event.getBlock());
	}

	@EventHandler
	public void onBlockFromTo(BlockFromToEvent event) {
		this.onBlockGeneric(event, event.getBlock());
	}

	@EventHandler
	public void onBlockFertilize(BlockFertilizeEvent event) {
		this.onBlockGeneric(event, event.getBlock());
	}

	@EventHandler
	public void onBlockFade(BlockFadeEvent event) {
		this.onBlockGeneric(event, event.getBlock());
	}

	@EventHandler
	public void onBlockDispense(BlockDispenseEvent event) {
		this.onBlockGeneric(event, event.getBlock());
	}

	@EventHandler
	public void onBlockDamage(BlockDamageEvent event) {
		this.onBlockGeneric(event, event.getBlock());
	}

	@EventHandler
	public void onBlockCook(BlockCookEvent event) {
		this.onBlockGeneric(event, event.getBlock());
	}

	@EventHandler
	public void onChunkLoad(ChunkLoadEvent event) {
		if (event.getWorld().getName().equals(this.sourceWorld.getName())) {	
			Long id = SynchronizerChunk.getId(event.getChunk());

			this.chunkManager.hydrate(id);
		}
	}

	@EventHandler
	public void onChunkUnload(ChunkUnloadEvent event) {
		Long id = SynchronizerChunk.getId(event.getChunk());

		if (this.borrowedChunks.containsKey(id)) {
			try {
				this.borrowedChunks.remove(id);
				this.chunkManager.dirty(id);
			} catch (IllegalStateException e) {
				e.printStackTrace();
			}
		}

		// todo: this might be triggering on every chunk
		// which kinda defeats the batching code
		if (event.isSaveChunk()) {
			this.chunkManager.flush();
		}
	}

	@EventHandler
	public void onBlockPistonExtend(BlockPistonExtendEvent event) {
		this.onBlocksGeneric(event, event.getBlocks());
	}
	
	@EventHandler
    public void onBlockBurn(BlockBurnEvent event) {
		this.onBlockGeneric(event, event.getBlock());
    }

	@EventHandler
    public void onBlockExplode(BlockExplodeEvent event) {
		this.onBlocksGeneric(event, event.blockList());
    }

	@EventHandler
    public void onEntityExplode(EntityExplodeEvent event) {
		this.onBlocksGeneric(event, event.blockList());
    }

	@EventHandler
    public void onBlockBreak(BlockBreakEvent event) {
		Block block = event.getBlock();

		if (this.shouldBeSynchronized(block)) {
			this.synchronizeBlock(this.airBlock);
		}

		if (this.shouldBeCancelled(block)) {
			event.setCancelled(true);
			Command.getBlockCancelledReply(event.getPlayer()).scheduleNextTickSingleton(this.ctx);
		}
    }

	public boolean shouldBeBlacklisted(Block block) {
		return this.isFromSourceWorld(block);
	}

	public boolean shouldBeWhitelisted(Block block) {
		return this.isFromTargetWorld(block);
	}

	public boolean shouldBeSynchronized(Block block) {
		return this.isFromSourceWorld(block);
	}

	public boolean shouldBeCancelled(Block block) {
		if (this.isFromTargetWorld(block)) {
			Location blockLocation = block.getLocation();
			long id = SynchronizerChunk.getId(
				SynchronizerChunk.getChunkCoord(blockLocation.getBlockX()),
				SynchronizerChunk.getChunkCoord(blockLocation.getBlockZ())
			);

			try {
				for (int[] coord : this.chunkManager.get(id).blocks) {
					if (coord[0] == block.getX()
						&& coord[1] == block.getY()
						&& coord[2] == block.getZ()) {
						return false;
					}
				};
			} catch (IllegalStateException e) {
				e.printStackTrace();
			}

			return true;
		}

		return false;
	}
	
	// TODO: this might be causing chunk loads
	private boolean isFromSourceWorld(Block block) {
		return this.sourceWorld.getName().equals(block.getWorld().getName());
	}

	// TODO: this might be causing chunk loads
	private boolean isFromTargetWorld(Block block) {
		return this.targetWorld.getName().equals(block.getWorld().getName());
	}

	public SynchronizerChunkManager getChunkManager() {
		return this.chunkManager;
	}
}
