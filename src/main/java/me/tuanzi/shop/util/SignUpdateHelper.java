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
            LOGGER.warn("[SignUpdate] Cannot update sign: shop or level is null");
            return;
        }

        BlockPos signPos = shop.getSignPos();
        if (signPos == null) {
            LOGGER.warn("[SignUpdate] Cannot update sign: signPos is null, shopId={}", shop.getShopId());
            return;
        }

        if (!(level.getBlockEntity(signPos) instanceof SignBlockEntity signEntity)) {
            LOGGER.warn("[SignUpdate] Block entity at {} is not a sign, shopId={}", signPos, shop.getShopId());
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
                ? ShopTranslationHelper.getRawTranslation("shop.sign.pattern.sell_cn")
                : ShopTranslationHelper.getRawTranslation("shop.sign.pattern.buy_cn");
        String line1 = shop.getTradeItem().getDisplayName().getString();
        String line2 = String.format(Locale.ROOT, "%.2f %s", price, currencyName);

        var frontText = signEntity.getFrontText()
                .setMessage(0, Component.literal(line0))
                .setMessage(1, shop.getTradeItem().getDisplayName())
                .setMessage(2, Component.literal(line2))
                .setMessage(3, Component.literal(""));

        signEntity.setText(frontText, true);
        signEntity.setChanged();
        level.sendBlockUpdated(signPos, level.getBlockState(signPos), level.getBlockState(signPos), 3);

        LOGGER.info("[SignUpdate] Sign refreshed and synced. shopId={}, pos={}, line0={}, line1={}, line2={}",
                shop.getShopId().toString().substring(0, 8), signPos, line0, line1, line2);

        DevFlowLogger.status("Sign Sync",
                "Updated sign text and synced to clients"
                        + "\n  " + line0
                        + "\n  " + line1
                        + "\n  " + line2
                        + "\n  Pos: [" + signPos.getX() + ", " + signPos.getY() + ", " + signPos.getZ() + "]");
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

        LOGGER.debug("[SignUpdate] Immediate mode completed, shopId={}", shop.getShopId().toString().substring(0, 8));
    }
}
