package me.tuanzi.shop.events;

import me.tuanzi.shop.ShopModule;
import me.tuanzi.shop.shop.ShopInstance;
import me.tuanzi.shop.shop.ShopManager;
import me.tuanzi.shop.utils.ShopTranslationHelper;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.permissions.Permission;
import net.minecraft.server.permissions.PermissionLevel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Optional;

public class BlockProtectionHandler {
    private static final Logger LOGGER = LoggerFactory.getLogger(ShopModule.MOD_ID);
    private final ShopManager shopManager;

    public BlockProtectionHandler(ShopManager shopManager) {
        this.shopManager = shopManager;
    }

    public boolean canBreakBlock(ServerPlayer player, BlockPos pos) {
        Optional<ShopInstance> shopOpt = shopManager.getShopByPos(pos);
        if (shopOpt.isEmpty()) {
            return true;
        }

        ShopInstance shop = shopOpt.get();
        if (shop.isOwner(player.getUUID()) || player.permissions().hasPermission(new Permission.HasCommandLevel(PermissionLevel.GAMEMASTERS))) {
            LOGGER.info("[商店保护] 玩家 {} 破坏了商店方块 - 商店ID: {}, 位置: {}", 
                    player.getName().getString(), shop.getShopId(), pos);
            return true;
        }

        LOGGER.warn("[商店保护] 玩家 {} 尝试破坏商店方块被阻止 - 商店ID: {}, 位置: {}", 
                player.getName().getString(), shop.getShopId(), pos);
        player.sendSystemMessage(ShopTranslationHelper.translatable("shop.no_permission"));
        return false;
    }

    public boolean canOpenContainer(ServerPlayer player, BlockPos pos) {
        Optional<ShopInstance> shopOpt = shopManager.getShopByPos(pos);
        if (shopOpt.isEmpty()) {
            return true;
        }

        ShopInstance shop = shopOpt.get();
        if (shop.isOwner(player.getUUID()) || player.permissions().hasPermission(new Permission.HasCommandLevel(PermissionLevel.GAMEMASTERS))) {
            return true;
        }

        LOGGER.warn("[商店保护] 玩家 {} 尝试打开商店容器被阻止 - 商店ID: {}, 位置: {}", 
                player.getName().getString(), shop.getShopId(), pos);
        player.sendSystemMessage(ShopTranslationHelper.translatable("shop.protected"));
        return false;
    }

    public boolean canInteractWithBlock(ServerPlayer player, BlockPos pos) {
        Optional<ShopInstance> shopOpt = shopManager.getShopByPos(pos);
        if (shopOpt.isEmpty()) {
            return true;
        }

        ShopInstance shop = shopOpt.get();
        return shop.isOwner(player.getUUID()) || player.permissions().hasPermission(new Permission.HasCommandLevel(PermissionLevel.GAMEMASTERS));
    }

    public boolean isShopBlock(BlockPos pos) {
        return shopManager.getShopByPos(pos).isPresent();
    }

    public boolean shouldProtectFromExplosion(BlockPos pos) {
        return isShopBlock(pos);
    }

    public boolean shouldProtectFromPiston(BlockPos pos) {
        return isShopBlock(pos);
    }
}
