package dev.smokefx;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Settings, stored in .minecraft/config/smokefx.json.
 * Edit the file and run /smokefx reload, or change values in-game with /smokefx commands.
 */
public final class SmokeFxConfig {

	/** Names of the individual effects that can be switched on/off. */
	public static final String[] EFFECTS = {
		"fire", "torches", "candles", "campfires", "furnaces", "lava", "burning_entities"
	};

	private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
	private static final Path FILE = FabricLoader.getInstance().getConfigDir().resolve("smokefx.json");
	private static SmokeFxConfig instance = new SmokeFxConfig();

	/** Master switch. */
	public boolean enabled = true;

	/** Particle amount. 0 = none, 1 = default, 2 = double, max 5. */
	public float particleMultiplier = 1.0f;

	/** How far (in blocks) around you the mod looks for fire, lava, etc. 4 - 32. */
	public int scanRadius = 16;

	/** Hard cap on particles this mod may spawn per tick, as an FPS safety net. */
	public int maxParticlesPerTick = 150;

	/** Whether your own burning character gets smoke too (can block your view in first person). */
	public boolean affectLocalPlayer = false;

	/** Per-effect on/off switches. */
	public Map<String, Boolean> effects = new LinkedHashMap<>();

	public static SmokeFxConfig get() {
		return instance;
	}

	public boolean isEffectEnabled(String name) {
		return effects.getOrDefault(name, true);
	}

	public void setEffect(String name, boolean value) {
		effects.put(name, value);
	}

	public static void load() {
		SmokeFxConfig loaded = null;
		if (Files.exists(FILE)) {
			try {
				loaded = GSON.fromJson(Files.readString(FILE), SmokeFxConfig.class);
			} catch (Exception e) {
				System.err.println("[SmokeFX] Could not read config, using defaults: " + e);
			}
		}
		instance = loaded != null ? loaded : new SmokeFxConfig();
		instance.sanitize();
		save(); // writes the file on first run and fills in any missing keys
	}

	public static void save() {
		try {
			Files.createDirectories(FILE.getParent());
			Files.writeString(FILE, GSON.toJson(instance));
		} catch (IOException e) {
			System.err.println("[SmokeFX] Could not save config: " + e);
		}
	}

	private void sanitize() {
		particleMultiplier = Math.max(0f, Math.min(5f, particleMultiplier));
		scanRadius = Math.max(4, Math.min(32, scanRadius));
		maxParticlesPerTick = Math.max(0, Math.min(1000, maxParticlesPerTick));
		if (effects == null) {
			effects = new LinkedHashMap<>();
		}
		for (String name : EFFECTS) {
			effects.putIfAbsent(name, true);
		}
	}
}
