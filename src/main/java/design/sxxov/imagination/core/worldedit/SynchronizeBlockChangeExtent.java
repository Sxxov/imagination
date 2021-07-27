package design.sxxov.imagination.core.worldedit;

import com.onarandombox.MultiverseCore.api.MultiverseWorld;
import com.sk89q.worldedit.WorldEditException;
import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldedit.extension.platform.Actor;
import com.sk89q.worldedit.extent.Extent;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.world.block.BlockStateHolder;

import org.bukkit.block.data.BlockData;

import design.sxxov.imagination.core.synchronizer.SynchronizerChange;
import design.sxxov.imagination.core.synchronizer.SynchronizerChangeManager;
import design.sxxov.imagination.core.synchronizer.SynchronizerChunk;

public class SynchronizeBlockChangeExtent extends BlockChangeExtent {
	private SynchronizerChangeManager synchronizerChangeManager;
	private Extent extent;

	public SynchronizeBlockChangeExtent(
		Extent extent,
		Actor actor,
		MultiverseWorld sourceWorld,
		MultiverseWorld targetWorld,
		SynchronizerChangeManager synchronizerChangeManager
	) {
		super(extent, actor, sourceWorld, targetWorld);

		this.synchronizerChangeManager = synchronizerChangeManager;
		this.extent = extent;
	}
	
	@Override
	public <T extends BlockStateHolder<T>> boolean setBlock(BlockVector3 location, T block) throws WorldEditException {
		if (this.getIsEnabled()) {
			int x = location.getBlockX();
			int y = location.getBlockY();
			int z = location.getBlockZ();
			
			this.synchronizerChangeManager.batch(
				new SynchronizerChange<BlockData>(
					new int[] { x, y, z },
					SynchronizerChunk.getId(
						SynchronizerChunk.getChunkCoord(x),
						SynchronizerChunk.getChunkCoord(z)
					),
					this.getTargetWorld().getCBWorld(),
					BukkitAdapter.adapt(block)
				)
			);
		}

		return this.extent.setBlock(location, block);
	}

	
	@Override
	protected MultiverseWorld getEnabledWorld() {
		return this.getSourceWorld();
	}
}
