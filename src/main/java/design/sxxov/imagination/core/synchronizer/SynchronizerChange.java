package design.sxxov.imagination.core.synchronizer;

import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.data.BlockData;

public class SynchronizerChange<T extends Object> {
	public static final int BLOCK_DATA_TYPE = 0;
	public static final int BLOCK_MATERIAL_TYPE = 1;

	public final int type;
	public final long targetChunkId;
	public final World targetWorld;
	public final int[] targetCoords;
	public final T targetData;

	public SynchronizerChange(int[] targetCoords, long targetChunkId, World targetWorld, T targetData) {
		if (targetData instanceof BlockData) {
			this.type = SynchronizerChange.BLOCK_DATA_TYPE;
		} else if (targetData instanceof Material) {
			this.type = SynchronizerChange.BLOCK_MATERIAL_TYPE;
		} else {
			throw new IllegalArgumentException("targetData(" + targetData + ") is not a type of BlockData or Material");
		}
		
		this.targetChunkId = targetChunkId;
		this.targetCoords = targetCoords;
		this.targetWorld = targetWorld;
		this.targetData = targetData;
	}
}
