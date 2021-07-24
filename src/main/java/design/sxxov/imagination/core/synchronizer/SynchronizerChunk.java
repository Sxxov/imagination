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
		return z ^ (x << 32);
	}

	public static int getChunkCoord(int coord) {
		return (int) Math.ceil((double) coord / 16);
	}
}
