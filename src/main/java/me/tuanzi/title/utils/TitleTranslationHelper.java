package me.tuanzi.title.utils;

import net.minecraft.commands.CommandSourceStack;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.level.ServerPlayer;

public class TitleTranslationHelper {
    public static void sendSuccess(CommandSourceStack source, String message) {
        source.sendSuccess(() -> Component.literal("§a" + message), false);
    }

    public static void sendFailure(CommandSourceStack source, String message) {
        source.sendFailure(Component.literal("§c" + message));
    }

    public static void sendMessage(ServerPlayer player, String message) {
        player.sendSystemMessage(Component.literal(message));
    }
}
