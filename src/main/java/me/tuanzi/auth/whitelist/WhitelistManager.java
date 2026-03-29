package me.tuanzi.auth.whitelist;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.loader.api.FabricLoader;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class WhitelistManager {
    private static final String DATA_DIR = FabricLoader.getInstance().getConfigDir().resolve("auth").toString();
    private static final String WHITELIST_FILE = DATA_DIR + File.separator + "whitelist.json";

    private Map<UUID, String> whitelistMap;
    private final Gson gson;

    public WhitelistManager() {
        this.whitelistMap = new HashMap<>();
        this.gson = new GsonBuilder().setPrettyPrinting().create();

        new File(DATA_DIR).mkdirs();
    }

    public void loadData() {
        try {
            File file = new File(WHITELIST_FILE);
            if (file.exists()) {
                FileReader reader = new FileReader(file);
                WhitelistData data = gson.fromJson(reader, WhitelistData.class);
                reader.close();

                if (data != null && data.getWhitelist() != null) {
                    whitelistMap.clear();
                    for (WhitelistEntry entry : data.getWhitelist()) {
                        if (entry.getUuid() != null && entry.getName() != null) {
                            whitelistMap.put(entry.getUuid(), entry.getName());
                        }
                    }
                }
            }
        } catch (IOException e) {
            System.err.println("加载白名单数据失败: " + e.getMessage());
        }
    }

    public void saveData() {
        try {
            WhitelistData data = new WhitelistData();
            for (Map.Entry<UUID, String> entry : whitelistMap.entrySet()) {
                data.getWhitelist().add(new WhitelistEntry(entry.getValue(), entry.getKey()));
            }

            FileWriter writer = new FileWriter(WHITELIST_FILE);
            gson.toJson(data, writer);
            writer.close();
        } catch (IOException e) {
            System.err.println("保存白名单数据失败: " + e.getMessage());
        }
    }

    public boolean addToWhitelist(String playerName) {
        UUID uuid = OfflineUUIDGenerator.generateOfflineUUID(playerName);
        if (whitelistMap.containsKey(uuid)) {
            return false;
        }
        whitelistMap.put(uuid, playerName);
        saveData();
        return true;
    }

    public boolean addToWhitelist(UUID uuid, String playerName) {
        if (whitelistMap.containsKey(uuid)) {
            return false;
        }
        whitelistMap.put(uuid, playerName);
        saveData();
        return true;
    }

    public boolean removeFromWhitelist(String playerName) {
        UUID uuid = OfflineUUIDGenerator.generateOfflineUUID(playerName);
        if (whitelistMap.remove(uuid) != null) {
            saveData();
            return true;
        }
        return false;
    }

    public boolean removeFromWhitelist(UUID uuid) {
        if (whitelistMap.remove(uuid) != null) {
            saveData();
            return true;
        }
        return false;
    }

    public boolean isInWhitelist(String playerName) {
        UUID uuid = OfflineUUIDGenerator.generateOfflineUUID(playerName);
        return whitelistMap.containsKey(uuid);
    }

    public boolean isInWhitelist(UUID uuid) {
        return whitelistMap.containsKey(uuid);
    }

    public String getPlayerName(UUID uuid) {
        return whitelistMap.get(uuid);
    }

    public Map<UUID, String> getWhitelistMap() {
        return whitelistMap;
    }

    public int getWhitelistSize() {
        return whitelistMap.size();
    }

    public void clearWhitelist() {
        whitelistMap.clear();
        saveData();
    }
}
