package me.tuanzi.statistics.listeners;

import me.tuanzi.statistics.StatisticsModule;
import me.tuanzi.statistics.data.PlayerStatistics;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.ChunkPos;

public class PlayerActivityListener {
    
    public static void register() {
        ServerTickEvents.END_SERVER_TICK.register(PlayerActivityListener::onServerTick);
    }
    
    private static void onServerTick(MinecraftServer server) {
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            String playerName = player.getName().getString();
            PlayerStatistics stats = StatisticsModule.getInstance().getDataManager().getPlayerStatistics(playerName);
            
            ChunkPos chunkPos = player.chunkPosition();
            String chunkKey = player.level().dimension().identifier().toString() + ":" + chunkPos.x() + "," + chunkPos.z();
            stats.addExploredChunk(chunkKey);
            
            if (player.isShiftKeyDown()) {
                stats.startSneaking();
            } else {
                stats.stopSneaking();
            }
            
            stats.updateCurrentSession();
        }
    }
}
