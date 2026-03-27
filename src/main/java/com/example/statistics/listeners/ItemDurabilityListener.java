package com.example.statistics.listeners;

import com.example.statistics.StatisticsModule;
import com.example.statistics.data.PlayerStatistics;
import net.fabricmc.fabric.api.event.player.UseItemCallback;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.item.ItemStack;
import net.minecraft.resources.Identifier;
import net.minecraft.core.registries.BuiltInRegistries;

public class ItemDurabilityListener implements UseItemCallback {
    @Override
    public InteractionResult interact(Player player, Level level, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        
        if (stack.isDamageableItem()) {
            int maxDamage = stack.getMaxDamage();
            int currentDamage = stack.getDamageValue();
            
            if (maxDamage > 0) {
                Identifier itemId = BuiltInRegistries.ITEM.getKey(stack.getItem());
                String itemName = itemId.toString();
                String playerName = player.getName().getString();
                
                PlayerStatistics stats = StatisticsModule.getInstance().getDataManager().getPlayerStatistics(playerName);
                stats.addDurabilityUsed(itemName, 1);
            }
        }
        
        return InteractionResult.PASS;
    }
}
