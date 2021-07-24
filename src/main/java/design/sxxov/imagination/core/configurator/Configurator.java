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

	public static void reload(Imagination ctx) {
		ctx.reloadConfig();
		Configurator.load(ctx);
	}

	public static void load(Imagination ctx) {
		ctx.saveDefaultConfig();

        FileConfiguration config = ctx.getConfig();

		Configurator.loadImaginations(config);
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
