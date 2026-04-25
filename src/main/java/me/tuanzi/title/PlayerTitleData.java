package me.tuanzi.title;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import java.util.*;

public class PlayerTitleData {
    private final UUID playerId;
    private final Set<String> unlockedTitles;
    private String equippedTitle;
    private final List<String> pendingNotifications;

    public PlayerTitleData(UUID playerId) {
        this.playerId = playerId;
        this.unlockedTitles = new HashSet<>();
        this.equippedTitle = null;
        this.pendingNotifications = new ArrayList<>();
    }

    public PlayerTitleData(UUID playerId, List<String> unlockedTitles, String equippedTitle, List<String> pendingNotifications) {
        this.playerId = playerId;
        this.unlockedTitles = new HashSet<>(unlockedTitles);
        this.equippedTitle = equippedTitle;
        this.pendingNotifications = new ArrayList<>(pendingNotifications);
    }

    public static final Codec<PlayerTitleData> CODEC = RecordCodecBuilder.create(instance ->
            instance.group(
                    Codec.STRING.xmap(UUID::fromString, UUID::toString).fieldOf("playerId").forGetter(PlayerTitleData::getPlayerId),
                    Codec.list(Codec.STRING).fieldOf("unlockedTitles").forGetter(data -> new ArrayList<>(data.unlockedTitles)),
                    Codec.STRING.optionalFieldOf("equippedTitle", "").forGetter(data -> data.equippedTitle != null ? data.equippedTitle : ""),
                    Codec.list(Codec.STRING).fieldOf("pendingNotifications").forGetter(data -> data.pendingNotifications)
            ).apply(instance, (uuid, unlocked, equipped, pending) -> 
                new PlayerTitleData(uuid, unlocked, equipped.isEmpty() ? null : equipped, pending))
    );

    public UUID getPlayerId() { return playerId; }
    public Set<String> getUnlockedTitles() { return unlockedTitles; }
    public String getEquippedTitle() { return equippedTitle; }
    public void setEquippedTitle(String equippedTitle) { this.equippedTitle = equippedTitle; }
    public List<String> getPendingNotifications() { return pendingNotifications; }
    
    public void unlockTitle(String titleId) {
        unlockedTitles.add(titleId);
    }
    
    public void addNotification(String message) {
        pendingNotifications.add(message);
    }
}
