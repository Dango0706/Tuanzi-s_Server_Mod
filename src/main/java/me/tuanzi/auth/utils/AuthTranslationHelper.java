package me.tuanzi.auth.utils;

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

public class AuthTranslationHelper {

    private static final Logger LOGGER = LoggerFactory.getLogger("AuthModule");
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
        try (InputStream is = AuthTranslationHelper.class.getResourceAsStream(path)) {
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

    public static String getLanguage(Object source) {
        if (source instanceof net.minecraft.commands.CommandSourceStack css) {
            net.minecraft.server.level.ServerPlayer player = css.getPlayer();
            if (player != null) return getLanguage(player);
        }
        if (source instanceof ServerPlayer player) {
            String lang = player.clientInformation().language();
            return lang != null ? lang : defaultLanguage;
        }
        return defaultLanguage;
    }

    public static String translate(String key, String languageCode) {
        String normalized = languageCode != null ? languageCode.toLowerCase() : defaultLanguage;
        Map<String, String> translations = normalized.startsWith("zh") ? zhCnTranslations : enUsTranslations;
        String translation = translations.get(key);
        if (translation == null && normalized.startsWith("zh")) {
            translation = enUsTranslations.get(key);
        } else if (translation == null) {
            translation = zhCnTranslations.get(key);
        }
        return translation != null ? translation : key;
    }

    public static String translate(String key, String languageCode, Object... args) {
        String template = translate(key, languageCode);
        if (template.equals(key)) {
            return key;
        }
        try {
            return String.format(template, args);
        } catch (Exception e) {
            return template;
        }
    }

    public static Component translateToComponent(String key, String languageCode) {
        return Component.literal(translate(key, languageCode));
    }

    public static Component translateToComponent(String key, String languageCode, Object... args) {
        return Component.literal(translate(key, languageCode, args));
    }

    public static void sendMessage(ServerPlayer player, String key) {
        player.sendSystemMessage(Component.literal(translate(key, getLanguage(player))));
    }

    public static void sendMessage(ServerPlayer player, String key, Object... args) {
        player.sendSystemMessage(Component.literal(translate(key, getLanguage(player), args)));
    }

    public static void sendSuccess(net.minecraft.commands.CommandSourceStack source, String key) {
        source.sendSuccess(() -> Component.literal(translate(key, getLanguage(source))), false);
    }

    public static void sendSuccess(net.minecraft.commands.CommandSourceStack source, String key, Object... args) {
        source.sendSuccess(() -> Component.literal(translate(key, getLanguage(source), args)), false);
    }

    public static void sendFailure(net.minecraft.commands.CommandSourceStack source, String key) {
        source.sendFailure(Component.literal(translate(key, getLanguage(source))));
    }

    public static void sendFailure(net.minecraft.commands.CommandSourceStack source, String key, Object... args) {
        source.sendFailure(Component.literal(translate(key, getLanguage(source), args)));
    }
}
