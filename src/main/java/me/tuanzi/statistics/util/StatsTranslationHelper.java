package me.tuanzi.statistics.util;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

public class StatsTranslationHelper {

    private static final Logger LOGGER = LoggerFactory.getLogger("StatisticsModule");
    private static final Map<String, String> zhCnTranslations = new HashMap<>();
    private static final Map<String, String> enUsTranslations = new HashMap<>();
    private static boolean initialized = false;
    private static String defaultLanguage = "zh_cn";

    public static void initialize() {
        if (initialized) return;
        
        try {
            loadTranslations("zh_cn", zhCnTranslations);
            loadTranslations("en_us", enUsTranslations);
            initialized = true;
            LOGGER.info("Translation system initialized. Loaded {} zh_cn and {} en_us translations", 
                zhCnTranslations.size(), enUsTranslations.size());
        } catch (Exception e) {
            LOGGER.error("Failed to load translations: {}", e.getMessage());
        }
    }

    private static void loadTranslations(String langCode, Map<String, String> targetMap) {
        String path = "/assets/tuanzis-server-mod/lang/" + langCode + ".json";
        try (InputStream is = StatsTranslationHelper.class.getResourceAsStream(path)) {
            if (is != null) {
                try (InputStreamReader reader = new InputStreamReader(is, StandardCharsets.UTF_8)) {
                    Map<String, String> translations = new Gson().fromJson(reader, new TypeToken<Map<String, String>>(){}.getType());
                    if (translations != null) {
                        targetMap.putAll(translations);
                        LOGGER.info("Loaded {} translations from {}", translations.size(), path);
                    }
                }
            } else {
                LOGGER.warn("Translation file not found: {}", path);
            }
        } catch (IOException e) {
            LOGGER.error("Failed to load {} translations: {}", langCode, e.getMessage());
        }
    }

    public static void setDefaultLanguage(String lang) {
        defaultLanguage = lang;
    }

    public static String translate(String key, String languageCode) {
        Map<String, String> translations = "zh_cn".equals(languageCode) ? zhCnTranslations : enUsTranslations;
        String translation = translations.get(key);
        return translation != null ? translation : key;
    }

    public static String translate(String key, String languageCode, Object... args) {
        String template = translate(key, languageCode);
        if (template.equals(key)) {
            return key;
        }
        return String.format(template, args);
    }

    public static Component translateToComponent(String key, String languageCode) {
        return Component.literal(translate(key, languageCode));
    }

    public static Component translateToComponent(String key, String languageCode, Object... args) {
        return Component.literal(translate(key, languageCode, args));
    }

    public static void sendMessage(ServerPlayer player, String key) {
        player.sendSystemMessage(Component.literal(translate(key, defaultLanguage)));
    }

    public static void sendMessage(ServerPlayer player, String key, Object... args) {
        player.sendSystemMessage(Component.literal(translate(key, defaultLanguage, args)));
    }

    public static void sendSuccess(net.minecraft.commands.CommandSourceStack source, String key) {
        source.sendSuccess(() -> Component.literal(translate(key, defaultLanguage)), false);
    }

    public static void sendSuccess(net.minecraft.commands.CommandSourceStack source, String key, Object... args) {
        source.sendSuccess(() -> Component.literal(translate(key, defaultLanguage, args)), false);
    }

    public static void sendFailure(net.minecraft.commands.CommandSourceStack source, String key) {
        source.sendFailure(Component.literal(translate(key, defaultLanguage)));
    }

    public static void sendFailure(net.minecraft.commands.CommandSourceStack source, String key, Object... args) {
        source.sendFailure(Component.literal(translate(key, defaultLanguage, args)));
    }
}
