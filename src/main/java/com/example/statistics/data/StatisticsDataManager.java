package com.example.statistics.data;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import net.fabricmc.loader.api.FabricLoader;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;

public class StatisticsDataManager {
    private static final String DATA_DIR = FabricLoader.getInstance().getConfigDir().resolve("statistics").toString();
    private static final String PLAYER_DATA_FILE = DATA_DIR + File.separator + "player_statistics.json";
    private static final String SERVER_DATA_FILE = DATA_DIR + File.separator + "server_statistics.json";
    private static final long AUTO_SAVE_INTERVAL = 5 * 60 * 1000;

    private Map<String, PlayerStatistics> playerStatisticsMap;
    private ServerStatistics serverStatistics;
    private Timer autoSaveTimer;
    private final Gson gson;

    public StatisticsDataManager() {
        this.playerStatisticsMap = new HashMap<>();
        this.serverStatistics = new ServerStatistics();
        this.gson = new GsonBuilder().setPrettyPrinting().create();

        new File(DATA_DIR).mkdirs();

        startAutoSaveTimer();
    }

    public void loadData() {
        try {
            File playerDataFile = new File(PLAYER_DATA_FILE);
            if (playerDataFile.exists()) {
                FileReader reader = new FileReader(playerDataFile);
                Type type = new TypeToken<Map<String, PlayerStatistics>>() {
                }.getType();
                playerStatisticsMap = gson.fromJson(reader, type);
                reader.close();
            }
        } catch (IOException e) {
            System.err.println("Failed to load player statistics: " + e.getMessage());
        }

        try {
            File serverDataFile = new File(SERVER_DATA_FILE);
            if (serverDataFile.exists()) {
                FileReader reader = new FileReader(serverDataFile);
                serverStatistics = gson.fromJson(reader, ServerStatistics.class);
                reader.close();
                serverStatistics.updateLastStartupTime();
            }
        } catch (IOException e) {
            System.err.println("Failed to load server statistics: " + e.getMessage());
        }
    }

    public void saveData() {
        try {
            FileWriter writer = new FileWriter(PLAYER_DATA_FILE);
            gson.toJson(playerStatisticsMap, writer);
            writer.close();
        } catch (IOException e) {
            System.err.println("Failed to save player statistics: " + e.getMessage());
        }

        try {
            serverStatistics.addUptime(serverStatistics.getCurrentSessionUptimeSeconds());
            serverStatistics.updateLastStartupTime();

            FileWriter writer = new FileWriter(SERVER_DATA_FILE);
            gson.toJson(serverStatistics, writer);
            writer.close();
        } catch (IOException e) {
            System.err.println("Failed to save server statistics: " + e.getMessage());
        }
    }

    private void startAutoSaveTimer() {
        autoSaveTimer = new Timer();
        autoSaveTimer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                saveData();
            }
        }, AUTO_SAVE_INTERVAL, AUTO_SAVE_INTERVAL);
    }

    public PlayerStatistics getPlayerStatistics(String playerName) {
        return playerStatisticsMap.computeIfAbsent(playerName, PlayerStatistics::new);
    }

    public ServerStatistics getServerStatistics() {
        return serverStatistics;
    }

    public Map<String, PlayerStatistics> getAllPlayerStatistics() {
        return playerStatisticsMap;
    }

    public void shutdown() {
        if (autoSaveTimer != null) {
            autoSaveTimer.cancel();
        }
        saveData();
    }
}
