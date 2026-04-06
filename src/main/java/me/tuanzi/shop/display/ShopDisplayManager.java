package me.tuanzi.shop.display;

import me.tuanzi.shop.shop.ShopInstance;
import me.tuanzi.shop.shop.ShopManager;
import me.tuanzi.shop.util.DevFlowLogger;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Display;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class ShopDisplayManager {
    private static final Logger LOGGER = LoggerFactory.getLogger("ShopModule/Display");
    private static final String SHOP_DISPLAY_TAG = "shop_display";
    private static final double DISPLAY_XZ_OFFSET = 0.5D;
    private static final double DISPLAY_Y_OFFSET = 0.9D;
    private static final double DISPLAY_SEARCH_RADIUS = 1.2D;
    private static final double DISPLAY_POSITION_EPSILON_SQR = 1.0E-4D;
    private static final double DISPLAY_VELOCITY_EPSILON_SQR = 1.0E-6D;
    private final ShopManager shopManager;
    private final Map<UUID, UUID> shopToDisplayMap = new ConcurrentHashMap<>();
    private final Map<UUID, UUID> displayToShopMap = new ConcurrentHashMap<>();

    public ShopDisplayManager(ShopManager shopManager) {
        this.shopManager = shopManager;
    }

    public void createDisplayForShop(ShopInstance shop, ServerLevel level) {
        if (shop == null || level == null) {
            DevFlowLogger.error("悬浮物品创建", "shop 或 level 为 null");
            LOGGER.warn("[商店展示] 创建悬浮物品失败：shop 或 level 为空");
            return;
        }

        DevFlowLogger.startFlow("悬浮物品创建");
        DevFlowLogger.param("悬浮物品创建", "shopId", shop.getShopId());
        DevFlowLogger.param("悬浮物品创建", "shopPos", shop.getShopPos());
        DevFlowLogger.param("悬浮物品创建", "tradeItem", shop.getTradeItem().getDisplayName().getString());

        BlockPos shopPos = shop.getShopPos();
        LOGGER.info("[商店展示] 正在为商店创建悬浮物品 - 商店ID: {}, 位置: [{}, {}, {}], 物品: {}",
                shop.getShopId(), shopPos.getX(), shopPos.getY(), shopPos.getZ(), 
                shop.getTradeItem().getDisplayName().getString());
        
        DevFlowLogger.step("悬浮物品创建", "准备物品实体数据");
        ItemStack displayItem = toDisplayItem(shop.getTradeItem());

        removeDisplayForShop(shop.getShopId(), level);
        removeLegacyItemDisplayNearShop(shop, level);

        DevFlowLogger.step("悬浮物品创建", "创建ItemEntity实体");
        Vec3 displayPos = getExpectedDisplayPos(shop);
        ItemEntity displayEntity = new ItemEntity(level, displayPos.x, displayPos.y, displayPos.z, displayItem, 0.0, 0.0, 0.0);
        applyDisplayStyle(displayEntity, displayItem);
        anchorDisplayEntity(displayEntity, shop);

        DevFlowLogger.param("悬浮物品创建", "displayPos", displayPos);

        DevFlowLogger.step("悬浮物品创建", "将实体添加到世界");
        if (level.addFreshEntity(displayEntity)) {
            shopToDisplayMap.put(shop.getShopId(), displayEntity.getUUID());
            displayToShopMap.put(displayEntity.getUUID(), shop.getShopId());
            
            LOGGER.info("[商店展示] 悬浮物品创建成功 - 展示实体ID: {}, 商店ID: {}",
                    displayEntity.getUUID(), shop.getShopId());
            
            DevFlowLogger.status("悬浮物品创建", "实体添加成功");
            DevFlowLogger.param("悬浮物品创建", "displayEntityId", displayEntity.getUUID());
            DevFlowLogger.endFlow("悬浮物品创建", true, "悬浮物品创建成功");
        } else {
            LOGGER.error("[商店展示] 悬浮物品创建失败 - 无法添加实体到世界, 商店ID: {}", shop.getShopId());
            DevFlowLogger.error("悬浮物品创建", "无法添加实体到世界");
            DevFlowLogger.endFlow("悬浮物品创建", false, "实体添加失败");
        }
    }

    public void removeDisplayForShop(UUID shopId, ServerLevel level) {
        if (shopId == null || level == null) {
            return;
        }
        LOGGER.info("[商店展示] 正在移除商店的悬浮物品 - 商店ID: {}", shopId);
        UUID displayId = shopToDisplayMap.remove(shopId);
        if (displayId != null) {
            displayToShopMap.remove(displayId);
            
            Entity entity = level.getEntity(displayId);
            if (entity != null) {
                entity.discard();
                LOGGER.info("[商店展示] 悬浮物品已移除 - 展示实体ID: {}, 商店ID: {}", displayId, shopId);
            } else {
                LOGGER.warn("[商店展示] 未找到悬浮物品实体 - 展示实体ID: {}, 商店ID: {}", displayId, shopId);
            }
        } else {
            LOGGER.warn("[商店展示] 商店没有关联的悬浮物品 - 商店ID: {}", shopId);
        }
    }

    public void updateDisplayItem(UUID shopId, ItemStack newItem, ServerLevel level) {
        if (shopId == null || level == null || newItem == null || newItem.isEmpty()) {
            return;
        }

        LOGGER.info("[商店展示] 正在更新商店悬浮物品 - 商店ID: {}, 新物品: {}", shopId, newItem.getDisplayName().getString());
        UUID displayId = shopToDisplayMap.get(shopId);
        if (displayId != null) {
            Entity entity = level.getEntity(displayId);
            if (entity instanceof ItemEntity itemEntity) {
                ItemStack displayItem = toDisplayItem(newItem);
                itemEntity.setItem(displayItem);
                applyDisplayStyle(itemEntity, displayItem);
                shopManager.getShopById(shopId).ifPresent(shop -> anchorDisplayEntity(itemEntity, shop));
                LOGGER.info("[商店展示] 悬浮物品已更新 - 展示实体ID: {}, 商店ID: {}, 新物品: {}",
                        displayId, shopId, newItem.getDisplayName().getString());
            } else {
                LOGGER.warn("[商店展示] 找到展示实体ID但实体类型异常或已不存在 - 展示实体ID: {}", displayId);
                removeDisplayReference(shopId, displayId);
                Optional<ShopInstance> shopOpt = shopManager.getShopById(shopId);
                shopOpt.ifPresent(shop -> createDisplayForShop(shop, level));
            }
        } else {
            LOGGER.warn("[商店展示] 商店没有关联的悬浮物品，尝试重新创建 - 商店ID: {}", shopId);
            Optional<ShopInstance> shopOpt = shopManager.getShopById(shopId);
            if (shopOpt.isPresent()) {
                createDisplayForShop(shopOpt.get(), level);
            }
        }
    }

    public void maintainAllDisplays(ServerLevel level) {
        if (level == null || shopToDisplayMap.isEmpty()) {
            return;
        }

        for (Map.Entry<UUID, UUID> entry : shopToDisplayMap.entrySet()) {
            UUID shopId = entry.getKey();
            UUID displayId = entry.getValue();
            Optional<ShopInstance> shopOpt = shopManager.getShopById(shopId);
            if (shopOpt.isEmpty() || !shopOpt.get().isValid()) {
                removeDisplayReference(shopId, displayId);
                continue;
            }

            ShopInstance shop = shopOpt.get();
            Entity entity = level.getEntity(displayId);
            if (!(entity instanceof ItemEntity itemEntity) || !entity.isAlive()) {
                removeDisplayReference(shopId, displayId);
                createDisplayForShop(shop, level);
                continue;
            }

            anchorDisplayEntity(itemEntity, shop);
            ItemStack expected = toDisplayItem(shop.getTradeItem());
            if (!ItemStack.isSameItemSameComponents(itemEntity.getItem(), expected) || itemEntity.getItem().getCount() != 1) {
                itemEntity.setItem(expected);
                itemEntity.setCustomName(expected.getDisplayName());
            }
        }
    }

    public void removeAllDisplays(ServerLevel level) {
        for (UUID displayId : displayToShopMap.keySet()) {
            Entity entity = level.getEntity(displayId);
            if (entity != null) {
                entity.discard();
            }
        }
        shopToDisplayMap.clear();
        displayToShopMap.clear();
    }

    public void initializeAllDisplays(ServerLevel level) {
        var shops = shopManager.getAllShops();
        LOGGER.debug("正在为 {} 个商店初始化悬浮物品显示", shops.size());
        shopToDisplayMap.clear();
        displayToShopMap.clear();
        cleanupLegacyDisplays(level);
        for (ShopInstance shop : shops) {
            createDisplayForShop(shop, level);
        }
    }

    public Optional<UUID> getDisplayIdForShop(UUID shopId) {
        return Optional.ofNullable(shopToDisplayMap.get(shopId));
    }

    public Optional<UUID> getShopIdForDisplay(UUID displayId) {
        return Optional.ofNullable(displayToShopMap.get(displayId));
    }

    public boolean isShopDisplay(UUID entityUuid) {
        return displayToShopMap.containsKey(entityUuid);
    }

    private void cleanupLegacyDisplays(ServerLevel level) {
        int removedCount = 0;
        for (ShopInstance shop : shopManager.getAllShops()) {
            removedCount += removeLegacyItemDisplayNearShop(shop, level);
        }
        if (removedCount > 0) {
            LOGGER.info("[商店展示] 已清理历史遗留的物品实体展示: {} 个", removedCount);
        }
    }

    private int removeLegacyItemDisplayNearShop(ShopInstance shop, ServerLevel level) {
        Vec3 expectedPos = getExpectedDisplayPos(shop);
        AABB searchBox = new AABB(
                expectedPos.x - DISPLAY_SEARCH_RADIUS, expectedPos.y - DISPLAY_SEARCH_RADIUS, expectedPos.z - DISPLAY_SEARCH_RADIUS,
                expectedPos.x + DISPLAY_SEARCH_RADIUS, expectedPos.y + DISPLAY_SEARCH_RADIUS, expectedPos.z + DISPLAY_SEARCH_RADIUS
        );

        int removed = 0;
        ItemStack expectedItem = toDisplayItem(shop.getTradeItem());
        for (Display.ItemDisplay displayEntity : level.getEntitiesOfClass(Display.ItemDisplay.class, searchBox)) {
            if (displayEntity.entityTags().contains(SHOP_DISPLAY_TAG)
                    || ItemStack.isSameItemSameComponents(displayEntity.getItemStack(), expectedItem)) {
                displayEntity.discard();
                removed++;
            }
        }

        for (ItemEntity itemEntity : level.getEntitiesOfClass(ItemEntity.class, searchBox)) {
            ItemStack stack = itemEntity.getItem();
            if (stack.isEmpty()) {
                continue;
            }

            boolean sameItem = ItemStack.isSameItemSameComponents(stack, expectedItem) || stack.is(expectedItem.getItem());
            boolean hasDisplayTag = itemEntity.entityTags().contains(SHOP_DISPLAY_TAG);
            boolean looksLikeOldDisplay = itemEntity.isNoGravity() && itemEntity.isCustomNameVisible() && itemEntity.hasPickUpDelay();
            boolean looksLikeDisplay = hasDisplayTag || looksLikeOldDisplay;
            if (sameItem && looksLikeDisplay) {
                itemEntity.discard();
                removed++;
            }
        }
        return removed;
    }

    private void removeDisplayReference(UUID shopId, UUID displayId) {
        if (shopId != null) {
            shopToDisplayMap.remove(shopId);
        }
        if (displayId != null) {
            displayToShopMap.remove(displayId);
        }
    }

    private ItemStack toDisplayItem(ItemStack source) {
        ItemStack displayItem = source.copy();
        displayItem.setCount(1);
        return displayItem;
    }

    private Vec3 getExpectedDisplayPos(ShopInstance shop) {
        BlockPos shopPos = shop.getShopPos();
        return new Vec3(
                shopPos.getX() + DISPLAY_XZ_OFFSET,
                shopPos.getY() + DISPLAY_Y_OFFSET,
                shopPos.getZ() + DISPLAY_XZ_OFFSET
        );
    }

    private void applyDisplayStyle(ItemEntity itemEntity, ItemStack displayItem) {
        itemEntity.setItem(displayItem);
        itemEntity.setNoGravity(true);
        itemEntity.noPhysics = true;
        itemEntity.setInvulnerable(true);
        itemEntity.setSilent(true);
        itemEntity.setNeverPickUp();
        itemEntity.setUnlimitedLifetime();
        itemEntity.addTag(SHOP_DISPLAY_TAG);
        itemEntity.setCustomName(displayItem.getDisplayName());
        itemEntity.setCustomNameVisible(true);
    }

    private void anchorDisplayEntity(ItemEntity itemEntity, ShopInstance shop) {
        Vec3 expectedPos = getExpectedDisplayPos(shop);
        if (itemEntity.position().distanceToSqr(expectedPos) > DISPLAY_POSITION_EPSILON_SQR) {
            itemEntity.setPos(expectedPos.x, expectedPos.y, expectedPos.z);
        }
        if (itemEntity.getDeltaMovement().lengthSqr() > DISPLAY_VELOCITY_EPSILON_SQR) {
            itemEntity.setDeltaMovement(Vec3.ZERO);
        }
        itemEntity.setNoGravity(true);
        itemEntity.noPhysics = true;
        itemEntity.setInvulnerable(true);
        itemEntity.setSilent(true);
        itemEntity.setNeverPickUp();
        itemEntity.setUnlimitedLifetime();
        if (!itemEntity.entityTags().contains(SHOP_DISPLAY_TAG)) {
            itemEntity.addTag(SHOP_DISPLAY_TAG);
        }
        itemEntity.setCustomNameVisible(true);
    }
}
