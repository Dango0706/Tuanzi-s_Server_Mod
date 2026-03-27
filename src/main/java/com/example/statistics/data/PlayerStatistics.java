package com.example.statistics.data;

import java.util.HashMap;
import java.util.Map;

public class PlayerStatistics {
    private String playerName;
    private long playTimeSeconds;
    private double distanceTraveled;
    private int blocksPlaced;
    private int blocksBroken;
    private int kills;
    private int deaths;
    private int durabilityUsed;
    private long damageDealt;
    private long damageTaken;
    
    private long firstJoinTime;
    private int loginDays;
    private String lastLoginDate;
    
    private int fishingAttempts;
    private int fishingSuccess;
    private int fishingFailures;
    
    private int itemsCrafted;
    
    private int anvilUses;
    
    private int itemsEnchanted;
    
    private int villagerTrades;
    
    private int chatMessagesSent;
    
    private int itemsDropped;
    
    private Map<String, Integer> blocksPlacedByType;
    private Map<String, Integer> blocksBrokenByType;
    private Map<String, Integer> killsByEntityType;
    private Map<String, Integer> deathsByEntityType;
    private Map<String, Integer> durabilityUsedByItemType;
    private Map<String, Long> damageDealtByEntityType;
    private Map<String, Long> damageTakenByEntityType;
    private Map<String, Integer> fishCaughtByType;
    private Map<String, Integer> itemsCraftedByType;
    private Map<String, Integer> itemsDroppedByType;
    
    public PlayerStatistics() {
        this.playTimeSeconds = 0;
        this.distanceTraveled = 0.0;
        this.blocksPlaced = 0;
        this.blocksBroken = 0;
        this.kills = 0;
        this.deaths = 0;
        this.durabilityUsed = 0;
        this.damageDealt = 0;
        this.damageTaken = 0;
        
        this.firstJoinTime = 0;
        this.loginDays = 0;
        this.lastLoginDate = "";
        
        this.fishingAttempts = 0;
        this.fishingSuccess = 0;
        this.fishingFailures = 0;
        
        this.itemsCrafted = 0;
        
        this.anvilUses = 0;
        
        this.itemsEnchanted = 0;
        
        this.villagerTrades = 0;
        
        this.chatMessagesSent = 0;
        
        this.itemsDropped = 0;
        
        this.blocksPlacedByType = new HashMap<>();
        this.blocksBrokenByType = new HashMap<>();
        this.killsByEntityType = new HashMap<>();
        this.deathsByEntityType = new HashMap<>();
        this.durabilityUsedByItemType = new HashMap<>();
        this.damageDealtByEntityType = new HashMap<>();
        this.damageTakenByEntityType = new HashMap<>();
        this.fishCaughtByType = new HashMap<>();
        this.itemsCraftedByType = new HashMap<>();
        this.itemsDroppedByType = new HashMap<>();
    }
    
    public PlayerStatistics(String playerName) {
        this();
        this.playerName = playerName;
    }
    
    public String getPlayerName() {
        return playerName;
    }
    
    public void setPlayerName(String playerName) {
        this.playerName = playerName;
    }
    
    public long getPlayTimeSeconds() {
        return playTimeSeconds;
    }
    
    public void setPlayTimeSeconds(long playTimeSeconds) {
        this.playTimeSeconds = playTimeSeconds;
    }
    
    public void addPlayTimeSeconds(long seconds) {
        this.playTimeSeconds += seconds;
    }
    
    public long getPlayTimeMinutes() {
        return playTimeSeconds / 60;
    }
    
    public long getPlayTimeHours() {
        return playTimeSeconds / 3600;
    }
    
    public double getDistanceTraveled() {
        return distanceTraveled;
    }
    
    public void setDistanceTraveled(double distanceTraveled) {
        this.distanceTraveled = distanceTraveled;
    }
    
    public void addDistanceTraveled(double distance) {
        this.distanceTraveled += distance;
    }
    
    public int getBlocksPlaced() {
        return blocksPlaced;
    }
    
    public void setBlocksPlaced(int blocksPlaced) {
        this.blocksPlaced = blocksPlaced;
    }
    
    public void addBlockPlaced(String blockType) {
        this.blocksPlaced++;
        this.blocksPlacedByType.put(blockType, this.blocksPlacedByType.getOrDefault(blockType, 0) + 1);
    }
    
