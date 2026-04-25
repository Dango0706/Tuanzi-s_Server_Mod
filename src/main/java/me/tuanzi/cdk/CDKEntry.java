package me.tuanzi.cdk;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import java.util.*;

public class CDKEntry {
    private final String code;
    private final CDKType type;
    private final int maxUses; // -1 for infinite
    private int currentUses;
    private long expireTime; // 0 for never
    private String successMessage;
    private final List<String> commands;
    private final Set<UUID> usedPlayers;

    public CDKEntry(String code, CDKType type, int maxUses) {
        this.code = code;
        this.type = type;
        this.maxUses = maxUses;
        this.currentUses = 0;
        this.expireTime = 0;
        this.successMessage = "§b兑换成功！";
        this.commands = new ArrayList<>();
        this.usedPlayers = new HashSet<>();
    }

    public CDKEntry(String code, CDKType type, int maxUses, int currentUses, long expireTime, String successMessage, List<String> commands, List<UUID> usedPlayers) {
        this.code = code;
        this.type = type;
        this.maxUses = maxUses;
        this.currentUses = currentUses;
        this.expireTime = expireTime;
        this.successMessage = successMessage;
        this.commands = new ArrayList<>(commands);
        this.usedPlayers = new HashSet<>(usedPlayers);
    }

    public static final Codec<CDKEntry> CODEC = RecordCodecBuilder.create(instance ->
            instance.group(
                    Codec.STRING.fieldOf("code").forGetter(CDKEntry::getCode),
                    Codec.STRING.xmap(CDKType::valueOf, CDKType::name).fieldOf("type").forGetter(CDKEntry::getType),
                    Codec.INT.fieldOf("maxUses").forGetter(CDKEntry::getMaxUses),
                    Codec.INT.fieldOf("currentUses").forGetter(CDKEntry::getCurrentUses),
                    Codec.LONG.fieldOf("expireTime").forGetter(CDKEntry::getExpireTime),
                    Codec.STRING.fieldOf("successMessage").forGetter(CDKEntry::getSuccessMessage),
                    Codec.list(Codec.STRING).fieldOf("commands").forGetter(CDKEntry::getCommands),
                    Codec.list(Codec.STRING.xmap(UUID::fromString, UUID::toString)).fieldOf("usedPlayers").forGetter(data -> new ArrayList<>(data.usedPlayers))
            ).apply(instance, CDKEntry::new)
    );

    public String getCode() { return code; }
    public CDKType getType() { return type; }
    public int getMaxUses() { return maxUses; }
    public int getCurrentUses() { return currentUses; }
    public long getExpireTime() { return expireTime; }
    public String getSuccessMessage() { return successMessage; }
    public List<String> getCommands() { return commands; }
    public Set<UUID> getUsedPlayers() { return usedPlayers; }

    public void setExpireTime(long expireTime) { this.expireTime = expireTime; }
    public void setSuccessMessage(String successMessage) { this.successMessage = successMessage; }
    public void incrementUses() { this.currentUses++; }
    public void markUsed(UUID playerId) { this.usedPlayers.add(playerId); }

    public boolean isExpired() {
        return expireTime > 0 && System.currentTimeMillis() > expireTime;
    }

    public boolean isFull() {
        return maxUses != -1 && currentUses >= maxUses;
    }

    public boolean hasUsed(UUID playerId) {
        return usedPlayers.contains(playerId);
    }
    
    public CDKEntry cloneWithNewCode(String newCode) {
        return new CDKEntry(newCode, type, maxUses, 0, expireTime, successMessage, new ArrayList<>(commands), new ArrayList<>());
    }
}
