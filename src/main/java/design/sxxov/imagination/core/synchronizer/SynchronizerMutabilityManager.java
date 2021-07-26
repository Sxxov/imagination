package design.sxxov.imagination.core.synchronizer;

import java.util.List;

import org.bukkit.block.Block;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.metadata.MetadataValue;
import org.bukkit.plugin.Plugin;

public class SynchronizerMutabilityManager {
	private static final String MUTABLE_METADATA_KEY = "imagination:mutable";
	private Plugin plugin;

	public SynchronizerMutabilityManager(Plugin plugin) {
		this.plugin = plugin;
	}

	public void setImmutable(Block block) {
		block.removeMetadata(
			SynchronizerMutabilityManager.MUTABLE_METADATA_KEY,
			this.plugin
		);
	}

	public void setMutable(Block block) {
		block.setMetadata(
			SynchronizerMutabilityManager.MUTABLE_METADATA_KEY,
			new FixedMetadataValue(this.plugin, true)
		);
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
