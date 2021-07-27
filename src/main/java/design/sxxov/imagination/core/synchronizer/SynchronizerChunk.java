package design.sxxov.imagination.core.synchronizer;

import java.io.Serializable;
import java.util.ArrayList;

import org.bukkit.Chunk;

public class SynchronizerChunk implements Serializable {
	private static final long serialVersionUID = 1L;
	protected transient long id;
	public ArrayList<int[]> blocks;

	public SynchronizerChunk(long id) {
		this.id = id;
		this.blocks = new ArrayList<>();
	}

	public SynchronizerChunk(long id, ArrayList<int[]> blocks) {
		this.id = id;
		this.blocks = blocks;
	}

	public long getId() {
		return this.id;
	}

	public static long getId(Chunk chunk) {
		return SynchronizerChunk.getId(chunk.getX(), chunk.getZ());
	}

	public static long getId(int x, int z) {
		return (x << 32) ^ z;
	}

	public static int[] getIdCoords(long id) {
		// 0000000000000000000000000000000011111111111111111111111111111111
		long mask = (1L << 32) - 1L;

		return new int[] {
			(int) ((id >> 32) & mask),
			(int) (id & mask),
		};
	}

	public static int getChunkCoord(int coord) {
		return (int) Math.ceil((double) coord / 16);
	}

	public static int getLocalCoord(int worldCoord) {
		return worldCoord & 0xf;
	}
}
