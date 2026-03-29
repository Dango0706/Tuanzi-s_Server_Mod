package me.tuanzi.statistics.scoreboard;

import me.tuanzi.statistics.StatisticsModule;
import me.tuanzi.statistics.data.PlayerStatistics;
import me.tuanzi.statistics.listeners.PlayerJoinListener;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.scores.DisplaySlot;
import net.minecraft.world.scores.Objective;
import net.minecraft.world.scores.ScoreHolder;
import net.minecraft.world.scores.Scoreboard;
import net.minecraft.world.scores.criteria.ObjectiveCriteria;

import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;

public class ScoreboardManager {
    private static ScoreboardManager instance;
    private final MinecraftServer server;
    private final Scoreboard scoreboard;
    private Objective currentObjective;
    private Timer rotationTimer;
    private Timer updateTimer;
    private String[] rotationStats = {"playTime", "playTimeMinutes", "playTimeHours", "distanceTraveled", "blocksPlaced", "blocksBroken", "kills", "deaths", "damageDealt", "damageTaken", "fishingAttempts", "itemsCrafted", "anvilUses", "itemsEnchanted", "villagerTrades", "chatMessagesSent", "itemsDropped", "loginDays"};
    private int currentRotationIndex = 0;
    private long rotationInterval = 30 * 1000;
    private long updateInterval = 1000;
    private String currentStatType = null;
    private boolean rotationEnabled = false;

    private ScoreboardManager(MinecraftServer server) {
        this.server = server;
        this.scoreboard = server.getScoreboard();
    }

    public static ScoreboardManager getInstance(MinecraftServer server) {
        if (instance == null) {
            instance = new ScoreboardManager(server);
        }
        return instance;
    }

    public void loadConfig() {
        ScoreboardConfig config = ScoreboardConfig.load();
        
        this.rotationInterval = config.getRotationIntervalMs();
        this.updateInterval = config.getUpdateIntervalMs();
        this.currentRotationIndex = config.getCurrentRotationIndex();
        
        if (config.getRotationStats() != null && config.getRotationStats().length > 0) {
            this.rotationStats = config.getRotationStats();
        }
        
        if (config.getCurrentStatType() != null && !config.getCurrentStatType().isEmpty()) {
            createScoreboard(config.getCurrentStatType());
        }
        
        if (config.isRotationEnabled()) {
            startRotation();
        }
        
        StatisticsModule.LOGGER.info("Scoreboard config loaded: statType={}, rotation={}", 
            config.getCurrentStatType(), config.isRotationEnabled());
    }

    public void saveConfig() {
        ScoreboardConfig config = new ScoreboardConfig();
        config.setCurrentStatType(currentStatType);
        config.setRotationEnabled(rotationEnabled);
        config.setRotationIntervalMs(rotationInterval);
        config.setUpdateIntervalMs(updateInterval);
        config.setRotationStats(rotationStats);
        config.setCurrentRotationIndex(currentRotationIndex);
        
        ScoreboardConfig.save(config);
        StatisticsModule.LOGGER.debug("Scoreboard config saved");
    }

    public void createScoreboard(String statType) {
        stopUpdateTimer();

        String objectiveName = "stats_" + statType;

        Objective existingObjective = scoreboard.getObjective(objectiveName);
        if (existingObjective != null) {
            scoreboard.removeObjective(existingObjective);
        }

        if (currentObjective != null && !currentObjective.getName().equals(objectiveName)) {
            scoreboard.removeObjective(currentObjective);
        }

        Component displayName = Component.literal(getStatDisplayName(statType));
        ObjectiveCriteria criterion = ObjectiveCriteria.DUMMY;

        try {
            currentObjective = scoreboard.addObjective(objectiveName, criterion, displayName, ObjectiveCriteria.RenderType.INTEGER, false, null);
            currentStatType = statType;

            updateScores(statType);

            scoreboard.setDisplayObjective(DisplaySlot.SIDEBAR, currentObjective);

            startUpdateTimer();
            
            saveConfig();
        } catch (IllegalArgumentException e) {
            StatisticsModule.LOGGER.error("Failed to create scoreboard: " + e.getMessage());
        }
    }

