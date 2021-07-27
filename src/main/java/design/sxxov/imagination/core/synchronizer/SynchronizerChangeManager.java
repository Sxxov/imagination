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
	private ArrayList<Long> threadBlockedChunkIds = new ArrayList<>();

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
		return this.applyAsync(world, false);
	}

	public CompletableFuture<Void> applyAsync(World world, boolean lazy) {
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

			int[] chunkCoords = SynchronizerChunk.getIdCoords(chunkId);

			if (lazy 
				&& !world.isChunkLoaded(chunkCoords[0], chunkCoords[1])) {
				continue;
			}

			ArrayList<SynchronizerChange> changes = entry.getValue();
			
			future.thenRun(() -> this.applyAsync(
				world,
				chunkId,
				changes
			));
		}

		future.thenRun(() -> {
			chunkIdToChanges.clear();
		});

		return future;
	}

	public void applySync(World world) {
		HashMap<Long, ArrayList<SynchronizerChange>> chunkIdToChanges = (
			this.targetWorldToChunkIdToChangesBuffer.get(world)
		);

		if (chunkIdToChanges == null) {
			return;
		}

		this.threadBlockedChunkIds.addAll(chunkIdToChanges.keySet());

		for (Map.Entry<Long, ArrayList<SynchronizerChange>> entry 
			: chunkIdToChanges.entrySet()) {
			long chunkId = entry.getKey();
			int[] chunkCoords = SynchronizerChunk.getIdCoords(chunkId);
			Chunk chunk = world.getChunkAt(chunkCoords[0], chunkCoords[1]);

			this.applySync(
				world,
				chunkId,
				entry.getValue(),
				chunk
			);
		}

		this.threadBlockedChunkIds.removeAll(chunkIdToChanges.keySet());

		chunkIdToChanges.clear();
	}

	public void applySync(World world, Long chunkId, Chunk chunk) {
		// safeguard against infinite loop if chunk provided is from an
		// unloaded chunk from a ChunkLoadEvent handler
		// this causes a stack overflow exception if unchecked
		if (this.threadBlockedChunkIds.contains(chunkId)) {
			return;
		}

		HashMap<Long, ArrayList<SynchronizerChange>> chunkIdToChanges = (
			this.targetWorldToChunkIdToChangesBuffer.get(world)
		);

		if (chunkIdToChanges == null) {
			return;
		}

		this.threadBlockedChunkIds.add(chunkId);

		this.applySync(
			world,
			chunkId,
			chunkIdToChanges.get(chunkId),
			chunk
		);

		chunkIdToChanges.remove(chunkId);
		this.threadBlockedChunkIds.remove(chunkId);
	}

	public CompletableFuture<Void> applyAsync(World world, Long chunkId, boolean lazy) {
		HashMap<Long, ArrayList<SynchronizerChange>> chunkIdToChanges = (
			this.targetWorldToChunkIdToChangesBuffer.get(world)
		);

		if (chunkIdToChanges == null) {
			return new CompletableFuture<>();
		}

		int[] chunkCoords = SynchronizerChunk.getIdCoords(chunkId);

		if (lazy 
			&& !world.isChunkLoaded(chunkCoords[0], chunkCoords[1])) {
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
			.getChunkAtAsyncUrgently(chunkCoords[0], chunkCoords[1])
			.thenAccept((chunk) -> {
				this.applySync(world, chunkId, changes, chunk);
			});
	}

	private void applySync(World world, Long chunkId, @Nullable ArrayList<SynchronizerChange> changes, Chunk chunk) {
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
				this.mutabilityManager.setMutable(block);
			} else {
				this.mutabilityManager.setImmutable(block);
			}

			this.chunkManager.dirty(chunkId);
		}
	}
}
