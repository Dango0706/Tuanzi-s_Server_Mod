package me.tuanzi.shop.util;

import me.tuanzi.shop.pricing.DynamicPricing;
import me.tuanzi.shop.shop.ShopInstance;
import me.tuanzi.shop.shop.ShopManager;
import me.tuanzi.shop.shop.ShopType;
import me.tuanzi.shop.utils.ShopTranslationHelper;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.entity.SignBlockEntity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Locale;

public class SignUpdateHelper {
    private static final Logger LOGGER = LoggerFactory.getLogger("ShopModule/SignUpdate");

    public static void updateSignForShop(ShopInstance shop, ServerLevel level) {
        if (shop == null || level == null) {
            LOGGER.warn("[告示牌更新] 无法更新告示牌：商店或世界为空");
            return;
        }

        BlockPos signPos = shop.getSignPos();
        if (signPos == null) {
            LOGGER.warn("[告示牌更新] 无法更新告示牌：告示牌坐标为空，商店ID={}", shop.getShopId());
            return;
        }

        if (!(level.getBlockEntity(signPos) instanceof SignBlockEntity signEntity)) {
            LOGGER.warn("[告示牌更新] 目标方块实体不是告示牌：位置={}, 商店ID={}", signPos, shop.getShopId());
            return;
        }

        boolean isSellShop = shop.getShopType() == ShopType.SELL;
        String currencyName = "Unknown";
        double price = shop.getCurrentPrice();

        MinecraftServer server = level.getServer();
        if (server != null) {
            ShopManager shopManager = ShopManager.getInstance(server);
            if (shopManager != null) {
                currencyName = shopManager.getCurrencyDisplayName(shop.getWalletTypeId());
                price = DynamicPricing.calculatePrice(shop);
            }
        }

        String line0 = isSellShop
                ? ShopTranslationHelper.getRawTranslation("shop.sign.pattern.sell")
                : ShopTranslationHelper.getRawTranslation("shop.sign.pattern.buy");

        if (shop.isInfinite()) {
            line0 = "§c" + line0;
        }
        
        // 获取不带括号的显示名称
        Component tradeItemDisplayName = shop.getTradeItem().getDisplayName();
        String line2 = String.format(Locale.ROOT, "%.2f %s", price, currencyName);

        var frontText = signEntity.getFrontText()
                .setMessage(0, Component.literal(line0))
                .setMessage(1, tradeItemDisplayName)
                .setMessage(2, Component.literal(line2))
                .setMessage(3, Component.literal(shop.getDescription() != null ? shop.getDescription() : ""));

        signEntity.setText(frontText, true);
        signEntity.setChanged();
        level.sendBlockUpdated(signPos, level.getBlockState(signPos), level.getBlockState(signPos), 3);

        LOGGER.info("[告示牌更新] 刷新并同步成功：商店ID={}, 位置={}, 第1行={}, 第2行={}, 第3行={}",
                shop.getShopId().toString().substring(0, 8), signPos, line0, tradeItemDisplayName.getString(), line2);

        DevFlowLogger.status("告示牌同步",
                "已更新告示牌文本并同步给客户端"
                        + "\n  " + line0
                        + "\n  " + tradeItemDisplayName.getString()
                        + "\n  " + line2
                        + "\n  坐标: [" + signPos.getX() + ", " + signPos.getY() + ", " + signPos.getZ() + "]");
    }

    public static void updateSignForShopImmediate(ShopInstance shop, ServerLevel level) {
        if (shop == null || level == null) {
            return;
        }

        BlockPos signPos = shop.getSignPos();
        if (signPos == null || !(level.getBlockEntity(signPos) instanceof SignBlockEntity)) {
            return;
        }

        updateSignForShop(shop, level);

        LOGGER.debug("[告示牌更新] 立即刷新完成，商店ID={}", shop.getShopId().toString().substring(0, 8));
    }
}