    public void startRotation() {
        if (rotationTimer != null) {
            rotationTimer.cancel();
        }
        
        rotationEnabled = true;

        rotationTimer = new Timer();
        rotationTimer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                rotateScoreboard();
            }
        }, 0, rotationInterval);
        
        saveConfig();
    }

    public void stopRotation() {
        if (rotationTimer != null) {
            rotationTimer.cancel();
            rotationTimer = null;
        }
        rotationEnabled = false;
        saveConfig();
    }

    private void rotateScoreboard() {
        String statType = rotationStats[currentRotationIndex];
        createScoreboard(statType);

        currentRotationIndex = (currentRotationIndex + 1) % rotationStats.length;
    }

    private void updateScores(String statType) {
        Map<String, PlayerStatistics> playerStats = StatisticsModule.getInstance().getDataManager().getAllPlayerStatistics();

        for (Map.Entry<String, PlayerStatistics> entry : playerStats.entrySet()) {
            String playerName = entry.getKey();
            PlayerStatistics stats = entry.getValue();

            int score = 0;
            switch (statType) {
                case "playTime":
                    score = (int) getTotalPlayTimeSeconds(playerName, stats);
                    break;
                case "playTimeMinutes":
                    score = (int) (getTotalPlayTimeSeconds(playerName, stats) / 60);
                    break;
                case "playTimeHours":
                    score = (int) (getTotalPlayTimeSeconds(playerName, stats) / 3600);
                    break;
                case "distanceTraveled":
                    score = (int) stats.getDistanceTraveled();
                    break;
                case "blocksPlaced":
                    score = stats.getBlocksPlaced();
                    break;
                case "blocksBroken":
                    score = stats.getBlocksBroken();
                    break;
                case "kills":
                    score = stats.getKills();
                    break;
                case "deaths":
                    score = stats.getDeaths();
                    break;
                case "damageDealt":
                    score = (int) stats.getDamageDealt();
                    break;
                case "damageTaken":
                    score = (int) stats.getDamageTaken();
                    break;
                case "fishingAttempts":
                    score = stats.getFishingAttempts();
                    break;
                case "itemsCrafted":
                    score = stats.getItemsCrafted();
                    break;
                case "anvilUses":
                    score = stats.getAnvilUses();
                    break;
                case "itemsEnchanted":
                    score = stats.getItemsEnchanted();
                    break;
                case "villagerTrades":
                    score = stats.getVillagerTrades();
                    break;
                case "chatMessagesSent":
                    score = stats.getChatMessagesSent();
                    break;
                case "itemsDropped":
                    score = stats.getItemsDropped();
                    break;
                case "loginDays":
                    score = stats.getLoginDays();
                    break;
            }

            ScoreHolder scoreHolder = ScoreHolder.forNameOnly(playerName);
            scoreboard.getOrCreatePlayerScore(scoreHolder, currentObjective).set(score);
        }
    }

    private long getTotalPlayTimeSeconds(String playerName, PlayerStatistics stats) {
        long savedPlayTimeSeconds = stats.getPlayTimeSeconds();
        long currentSessionSeconds = 0;

        long loginTime = PlayerJoinListener.getLoginTime(playerName);
        long currentTime = System.currentTimeMillis() / 1000;
        if (currentTime - loginTime < 86400) {
            currentSessionSeconds = currentTime - loginTime;
        }

        return savedPlayTimeSeconds + currentSessionSeconds;
    }

    private String getStatDisplayName(String statType) {
        switch (statType) {
            case "playTime":
                return "§6在线时间 (秒)";
            case "playTimeMinutes":
                return "§6在线时间 (分钟)";
            case "playTimeHours":
                return "§6在线时间 (小时)";
            case "distanceTraveled":
                return "§6移动距离 (米)";
            case "blocksPlaced":
                return "§6放置方块";
            case "blocksBroken":
                return "§6破坏方块";
            case "kills":
                return "§6击杀数";
            case "deaths":
                return "§6死亡数";
            case "damageDealt":
                return "§6造成伤害";
            case "damageTaken":
                return "§6受到伤害";
            case "fishingAttempts":
                return "§6钓鱼次数";
            case "itemsCrafted":
                return "§6合成物品";
            case "anvilUses":
                return "§6使用铁砧";
            case "itemsEnchanted":
                return "§6附魔物品";
            case "villagerTrades":
                return "§6村民交易";
            case "chatMessagesSent":
                return "§6聊天消息";
            case "itemsDropped":
                return "§6丢弃物品";
            case "loginDays":
                return "§6登录天数";
            default:
                return "§6" + statType;
        }
    }

    public void setRotationInterval(long interval) {
        this.rotationInterval = interval;
        if (rotationTimer != null) {
            stopRotation();
            startRotation();
        }
        saveConfig();
    }

    public void setRotationStats(String[] stats) {
        this.rotationStats = stats;
        currentRotationIndex = 0;
        saveConfig();
    }

    public long getUpdateInterval() {
        return updateInterval / 50;
    }

    public void setUpdateInterval(long ticks) {
        this.updateInterval = ticks * 50;
        if (updateTimer != null) {
            stopUpdateTimer();
            startUpdateTimer();
        }
        saveConfig();
    }

    private void startUpdateTimer() {
        if (updateTimer != null) {
            updateTimer.cancel();
        }

        updateTimer = new Timer();
        updateTimer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                if (currentObjective != null && currentStatType != null) {
                    updateScores(currentStatType);
                }
            }
        }, updateInterval, updateInterval);
    }

    private void stopUpdateTimer() {
        if (updateTimer != null) {
            updateTimer.cancel();
            updateTimer = null;
        }
    }

    public void removeScoreboard() {
        stopUpdateTimer();
        stopRotation();
        if (currentObjective != null) {
            scoreboard.removeObjective(currentObjective);
            currentObjective = null;
        }
        currentStatType = null;
        rotationEnabled = false;
        saveConfig();
    }

    public static void reset() {
        if (instance != null) {
            instance.stopUpdateTimer();
            instance.stopRotation();
            instance.currentObjective = null;
            instance.currentStatType = null;
            instance.rotationEnabled = false;
            instance = null;
        }
    }
}
