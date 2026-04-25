package me.tuanzi.cdk;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

public record CDKHistoryEntry(String code, String timestamp, List<String> commands) {
    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    public static CDKHistoryEntry now(String code, List<String> commands) {
        return new CDKHistoryEntry(code, LocalDateTime.now().format(FORMATTER), List.copyOf(commands));
    }
}
