package design.sxxov.imagination.core.synchronizer;

import java.rmi.UnexpectedException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.function.BiConsumer;

import javax.annotation.Nullable;

import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.data.BlockData;
import org.bukkit.craftbukkit.v1_17_R1.CraftChunk;
import org.bukkit.craftbukkit.v1_17_R1.block.data.CraftBlockData;
import org.bukkit.craftbukkit.v1_17_R1.entity.CraftPlayer;
import org.bukkit.entity.Player;

import net.minecraft.core.BlockPosition;
import net.minecraft.network.protocol.game.PacketPlayOutMapChunk;
import net.minecraft.server.network.PlayerConnection;

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

			SynchronizerChangeManager.refreshChunk(chunk);
		}

		chunkIdToChanges.clear();
	}

	public void applySync(World world, Long chunkId, Chunk chunk) {
		HashMap<Long, ArrayList<SynchronizerChange>> chunkIdToChanges = (
			this.targetWorldToChunkIdToChangesBuffer.get(world)
		);

		if (chunkIdToChanges == null) {
			return;
		}

		this.applySync(
			world,
			chunkId,
			chunkIdToChanges.get(chunkId),
			chunk
		);

		chunkIdToChanges.remove(chunkId);
		SynchronizerChangeManager.refreshChunk(chunk);
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
				SynchronizerChangeManager.refreshChunk(chunk);
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
					// block.setType(material);
					// block.setBlockData((BlockData) change.targetData);
					SynchronizerChangeManager.setBlockFast(block, (BlockData) change.targetData, chunk);
					break;
				case SynchronizerChange.BLOCK_MATERIAL_TYPE:
					material = (Material) change.targetData;
					// block.setType(material);
					SynchronizerChangeManager.setBlockFast(block, (Material) change.targetData, chunk);
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

	public static void setBlockFast(Block block, Material material, Chunk chunk) {
		SynchronizerChangeManager.setBlockFast(block, material.createBlockData(), chunk);
	}

	public static void setBlockFast(Block block, BlockData blockData, Chunk chunk) {
		if (!(blockData instanceof CraftBlockData)) {
			throw new IllegalArgumentException("Attempted to call setBlockFast with blockData that isn't instance of CraftBlockData");
		}

		net.minecraft.world.level.chunk.Chunk chunkNMS = ((CraftChunk) chunk).getHandle();
		Location location = block.getLocation();

		int x = location.getBlockX() & 0xf;
		int y = location.getBlockY();
		int z = location.getBlockZ() & 0xf;

		chunkNMS.setType(
			new BlockPosition(x, y, z),
			((CraftBlockData) blockData).getState(),
			false
		);
	}

	public static boolean refreshChunk(Chunk chunk) {
		return SynchronizerChangeManager.refreshChunk(
			chunk.getWorld(),
			chunk.getX(),
			chunk.getZ()
		);
	}

	@SuppressWarnings("deprecation")
	public static boolean refreshChunk(World world, int x, int z) {
		return world.refreshChunk(x, z);
	}

	//#region Yanked from https://www.spigotmc.org/threads/1-9-chunkcoordintpairqueue-symbol-not-found.129917/
	private static void refreshPlayer(Player player) {
		PlayerConnection connection = ((CraftPlayer) player).getHandle().b;

        forEachChunkInFOV(player, (cx, cz) -> {
			Chunk chunk = player.getWorld().getChunkAt(cx, cz);

			// The client doesn't know about it yet so there is nothing to refresh
			if (chunk == null 
				|| !chunk.isLoaded()) {
				return; 
			}

			connection.sendPacket(
				new PacketPlayOutMapChunk(((CraftChunk) chunk).getHandle(), true)
			);
        });
	}

	private static void forEachChunkInFOV(Player player, BiConsumer<Integer, Integer> consumer) {
        int playerChunkX = player.getLocation().getBlockX() / 16;
        int playerChunkZ = player.getLocation().getBlockZ() / 16;
        int viewDist = Bukkit.getViewDistance();

        for (int cx = playerChunkX - viewDist; cx <= playerChunkX + viewDist; cx++) {
            for (int cz = playerChunkZ - viewDist; cz <= playerChunkZ + viewDist; cz++) {
                consumer.accept(cx, cz);
            }
        }
    }
	//#endregion
}
