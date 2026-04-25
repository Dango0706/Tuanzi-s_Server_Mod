package me.tuanzi.cdk;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import me.tuanzi.auth.AuthModule;
import me.tuanzi.auth.whitelist.WhitelistManager;
import net.fabricmc.loader.api.FabricLoader;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class CDKHistoryManager {
    private static final Path DATA_DIR = FabricLoader.getInstance().getConfigDir().resolve("tuanzis_exports");
    private static final Path HISTORY_FILE = FabricLoader.getInstance().getConfigDir().resolve("cdk_history.json");
    private final Gson gson = new GsonBuilder().setPrettyPrinting().create();
    private CDKHistoryData data = new CDKHistoryData();

    public CDKHistoryManager() {
        try {
            Files.createDirectories(DATA_DIR);
            load();
        } catch (IOException ignored) {}
    }

    private void load() {
        File file = HISTORY_FILE.toFile();
        if (file.exists()) {
            try (FileReader reader = new FileReader(file)) {
                data = gson.fromJson(reader, CDKHistoryData.class);
            } catch (IOException ignored) {}
        }
    }

    public void save() {
        try (FileWriter writer = new FileWriter(HISTORY_FILE.toFile())) {
            gson.toJson(data, writer);
        } catch (IOException ignored) {}
    }

    public void addRecord(UUID playerId, String code, List<String> commands) {
        data.addRecord(playerId, code, commands);
        save();
    }

    public List<CDKHistoryEntry> getPlayerHistory(UUID playerId) {
        return data.getPlayerHistory(playerId);
    }

    // --- 增强导出逻辑 ---

    public String exportHistory(String format) throws IOException {
        String filename = "cdk_history_" + System.currentTimeMillis() + "." + format;
        Path path = DATA_DIR.resolve(filename);
        WhitelistManager wm = AuthModule.getInstance().getWhitelistManager();

        if (format.equals("csv")) {
            StringBuilder sb = new StringBuilder("PlayerName,PlayerUUID,CDKCode,Timestamp,Commands\n");
            for (Map.Entry<UUID, List<CDKHistoryEntry>> entry : data.getHistory().entrySet()) {
                String name = wm != null ? wm.getPlayerName(entry.getKey()) : "Unknown";
                for (CDKHistoryEntry record : entry.getValue()) {
                    sb.append(name).append(",")
                      .append(entry.getKey()).append(",")
                      .append(record.code()).append(",")
                      .append(record.timestamp()).append(",")
                      .append("\"").append(String.join(" | ", record.commands())).append("\"\n");
                }
            }
            Files.writeString(path, sb.toString(), StandardCharsets.UTF_8);
        } else if (format.equals("json")) {
            Files.writeString(path, gson.toJson(data), StandardCharsets.UTF_8);
        } else {
            StringBuilder sb = new StringBuilder("CDK Redemption History\n==========================\n");
            for (Map.Entry<UUID, List<CDKHistoryEntry>> entry : data.getHistory().entrySet()) {
                String name = wm != null ? wm.getPlayerName(entry.getKey()) : "Unknown";
                sb.append("Player: ").append(name).append(" (").append(entry.getKey()).append(")\n");
                for (CDKHistoryEntry record : entry.getValue()) {
                    sb.append("  - [").append(record.timestamp()).append("] ").append(record.code())
                      .append(" | Cmds: ").append(record.commands()).append("\n");
                }
                sb.append("\n");
            }
            Files.writeString(path, sb.toString(), StandardCharsets.UTF_8);
        }
        return filename;
    }

    public String exportCDKs(String format, Collection<CDKEntry> cdks) throws IOException {
        String filename = "cdk_pool_" + System.currentTimeMillis() + "." + format;
        Path path = DATA_DIR.resolve(filename);

        if (format.equals("csv")) {
            StringBuilder sb = new StringBuilder("Code,Type,Status,Uses,MaxUses,Expiry,Commands\n");
            for (CDKEntry e : cdks) {
                String status = e.isExpired() ? "Expired" : (e.isFull() ? "Full" : "Valid");
                sb.append(e.getCode()).append(",")
                  .append(e.getType()).append(",")
                  .append(status).append(",")
                  .append(e.getCurrentUses()).append(",")
                  .append(e.getMaxUses() == -1 ? "Infinite" : e.getMaxUses()).append(",")
                  .append(e.getExpireTime() == 0 ? "Never" : e.getExpireTime()).append(",")
                  .append("\"").append(String.join(" | ", e.getCommands())).append("\"\n");
            }
            Files.writeString(path, sb.toString(), StandardCharsets.UTF_8);
        } else if (format.equals("json")) {
            Files.writeString(path, gson.toJson(cdks), StandardCharsets.UTF_8);
        } else {
            StringBuilder sb = new StringBuilder("CDK Pool Configuration\n==========================\n");
            for (CDKEntry e : cdks) {
                String status = e.isExpired() ? "EXPIRED" : (e.isFull() ? "FULL" : "VALID");
                sb.append("[").append(status).append("] ").append(e.getCode())
                  .append("\n  Type: ").append(e.getType())
                  .append("\n  Uses: ").append(e.getCurrentUses()).append("/").append(e.getMaxUses() == -1 ? "∞" : e.getMaxUses())
                  .append("\n  Cmds: ").append(e.getCommands()).append("\n\n");
            }
            Files.writeString(path, sb.toString(), StandardCharsets.UTF_8);
        }
        return filename;
    }
}
