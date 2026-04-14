package me.tuanzi.shop.utils;

import me.tuanzi.shop.ShopModule;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.locale.Language;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

public class ItemUtils {
    private static final Logger LOGGER = LoggerFactory.getLogger(ShopModule.MOD_ID);
    
    private ItemUtils() {
    }

    public static Optional<ItemStack> findItemByDisplayName(String displayName, Level level) {
        return findItemByDisplayName(displayName, level, "en_us");
    }

    public static Optional<ItemStack> findItemByDisplayName(String displayName, Level level, String preferredLanguage) {
        if (displayName == null || displayName.isEmpty()) {
            return Optional.empty();
        }

        String searchName = displayName.trim();
        String normalizedSearch = normalizeTokenForCompare(searchName);
        
        LOGGER.info("[商店调试] 开始查找物品: '{}', 客户端语言: {}", searchName, preferredLanguage);
        
        for (var item : BuiltInRegistries.ITEM) {
            if (item == Items.AIR) {
                continue;
            }

            ItemStack stack = new ItemStack(item);
            String translationKey = item.getDescriptionId();

            Set<String> candidates = new LinkedHashSet<>();
            candidates.add(translationKey);

            Identifier itemId = BuiltInRegistries.ITEM.getKey(item);
            if (itemId != null) {
                candidates.add(itemId.toString());
                candidates.add(itemId.getPath());
                candidates.add(itemId.getPath().replace('_', ' '));
                candidates.add(snakeToLowerCamel(itemId.getPath()));
            }

            String enUsName = Language.getInstance().getOrDefault(translationKey, translationKey);
            candidates.add(enUsName);

            String preferredTranslated = ShopTranslationHelper.getRawTranslation(translationKey, preferredLanguage);
            if (!preferredTranslated.equals(translationKey)) {
                candidates.add(preferredTranslated);
            }

            String zhName = ShopTranslationHelper.getRawTranslation(translationKey, "zh_cn");
            if (!zhName.equals(translationKey)) {
                candidates.add(zhName);
            }

            String enName = ShopTranslationHelper.getRawTranslation(translationKey, "en_us");
            if (!enName.equals(translationKey)) {
                candidates.add(enName);
            }

            for (String candidate : candidates) {
                if (candidate == null || candidate.isBlank()) {
                    continue;
                }

                if (candidate.equalsIgnoreCase(searchName) || normalizeTokenForCompare(candidate).equals(normalizedSearch)) {
                    LOGGER.info("[商店调试] 名称匹配: '{}' -> {}", candidate, translationKey);
                    return Optional.of(stack);
                }
            }
        }
        
        LOGGER.warn("[商店调试] 未找到物品: '{}'", searchName);
        return Optional.empty();
    }

    public static Optional<ItemStack> parseItemStackFlexible(String input, Level level) {
        return parseItemStackFlexible(input, level, "en_us");
    }

    public static Optional<ItemStack> parseItemStackFlexible(String input, Level level, String preferredLanguage) {
        if (input == null || input.isEmpty()) {
            return Optional.empty();
        }

        String trimmed = input.trim();

        for (String candidate : buildRegistryIdCandidates(trimmed)) {
            try {
                Identifier itemId = candidate.contains(":")
                        ? Identifier.parse(candidate)
                        : Identifier.fromNamespaceAndPath("minecraft", candidate);

                var item = BuiltInRegistries.ITEM.getValue(itemId);
                if (item != null && item != Items.AIR) {
                    LOGGER.info("[商店调试] 物品ID匹配: '{}'", itemId);
                    return Optional.of(new ItemStack(item));
                }
            } catch (Exception ignored) {
            }
        }

        return findItemByDisplayName(trimmed, level, preferredLanguage);
    }

    private static Set<String> buildRegistryIdCandidates(String rawInput) {
        String trimmed = rawInput.trim();
        LinkedHashSet<String> candidates = new LinkedHashSet<>();

        if (trimmed.isEmpty()) {
            return candidates;
        }

        String normalized = normalizeItemPathInput(trimmed);
        candidates.add(normalized);

        if (normalized.startsWith("item.minecraft.")) {
            candidates.add(normalized.substring("item.minecraft.".length()));
        }

        if (normalized.startsWith("minecraft:")) {
            candidates.add(normalized.substring("minecraft:".length()));
        }

        if (normalized.startsWith("minecraft.")) {
            candidates.add(normalized.substring("minecraft.".length()));
        }

        return candidates;
    }

    private static String normalizeItemPathInput(String value) {
        String normalized = value.trim()
                .replace('-', '_')
                .replace(' ', '_');

        normalized = toSnakeCase(normalized).toLowerCase(Locale.ROOT);
        return normalized;
    }

    private static String normalizeTokenForCompare(String value) {
        return value.toLowerCase(Locale.ROOT)
                .replaceAll("\\s+", "")
                .replace(" ", "")
                .replace("_", "")
                .replace("-", "")
                .replace(":", "")
                .replace(".", "")
                .replace("§", "");
    }

    private static String toSnakeCase(String value) {
        StringBuilder out = new StringBuilder(value.length() + 8);
        for (int i = 0; i < value.length(); i++) {
            char c = value.charAt(i);
            if (Character.isUpperCase(c)) {
                if (i > 0 && value.charAt(i - 1) != '_' && value.charAt(i - 1) != ':') {
                    out.append('_');
                }
                out.append(Character.toLowerCase(c));
            } else {
                out.append(c);
            }
        }
        return out.toString();
    }

    private static String snakeToLowerCamel(String snakeCase) {
        if (snakeCase == null || snakeCase.isEmpty()) {
            return snakeCase;
        }

        StringBuilder result = new StringBuilder(snakeCase.length());
        boolean upperNext = false;
        for (int i = 0; i < snakeCase.length(); i++) {
            char c = snakeCase.charAt(i);
            if (c == '_') {
                upperNext = true;
                continue;
            }
            if (upperNext) {
                result.append(Character.toUpperCase(c));
                upperNext = false;
            } else {
                result.append(c);
            }
        }
        return result.toString();
    }

    public static boolean itemsMatch(ItemStack a, ItemStack b) {
        if (a.isEmpty() && b.isEmpty()) {
            return true;
        }
        if (a.isEmpty() || b.isEmpty()) {
            return false;
        }
        if (!a.is(b.getItem())) {
            return false;
        }
        return componentsMatch(a, b);
    }

    public static boolean componentsMatch(ItemStack a, ItemStack b) {
        return Objects.equals(a.getComponentsPatch(), b.getComponentsPatch());
    }

    public static ItemStack copyItemStack(ItemStack original) {
        if (original.isEmpty()) {
            return ItemStack.EMPTY;
        }
        return original.copy();
    }

    public static ItemStack copyItemStackWithCount(ItemStack original, int count) {
        if (original.isEmpty() || count <= 0) {
            return ItemStack.EMPTY;
        }
        ItemStack copy = original.copy();
        copy.setCount(count);
        return copy;
    }

    public static String getItemDisplayName(ItemStack stack) {
        if (stack.isEmpty()) {
            return ShopTranslationHelper.getRawTranslation("block.minecraft.unknown");
        }
        return stack.getDisplayName().getString();
    }

    public static String getItemId(ItemStack stack) {
        if (stack.isEmpty()) {
            return "minecraft:air";
        }
        return stack.getItem().getDescriptionId();
    }

    public static String getItemRegistryId(ItemStack stack) {
        if (stack.isEmpty()) {
            return "minecraft:air";
        }
        return BuiltInRegistries.ITEM.getKey(stack.getItem()).toString();
    }
}
