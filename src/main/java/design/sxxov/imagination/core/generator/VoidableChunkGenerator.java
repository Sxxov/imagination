package design.sxxov.imagination.core.generator;

import java.util.Random;

import org.bukkit.World;
import org.bukkit.generator.ChunkGenerator;

public abstract class VoidableChunkGenerator extends ChunkGenerator {
	@Override
	public ChunkData generateChunkData(World world, Random random, int x, int z, BiomeGrid biome) {
		if (this.isVoidChunk(world, random, x, z, biome)) {
			return this.createChunkData(world);
		}

		return super.generateChunkData(world, random, x, z, biome);
	}

	public abstract boolean isVoidChunk(World world, Random random, int x, int z, BiomeGrid biome);
}
