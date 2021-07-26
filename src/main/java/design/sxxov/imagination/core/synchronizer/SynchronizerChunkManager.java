package design.sxxov.imagination.core.synchronizer;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

import org.bukkit.plugin.Plugin;
import org.bukkit.util.io.BukkitObjectInputStream;
import org.bukkit.util.io.BukkitObjectOutputStream;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import design.sxxov.imagination.Imagination;

public class SynchronizerChunkManager {
	private static final String RELATIVE_CHUNK_DIR = "chunks";
	private Plugin ctx;
	public String targetWorldName;
	private HashMap<Long, SynchronizerChunk> pool = new HashMap<>();
	private HashSet<Long> dirty = new HashSet<>();

	public SynchronizerChunkManager(Plugin ctx, @NotNull String targetWorldName) {
		this.targetWorldName = targetWorldName;
		this.ctx = ctx;

		File directory = new File(this.getDirectory());

		if (!directory.exists()) {
			directory.mkdirs();
		}
	}

	public void hydrate(long id) {
		// is already hydrated
		if (this.pool.containsKey(id)) {
			return;
		}

		SynchronizerChunk fetchedChunk = this.fetch(id);

		if (fetchedChunk == null) {
			this.pool.put(id, new SynchronizerChunk(id));

			return;
		}

		this.pool.put(id, fetchedChunk);
	}

	public SynchronizerChunk get(long id) throws IllegalStateException {
		SynchronizerChunk chunk = this.pool.get(id);

		if (chunk == null) {
			// Imagination.logger.warning("Chunk(" + id + ") was not hydrated before being borrowed");

			this.hydrate(id);

			chunk = this.pool.get(id);
		}

		return chunk;
	}

	public void dirty(long id) {
		this.dirty.add(id);
	}

	public void flush() {
		for (Long id : this.dirty) {
			SynchronizerChunk chunk = this.pool.get(id);

			if (chunk == null
				|| chunk.blocks == null) {
				Imagination.logger.warning("Attempted to flush chunk that's null");

				continue;
			}

			// don't commit empty chunks to disk
			if (chunk.blocks.size() == 0) {
				continue;
			}

			String path = this.getPath(id);

			try {
				BukkitObjectOutputStream out = new BukkitObjectOutputStream(
					new GZIPOutputStream(
						new FileOutputStream(path)
					)
				);
	
				out.writeObject(chunk);
				out.close();

				this.pool.remove(id);
			} catch (IOException e) {
				Imagination.logger.warning("Failed to write file(" + path + ") to disk");
				Imagination.logger.warning("Chunk(" + id + ") may default to completely read-only on next load");
				// e.printStackTrace();
			}
		}

		this.dirty.clear();
	}

	private @Nullable SynchronizerChunk fetch(long id) {
		String path = this.getPath(id);
		
		if (!new File(path).exists()) {
			return null;
		}

		try {
            BukkitObjectInputStream in = new BukkitObjectInputStream(
				new GZIPInputStream(
					new FileInputStream(path)
				)
			);
            SynchronizerChunk chunk = (SynchronizerChunk) in.readObject();
			in.close();

			if (chunk == null) {
				return null;
			}
			
			chunk.id = id;
			return chunk;
        } catch (ClassNotFoundException | IOException e) {
			Imagination.logger.warning("Failed to fetch file(" + path + ") from disk");
			Imagination.logger.warning("Chunk(" + id + ") will default to being completely readonly");
            // e.printStackTrace();
        }

		return null;
	}

	private String getDirectory() {
		return this.ctx.getDataFolder().getAbsolutePath()
		+ "/"
		+ SynchronizerChunkManager.RELATIVE_CHUNK_DIR
		+ "/"
		+ this.targetWorldName.replaceAll("[^\\w _@.#&+-]", "_");
	}

	private String getPath(long id) {
		return this.getDirectory() + "/" + Long.toString(id);
	}
}