    public int getBlocksBroken() {
        return blocksBroken;
    }
    
    public void setBlocksBroken(int blocksBroken) {
        this.blocksBroken = blocksBroken;
    }
    
    public void addBlockBroken(String blockType) {
        this.blocksBroken++;
        this.blocksBrokenByType.put(blockType, this.blocksBrokenByType.getOrDefault(blockType, 0) + 1);
    }
    
    public int getKills() {
        return kills;
    }
    
    public void setKills(int kills) {
        this.kills = kills;
    }
    
    public void addKill(String entityType) {
        this.kills++;
        this.killsByEntityType.put(entityType, this.killsByEntityType.getOrDefault(entityType, 0) + 1);
    }
    
    public int getDeaths() {
        return deaths;
    }
    
    public void setDeaths(int deaths) {
        this.deaths = deaths;
    }
    
    public void addDeath(String entityType) {
        this.deaths++;
        this.deathsByEntityType.put(entityType, this.deathsByEntityType.getOrDefault(entityType, 0) + 1);
    }
    
    public int getDurabilityUsed() {
        return durabilityUsed;
    }
    
    public void setDurabilityUsed(int durabilityUsed) {
        this.durabilityUsed = durabilityUsed;
    }
    
    public void addDurabilityUsed(String itemType, int amount) {
        this.durabilityUsed += amount;
        this.durabilityUsedByItemType.put(itemType, this.durabilityUsedByItemType.getOrDefault(itemType, 0) + amount);
    }
    
    public Map<String, Integer> getBlocksPlacedByType() {
        return blocksPlacedByType;
    }
    
    public void setBlocksPlacedByType(Map<String, Integer> blocksPlacedByType) {
        this.blocksPlacedByType = blocksPlacedByType;
    }
    
    public Map<String, Integer> getBlocksBrokenByType() {
        return blocksBrokenByType;
    }
    
    public void setBlocksBrokenByType(Map<String, Integer> blocksBrokenByType) {
        this.blocksBrokenByType = blocksBrokenByType;
    }
    
    public Map<String, Integer> getKillsByEntityType() {
        return killsByEntityType;
    }
    
    public void setKillsByEntityType(Map<String, Integer> killsByEntityType) {
        this.killsByEntityType = killsByEntityType;
    }
    
    public Map<String, Integer> getDeathsByEntityType() {
        return deathsByEntityType;
    }
    
    public void setDeathsByEntityType(Map<String, Integer> deathsByEntityType) {
        this.deathsByEntityType = deathsByEntityType;
    }
    
    public Map<String, Integer> getDurabilityUsedByItemType() {
        return durabilityUsedByItemType;
    }
    
    public void setDurabilityUsedByItemType(Map<String, Integer> durabilityUsedByItemType) {
        this.durabilityUsedByItemType = durabilityUsedByItemType;
    }
    
    public long getDamageDealt() {
        return damageDealt;
    }
    
    public void setDamageDealt(long damageDealt) {
        this.damageDealt = damageDealt;
    }
    
    public void addDamageDealt(String entityType, long amount) {
        this.damageDealt += amount;
        this.damageDealtByEntityType.put(entityType, this.damageDealtByEntityType.getOrDefault(entityType, 0L) + amount);
    }
    
    public long getDamageTaken() {
        return damageTaken;
    }
    
    public void setDamageTaken(long damageTaken) {
        this.damageTaken = damageTaken;
    }
    
    public void addDamageTaken(String entityType, long amount) {
        this.damageTaken += amount;
        this.damageTakenByEntityType.put(entityType, this.damageTakenByEntityType.getOrDefault(entityType, 0L) + amount);
    }
    
    public Map<String, Long> getDamageDealtByEntityType() {
        return damageDealtByEntityType;
    }
    
    public void setDamageDealtByEntityType(Map<String, Long> damageDealtByEntityType) {
        this.damageDealtByEntityType = damageDealtByEntityType;
    }
    
    public Map<String, Long> getDamageTakenByEntityType() {
        return damageTakenByEntityType;
    }
    
    public void setDamageTakenByEntityType(Map<String, Long> damageTakenByEntityType) {
        this.damageTakenByEntityType = damageTakenByEntityType;
    }
    
