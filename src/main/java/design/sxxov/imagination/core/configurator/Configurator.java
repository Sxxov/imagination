package design.sxxov.imagination.core.configurator;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;

import design.sxxov.imagination.Imagination;

public class Configurator {
	public static HashMap<String, List<String>> imaginations = new HashMap<>();
	public static ConfiguratorImagination imagination;

	public static void reload(Imagination ctx) {
		ctx.reloadConfig();
		Configurator.load(ctx);
	}

	public static void load(Imagination ctx) {
		ctx.saveDefaultConfig();

        FileConfiguration config = ctx.getConfig();

		Configurator.loadImaginations(config);
		Configurator.loadImagination(config);
	}

	private static void loadImagination(FileConfiguration config) {
		ConfigurationSection section = config.getConfigurationSection("imagination");
		Configurator.imagination = new ConfiguratorImagination();

		// empty config
		if (section == null) {
			return;
		}

		ConfigurationSection playerMovementSection = section.getConfigurationSection("playerMovement");

		if (playerMovementSection == null) {
			return;
		}

		Configurator.imagination.playerMovement.radius = playerMovementSection.getInt("radius");
	}

	private static void loadImaginations(FileConfiguration config) {
		ConfigurationSection section = config.getConfigurationSection("imaginations");
		
		// empty config
		if (section == null) {
			return;
		}

		HashMap<String, Object> sourceToTargetWorldRaw = (HashMap<String, Object>) section.getValues(true);

		for (Map.Entry<String, Object> entry : sourceToTargetWorldRaw.entrySet()) {
			if (entry.getValue() instanceof String) {
				ArrayList<String> list = new ArrayList<>();
				list.add((String) entry.getValue());
				
				Configurator.imaginations.put(entry.getKey(), list);
			}

			if (entry.getValue() instanceof List) {
				ArrayList<String> list = new ArrayList<>();
				List<?> objects = (List<?>) entry.getValue();
				for (Object object : objects) {
					if (object instanceof String) {
						list.add((String) object);
					}
				}
				
				Configurator.imaginations.put(entry.getKey(), list);
			}
		}
	}
}
