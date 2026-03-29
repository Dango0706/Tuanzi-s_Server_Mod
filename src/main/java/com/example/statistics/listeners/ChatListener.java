package com.example.statistics.listeners;

import com.example.statistics.StatisticsModule;
import com.example.statistics.data.PlayerStatistics;
import net.fabricmc.fabric.api.message.v1.ServerMessageEvents;

public class ChatListener {

    public static void register() {
        ServerMessageEvents.CHAT_MESSAGE.register((message, sender, params) -> {
            String playerName = sender.getName().getString();
            PlayerStatistics stats = StatisticsModule.getInstance().getDataManager().getPlayerStatistics(playerName);
            stats.addChatMessage();

            StatisticsModule.LOGGER.debug("Player {} sent chat message, total: {}", playerName, stats.getChatMessagesSent());
        });
    }
}
