package design.sxxov.imagination;

import java.rmi.UnexpectedException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.logging.Logger;

import com.onarandombox.MultiverseCore.api.MultiverseWorld;

import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.java.JavaPlugin;

import design.sxxov.imagination.core.commander.Commander;
import design.sxxov.imagination.core.configurator.Configurator;
import design.sxxov.imagination.core.movementer.Movementer;
import design.sxxov.imagination.core.multiverser.Multiverser;
import design.sxxov.imagination.core.synchronizer.Synchronizer;

public class Imagination extends JavaPlugin implements Listener {
    public static Logger logger;
    public ArrayList<Synchronizer> synchronizers = new ArrayList<>();
    public HashMap<UUID, ArrayList<Movementer>> uuidToMovementers = new HashMap<>();

    @Override
    public void onEnable() {
        Imagination.logger = this.getLogger();

        Configurator.load(this);
        Multiverser.load();

        this.configure();
        for (Player player : this.getServer().getOnlinePlayers()) {
            this.registerMovementers(player);
        }

        Imagination.logger.info("Imagination running wild!");
        Bukkit.getServer().getPluginManager().registerEvents(this, this);
    }

    @Override
    public void onDisable() {
        for (Player player : this.getServer().getOnlinePlayers()) {
            this.destroyMovementers(player);
        }

        this.uuidToMovementers = new HashMap<>();

        this.destroySynchronizers();
        
        Imagination.logger.info("Remembered imaginations, goodnight.");
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

    public void destroySynchronizers() {
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

    private void registerMovementers(Player player) {
        ArrayList<Movementer> movementers = new ArrayList<>();

        for (Map.Entry<String, List<String>> entry : Configurator.imaginations.entrySet()) {
            for (String targetWorldName : entry.getValue()) {
                new Movementer(
                        this,
                        player,
                        targetWorldName,
                        Configurator.imagination.playerMovement.radius
                );
            }
        }

        this.uuidToMovementers.put(
            player.getUniqueId(), 
            movementers
        );
    }

    private void destroyMovementers(Player player) {
        UUID uuid = player.getUniqueId();
        ArrayList<Movementer> movementers = this.uuidToMovementers.get(uuid);

        // broken state, probably a reload xd
        if (movementers == null) {
            return;
        }

        for (Movementer movementer : movementers) {
            movementer.destroy();
        }

        this.uuidToMovementers.remove(uuid);
    } 
    
    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        this.registerMovementers(event.getPlayer());
    }

    @EventHandler
    public void onPlayerLeave(PlayerQuitEvent event) {
        this.destroyMovementers(event.getPlayer());
    }
}
