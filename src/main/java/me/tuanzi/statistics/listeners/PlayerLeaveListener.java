package me.tuanzi.statistics.listeners;

import me.tuanzi.statistics.StatisticsModule;
import me.tuanzi.statistics.data.PlayerStatistics;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.ServerGamePacketListenerImpl;

public class PlayerLeaveListener implements ServerPlayConnectionEvents.Disconnect {
    @Override
    public void onPlayDisconnect(ServerGamePacketListenerImpl handler, MinecraftServer server) {
        ServerPlayer player = handler.player;
        String playerName = player.getName().getString();

        long loginTime = PlayerJoinListener.getLoginTime(playerName);
        long logoutTime = System.currentTimeMillis() / 1000;
        long playTimeSeconds = logoutTime - loginTime;

        PlayerStatistics stats = StatisticsModule.getInstance().getDataManager().getPlayerStatistics(playerName);
        stats.addPlayTimeSeconds(playTimeSeconds);

        PlayerJoinListener.removeLoginTime(playerName);
        PlayerMoveListener.removePlayerPosition(player.getUUID());

        StatisticsModule.LOGGER.info("Player {} left, played for {} seconds", playerName, playTimeSeconds);
    }
}
