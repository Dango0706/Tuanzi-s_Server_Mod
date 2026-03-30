package me.tuanzi.auth.utils;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

public class TranslationHelper {

    public static Component translatable(String key) {
        return Component.translatable(key);
    }

    public static Component translatable(String key, Object... args) {
        return Component.translatable(key, args);
    }

    public static void sendMessage(ServerPlayer player, String key) {
        player.sendSystemMessage(translatable(key));
    }

    public static void sendMessage(ServerPlayer player, String key, Object... args) {
        player.sendSystemMessage(translatable(key, args));
    }

    public static void sendSuccess(net.minecraft.commands.CommandSourceStack source, String key) {
        source.sendSuccess(() -> translatable(key), false);
    }

    public static void sendSuccess(net.minecraft.commands.CommandSourceStack source, String key, Object... args) {
        source.sendSuccess(() -> translatable(key, args), false);
    }

    public static void sendFailure(net.minecraft.commands.CommandSourceStack source, String key) {
        source.sendFailure(translatable(key));
    }

    public static void sendFailure(net.minecraft.commands.CommandSourceStack source, String key, Object... args) {
        source.sendFailure(translatable(key, args));
    }
}
