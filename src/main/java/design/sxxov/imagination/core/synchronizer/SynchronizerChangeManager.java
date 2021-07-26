package design.sxxov.imagination.core.synchronizer;

import java.rmi.UnexpectedException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

import javax.annotation.Nullable;

import org.bukkit.Chunk;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.data.BlockData;

@SuppressWarnings("rawtypes")
public class SynchronizerChangeManager {
	private SynchronizerChunkManager chunkManager;
	private SynchronizerMutabilityManager mutabilityManager;
	private HashMap<World, HashMap<Long, ArrayList<SynchronizerChange>>> targetWorldToChunkIdToChangesBuffer = new HashMap<>();

	public SynchronizerChangeManager(SynchronizerChunkManager chunkManager, SynchronizerMutabilityManager mutabilityManager) {
		this.chunkManager = chunkManager;
		this.mutabilityManager = mutabilityManager;
	}

	public void batch(SynchronizerChange change) {
		HashMap<Long, ArrayList<SynchronizerChange>> chunkToChanges = this.targetWorldToChunkIdToChangesBuffer.get(change.targetWorld);

		if (chunkToChanges == null) {
			HashMap<Long, ArrayList<SynchronizerChange>> newChunkToChanges = new HashMap<>();
			ArrayList<SynchronizerChange> newChanges = new ArrayList<>();

			newChunkToChanges.put(change.targetChunkId, newChanges);
			newChanges.add(change);

			this.targetWorldToChunkIdToChangesBuffer.put(change.targetWorld, newChunkToChanges);

			return;
		}

		ArrayList<SynchronizerChange> changes = chunkToChanges.get(change.targetChunkId);

		if (changes == null) {
			ArrayList<SynchronizerChange> newChanges = new ArrayList<>();
			chunkToChanges.put(change.targetChunkId, newChanges);
			newChanges.add(change);

			return;
		}

		changes.add(change);
	}

	public CompletableFuture<Void> applyAsync(World world) {
		CompletableFuture<Void> future = new CompletableFuture<Void>();
		HashMap<Long, ArrayList<SynchronizerChange>> chunkIdToChanges = (
			this.targetWorldToChunkIdToChangesBuffer.get(world)
		);

		if (chunkIdToChanges == null) {
			return new CompletableFuture<>();
		}

		for (Map.Entry<Long, ArrayList<SynchronizerChange>> entry 
			: chunkIdToChanges.entrySet()) {
			long chunkId = entry.getKey();
			ArrayList<SynchronizerChange> changes = entry.getValue();

			chunkIdToChanges.remove(chunkId);
			
			future.thenRunAsync(() -> this.applyAsync(
				world,
				chunkId,
				changes
			));
		}

		return future;
	}

	public void applySync(World world) {
		HashMap<Long, ArrayList<SynchronizerChange>> chunkIdToChanges = (
			this.targetWorldToChunkIdToChangesBuffer.get(world)
		);

		if (chunkIdToChanges == null) {
			return;
		}

		for (Map.Entry<Long, ArrayList<SynchronizerChange>> entry 
			: chunkIdToChanges.entrySet()) {
			long chunkId = entry.getKey();
			int[] chunkCoords = SynchronizerChunk.getIdCoords(chunkId);

			this.applySync(
				world,
				chunkId,
				entry.getValue(),
				world.getChunkAt(chunkCoords[0], chunkCoords[1])
			);
		}

		chunkIdToChanges.clear();
	}

	public void applySync(World world, Long chunkId) {
		HashMap<Long, ArrayList<SynchronizerChange>> chunkIdToChanges = (
			this.targetWorldToChunkIdToChangesBuffer.get(world)
		);

		if (chunkIdToChanges == null) {
			return;
		}

		int[] chunkCoords = SynchronizerChunk.getIdCoords(chunkId);

		this.applySync(
			world,
			chunkId,
			chunkIdToChanges.get(chunkId),
			world.getChunkAt(chunkCoords[0], chunkCoords[1])
		);

		chunkIdToChanges.remove(chunkId);
	}

	public CompletableFuture<Void> applyAsync(World world, Long chunkId) {
		HashMap<Long, ArrayList<SynchronizerChange>> chunkIdToChanges = (
			this.targetWorldToChunkIdToChangesBuffer.get(world)
		);

		if (chunkIdToChanges == null) {
			return new CompletableFuture<>();
		}

		ArrayList<SynchronizerChange> changes = chunkIdToChanges.get(chunkId);

		chunkIdToChanges.remove(chunkId);

		return this.applyAsync(
			world,
			chunkId,
			changes
		);
	}

	private CompletableFuture<Void> applyAsync(World world, Long chunkId, ArrayList<SynchronizerChange> changes) {
		int[] chunkCoords = SynchronizerChunk.getIdCoords(chunkId);

		return world
			.getChunkAtAsync(chunkCoords[0], chunkCoords[1])
			.thenAccept((chunk) -> {
				this.applySync(world, chunkId, changes, chunk);
			});
	}

	private void applySync(World world, Long chunkId, @Nullable ArrayList<SynchronizerChange> changes, Chunk chunk) {
		ArrayList<int[]> currentBlocks = this.chunkManager.get(chunkId).blocks;

		if (changes == null) {
			return;
		}
				
		for (SynchronizerChange change : changes) {
			Block block = world.getBlockAt(
				change.targetCoords[0],
				change.targetCoords[1],
				change.targetCoords[2]
			);

			Material material;

			switch (change.type) {
				case SynchronizerChange.BLOCK_DATA_TYPE:
					material = ((BlockData) change.targetData).getMaterial();
					block.setType(material);
					block.setBlockData((BlockData) change.targetData);
					break;
				case SynchronizerChange.BLOCK_MATERIAL_TYPE:
					material = (Material) change.targetData;
					block.setType(material);
					break;
				default:
					new UnexpectedException("Unexpected change type(" + change.type + ")")
						.printStackTrace();
					return;
			}

			if (material == Material.AIR) {
				currentBlocks.removeIf(
					(currentBlock) -> (
						currentBlock[0] == change.targetCoords[0]
						&& currentBlock[1] == change.targetCoords[1]
						&& currentBlock[2] == change.targetCoords[2]
					)
				);

				this.mutabilityManager.setMutable(block);
			} else {
				currentBlocks.add(change.targetCoords);

				this.mutabilityManager.setImmutable(block);
			}

			this.chunkManager.dirty(chunkId);
		}
	}
}
