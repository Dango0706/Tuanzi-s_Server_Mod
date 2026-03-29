package com.example.statistics.listeners;

import com.example.statistics.StatisticsModule;
import com.example.statistics.data.PlayerStatistics;
import net.fabricmc.fabric.api.networking.v1.PacketSender;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.ServerGamePacketListenerImpl;

import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;

public class PlayerJoinListener implements ServerPlayConnectionEvents.Join {
    private static final Map<String, Long> playerLoginTimes = new HashMap<>();
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    public static long getLoginTime(String playerName) {
        return playerLoginTimes.getOrDefault(playerName, System.currentTimeMillis() / 1000);
    }

    public static void removeLoginTime(String playerName) {
        playerLoginTimes.remove(playerName);
    }

    @Override
    public void onPlayReady(ServerGamePacketListenerImpl handler, PacketSender sender, MinecraftServer server) {
        ServerPlayer player = handler.player;
        String playerName = player.getName().getString();

        long loginTime = System.currentTimeMillis() / 1000;
        playerLoginTimes.put(playerName, loginTime);

        PlayerStatistics stats = StatisticsModule.getInstance().getDataManager().getPlayerStatistics(playerName);

        if (stats.getFirstJoinTime() == 0) {
            stats.setFirstJoinTime(loginTime);
            StatisticsModule.LOGGER.info("Player {} first join recorded", playerName);
        }

        String today = LocalDate.now(ZoneId.systemDefault()).format(DATE_FORMATTER);
        String lastLoginDate = stats.getLastLoginDate();

        if (!today.equals(lastLoginDate)) {
            stats.incrementLoginDays();
            stats.setLastLoginDate(today);
            StatisticsModule.LOGGER.info("Player {} login day incremented to {}", playerName, stats.getLoginDays());
        }

        StatisticsModule.LOGGER.info("Player {} joined, tracking statistics", playerName);
    }
}
