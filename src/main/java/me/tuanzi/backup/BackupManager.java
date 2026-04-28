package me.tuanzi.backup;

import me.tuanzi.statistics.StatisticsModule;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.storage.LevelResource;

import java.io.*;
import java.nio.file.*;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

public class BackupManager {
    private static final Path BACKUP_DIR = Paths.get("./mod_backups");
    private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("yyyyMMdd_HHmmss");
    private static BackupManager instance;
    private final BackupConfig config;
    private long lastBackupTime = 0;

    private BackupManager() {
        this.config = BackupConfig.load();
        try {
            Files.createDirectories(BACKUP_DIR);
        } catch (IOException e) {
            StatisticsModule.LOGGER.error("Failed to create backup directory", e);
        }
    }

    public static BackupManager getInstance() {
        if (instance == null) {
            instance = new BackupManager();
        }
        return instance;
    }

    public void init() {
        ServerTickEvents.END_SERVER_TICK.register(server -> {
            long currentTime = System.currentTimeMillis();
            if (lastBackupTime == 0) {
                lastBackupTime = currentTime;
            }
            if (currentTime - lastBackupTime > (long) config.getBackupIntervalHours() * 60 * 60 * 1000) {
                performBackup(server, "auto");
                lastBackupTime = currentTime;
            }
        });
    }

    public String performBackup(MinecraftServer server, String type) {
        String timestamp = DATE_FORMAT.format(new Date());
        String fileName = "backup_" + type + "_" + timestamp + ".zip";
        Path zipPath = BACKUP_DIR.resolve(fileName);

        // 确保数据已保存
        // server.saveAll(true, true, true); 
        
        try (ZipOutputStream zos = new ZipOutputStream(new FileOutputStream(zipPath.toFile()))) {
            // 1. 备份 world/data 中的 NBT 文件
            Path dataDir = server.getWorldPath(LevelResource.ROOT).resolve("data");
            backupFile(dataDir.resolve("economy-module_economy_data.dat"), "world_data/economy.dat", zos);
            backupFile(dataDir.resolve("cdk-module_cdk_data.dat"), "world_data/cdk.dat", zos);
            backupFile(dataDir.resolve("shop-module_shop_data.dat"), "world_data/shop.dat", zos);
            backupFile(dataDir.resolve("title-module_title_data.dat"), "world_data/title.dat", zos);

            // 2. 备份 config 中的统计和白名单
            Path configDir = FabricLoader.getInstance().getGameDir().resolve("config");
            backupDir(configDir.resolve("statistics"), "config/statistics", zos);
            backupDir(configDir.resolve("auth"), "config/auth", zos);

            return timestamp;
        } catch (IOException e) {
            StatisticsModule.LOGGER.error("Backup failed", e);
            return null;
        }
    }

    private void backupFile(Path source, String zipPath, ZipOutputStream zos) throws IOException {
        if (Files.exists(source)) {
            zos.putNextEntry(new ZipEntry(zipPath));
            Files.copy(source, zos);
            zos.closeEntry();
        }
    }

    private void backupDir(Path sourceDir, String zipPathPrefix, ZipOutputStream zos) throws IOException {
        if (Files.exists(sourceDir)) {
            Files.walk(sourceDir).forEach(path -> {
                if (Files.isRegularFile(path)) {
                    String relativePath = sourceDir.relativize(path).toString();
                    try {
                        backupFile(path, zipPathPrefix + "/" + relativePath, zos);
                    } catch (IOException e) {
                        StatisticsModule.LOGGER.error("Failed to backup file in directory: " + path, e);
                    }
                }
            });
        }
    }

    public boolean restoreBackup(MinecraftServer server, String timestamp) {
        Path zipPath = null;
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(BACKUP_DIR, "backup_*_" + timestamp + ".zip")) {
            for (Path entry : stream) {
                zipPath = entry;
                break;
            }
        } catch (IOException e) {
            StatisticsModule.LOGGER.error("Failed to find backup", e);
            return false;
        }

        if (zipPath == null || !Files.exists(zipPath)) return false;

        try (ZipInputStream zis = new ZipInputStream(new FileInputStream(zipPath.toFile()))) {
            ZipEntry entry;
            while ((entry = zis.getNextEntry()) != null) {
                Path targetPath;
                if (entry.getName().startsWith("world_data/")) {
                    String fileName = entry.getName().substring("world_data/".length());
                    String realName = switch (fileName) {
                        case "economy.dat" -> "economy-module_economy_data.dat";
                        case "cdk.dat" -> "cdk-module_cdk_data.dat";
                        case "shop.dat" -> "shop-module_shop_data.dat";
                        case "title.dat" -> "title-module_title_data.dat";
                        default -> fileName;
                    };
                    targetPath = server.getWorldPath(LevelResource.ROOT).resolve("data").resolve(realName);
                } else {
                    targetPath = FabricLoader.getInstance().getGameDir().resolve(entry.getName());
                }

                Files.createDirectories(targetPath.getParent());
                Files.copy(zis, targetPath, StandardCopyOption.REPLACE_EXISTING);
                zis.closeEntry();
            }
            return true;
        } catch (IOException e) {
            StatisticsModule.LOGGER.error("Restore failed", e);
            return false;
        }
    }

    public BackupConfig getConfig() {
        return config;
    }

    public void reloadConfig() {
        BackupConfig newConfig = BackupConfig.load();
        this.config.setBackupIntervalHours(newConfig.getBackupIntervalHours());
    }
}
