package design.sxxov.imagination.core.worldedit;

import com.onarandombox.MultiverseCore.api.MultiverseWorld;
import com.sk89q.worldedit.WorldEditException;
import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldedit.extension.platform.Actor;
import com.sk89q.worldedit.extent.Extent;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.world.block.BlockStateHolder;

import org.bukkit.Location;
import org.bukkit.block.Block;

public class SynchronizeBlockChangeExtent extends BlockChangeExtent {
	public SynchronizeBlockChangeExtent(
		Extent extent,
		Actor actor,
		MultiverseWorld sourceWorld,
		MultiverseWorld targetWorld
	) {
		super(extent, actor, sourceWorld, targetWorld);
	}
	
	@Override
	public <T extends BlockStateHolder<T>> boolean setBlock(BlockVector3 location, T block) throws WorldEditException {
		if (this.getIsEnabled()) {	
			Block targetWorldBlock = new Location(
				this.getTargetWorld().getCBWorld(), 
				location.getX(), 
				location.getY(), 
				location.getZ()
			)
				.getBlock();
			
			targetWorldBlock.setType(BukkitAdapter.adapt(block.getBlockType()));
			targetWorldBlock.getState().setBlockData(BukkitAdapter.adapt(block));
		}

		return super.setBlock(location, block);
	}

	
	@Override
	protected MultiverseWorld getEnabledWorld() {
		return this.getSourceWorld();
	}
}
