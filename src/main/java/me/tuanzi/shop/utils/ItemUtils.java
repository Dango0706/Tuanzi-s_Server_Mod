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

import java.util.Objects;
import java.util.Optional;

public class ItemUtils {
    private static final Logger LOGGER = LoggerFactory.getLogger(ShopModule.MOD_ID);
    
    private ItemUtils() {
    }

    public static Optional<ItemStack> findItemByDisplayName(String displayName, Level level) {
        if (displayName == null || displayName.isEmpty()) {
            return Optional.empty();
        }

        String searchName = displayName.trim();
        String searchLower = searchName.toLowerCase();
        
        LOGGER.info("[商店调试] 开始查找物品: '{}'", searchName);
        
        for (var item : BuiltInRegistries.ITEM) {
            if (item == Items.AIR) {
                continue;
            }
            
            ItemStack stack = new ItemStack(item);
            
            String translationKey = item.getDescriptionId();
            String translatedName = Language.getInstance().getOrDefault(translationKey, translationKey);
            
            String translatedNameClean = translatedName.toLowerCase();
            
            if (translatedName.equals(searchName) || translatedNameClean.equals(searchLower)) {
                LOGGER.info("[商店调试] 名称精确匹配: '{}' -> {}", translatedName, translationKey);
                return Optional.of(stack);
            }
            
            if (translatedNameClean.contains(searchLower) || searchLower.contains(translatedNameClean)) {
                LOGGER.info("[商店调试] 名称包含匹配: '{}' -> {}", translatedName, translationKey);
                return Optional.of(stack);
            }
        }
        
        LOGGER.warn("[商店调试] 未找到物品: '{}'", searchName);
        return Optional.empty();
    }

    public static Optional<ItemStack> parseItemStackFlexible(String input, Level level) {
        if (input == null || input.isEmpty()) {
            return Optional.empty();
        }

        String trimmed = input.trim();
        
        try {
            Identifier itemId;
            if (trimmed.contains(":")) {
                itemId = Identifier.parse(trimmed);
            } else {
                itemId = Identifier.fromNamespaceAndPath("minecraft", trimmed.toLowerCase().replace(" ", "_"));
            }

            var item = BuiltInRegistries.ITEM.getValue(itemId);
            if (item != null && item != Items.AIR) {
                LOGGER.info("[商店调试] 物品ID匹配: '{}'", itemId);
                return Optional.of(new ItemStack(item));
            }
        } catch (Exception ignored) {
        }

        return findItemByDisplayName(trimmed, level);
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
            return "Empty";
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
