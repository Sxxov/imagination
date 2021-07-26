package design.sxxov.imagination.core.synchronizer;

import java.util.HashMap;
import java.util.List;

import javax.annotation.Nullable;

import com.onarandombox.MultiverseCore.api.MultiverseWorld;
import com.sk89q.worldedit.EditSession.Stage;
import com.sk89q.worldedit.WorldEdit;
import com.sk89q.worldedit.event.extent.EditSessionEvent;
import com.sk89q.worldedit.util.eventbus.Subscribe;

import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.block.data.BlockData;
import org.bukkit.entity.EntityType;
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
	protected MultiverseWorld sourceWorld;
	protected MultiverseWorld targetWorld;
	protected World sourceWorldCB;
	protected World targetWorldCB;
	protected String sourceWorldName;
	protected String targetWorldName;
	protected SynchronizerChunkManager chunkManager;
	protected SynchronizerMutabilityManager mutabilityManager;
	protected SynchronizerChangeManager changeManager;
	protected HashMap<Long, SynchronizerChunk> borrowedChunks = new HashMap<>();
	private Plugin ctx;

	public Synchronizer(Plugin ctx, MultiverseWorld sourceWorld, MultiverseWorld targetWorld) {
		this.ctx = ctx;
		this.sourceWorld = sourceWorld;
		this.targetWorld = targetWorld;
		this.sourceWorldCB = sourceWorld.getCBWorld();
		this.targetWorldCB = targetWorld.getCBWorld();
		this.sourceWorldName = sourceWorld.getName();
		this.targetWorldName = targetWorld.getName();
		this.chunkManager = new SynchronizerChunkManager(this.ctx, targetWorld.getName());
		this.mutabilityManager = new SynchronizerMutabilityManager(this.ctx);
		this.changeManager = new SynchronizerChangeManager(this.chunkManager, this.mutabilityManager);

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
		this.changeManager.applySync(this.targetWorldCB);
		this.chunkManager.flush();
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

	/**
	 * @return true if synchronized, false if not
	 */
	private boolean onGeneric(Block block, @Nullable Material synchronizeReplacement) {
		Long chunkId = SynchronizerChunk.getId(
			SynchronizerChunk.getChunkCoord(block.getX()),
			SynchronizerChunk.getChunkCoord(block.getZ())
		);

		if (this.shouldBeSynchronized(block)) {
			if (synchronizeReplacement == null) {
				this.changeManager.batch(
					new SynchronizerChange<BlockData>(
						new int[] {
							block.getX(),
							block.getY(),
							block.getZ(),
						},
						chunkId,
						this.targetWorldCB,
						block.getBlockData()
					)
				);
			} else {
				this.changeManager.batch(
					new SynchronizerChange<Material>(
						new int[] {
							block.getX(),
							block.getY(),
							block.getZ(),
						},
						chunkId,
						this.targetWorldCB,
						synchronizeReplacement
					)
				);
			}

			return true;
		}

		return false;
	}

	/**
	 * @return true if cancelled, false if not
	 */
	private boolean onCancellableGeneric(Cancellable event, Block block, Material synchronizeReplacement) {
		boolean synced = this.onGeneric(block, synchronizeReplacement);

		if (synced) {
			return false;
		}
		
		if (this.shouldBeCancelled(block)) {
			event.setCancelled(true);

			return true;
		}

		return false;
	}

	
	/**
	 * @return true if cancelled, false if not
	 */
	private boolean onCancellableGeneric(Cancellable event, Iterable<Block> blocks, Material synchronizeReplacement) {
		for (Block block : blocks) {
			boolean isCancelled = this.onCancellableGeneric(event, block, synchronizeReplacement);

			if (isCancelled) {
				return true;
			}
		}

		return false;
	}

	/**
	 * @return true if synchronized, false if not
	 */
	private boolean onGeneric(Block block) {
		return this.onGeneric(block, null);
	}

	/**
	 * @return true if cancelled, false if not
	 */
	private boolean onCancellableGeneric(Cancellable event, Block block) {
		return this.onCancellableGeneric(event, block, null);
	}

	/**
	 * @return true if cancelled, false if not
	 */
	private boolean onCancellableGeneric(Cancellable event, Iterable<Block> blocks) {
		return this.onCancellableGeneric(event, blocks, null);
	}

	
	@EventHandler
	public void onChunkLoad(ChunkLoadEvent event) {
		World world = event.getWorld();
		Chunk chunk = event.getChunk();
		Long chunkId = SynchronizerChunk.getId(chunk);

		if (world.getName().equals(this.targetWorldName)) {
			// this.changeManager.applyAsync(world, chunkId);
			this.changeManager.applySync(world, chunkId, chunk);
		}

		if (world.getName().equals(this.sourceWorldName)) {
			this.chunkManager.hydrate(chunkId);
		}
	}

	@EventHandler
	public void onChunkUnload(ChunkUnloadEvent event) {
		World world = event.getWorld();
		Long chunkId = SynchronizerChunk.getId(event.getChunk());

		if (world.getName().equals(this.sourceWorldName)) {
			this.changeManager.applyAsync(world, chunkId);
		}
	}
	
	@EventHandler
	public void onBlockPlace(BlockPlaceEvent event) {
		Block block = event.getBlock();

		this.onGeneric(block);

		if (this.isFromTargetWorld(block)) {
			this.mutabilityManager.setMutable(block);
		}
	}

	@EventHandler
    public void onBlockBreak(BlockBreakEvent event) {
		Block block = event.getBlock();

		Long chunkId = SynchronizerChunk.getId(
			SynchronizerChunk.getChunkCoord(block.getX()),
			SynchronizerChunk.getChunkCoord(block.getZ())
		);

		if (this.shouldBeSynchronized(block)) {
			this.changeManager.batch(
				new SynchronizerChange<Material>(
					new int[] {
						block.getX(),
						block.getY(),
						block.getZ(),
					},
					chunkId,
					this.targetWorldCB,
					Material.AIR
				)
			);

			return;
		}
		
		if (this.shouldBeCancelled(block)) {
			event.setCancelled(true);
			Command.getBlockCancelledReply(event.getPlayer()).scheduleNextTickSingleton(this.ctx);
		}
    }

	//#region Miscellanious events

	@EventHandler
	public void onBrew(BrewEvent event) {
		this.onCancellableGeneric(event, event.getBlock());
	}

	@EventHandler
	public void onBrewingStandFuel(BrewingStandFuelEvent event) {
		this.onCancellableGeneric(event, event.getBlock());
	}

	@EventHandler
	public void onCauldronLevelChange(CauldronLevelChangeEvent event) {
		this.onCancellableGeneric(event, event.getBlock());
	}

	@EventHandler
	public void onFluidLevelChange(FluidLevelChangeEvent event) {
		this.onCancellableGeneric(event, event.getBlock());
	}

	@EventHandler
	public void onFurnaceBurn(FurnaceBurnEvent event) {
		this.onCancellableGeneric(event, event.getBlock());
	}

	@EventHandler
	public void onLeavesDecay(LeavesDecayEvent event) {
		this.onCancellableGeneric(event, event.getBlock(), Material.AIR);
	}

	@EventHandler
	public void onMoistureChange(MoistureChangeEvent event) {
		this.onCancellableGeneric(event, event.getBlock());
	}

	@EventHandler
	public void onSignChange(SignChangeEvent event) {
		this.onCancellableGeneric(event, event.getBlock());
	}

	@EventHandler
	public void onSpongeAbsorb(SpongeAbsorbEvent event) {
		List<BlockState> blockStates = event.getBlocks();

		for (BlockState blockState : blockStates) {
			this.onCancellableGeneric(event, blockState.getBlock(), blockState.getType());
		}
	}

	@EventHandler
	public void onBlockIgnite(BlockIgniteEvent event) {
		this.onCancellableGeneric(event, event.getBlock());
	}

	@EventHandler
	public void onBlockGrow(BlockGrowEvent event) {
		this.onCancellableGeneric(event, event.getBlock());
	}

	@EventHandler
	public void onBlockFromTo(BlockFromToEvent event) {
		this.onCancellableGeneric(event, event.getBlock());
	}

	@EventHandler
	public void onBlockFertilize(BlockFertilizeEvent event) {
		this.onCancellableGeneric(event, event.getBlock());
	}

	@EventHandler
	public void onBlockFade(BlockFadeEvent event) {
		this.onCancellableGeneric(event, event.getBlock());
	}

	@EventHandler
	public void onBlockDispense(BlockDispenseEvent event) {
		this.onCancellableGeneric(event, event.getBlock());
	}

	@EventHandler
	public void onBlockDamage(BlockDamageEvent event) {
		this.onCancellableGeneric(event, event.getBlock());
	}

	@EventHandler
	public void onBlockCook(BlockCookEvent event) {
		this.onCancellableGeneric(event, event.getBlock());
	}

	@EventHandler
	public void onBlockPistonExtend(BlockPistonExtendEvent event) {
		this.onCancellableGeneric(event, event.getBlocks());
	}
	
	@EventHandler
    public void onBlockBurn(BlockBurnEvent event) {
		this.onCancellableGeneric(event, event.getBlock());
    }

	@EventHandler
    public void onBlockExplode(BlockExplodeEvent event) {
		this.onCancellableGeneric(event, event.getBlock(), Material.AIR);
		this.onCancellableGeneric(event, event.blockList(), Material.AIR);
    }

	@EventHandler
    public void onEntityExplode(EntityExplodeEvent event) {
		if (event.getEntityType() == EntityType.PRIMED_TNT) {
			this.onCancellableGeneric(event, event.getLocation().getBlock(), Material.AIR);
		}

		this.onCancellableGeneric(event, event.blockList(), Material.AIR);
    }

	//#endregion Miscellanious events

	public boolean shouldBeSynchronized(Block block) {
		return this.isFromSourceWorld(block);
	}

	public boolean shouldBeCancelled(Block block) {
		if (this.isFromTargetWorld(block)) {
			return !SynchronizerMutabilityManager.isMutable(block);
		}

		return false;
	}
	
	private boolean isFromSourceWorld(Block block) {
		return this.sourceWorldName.equals(block.getWorld().getName());
	}

	private boolean isFromTargetWorld(Block block) {
		return this.targetWorldName.equals(block.getWorld().getName());
	}

	public SynchronizerChunkManager getChunkManager() {
		return this.chunkManager;
	}

	public SynchronizerChangeManager getChangeManager() {
		return this.changeManager;
	}

	public MultiverseWorld getSourceWorld() {
		return this.sourceWorld;
	}

	public MultiverseWorld getTargetWorld() {
		return this.targetWorld;
	}

	public String getSourceWorldName() {
		return this.sourceWorldName;
	}

	public String getTargetWorldName() {
		return this.targetWorldName;
	}

	public World getSourceWorldCB() {
		return this.sourceWorldCB;
	}

	public World getTargetWorldCB() {
		return this.targetWorldCB;
	}
}
