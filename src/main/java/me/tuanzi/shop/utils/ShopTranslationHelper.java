package me.tuanzi.shop.utils;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import me.tuanzi.translation.MinecraftLanguageHelper;
import net.minecraft.network.chat.Component;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class ShopTranslationHelper {
    private static final Map<String, String> ZH_CN_TRANSLATIONS = new HashMap<>();
    private static final Map<String, String> EN_US_TRANSLATIONS = new HashMap<>();
    private static final String DEFAULT_LANGUAGE = "zh_cn";

    static {
        loadTranslations("zh_cn", ZH_CN_TRANSLATIONS);
        loadTranslations("en_us", EN_US_TRANSLATIONS);
    }

    private ShopTranslationHelper() {
    }

    private static void loadTranslations(String langCode, Map<String, String> targetMap) {
        String path = "/assets/tuanzis-server-mod/lang/" + langCode + ".json";
        try (InputStream inputStream = ShopTranslationHelper.class.getResourceAsStream(path)) {
            if (inputStream == null) {
                return;
            }
            try (InputStreamReader reader = new InputStreamReader(inputStream, StandardCharsets.UTF_8)) {
                Map<String, String> loaded = new Gson().fromJson(reader, new TypeToken<Map<String, String>>() {
                }.getType());
                if (loaded != null) {
                    targetMap.putAll(loaded);
                }
            }
        } catch (Exception ignored) {
        }
    }

    private static String getTranslationTemplate(String key, String language) {
        String normalizedLanguage = normalizeLanguage(language);

        String gameTranslation = MinecraftLanguageHelper.translateVanilla(key, normalizedLanguage);
        if (!gameTranslation.equals(key)) {
            return gameTranslation;
        }

        Map<String, String> preferred = "en_us".equals(normalizedLanguage) ? EN_US_TRANSLATIONS : ZH_CN_TRANSLATIONS;
        String template = preferred.get(key);
        if (template != null) {
            return template;
        }

        template = ZH_CN_TRANSLATIONS.get(key);
        if (template != null) {
            return template;
        }

        return EN_US_TRANSLATIONS.get(key);
    }

    public static Component translatable(String key) {
        String text = getTranslationTemplate(key, DEFAULT_LANGUAGE);
        if (text != null) {
            return Component.literal(text);
        }
        return Component.translatable(key);
    }

    public static Component translatable(String key, Object... args) {
        String template = getTranslationTemplate(key, DEFAULT_LANGUAGE);
        if (template != null) {
            try {
                return Component.literal(String.format(Locale.ROOT, template, args));
            } catch (Exception ignored) {
                return Component.literal(template);
            }
        }
        return Component.translatable(key, args);
    }

    public static Component literal(String text) {
        return Component.literal(text);
    }

    public static Component colored(String text) {
        return Component.literal(text);
    }

    public static String getRawTranslation(String key) {
        String text = getTranslationTemplate(key, DEFAULT_LANGUAGE);
        return text != null ? text : key;
    }

    public static String getRawTranslation(String key, String language) {
        String normalizedLang = normalizeLanguage(language);
        String text = getTranslationTemplate(key, normalizedLang);
        return text != null ? text : key;
    }

    public static boolean hasTranslation(String key, String language) {
        return !getRawTranslation(key, language).equals(key);
    }

    private static String normalizeLanguage(String language) {
        return MinecraftLanguageHelper.normalizeLanguage(language);
    }

    public static Component getSellPattern() {
        return Component.literal(getRawTranslation("shop.sign.pattern.sell"));
    }

    public static Component getBuyPattern() {
        return Component.literal(getRawTranslation("shop.sign.pattern.buy"));
    }

    public static Component getSellPatternCN() {
        return Component.literal(getRawTranslation("shop.sign.pattern.sell_cn"));
    }

    public static Component getBuyPatternCN() {
        return Component.literal(getRawTranslation("shop.sign.pattern.buy_cn"));
    }

    public static boolean isSellPattern(String text) {
        return getRawTranslation("shop.sign.pattern.sell").equalsIgnoreCase(text)
                || getRawTranslation("shop.sign.pattern.sell_cn").equalsIgnoreCase(text);
    }

    public static boolean isBuyPattern(String text) {
        return getRawTranslation("shop.sign.pattern.buy").equalsIgnoreCase(text)
                || getRawTranslation("shop.sign.pattern.buy_cn").equalsIgnoreCase(text);
    }

    public static Component shopTypeDisplayName(boolean isSellShop) {
        return isSellShop ? translatable("shop.type.sell") : translatable("shop.type.buy");
    }
}
