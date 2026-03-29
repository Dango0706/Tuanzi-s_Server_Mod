package com.example.statistics.floatingtext;

import com.example.statistics.StatisticsModule;
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

public class FloatingTextDataManager {
    private static final String DATA_DIR = FabricLoader.getInstance().getConfigDir().resolve("statistics").toString();
    private static final String DATA_FILE = DATA_DIR + File.separator + "floating_texts.json";
    private final Gson gson;

    public FloatingTextDataManager() {
        this.gson = new GsonBuilder().setPrettyPrinting().create();
        new File(DATA_DIR).mkdirs();
    }

    public Map<String, FloatingTextData> loadData() {
        try {
            File file = new File(DATA_FILE);
            if (!file.exists()) {
                return new HashMap<>();
            }

            FileReader reader = new FileReader(file);
            Type type = new TypeToken<Map<String, FloatingTextData>>() {
            }.getType();
            Map<String, FloatingTextData> data = gson.fromJson(reader, type);
            reader.close();

            return data != null ? data : new HashMap<>();
        } catch (IOException e) {
            StatisticsModule.LOGGER.error("Failed to load floating texts data: {}", e.getMessage());
            return new HashMap<>();
        }
    }

    public void saveData(Map<String, FloatingTextData> data) {
        try {
            FileWriter writer = new FileWriter(DATA_FILE);
            gson.toJson(data, writer);
            writer.close();
        } catch (IOException e) {
            StatisticsModule.LOGGER.error("Failed to save floating texts data: {}", e.getMessage());
        }
    }
}
