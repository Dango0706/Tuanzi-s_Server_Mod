package me.tuanzi.utils;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import org.slf4j.Logger;

public class SafeFileIO {

    /**
     * Safely writes a string to a file using an atomic write process and maintaining a backup.
     * 1. Copies existing target file to target.bak
     * 2. Writes new content to target.tmp
     * 3. Atomically moves target.tmp to target
     *
     * @param targetFile The path to the file to write to.
     * @param content    The content to write.
     * @param logger     Optional logger to report backup/temp file failures.
     * @throws IOException If the writing or atomic move fails.
     */
    public static void writeStringSafely(Path targetFile, String content, Logger logger) throws IOException {
        Path tempFile = targetFile.resolveSibling(targetFile.getFileName() + ".tmp");
        Path backupFile = targetFile.resolveSibling(targetFile.getFileName() + ".bak");

        // 1. Create a backup if the target file exists
        if (Files.exists(targetFile)) {
            try {
                Files.copy(targetFile, backupFile, StandardCopyOption.REPLACE_EXISTING);
            } catch (IOException e) {
                if (logger != null) {
                    logger.warn("Failed to create backup for {}: {}", targetFile, e.getMessage());
                }
            }
        }

        // 2. Write new content to a temporary file
        Files.writeString(tempFile, content, StandardCharsets.UTF_8);

        // 3. Atomically move the temporary file to the target location
        Files.move(tempFile, targetFile, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
    }
    
    /**
     * Attempts to read a file as a string. If it fails, attempts to recover from the .bak file.
     *
     * @param targetFile The path to the file to read.
     * @param logger     Logger to report fallback usage.
     * @return The content of the file, or null if both target and backup fail to read (or don't exist).
     */
    public static String readStringSafely(Path targetFile, Logger logger) {
        if (Files.exists(targetFile)) {
            try {
                return Files.readString(targetFile, StandardCharsets.UTF_8);
            } catch (Exception e) {
                if (logger != null) {
                    logger.error("Failed to read primary file {}, attempting to use backup. Error: {}", targetFile, e.getMessage());
                }
            }
        }

        Path backupFile = targetFile.resolveSibling(targetFile.getFileName() + ".bak");
        if (Files.exists(backupFile)) {
            try {
                String content = Files.readString(backupFile, StandardCharsets.UTF_8);
                if (logger != null) {
                    logger.info("Successfully recovered data from backup file {}", backupFile);
                }
                return content;
            } catch (Exception e) {
                if (logger != null) {
                    logger.error("Failed to read backup file {}. Error: {}", backupFile, e.getMessage());
                }
            }
        }

        return null;
    }
}
