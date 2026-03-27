package com.example.statistics.listeners;

import com.example.statistics.StatisticsModule;
import com.example.statistics.data.PlayerStatistics;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class PlayerMoveListener implements ServerTickEvents.EndTick {
    private static final Map<UUID, Vec3> lastPositions = new HashMap<>();
    private static final Map<UUID, Integer> tickCounters = new HashMap<>();
    private static final int CHECK_INTERVAL = 20; // 每秒检查一次（20 ticks）
    
    @Override
    public void onEndTick(MinecraftServer server) {
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            UUID playerId = player.getUUID();
            Vec3 currentPos = player.position();
            
            tickCounters.put(playerId, tickCounters.getOrDefault(playerId, 0) + 1);
            
            if (tickCounters.get(playerId) >= CHECK_INTERVAL) {
                tickCounters.put(playerId, 0);
                
                Vec3 lastPos = lastPositions.get(playerId);
                if (lastPos != null) {
                    double distance = calculateDistance(lastPos, currentPos);
                    
                    if (distance > 0.1 && distance < 100) {
                        String playerName = player.getName().getString();
                        PlayerStatistics stats = StatisticsModule.getInstance().getDataManager().getPlayerStatistics(playerName);
                        stats.addDistanceTraveled(distance);
                    }
                }
                
                lastPositions.put(playerId, currentPos);
            }
        }
    }
    
    private double calculateDistance(Vec3 pos1, Vec3 pos2) {
        double dx = pos2.x - pos1.x;
        double dy = pos2.y - pos1.y;
        double dz = pos2.z - pos1.z;
        return Math.sqrt(dx * dx + dy * dy + dz * dz);
    }
    
    public static void removePlayerPosition(UUID playerId) {
        lastPositions.remove(playerId);
        tickCounters.remove(playerId);
    }
}
