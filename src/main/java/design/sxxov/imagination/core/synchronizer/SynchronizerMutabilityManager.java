package design.sxxov.imagination.core.synchronizer;

import java.util.List;

import org.bukkit.block.Block;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.metadata.MetadataValue;
import org.bukkit.plugin.Plugin;

public class SynchronizerMutabilityManager {
	private static final String MUTABLE_METADATA_KEY = "imagination:mutable";
	private Plugin plugin;
	private SynchronizerChunkManager chunkManager;

	public SynchronizerMutabilityManager(Plugin plugin, SynchronizerChunkManager chunkManager) {
		this.plugin = plugin;
		this.chunkManager = chunkManager;
	}

	public void setImmutable(Block block) {
		block.removeMetadata(
			SynchronizerMutabilityManager.MUTABLE_METADATA_KEY,
			this.plugin
		);

		int x = block.getX();
		int y = block.getY();
		int z = block.getZ();

		long chunkId = SynchronizerChunk.getId(
			SynchronizerChunk.getChunkCoord(x),
			SynchronizerChunk.getChunkCoord(z)
		);

		this.chunkManager.get(chunkId)
			.blocks
			.removeIf(
				(currentBlock) -> (
					currentBlock[0] == x
					&& currentBlock[1] == y
					&& currentBlock[2] == z
				)
			);

		this.chunkManager.dirty(chunkId);
	}

	public void setMutable(Block block) {
		block.setMetadata(
			SynchronizerMutabilityManager.MUTABLE_METADATA_KEY,
			new FixedMetadataValue(this.plugin, true)
		);

		int x = block.getX();
		int y = block.getY();
		int z = block.getZ();

		long chunkId = SynchronizerChunk.getId(
			SynchronizerChunk.getChunkCoord(x),
			SynchronizerChunk.getChunkCoord(z)
		);

		this.chunkManager.get(chunkId)
			.blocks
			.add(new int[] { x, y, z });

		this.chunkManager.dirty(chunkId);
	}

	public static boolean isMutable(Block block) {
		List<MetadataValue> metadataList = block
			.getMetadata(SynchronizerMutabilityManager.MUTABLE_METADATA_KEY);

		if (metadataList == null
			|| metadataList.size() == 0) {
			return false;
		}
		
		return metadataList
			.get(0)
			.asBoolean();
	}
}
