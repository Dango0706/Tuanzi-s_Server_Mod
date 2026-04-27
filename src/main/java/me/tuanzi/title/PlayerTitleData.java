package me.tuanzi.title;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import java.util.*;

public class PlayerTitleData {
    private final UUID playerId;
    private final Map<String, Long> titleExpiries; // titleId -> expiry timestamp (-1 for permanent)
    private String equippedTitle;
    private final List<String> pendingNotifications;

    public PlayerTitleData(UUID playerId) {
        this.playerId = playerId;
        this.titleExpiries = new HashMap<>();
        this.equippedTitle = null;
        this.pendingNotifications = new ArrayList<>();
    }

    public PlayerTitleData(UUID playerId, Map<String, Long> titleExpiries, String equippedTitle, List<String> pendingNotifications) {
        this.playerId = playerId;
        this.titleExpiries = new HashMap<>(titleExpiries);
        this.equippedTitle = equippedTitle;
        this.pendingNotifications = new ArrayList<>(pendingNotifications);
    }

    public static final Codec<PlayerTitleData> CODEC = RecordCodecBuilder.create(instance ->
            instance.group(
                    Codec.STRING.xmap(UUID::fromString, UUID::toString).fieldOf("playerId").forGetter(PlayerTitleData::getPlayerId),
                    Codec.unboundedMap(Codec.STRING, Codec.LONG).optionalFieldOf("titleExpiries", new HashMap<>()).forGetter(data -> data.titleExpiries),
                    Codec.STRING.optionalFieldOf("equippedTitle", "").forGetter(data -> data.equippedTitle != null ? data.equippedTitle : ""),
                    Codec.list(Codec.STRING).fieldOf("pendingNotifications").forGetter(data -> data.pendingNotifications),
                    // Compatibility for old "unlockedTitles" list
                    Codec.list(Codec.STRING).optionalFieldOf("unlockedTitles", new ArrayList<>()).forGetter(data -> new ArrayList<>())
            ).apply(instance, (uuid, expiries, equipped, pending, oldUnlocked) -> {
                PlayerTitleData data = new PlayerTitleData(uuid, expiries, equipped.isEmpty() ? null : equipped, pending);
                // Migrate old data
                for (String titleId : oldUnlocked) {
                    data.titleExpiries.putIfAbsent(titleId, -1L);
                }
                return data;
            })
    );

    public UUID getPlayerId() { return playerId; }
    public Map<String, Long> getTitleExpiries() { return titleExpiries; }
    public String getEquippedTitle() { return equippedTitle; }
    public void setEquippedTitle(String equippedTitle) { this.equippedTitle = equippedTitle; }
    public List<String> getPendingNotifications() { return pendingNotifications; }
    
    public void unlockTitle(String titleId, long expiry) {
        titleExpiries.put(titleId, expiry);
    }
    
    public boolean hasTitle(String titleId) {
        if (!titleExpiries.containsKey(titleId)) return false;
        long expiry = titleExpiries.get(titleId);
        return expiry == -1 || System.currentTimeMillis() < expiry;
    }

    public void removeTitle(String titleId) {
        titleExpiries.remove(titleId);
    }
    
    public void addNotification(String message) {
        pendingNotifications.add(message);
    }
}
