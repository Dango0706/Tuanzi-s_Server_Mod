package me.tuanzi.shop.display;

import me.tuanzi.shop.shop.ShopInstance;
import me.tuanzi.shop.shop.ShopManager;
import me.tuanzi.shop.util.DevFlowLogger;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class ShopDisplayManager {
    private static final Logger LOGGER = LoggerFactory.getLogger("ShopModule/Display");
    private final ShopManager shopManager;
    private final Map<UUID, UUID> shopToDisplayMap = new ConcurrentHashMap<>();
    private final Map<UUID, UUID> displayToShopMap = new ConcurrentHashMap<>();

    public ShopDisplayManager(ShopManager shopManager) {
        this.shopManager = shopManager;
    }

    public void createDisplayForShop(ShopInstance shop, ServerLevel level) {
        DevFlowLogger.startFlow("悬浮物品创建");
        DevFlowLogger.param("悬浮物品创建", "shopId", shop.getShopId());
        DevFlowLogger.param("悬浮物品创建", "shopPos", shop.getShopPos());
        DevFlowLogger.param("悬浮物品创建", "tradeItem", shop.getTradeItem().getDisplayName().getString());

        if (shop == null || level == null) {
            DevFlowLogger.error("悬浮物品创建", "shop 或 level 为 null");
            LOGGER.warn("[Display] createDisplayForShop 被调用但 shop 或 level 为 null");
            return;
        }

        BlockPos shopPos = shop.getShopPos();
        LOGGER.info("[Display] 正在为商店创建悬浮物品 - ShopId: {}, 位置: [{}, {}, {}], 物品: {}", 
                shop.getShopId(), shopPos.getX(), shopPos.getY(), shopPos.getZ(), 
                shop.getTradeItem().getDisplayName().getString());
        
        DevFlowLogger.step("悬浮物品创建", "准备物品实体数据");
        ItemStack displayItem = shop.getTradeItem().copy();
        displayItem.setCount(1);

        DevFlowLogger.step("悬浮物品创建", "创建ItemEntity实体");
        ItemEntity displayEntity = new ItemEntity(EntityType.ITEM, level);
        displayEntity.setItem(displayItem);
        
        Vec3 displayPos = new Vec3(
                shopPos.getX() + 0.5,
                shopPos.getY() + 1.6,
                shopPos.getZ() + 0.5
        );
        displayEntity.setPos(displayPos);
        displayEntity.setPickUpDelay(Integer.MAX_VALUE);
        displayEntity.setUnlimitedLifetime();
        displayEntity.setNoGravity(true);
        displayEntity.setInvulnerable(true);
        displayEntity.setSilent(true);
        displayEntity.setCustomName(displayItem.getDisplayName());
        displayEntity.setCustomNameVisible(true);

        DevFlowLogger.param("悬浮物品创建", "displayPos", displayPos);

        DevFlowLogger.step("悬浮物品创建", "将实体添加到世界");
        if (level.addFreshEntity(displayEntity)) {
            shopToDisplayMap.put(shop.getShopId(), displayEntity.getUUID());
            displayToShopMap.put(displayEntity.getUUID(), shop.getShopId());
            
            LOGGER.info("[Display] 悬浮物品创建成功 - DisplayId: {}, ShopId: {}", 
                    displayEntity.getUUID(), shop.getShopId());
            
            DevFlowLogger.status("悬浮物品创建", "实体添加成功");
            DevFlowLogger.param("悬浮物品创建", "displayEntityId", displayEntity.getUUID());
            DevFlowLogger.endFlow("悬浮物品创建", true, "悬浮物品创建成功");
        } else {
            LOGGER.error("[Display] 悬浮物品创建失败 - 无法添加实体到世界, ShopId: {}", shop.getShopId());
            DevFlowLogger.error("悬浮物品创建", "无法添加实体到世界");
            DevFlowLogger.endFlow("悬浮物品创建", false, "实体添加失败");
        }
    }

    public void removeDisplayForShop(UUID shopId, ServerLevel level) {
        LOGGER.info("[Display] 正在移除商店的悬浮物品 - ShopId: {}", shopId);
        UUID displayId = shopToDisplayMap.remove(shopId);
        if (displayId != null) {
            displayToShopMap.remove(displayId);
            
            Entity entity = level.getEntity(displayId);
            if (entity != null) {
                entity.discard();
                LOGGER.info("[Display] 悬浮物品已移除 - DisplayId: {}, ShopId: {}", displayId, shopId);
            } else {
                LOGGER.warn("[Display] 未找到悬浮物品实体 - DisplayId: {}, ShopId: {}", displayId, shopId);
            }
        } else {
            LOGGER.warn("[Display] 商店没有关联的悬浮物品 - ShopId: {}", shopId);
        }
    }

    public void updateDisplayItem(UUID shopId, ItemStack newItem, ServerLevel level) {
        LOGGER.info("[Display] 正在更新商店悬浮物品 - ShopId: {}, 新物品: {}", shopId, newItem.getDisplayName().getString());
        UUID displayId = shopToDisplayMap.get(shopId);
        if (displayId != null) {
            Entity entity = level.getEntity(displayId);
            if (entity instanceof ItemEntity itemEntity) {
                ItemStack displayItem = newItem.copy();
                displayItem.setCount(1);
                itemEntity.setItem(displayItem);
                itemEntity.setCustomName(displayItem.getDisplayName());
                LOGGER.info("[Display] 悬浮物品已更新 - DisplayId: {}, ShopId: {}, 新物品: {}", 
                        displayId, shopId, newItem.getDisplayName().getString());
            } else {
                LOGGER.warn("[Display] 找到 DisplayId 但实体不是 ItemEntity 或不存在 - DisplayId: {}", displayId);
            }
        } else {
            LOGGER.warn("[Display] 商店没有关联的悬浮物品，尝试重新创建 - ShopId: {}", shopId);
            Optional<ShopInstance> shopOpt = shopManager.getShopById(shopId);
            if (shopOpt.isPresent()) {
                createDisplayForShop(shopOpt.get(), level);
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
        LOGGER.debug("Initializing displays for {} shops", shops.size());
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
}
