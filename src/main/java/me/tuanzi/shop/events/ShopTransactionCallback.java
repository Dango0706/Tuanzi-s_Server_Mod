package me.tuanzi.shop.events;

import me.tuanzi.shop.shop.ShopInstance;
import net.fabricmc.fabric.api.event.Event;
import net.fabricmc.fabric.api.event.EventFactory;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;

public interface ShopTransactionCallback {
    Event<ShopTransactionCallback> EVENT = EventFactory.createArrayBacked(ShopTransactionCallback.class,
            (listeners) -> (player, shop, item, quantity, totalPrice, isBuy) -> {
                for (ShopTransactionCallback listener : listeners) {
                    listener.onTransaction(player, shop, item, quantity, totalPrice, isBuy);
                }
            });

    /**
     * @param isBuy true 为玩家从商店买入, false 为玩家向商店卖出
     */
    void onTransaction(ServerPlayer player, ShopInstance shop, ItemStack item, int quantity, double totalPrice, boolean isBuy);
}
