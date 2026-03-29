package com.example.statistics.listeners;

import com.example.statistics.StatisticsModule;
import com.example.statistics.data.PlayerStatistics;
import net.fabricmc.fabric.api.networking.v1.PacketSender;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.ServerGamePacketListenerImpl;

public class SessionListener {
    
    public static void register() {
        ServerPlayConnectionEvents.JOIN.register(SessionListener::onJoin);
        ServerPlayConnectionEvents.DISCONNECT.register(SessionListener::onDisconnect);
    }
    
    private static void onJoin(ServerGamePacketListenerImpl handler, PacketSender sender, MinecraftServer server) {
        ServerPlayer player = handler.getPlayer();
        String playerName = player.getName().getString();
        PlayerStatistics stats = StatisticsModule.getInstance().getDataManager().getPlayerStatistics(playerName);
        
        stats.setCurrentSessionStart(System.currentTimeMillis() / 1000);
        stats.setLastRespawnTime(System.currentTimeMillis() / 1000);
        
        StatisticsModule.LOGGER.debug("Player {} session started", playerName);
    }
    
    private static void onDisconnect(ServerGamePacketListenerImpl handler, MinecraftServer server) {
        ServerPlayer player = handler.getPlayer();
        String playerName = player.getName().getString();
        PlayerStatistics stats = StatisticsModule.getInstance().getDataManager().getPlayerStatistics(playerName);
        
        stats.updateCurrentSession();
        stats.updateLongestSession();
        
        if (stats.isSneaking()) {
            stats.stopSneaking();
        }
        
        stats.setCurrentSessionStart(0);
        
        StatisticsModule.LOGGER.debug("Player {} session ended, longest: {}s", playerName, stats.getLongestSessionSeconds());
    }
}
