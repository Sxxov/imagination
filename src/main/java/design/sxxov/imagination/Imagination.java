package design.sxxov.imagination;

import java.rmi.UnexpectedException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.logging.Logger;

import com.onarandombox.MultiverseCore.api.MultiverseWorld;

import org.bukkit.World;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.event.Listener;
import org.bukkit.generator.ChunkGenerator;
import org.bukkit.plugin.java.JavaPlugin;

import design.sxxov.imagination.core.commander.Commander;
import design.sxxov.imagination.core.configurator.Configurator;
import design.sxxov.imagination.core.generator.VoidableChunkGenerator;
import design.sxxov.imagination.core.multiverser.Multiverser;
import design.sxxov.imagination.core.synchronizer.Synchronizer;

public class Imagination extends JavaPlugin implements Listener {
    public static Logger logger;
    public ArrayList<Synchronizer> synchronizers = new ArrayList<>();

    @Override
    public void onEnable() {
        Imagination.logger = this.getLogger();

        Configurator.load(this);
        Multiverser.load();

        this.configure();

        Imagination.logger.info("Imagination running wild!");
    }

    @Override
    public void onDisable() {
        this.destroy();
        Imagination.logger.info("Remembered imaginations, goodnight.");
    }

    @Override
    public ChunkGenerator getDefaultWorldGenerator(String worldName, String id) {
        for (Map.Entry<String, List<String>> entry : Configurator.imaginations.entrySet()) {
            String sourceWorldName = entry.getKey();

            if (worldName.equals(sourceWorldName)) {
                break;
            }

            for (String targetWorldName : entry.getValue()) {
                if (worldName.equals(targetWorldName)) {
                    MultiverseWorld sourceWorld = Multiverser.getMVWorld(sourceWorldName);

                    if (sourceWorld == null) {
                        throw new IllegalStateException("Attempted to get default world generator of source world, but the source world doesn't exist");
                    }

                    return new VoidableChunkGenerator() {
                        @Override
                        public boolean isVoidChunk(World world, Random random, int x, int z, BiomeGrid biome) {
                            return sourceWorld.getCBWorld().isChunkLoaded(x, z);
                        }
                    };
                }
            }
        }
        
        return super.getDefaultWorldGenerator(worldName, id);
    }

    public void configure() {
        for (Map.Entry<String, List<String>> entry : Configurator.imaginations.entrySet()) {
            String sourceWorldName = entry.getKey();
            MultiverseWorld sourceWorld = Multiverser.getMVWorld(sourceWorldName);

            if (sourceWorld == null) {
                Imagination.logger.warning("Attempted to load a non-existent world(" + sourceWorldName + ") as a source");

                continue;
            }

            for (String targetWorldName : entry.getValue()) {
                MultiverseWorld targetWorld = Multiverser.getMVWorld(targetWorldName);

                if (targetWorld == null) {
                    Imagination.logger.info("Creating new target world(" + targetWorldName + ") from source world(" + sourceWorldName + ")");

                    targetWorld = Multiverser.cloneMVWorld(sourceWorld, targetWorldName);
                }

                this.synchronizers.add(
                    new Synchronizer(
                        this, 
                        sourceWorld,
                        targetWorld
                    )
                );
            }
        }
    }

    public void destroy() {
        for (Synchronizer synchronizer : this.synchronizers) {
            synchronizer.flush();
            synchronizer.destroy();
        }

        this.synchronizers = new ArrayList<>();
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        try {
            return new Commander(this).onCommand(sender, command, label, args);
        } catch (UnexpectedException e) {
            e.printStackTrace();
        }

        return super.onCommand(sender, command, label, args);
    }

    public ClassLoader getPluginClassLoader() {
        return this.getClassLoader();
    }
}
