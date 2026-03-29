package com.example.mixin;

import com.example.statistics.StatisticsModule;
import com.example.statistics.data.PlayerStatistics;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Player.class)
public abstract class PlayerExperienceConsumeMixin {
    
    @Inject(method = "onEnchantmentPerformed", at = @At("HEAD"))
    private void onEnchantmentPerformed(ItemStack itemStack, int enchantmentCost, CallbackInfo ci) {
        Player player = (Player)(Object)this;
        if (player instanceof ServerPlayer serverPlayer && enchantmentCost > 0) {
            String playerName = serverPlayer.getName().getString();
            
            int xpConsumed = calculateXpForLevel(enchantmentCost);
            
            PlayerStatistics stats = StatisticsModule.getInstance().getDataManager().getPlayerStatistics(playerName);
            stats.addExperienceConsumed(xpConsumed);
            StatisticsModule.LOGGER.debug("Player {} consumed {} XP for enchantment, total consumed: {}", 
                playerName, xpConsumed, stats.getTotalExperienceConsumed());
        }
    }
    
    private int calculateXpForLevel(int levels) {
        int totalXp = 0;
        for (int i = 0; i < levels; i++) {
            totalXp += getXpNeededForLevel(i);
        }
        return totalXp;
    }
    
    private int getXpNeededForLevel(int level) {
        if (level >= 30) {
            return 112 + (level - 30) * 9;
        } else if (level >= 15) {
            return 37 + (level - 15) * 5;
        } else {
            return 7 + level * 2;
        }
    }
}
