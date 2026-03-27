package com.example.mixin;

import com.example.statistics.StatisticsModule;
import com.example.statistics.data.PlayerStatistics;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.MerchantResultSlot;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(MerchantResultSlot.class)
public abstract class MerchantResultSlotMixin {
    
    @Inject(method = "onTake", at = @At("HEAD"))
    private void onTrade(Player player, ItemStack carried, CallbackInfo ci) {
        if (player instanceof ServerPlayer) {
            String playerName = player.getName().getString();
            PlayerStatistics stats = StatisticsModule.getInstance().getDataManager().getPlayerStatistics(playerName);
            stats.addVillagerTrade();
            StatisticsModule.LOGGER.debug("Player {} traded with villager, total: {}", playerName, stats.getVillagerTrades());
        }
    }
}
