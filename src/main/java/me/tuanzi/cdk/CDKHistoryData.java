package me.tuanzi.cdk;

import java.util.*;

public class CDKHistoryData {
    private final Map<UUID, List<CDKHistoryEntry>> history = new HashMap<>();

    public Map<UUID, List<CDKHistoryEntry>> getHistory() {
        return history;
    }

    public void addRecord(UUID playerId, String code, java.util.List<String> commands) {
        history.computeIfAbsent(playerId, k -> new ArrayList<>()).add(CDKHistoryEntry.now(code, commands));
    }

    public List<CDKHistoryEntry> getPlayerHistory(UUID playerId) {
        return history.getOrDefault(playerId, Collections.emptyList());
    }
}
