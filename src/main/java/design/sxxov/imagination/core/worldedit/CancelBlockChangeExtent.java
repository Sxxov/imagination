package design.sxxov.imagination.core.worldedit;

import com.onarandombox.MultiverseCore.api.MultiverseWorld;
import com.sk89q.worldedit.WorldEditException;
import com.sk89q.worldedit.extension.platform.Actor;
import com.sk89q.worldedit.extent.Extent;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.world.block.BlockStateHolder;

import org.bukkit.plugin.Plugin;

import design.sxxov.imagination.core.commander.command.Command;
import design.sxxov.imagination.core.synchronizer.Synchronizer;
import design.sxxov.imagination.core.synchronizer.SynchronizerChunk;

public class CancelBlockChangeExtent extends BlockChangeExtent {
	private Synchronizer synchronizer;
	private Plugin ctx;

	public CancelBlockChangeExtent(
		Extent extent,
		Actor actor, 
		MultiverseWorld sourceWorld,
		MultiverseWorld targetWorld,
		Synchronizer synchronizer,
		Plugin ctx
	) {
		super(extent, actor, sourceWorld, targetWorld);

		this.synchronizer = synchronizer;
		this.ctx = ctx;
	}
	
	@Override
	public <T extends BlockStateHolder<T>> boolean setBlock(BlockVector3 location, T block) throws WorldEditException {
		if (!this.getIsEnabled()) {
			return super.setBlock(location, block);
		}

		int x = location.getBlockX();
		int y = location.getBlockY();
		int z = location.getBlockZ();
		long id = SynchronizerChunk.getId(
			SynchronizerChunk.getChunkCoord(x),
			SynchronizerChunk.getChunkCoord(z)
		);

		try {
 			for (int[] coord : this.synchronizer.getChunkManager().get(id).blocks) {
				if (coord[0] == x
					&& coord[1] == y
					&& coord[2] == z) {		
					return super.setBlock(location, block);
				}
			};
		} catch (IllegalStateException e) {
			e.printStackTrace();
		}

		Command.getBlockCancelledReply(this.getPlayer()).scheduleNextTickSingleton(this.ctx);

		return false;
	}

	@Override
	protected MultiverseWorld getEnabledWorld() {
		return this.getTargetWorld();
	}
}
