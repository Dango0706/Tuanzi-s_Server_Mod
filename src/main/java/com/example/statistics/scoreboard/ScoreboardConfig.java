package com.example.statistics.scoreboard;

import com.example.statistics.StatisticsModule;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.loader.api.FabricLoader;

import java.io.*;
import java.util.Arrays;

public class ScoreboardConfig {
    private static final String CONFIG_DIR = FabricLoader.getInstance().getConfigDir().resolve("statistics").toString();
    private static final String CONFIG_FILE = CONFIG_DIR + File.separator + "scoreboard_config.json";
    private static final Gson gson = new GsonBuilder().setPrettyPrinting().create();
    
    private String currentStatType;
    private boolean rotationEnabled;
    private long rotationIntervalMs;
    private long updateIntervalMs;
    private String[] rotationStats;
    private int currentRotationIndex;
    
    public ScoreboardConfig() {
        this.rotationEnabled = false;
        this.rotationIntervalMs = 30000;
        this.updateIntervalMs = 1000;
        this.rotationStats = new String[]{"playTime", "playTimeMinutes", "playTimeHours", "distanceTraveled", "blocksPlaced", "blocksBroken", "kills", "deaths", "damageDealt", "damageTaken", "fishingAttempts", "itemsCrafted", "anvilUses", "itemsEnchanted", "villagerTrades", "chatMessagesSent", "itemsDropped", "loginDays"};
        this.currentRotationIndex = 0;
    }
    
    public static ScoreboardConfig load() {
        try {
            File file = new File(CONFIG_FILE);
            if (!file.exists()) {
                return new ScoreboardConfig();
            }
            
            FileReader reader = new FileReader(file);
            ScoreboardConfig config = gson.fromJson(reader, ScoreboardConfig.class);
            reader.close();
            
            return config != null ? config : new ScoreboardConfig();
        } catch (IOException e) {
            StatisticsModule.LOGGER.error("Failed to load scoreboard config: {}", e.getMessage());
            return new ScoreboardConfig();
        }
    }
    
    public static void save(ScoreboardConfig config) {
        try {
            new File(CONFIG_DIR).mkdirs();
            FileWriter writer = new FileWriter(CONFIG_FILE);
            gson.toJson(config, writer);
            writer.close();
        } catch (IOException e) {
            StatisticsModule.LOGGER.error("Failed to save scoreboard config: {}", e.getMessage());
        }
    }
    
    public String getCurrentStatType() {
        return currentStatType;
    }
    
    public void setCurrentStatType(String currentStatType) {
        this.currentStatType = currentStatType;
    }
    
    public boolean isRotationEnabled() {
        return rotationEnabled;
    }
    
    public void setRotationEnabled(boolean rotationEnabled) {
        this.rotationEnabled = rotationEnabled;
    }
    
    public long getRotationIntervalMs() {
        return rotationIntervalMs;
    }
    
    public void setRotationIntervalMs(long rotationIntervalMs) {
        this.rotationIntervalMs = rotationIntervalMs;
    }
    
    public long getUpdateIntervalMs() {
        return updateIntervalMs;
    }
    
    public void setUpdateIntervalMs(long updateIntervalMs) {
        this.updateIntervalMs = updateIntervalMs;
    }
    
    public String[] getRotationStats() {
        return rotationStats;
    }
    
    public void setRotationStats(String[] rotationStats) {
        this.rotationStats = rotationStats;
    }
    
    public int getCurrentRotationIndex() {
        return currentRotationIndex;
    }
    
    public void setCurrentRotationIndex(int currentRotationIndex) {
        this.currentRotationIndex = currentRotationIndex;
    }
}
