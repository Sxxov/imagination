package design.sxxov.imagination.core.multiverser;

import java.io.File;

import com.onarandombox.MultiverseCore.MultiverseCore;
import com.onarandombox.MultiverseCore.api.MultiverseWorld;

import org.apache.commons.lang.exception.CloneFailedException;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.World;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.PluginManager;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class Multiverser {
	public static MultiverseCore mv;

	public static void load() {
		PluginManager pluginManager = Bukkit.getServer().getPluginManager();
		Plugin plugin = pluginManager.getPlugin("Multiverse-Core");

		if (!(plugin instanceof MultiverseCore)) {
			throw new IllegalStateException("Multiver-Core was not found to be installed in the server");
		}

		Multiverser.mv = (MultiverseCore) plugin;
	}

	public static MultiverseWorld cloneMVWorld(
		@NotNull MultiverseWorld world,
		String newName
	) throws CloneFailedException {
		int lastIndexOfSlashForward = newName.lastIndexOf("/");
		int lastIndexOfSlashBackward = newName.lastIndexOf("\\");
		int lastIndexOfDelimiter = lastIndexOfSlashForward == -1 
			? lastIndexOfSlashBackward 
			: lastIndexOfSlashForward;

		if (lastIndexOfDelimiter != -1) {
			new File(newName.substring(0, lastIndexOfDelimiter)).mkdirs();
		}

		boolean createWorldResult = Multiverser.mv
			.getMVWorldManager()
			.cloneWorld(
				world.getName(), 
				newName
			);

		MultiverseWorld newWorld = Multiverser.getMVWorld(newName);

		if (!createWorldResult
			|| newWorld == null) {
			throw new CloneFailedException("Failed to clone world(" + world.getName() + ") to world(" + newName + ")");
		}

		return newWorld;
	}

	public static MultiverseWorld createImaginationWorld(
		@NotNull MultiverseWorld sourceWorld,
		String targetWorldName
	) throws CloneFailedException {
		MultiverseWorld newWorld = Multiverser.cloneMVWorld(sourceWorld, targetWorldName);

		newWorld.setKeepSpawnInMemory(false);
		newWorld.setGameMode(GameMode.CREATIVE);
		newWorld.getCBWorld().setFullTime(sourceWorld.getCBWorld().getFullTime());

		return newWorld;
	}

	public static @Nullable MultiverseWorld getMVWorld(World world) {
		return Multiverser.mv.getMVWorldManager().getMVWorld(world);
	}

	public static @Nullable MultiverseWorld getMVWorld(String world) {
		return Multiverser.mv.getMVWorldManager().getMVWorld(world);
	}

	public static @Nullable MultiverseWorld getMVWorld(String world, boolean checkAliases) {
		return Multiverser.mv.getMVWorldManager().getMVWorld(world, checkAliases);
	}
}
