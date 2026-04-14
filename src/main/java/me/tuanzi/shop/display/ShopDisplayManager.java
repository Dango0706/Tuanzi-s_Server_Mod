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
    private static final double DISPLAY_SEARCH_RADIUS = 0.4D;
    private static final double DISPLAY_POSITION_EPSILON_SQR = 1.0E-4D;
    private static final double DISPLAY_VELOCITY_EPSILON_SQR = 1.0E-6D;
    
    // 容错等待设置：如果实体在加载中暂时找不到，等待 100 tick (约5秒) 再尝试重建
    private static final int MISSING_TOLERANCE_TICKS = 100;

    private final ShopManager shopManager;
    private final Map<UUID, UUID> shopToDisplayMap = new ConcurrentHashMap<>();
    private final Map<UUID, UUID> displayToShopMap = new ConcurrentHashMap<>();
    
    // 记录实体连续找不到的次数 (ShopID -> Ticks)
    private final Map<UUID, Integer> missingTicksMap = new ConcurrentHashMap<>();

    public ShopDisplayManager(ShopManager shopManager) {
        this.shopManager = shopManager;
    }

    public void createDisplayForShop(ShopInstance shop, ServerLevel level) {
        if (shop == null || level == null) {
            return;
        }

        UUID shopId = shop.getShopId();
        // 清除容错计数器
        missingTicksMap.remove(shopId);

        BlockPos shopPos = shop.getShopPos();
        ItemStack displayItem = toDisplayItem(shop.getTradeItem());

        // 先清理该位置可能残留的旧实体 (双重保险)
        removeDisplayForShop(shopId, level);
        removeLegacyItemDisplayNearShop(shop, level);

        Vec3 displayPos = getExpectedDisplayPos(shop);
        ItemEntity displayEntity = new ItemEntity(level, displayPos.x, displayPos.y, displayPos.z, displayItem, 0.0, 0.0, 0.0);
        applyDisplayStyle(displayEntity, displayItem);
        anchorDisplayEntity(displayEntity, shop);

        if (level.addFreshEntity(displayEntity)) {
            shopToDisplayMap.put(shopId, displayEntity.getUUID());
            displayToShopMap.put(displayEntity.getUUID(), shopId);
            
            LOGGER.debug("[商店展示] 悬浮物品创建成功 - 展示实体ID: {}, 商店ID: {}",
                    displayEntity.getUUID(), shopId);
        } else {
            LOGGER.error("[商店展示] 悬浮物品创建失败 - 无法添加实体到世界, 商店ID: {}", shopId);
        }
    }

    public void removeDisplayForShop(UUID shopId, ServerLevel level) {
        if (shopId == null || level == null) {
            return;
        }
        
        missingTicksMap.remove(shopId);
        UUID displayId = shopToDisplayMap.remove(shopId);
        if (displayId != null) {
            displayToShopMap.remove(displayId);
            Entity entity = level.getEntity(displayId);
            if (entity != null) {
                entity.discard();
            }
        }

        // 强力位置扫描清理
        shopManager.getShopById(shopId).ifPresent(shop -> {
            removeLegacyItemDisplayNearShop(shop, level);
        });
    }

    public void updateDisplayItem(UUID shopId, ItemStack newItem, ServerLevel level) {
        if (shopId == null || level == null || newItem == null || newItem.isEmpty()) {
            return;
        }

        UUID displayId = shopToDisplayMap.get(shopId);
        if (displayId != null) {
            Entity entity = level.getEntity(displayId);
            if (entity instanceof ItemEntity itemEntity) {
                ItemStack displayItem = toDisplayItem(newItem);
                itemEntity.setItem(displayItem);
                applyDisplayStyle(itemEntity, displayItem);
                shopManager.getShopById(shopId).ifPresent(shop -> anchorDisplayEntity(itemEntity, shop));
            } else {
                removeDisplayForShop(shopId, level);
                shopManager.getShopById(shopId).ifPresent(shop -> createDisplayForShop(shop, level));
            }
        } else {
            shopManager.getShopById(shopId).ifPresent(shop -> createDisplayForShop(shop, level));
        }
    }

    public void maintainAllDisplays(ServerLevel level) {
        if (level == null) {
            return;
        }

        // 核心优化：不再仅遍历 Map，而是遍历所有商店以确保重启后能自动补全
        for (ShopInstance shop : shopManager.getAllShops()) {
            if (!shop.isValid()) {
                removeDisplayForShop(shop.getShopId(), level);
                continue;
            }

            UUID shopId = shop.getShopId();
            BlockPos shopPos = shop.getShopPos();

            // 检查区块加载状态
            if (!level.isLoaded(shopPos)) {
                missingTicksMap.remove(shopId);
                continue;
            }

            UUID displayId = shopToDisplayMap.get(shopId);
            Entity entity = displayId != null ? level.getEntity(displayId) : null;

            if (entity == null || !entity.isAlive()) {
                // 关键容错：如果内存里没记录或实体丢了，累加计数
                int missingCount = missingTicksMap.getOrDefault(shopId, 0) + 1;
                missingTicksMap.put(shopId, missingCount);
                
                // 积攒一定时间后尝试恢复或重建
                if (missingCount >= MISSING_TOLERANCE_TICKS) {
                    LOGGER.debug("[商店展示] 正在为商店 {} 恢复/重建悬浮物...", shopId);
                    int found = tryToRecoverDisplayReference(shop, level);
                    if (found == 0) {
                        createDisplayForShop(shop, level);
                    }
                    missingTicksMap.remove(shopId);
                }
                continue;
            }

            // 找到了且存活
            missingTicksMap.remove(shopId);
            if (entity instanceof ItemEntity itemEntity) {
                anchorDisplayEntity(itemEntity, shop);
                
                ItemStack expected = toDisplayItem(shop.getTradeItem());
                if (!ItemStack.isSameItemSameComponents(itemEntity.getItem(), expected)) {
                    itemEntity.setItem(expected);
                    itemEntity.setCustomName(expected.getDisplayName());
                }
            }
        }
    }

    /**
     * 尝试通过扫描世界中的实体来恢复内存引用，防止重复创建
     */
    private int tryToRecoverDisplayReference(ShopInstance shop, ServerLevel level) {
        Vec3 expectedPos = getExpectedDisplayPos(shop);
        AABB searchBox = new AABB(
                expectedPos.x - 0.2, expectedPos.y - 0.2, expectedPos.z - 0.2,
                expectedPos.x + 0.2, expectedPos.y + 0.2, expectedPos.z + 0.2
        );

        for (ItemEntity itemEntity : level.getEntitiesOfClass(ItemEntity.class, searchBox)) {
            if (itemEntity.entityTags().contains(SHOP_DISPLAY_TAG) && itemEntity.isAlive()) {
                shopToDisplayMap.put(shop.getShopId(), itemEntity.getUUID());
                displayToShopMap.put(itemEntity.getUUID(), shop.getShopId());
                LOGGER.debug("[商店展示] 成功找回丢失的实体引用 - 商店ID: {}, 实体ID: {}", 
                        shop.getShopId(), itemEntity.getUUID());
                return 1;
            }
        }
        return 0;
    }

    public void removeAllDisplays(ServerLevel level) {
        for (UUID displayId : displayToShopMap.keySet()) {
            Entity entity = level.getEntity(displayId);
            if (entity != null) entity.discard();
        }
        shopToDisplayMap.clear();
        displayToShopMap.clear();
        missingTicksMap.clear();
    }

    public void initializeAllDisplays(ServerLevel level) {
        var shops = shopManager.getAllShops();
        shopToDisplayMap.clear();
        displayToShopMap.clear();
        missingTicksMap.clear();
        cleanupLegacyDisplays(level);
        for (ShopInstance shop : shops) {
            createDisplayForShop(shop, level);
        }
    }

    private void cleanupLegacyDisplays(ServerLevel level) {
        for (ShopInstance shop : shopManager.getAllShops()) {
            removeLegacyItemDisplayNearShop(shop, level);
        }
    }

    private int removeLegacyItemDisplayNearShop(ShopInstance shop, ServerLevel level) {
        Vec3 expectedPos = getExpectedDisplayPos(shop);
        AABB searchBox = new AABB(
                expectedPos.x - DISPLAY_SEARCH_RADIUS, expectedPos.y - DISPLAY_SEARCH_RADIUS, expectedPos.z - DISPLAY_SEARCH_RADIUS,
                expectedPos.x + DISPLAY_SEARCH_RADIUS, expectedPos.y + DISPLAY_SEARCH_RADIUS, expectedPos.z + DISPLAY_SEARCH_RADIUS
        );

        int removed = 0;
        // 清理 ItemDisplay
        for (Display.ItemDisplay displayEntity : level.getEntitiesOfClass(Display.ItemDisplay.class, searchBox)) {
            if (displayEntity.entityTags().contains(SHOP_DISPLAY_TAG)) {
                displayEntity.discard();
                removed++;
            }
        }
        // 清理 ItemEntity
        for (ItemEntity itemEntity : level.getEntitiesOfClass(ItemEntity.class, searchBox)) {
            if (itemEntity.entityTags().contains(SHOP_DISPLAY_TAG)) {
                itemEntity.discard();
                removed++;
            }
        }
        return removed;
    }

    private void removeDisplayReference(UUID shopId, UUID displayId) {
        if (shopId != null) shopToDisplayMap.remove(shopId);
        if (displayId != null) displayToShopMap.remove(displayId);
        missingTicksMap.remove(shopId);
    }

    private ItemStack toDisplayItem(ItemStack source) {
        ItemStack displayItem = source.copy();
        displayItem.setCount(1);
        return displayItem;
    }

    private Vec3 getExpectedDisplayPos(ShopInstance shop) {
        BlockPos shopPos = shop.getShopPos();
        return new Vec3(shopPos.getX() + DISPLAY_XZ_OFFSET, shopPos.getY() + DISPLAY_Y_OFFSET, shopPos.getZ() + DISPLAY_XZ_OFFSET);
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
