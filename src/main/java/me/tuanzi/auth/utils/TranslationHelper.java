package me.tuanzi.auth.utils;

import net.minecraft.commands.CommandSourceStack;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

public class TranslationHelper {

    public static void initialize() {
        AuthTranslationHelper.initialize();
    }

    public static void setDefaultLanguage(String lang) {
        AuthTranslationHelper.setDefaultLanguage(lang);
    }

    public static String getLanguage(Object source) {
        return AuthTranslationHelper.getLanguage(source);
    }

    public static String translate(String key, String lang, Object... args) {
        return AuthTranslationHelper.translate(key, lang, args);
    }

    public static Component translatable(String key) {
        return Component.literal(AuthTranslationHelper.translate(key, "zh_cn"));
    }

    public static Component translatable(String key, Object... args) {
        return Component.literal(AuthTranslationHelper.translate(key, "zh_cn", args));
    }

    public static void sendMessage(ServerPlayer player, String key) {
        AuthTranslationHelper.sendMessage(player, key);
    }

    public static void sendMessage(ServerPlayer player, String key, Object... args) {
        AuthTranslationHelper.sendMessage(player, key, args);
    }

    public static void sendSuccess(CommandSourceStack source, String key) {
        AuthTranslationHelper.sendSuccess(source, key);
    }

    public static void sendSuccess(CommandSourceStack source, String key, Object... args) {
        AuthTranslationHelper.sendSuccess(source, key, args);
    }

    public static void sendFailure(CommandSourceStack source, String key) {
        AuthTranslationHelper.sendFailure(source, key);
    }

    public static void sendFailure(CommandSourceStack source, String key, Object... args) {
        AuthTranslationHelper.sendFailure(source, key, args);
    }
}
