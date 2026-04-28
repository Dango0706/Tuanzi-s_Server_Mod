package me.tuanzi.economy.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import me.tuanzi.economy.EconomyModule;
import net.fabricmc.loader.api.FabricLoader;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;

public class EconomyConfig {
    private static final String CONFIG_DIR = FabricLoader.getInstance().getConfigDir().resolve("economy").toString();
    private static final String CONFIG_FILE = CONFIG_DIR + File.separator + "economy_config.json";
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    private boolean allowSameIPTransfer = true;

    public EconomyConfig() {
    }

    public boolean isAllowSameIPTransfer() {
        return allowSameIPTransfer;
    }

    public void setAllowSameIPTransfer(boolean allowSameIPTransfer) {
        this.allowSameIPTransfer = allowSameIPTransfer;
    }

    public static EconomyConfig load() {
        File file = new File(CONFIG_FILE);
        if (file.exists()) {
            try (FileReader reader = new FileReader(file)) {
                EconomyConfig config = GSON.fromJson(reader, EconomyConfig.class);
                return config != null ? config : new EconomyConfig();
            } catch (IOException e) {
                EconomyModule.LOGGER.error("Failed to load economy config: {}", e.getMessage());
            }
        }
        EconomyConfig defaultConfig = new EconomyConfig();
        save(defaultConfig);
        return defaultConfig;
    }

    public static void save(EconomyConfig config) {
        File dir = new File(CONFIG_DIR);
        if (!dir.exists()) {
            dir.mkdirs();
        }
        try (FileWriter writer = new FileWriter(CONFIG_FILE)) {
            GSON.toJson(config, writer);
        } catch (IOException e) {
            EconomyModule.LOGGER.error("Failed to save economy config: {}", e.getMessage());
        }
    }
}
