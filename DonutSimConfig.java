package net.donutsim.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.loader.api.FabricLoader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;

public class DonutSimConfig {
    private static final Logger LOGGER = LoggerFactory.getLogger("donutsim");
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Path CONFIG_PATH = FabricLoader.getInstance().getConfigDir().resolve("donutsim.json");

    private static DonutSimConfig instance;

    public double balance = 1_250_000_000.0;
    public long shards = 4200L;
    public int kills = 142;
    public int deaths = 19;
    public String teamName = "[Admin]";
    public int keyallTimerSeconds = 872; // 14m 32s
    public boolean hudEnabled = true;
    public int hudX = -1; // -1 auto-anchors to right edge
    public int hudY = 60;
    public float hudScale = 1.0f;

    public static DonutSimConfig get() {
        if (instance == null) {
            load();
        }
        return instance;
    }

    public static void load() {
        if (Files.exists(CONFIG_PATH)) {
            try (Reader reader = Files.newBufferedReader(CONFIG_PATH)) {
                DonutSimConfig loaded = GSON.fromJson(reader, DonutSimConfig.class);
                if (loaded != null) {
                    instance = loaded;
                    sanitize();
                    return;
                }
            } catch (Exception e) {
                LOGGER.warn("Failed to read config/donutsim.json, using defaults.", e);
            }
        }
        instance = new DonutSimConfig();
        save();
    }

    private static void sanitize() {
        if (instance.balance < 0) instance.balance = 0;
        if (instance.shards < 0) instance.shards = 0;
        if (instance.kills < 0) instance.kills = 0;
        if (instance.deaths < 0) instance.deaths = 0;
        if (instance.teamName == null || instance.teamName.isBlank()) instance.teamName = "None";
        if (instance.keyallTimerSeconds <= 0) instance.keyallTimerSeconds = 900;
        if (instance.hudScale < 0.5f || instance.hudScale > 2.5f) instance.hudScale = 1.0f;
    }

    public static void save() {
        if (instance == null) {
            instance = new DonutSimConfig();
        }
        try {
            Files.createDirectories(CONFIG_PATH.getParent());
            try (Writer writer = Files.newBufferedWriter(CONFIG_PATH)) {
                GSON.toJson(instance, writer);
            }
        } catch (IOException e) {
            LOGGER.error("Failed to save config/donutsim.json", e);
        }
    }
}