    public long getFirstJoinTime() {
        return firstJoinTime;
    }
    
    public void setFirstJoinTime(long firstJoinTime) {
        this.firstJoinTime = firstJoinTime;
    }
    
    public int getLoginDays() {
        return loginDays;
    }
    
    public void setLoginDays(int loginDays) {
        this.loginDays = loginDays;
    }
    
    public void incrementLoginDays() {
        this.loginDays++;
    }
    
    public String getLastLoginDate() {
        return lastLoginDate;
    }
    
    public void setLastLoginDate(String lastLoginDate) {
        this.lastLoginDate = lastLoginDate;
    }
    
    public int getFishingAttempts() {
        return fishingAttempts;
    }
    
    public void setFishingAttempts(int fishingAttempts) {
        this.fishingAttempts = fishingAttempts;
    }
    
    public void addFishingAttempt() {
        this.fishingAttempts++;
    }
    
    public int getFishingSuccess() {
        return fishingSuccess;
    }
    
    public void setFishingSuccess(int fishingSuccess) {
        this.fishingSuccess = fishingSuccess;
    }
    
    public void addFishingSuccess(String itemType) {
        this.fishingSuccess++;
        this.fishCaughtByType.put(itemType, this.fishCaughtByType.getOrDefault(itemType, 0) + 1);
    }
    
    public int getFishingFailures() {
        return fishingFailures;
    }
    
    public void setFishingFailures(int fishingFailures) {
        this.fishingFailures = fishingFailures;
    }
    
    public void addFishingFailure() {
        this.fishingFailures++;
    }
    
    public Map<String, Integer> getFishCaughtByType() {
        return fishCaughtByType;
    }
    
    public void setFishCaughtByType(Map<String, Integer> fishCaughtByType) {
        this.fishCaughtByType = fishCaughtByType;
    }
    
    public int getItemsCrafted() {
        return itemsCrafted;
    }
    
    public void setItemsCrafted(int itemsCrafted) {
        this.itemsCrafted = itemsCrafted;
    }
    
    public void addItemCrafted(String itemType) {
        this.itemsCrafted++;
        this.itemsCraftedByType.put(itemType, this.itemsCraftedByType.getOrDefault(itemType, 0) + 1);
    }
    
    public Map<String, Integer> getItemsCraftedByType() {
        return itemsCraftedByType;
    }
    
    public void setItemsCraftedByType(Map<String, Integer> itemsCraftedByType) {
        this.itemsCraftedByType = itemsCraftedByType;
    }
    
    public int getAnvilUses() {
        return anvilUses;
    }
    
    public void setAnvilUses(int anvilUses) {
        this.anvilUses = anvilUses;
    }
    
    public void addAnvilUse() {
        this.anvilUses++;
    }
    
    public int getItemsEnchanted() {
        return itemsEnchanted;
    }
    
    public void setItemsEnchanted(int itemsEnchanted) {
        this.itemsEnchanted = itemsEnchanted;
    }
    
    public void addItemEnchanted() {
        this.itemsEnchanted++;
    }
    
    public int getVillagerTrades() {
        return villagerTrades;
    }
    
    public void setVillagerTrades(int villagerTrades) {
        this.villagerTrades = villagerTrades;
    }
    
    public void addVillagerTrade() {
        this.villagerTrades++;
    }
    
    public int getChatMessagesSent() {
        return chatMessagesSent;
    }
    
    public void setChatMessagesSent(int chatMessagesSent) {
        this.chatMessagesSent = chatMessagesSent;
    }
    
    public void addChatMessage() {
        this.chatMessagesSent++;
    }
    
    public int getItemsDropped() {
        return itemsDropped;
    }
    
    public void setItemsDropped(int itemsDropped) {
        this.itemsDropped = itemsDropped;
    }
    
    public void addItemDropped(String itemType) {
        this.itemsDropped++;
        this.itemsDroppedByType.put(itemType, this.itemsDroppedByType.getOrDefault(itemType, 0) + 1);
    }
    
    public Map<String, Integer> getItemsDroppedByType() {
        return itemsDroppedByType;
    }
    
    public void setItemsDroppedByType(Map<String, Integer> itemsDroppedByType) {
        this.itemsDroppedByType = itemsDroppedByType;
    }
}
