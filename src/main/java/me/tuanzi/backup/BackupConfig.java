package me.tuanzi.backup;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.loader.api.FabricLoader;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;

public class BackupConfig {
    private static final File CONFIG_FILE = FabricLoader.getInstance().getConfigDir().resolve("mod_backup_config.json").toFile();
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    private int backupIntervalHours = 12; // 默认12小时

    public static BackupConfig load() {
        if (CONFIG_FILE.exists()) {
            try (FileReader reader = new FileReader(CONFIG_FILE)) {
                return GSON.fromJson(reader, BackupConfig.class);
            } catch (IOException e) {
                me.tuanzi.statistics.StatisticsModule.LOGGER.error("Failed to load backup config", e);
            }
        }
        BackupConfig config = new BackupConfig();
        config.save();
        return config;
    }

    public void save() {
        try (FileWriter writer = new FileWriter(CONFIG_FILE)) {
            GSON.toJson(this, writer);
        } catch (IOException e) {
            me.tuanzi.statistics.StatisticsModule.LOGGER.error("Failed to save backup config", e);
        }
    }

    public int getBackupIntervalHours() {
        return backupIntervalHours;
    }

    public void setBackupIntervalHours(int backupIntervalHours) {
        this.backupIntervalHours = backupIntervalHours;
    }
}
