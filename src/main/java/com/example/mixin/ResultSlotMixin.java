package com.example.mixin;

import com.example.statistics.StatisticsModule;
import com.example.statistics.data.PlayerStatistics;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ResultSlot;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ResultSlot.class)
public abstract class ResultSlotMixin {
    
    @Inject(method = "onTake", at = @At("HEAD"))
    private void onCraftItem(Player player, ItemStack stack, CallbackInfo ci) {
        if (player instanceof net.minecraft.server.level.ServerPlayer) {
            String playerName = player.getName().getString();
            PlayerStatistics stats = StatisticsModule.getInstance().getDataManager().getPlayerStatistics(playerName);
            
            String itemType = stack.getItem().getDescriptionId();
            stats.addItemCrafted(itemType);
            
            StatisticsModule.LOGGER.debug("Player {} crafted item: {}, total: {}", playerName, itemType, stats.getItemsCrafted());
        }
    }
}
