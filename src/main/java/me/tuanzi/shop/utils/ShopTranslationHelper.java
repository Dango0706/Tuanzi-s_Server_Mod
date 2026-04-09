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

    public static String getLanguage(Object source) {
        if (source instanceof net.minecraft.commands.CommandSourceStack css) {
            net.minecraft.server.level.ServerPlayer player = css.getPlayer();
            if (player != null) return getLanguage(player);
        }
        if (source instanceof net.minecraft.server.level.ServerPlayer player) {
            String lang = player.clientInformation().language();
            return lang != null ? lang : DEFAULT_LANGUAGE;
        }
        return DEFAULT_LANGUAGE;
    }

    public static Component translatable(Object source, String key) {
        String lang = getLanguage(source);
        String text = getTranslationTemplate(key, lang);
        if (text != null) {
            return Component.literal(parseColor(text));
        }
        return Component.translatable(key);
    }

    public static Component translatable(Object source, String key, Object... args) {
        String lang = getLanguage(source);
        String template = getTranslationTemplate(key, lang);
        if (template != null) {
            try {
                Object[] parsedArgs = new Object[args.length];
                for (int i = 0; i < args.length; i++) {
                    if (args[i] instanceof String s) {
                        parsedArgs[i] = parseColor(s);
                    } else {
                        parsedArgs[i] = args[i];
                    }
                }
                return Component.literal(parseColor(String.format(Locale.ROOT, template, parsedArgs)));
            } catch (Exception ignored) {
                return Component.literal(parseColor(template));
            }
        }
        return Component.translatable(key, args);
    }

    public static Component translatable(String key) {
        return translatable(null, key);
    }

    public static Component translatable(String key, Object... args) {
        return translatable(null, key, args);
    }

    public static Component literal(String text) {
        return Component.literal(parseColor(text));
    }

    public static Component colored(String text) {
        return Component.literal(parseColor(text));
    }

    public static String parseColor(String text) {
        if (text == null) return "";
        // If the text starts with a single letter color code (like 'a', 'c', 'e') and is followed by content, 
        // it's likely a shortcut used in the command classes.
        if (text.length() > 1 && "0123456789abcdefklmnor".indexOf(text.charAt(0)) != -1 && text.charAt(1) != ' ') {
            text = "§" + text;
        }
        return text.replace("&", "§");
    }

    public static String getRawTranslation(String key) {
        return getRawTranslation(key, DEFAULT_LANGUAGE);
    }

    public static String getRawTranslation(String key, String language) {
        String normalizedLang = normalizeLanguage(language);
        String text = getTranslationTemplate(key, normalizedLang);
        return text != null ? text : key;
    }

    public static String getRawTranslation(Object source, String key) {
        return getRawTranslation(key, getLanguage(source));
    }

    public static String getRawTranslation(Object source, String key, Object... args) {
        String template = getRawTranslation(key, getLanguage(source));
        try {
            return String.format(template, args);
        } catch (Exception e) {
            return template;
        }
    }

    public static boolean hasTranslation(String key, String language) {
        return !getRawTranslation(key, language).equals(key);
    }

    private static String normalizeLanguage(String language) {
        return MinecraftLanguageHelper.normalizeLanguage(language);
    }

    public static Component getSellPattern(String lang) {
        return Component.literal(getRawTranslation("shop.sign.pattern.sell", lang));
    }

    public static Component getBuyPattern(String lang) {
        return Component.literal(getRawTranslation("shop.sign.pattern.buy", lang));
    }

    public static Component getSellPatternCN() {
        return Component.literal(getRawTranslation("shop.sign.pattern.sell_cn", "zh_cn"));
    }

    public static Component getBuyPatternCN() {
        return Component.literal(getRawTranslation("shop.sign.pattern.buy_cn", "zh_cn"));
    }

    public static boolean isSellPattern(String text) {
        return getRawTranslation("shop.sign.pattern.sell", "en_us").equalsIgnoreCase(text)
                || getRawTranslation("shop.sign.pattern.sell", "zh_cn").equalsIgnoreCase(text)
                || getRawTranslation("shop.sign.pattern.sell_cn", "zh_cn").equalsIgnoreCase(text);
    }

    public static boolean isBuyPattern(String text) {
        return getRawTranslation("shop.sign.pattern.buy", "en_us").equalsIgnoreCase(text)
                || getRawTranslation("shop.sign.pattern.buy", "zh_cn").equalsIgnoreCase(text)
                || getRawTranslation("shop.sign.pattern.buy_cn", "zh_cn").equalsIgnoreCase(text);
    }

    public static Component shopTypeDisplayName(Object source, boolean isSellShop) {
        return isSellShop ? translatable(source, "shop.type.sell") : translatable(source, "shop.type.buy");
    }

    public static Component shopTypeDisplayName(boolean isSellShop) {
        return shopTypeDisplayName(null, isSellShop);
    }

    public static Component header(Object source, String key) {
        Component title = translatable(source, key);
        return Component.empty()
                .append(colored("§e========== [ "))
                .append(title)
                .append(colored(" §e] =========="));
    }
}
