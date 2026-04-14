package me.tuanzi.statistics.floatingtext;

import me.tuanzi.statistics.StatisticsModule;
import me.tuanzi.statistics.data.PlayerStatistics;
import me.tuanzi.statistics.listeners.PlayerJoinListener;
import me.tuanzi.statistics.util.StatsTranslationHelper;

import java.util.*;
import java.util.stream.Collectors;

public class LeaderboardFormatter {

    private static final Map<String, String> STAT_COLOR_CODES = new HashMap<>();

    static {
        STAT_COLOR_CODES.put("black", "§0");
        STAT_COLOR_CODES.put("dark_blue", "§1");
        STAT_COLOR_CODES.put("dark_green", "§2");
        STAT_COLOR_CODES.put("dark_aqua", "§3");
        STAT_COLOR_CODES.put("dark_red", "§4");
        STAT_COLOR_CODES.put("dark_purple", "§5");
        STAT_COLOR_CODES.put("gold", "§6");
        STAT_COLOR_CODES.put("gray", "§7");
        STAT_COLOR_CODES.put("dark_gray", "§8");
        STAT_COLOR_CODES.put("blue", "§9");
        STAT_COLOR_CODES.put("green", "§a");
        STAT_COLOR_CODES.put("aqua", "§b");
        STAT_COLOR_CODES.put("red", "§c");
        STAT_COLOR_CODES.put("light_purple", "§d");
        STAT_COLOR_CODES.put("yellow", "§e");
        STAT_COLOR_CODES.put("white", "§f");
    }

    public static String formatLeaderboard(String statType, String displayName, String color) {
        String colorCode = STAT_COLOR_CODES.getOrDefault(color, "§6");
        
        String statName = StatsTranslationHelper.translate("stats.type." + statType, null);
        String title = displayName != null && !displayName.isEmpty() ? displayName : statName;
        String boardTitle = StatsTranslationHelper.translate("stats.leaderboard.title_suffix", null);

        StringBuilder sb = new StringBuilder();
        sb.append(colorCode).append(title).append(boardTitle).append("\n");

        List<Map.Entry<String, Long>> topPlayers = getTopPlayers(statType, 10);

        int rank = 1;
        for (Map.Entry<String, Long> entry : topPlayers) {
            String playerName = entry.getKey();
            long value = entry.getValue();
            String formattedValue = formatValue(statType, value);

            sb.append("§f").append(rank).append(" ")
                    .append(playerName).append("  ")
                    .append(colorCode).append(formattedValue).append("\n");

            rank++;
        }

        if (topPlayers.isEmpty()) {
            sb.append(StatsTranslationHelper.translate("stats.leaderboard.empty", null)).append("\n");
        }

        return sb.toString().trim();
    }

    public static List<Map.Entry<String, Long>> getTopPlayers(String statType, int limit) {
        Map<String, PlayerStatistics> allStats = StatisticsModule.getInstance().getDataManager().getAllPlayerStatistics();

        return allStats.entrySet().stream()
                .map(entry -> new AbstractMap.SimpleEntry<>(entry.getKey(), getStatValue(entry.getValue(), entry.getKey(), statType)))
                .sorted((a, b) -> Long.compare(b.getValue(), a.getValue()))
                .limit(limit)
                .collect(Collectors.toList());
    }

    private static long getStatValue(PlayerStatistics stats, String playerName, String statType) {
        return switch (statType) {
            case "playTime" -> {
                long savedPlayTimeSeconds = stats.getPlayTimeSeconds();
                long currentSessionSeconds = 0;
                long loginTime = PlayerJoinListener.getLoginTime(playerName);
                long currentTime = System.currentTimeMillis() / 1000;
                if (currentTime - loginTime < 86400) {
                    currentSessionSeconds = currentTime - loginTime;
                }
                yield savedPlayTimeSeconds + currentSessionSeconds;
            }
            case "distanceTraveled" -> (long) stats.getDistanceTraveled();
            case "blocksPlaced" -> stats.getBlocksPlaced();
            case "blocksBroken" -> stats.getBlocksBroken();
            case "kills" -> stats.getKills();
            case "deaths" -> stats.getDeaths();
            case "damageDealt" -> stats.getDamageDealt();
            case "damageTaken" -> stats.getDamageTaken();
            case "fishingAttempts" -> stats.getFishingAttempts();
            case "fishingSuccess" -> stats.getFishingSuccess();
            case "itemsCrafted" -> stats.getItemsCrafted();
            case "anvilUses" -> stats.getAnvilUses();
            case "itemsEnchanted" -> stats.getItemsEnchanted();
            case "villagerTrades" -> stats.getVillagerTrades();
            case "chatMessagesSent" -> stats.getChatMessagesSent();
            case "itemsDropped" -> stats.getItemsDropped();
            case "loginDays" -> stats.getLoginDays();
            default -> 0L;
        };
    }

    private static String formatValue(String statType, long value) {
        return switch (statType) {
            case "playTime" -> {
                long hours = value / 3600;
                long minutes = (value % 3600) / 60;
                String hUnit = StatsTranslationHelper.translate("stats.unit.hours", null);
                String mUnit = StatsTranslationHelper.translate("stats.unit.minutes", null);
                yield hours + hUnit + minutes + mUnit;
            }
            case "distanceTraveled" -> {
                String mUnit = StatsTranslationHelper.translate("stats.unit.meters", null);
                yield String.format("%.0f%s", (double) value, mUnit);
            }
            default -> String.valueOf(value);
        };
    }

    public static String getStatDisplayName(String statType) {
        return StatsTranslationHelper.translate("stats.type." + statType, null);
    }

    public static Set<String> getSupportedStatTypes() {
        return Set.of("playTime", "distanceTraveled", "blocksPlaced", "blocksBroken", "kills", "deaths", "damageDealt", "damageTaken", "fishingAttempts", "itemsCrafted", "anvilUses", "itemsEnchanted", "villagerTrades", "chatMessagesSent", "itemsDropped", "loginDays");
    }

    public static Set<String> getSupportedColors() {
        return STAT_COLOR_CODES.keySet();
    }

    public static String getColorCode(String color) {
        return STAT_COLOR_CODES.getOrDefault(color, "§6");
    }
}
