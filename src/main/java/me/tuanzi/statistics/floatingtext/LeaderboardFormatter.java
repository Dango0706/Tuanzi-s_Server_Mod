package me.tuanzi.statistics.floatingtext;

import me.tuanzi.statistics.StatisticsModule;
import me.tuanzi.statistics.data.PlayerStatistics;
import me.tuanzi.statistics.listeners.PlayerJoinListener;

import java.util.*;
import java.util.stream.Collectors;

public class LeaderboardFormatter {

    private static final Map<String, String> STAT_DISPLAY_NAMES = new HashMap<>();
    private static final Map<String, String> STAT_COLOR_CODES = new HashMap<>();

    static {
        STAT_DISPLAY_NAMES.put("playTime", "在线时间");
        STAT_DISPLAY_NAMES.put("distanceTraveled", "移动距离");
        STAT_DISPLAY_NAMES.put("blocksPlaced", "放置方块");
        STAT_DISPLAY_NAMES.put("blocksBroken", "破坏方块");
        STAT_DISPLAY_NAMES.put("kills", "击杀数");
        STAT_DISPLAY_NAMES.put("deaths", "死亡数");
        STAT_DISPLAY_NAMES.put("damageDealt", "造成伤害");
        STAT_DISPLAY_NAMES.put("damageTaken", "受到伤害");
        STAT_DISPLAY_NAMES.put("fishingAttempts", "钓鱼次数");
        STAT_DISPLAY_NAMES.put("fishingSuccess", "钓鱼成功");
        STAT_DISPLAY_NAMES.put("itemsCrafted", "合成物品");
        STAT_DISPLAY_NAMES.put("anvilUses", "铁砧使用");
        STAT_DISPLAY_NAMES.put("itemsEnchanted", "附魔物品");
        STAT_DISPLAY_NAMES.put("villagerTrades", "村民交易");
        STAT_DISPLAY_NAMES.put("chatMessagesSent", "聊天消息");
        STAT_DISPLAY_NAMES.put("itemsDropped", "丢弃物品");
        STAT_DISPLAY_NAMES.put("loginDays", "登录天数");

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
        String title = displayName != null && !displayName.isEmpty() ? displayName : STAT_DISPLAY_NAMES.getOrDefault(statType, statType);

        StringBuilder sb = new StringBuilder();
        sb.append(colorCode).append(title).append("排行榜\n");

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
            sb.append("§7暂无数据\n");
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
        long baseValue = switch (statType) {
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

        return baseValue;
    }

    private static String formatValue(String statType, long value) {
        return switch (statType) {
            case "playTime" -> {
                long hours = value / 3600;
                long minutes = (value % 3600) / 60;
                yield hours + "时" + minutes + "分";
            }
            case "distanceTraveled" -> String.format("%.0f米", (double) value);
            default -> String.valueOf(value);
        };
    }

    public static String getStatDisplayName(String statType) {
        return STAT_DISPLAY_NAMES.getOrDefault(statType, statType);
    }

    public static Set<String> getSupportedStatTypes() {
        return STAT_DISPLAY_NAMES.keySet();
    }

    public static Set<String> getSupportedColors() {
        return STAT_COLOR_CODES.keySet();
    }

    public static String getColorCode(String color) {
        return STAT_COLOR_CODES.getOrDefault(color, "§6");
    }
}
