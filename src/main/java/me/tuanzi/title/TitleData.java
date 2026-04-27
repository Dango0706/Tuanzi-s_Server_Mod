package me.tuanzi.title;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class TitleData {
    private final Map<String, Title> titles = new HashMap<>();
    private final Map<UUID, PlayerTitleData> playerTitles = new HashMap<>();

    public Map<String, Title> getTitles() { return titles; }
    public Map<UUID, PlayerTitleData> getPlayerTitles() { return playerTitles; }

    public PlayerTitleData getOrCreatePlayerTitleData(UUID playerId) {
        return playerTitles.computeIfAbsent(playerId, PlayerTitleData::new);
    }

    public void addTitle(Title title) {
        titles.put(title.id(), title);
    }

    public void removeTitle(String titleId) {
        titles.remove(titleId);
        // Also remove from all players
        for (PlayerTitleData data : playerTitles.values()) {
            data.removeTitle(titleId);
            if (titleId.equals(data.getEquippedTitle())) {
                data.setEquippedTitle(null);
            }
        }
    }
}
