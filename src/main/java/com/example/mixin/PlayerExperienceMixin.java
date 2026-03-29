package com.example.mixin;

import com.example.statistics.StatisticsModule;
import com.example.statistics.data.PlayerStatistics;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Player.class)
public abstract class PlayerExperienceMixin {
    
    @Inject(method = "giveExperiencePoints", at = @At("HEAD"))
    private void onGiveExperiencePoints(int amount, CallbackInfo ci) {
        Player player = (Player)(Object)this;
        if (player instanceof ServerPlayer serverPlayer && amount > 0) {
            String playerName = serverPlayer.getName().getString();
            PlayerStatistics stats = StatisticsModule.getInstance().getDataManager().getPlayerStatistics(playerName);
            stats.addExperienceGained(amount);
            StatisticsModule.LOGGER.debug("Player {} gained {} experience, total: {}", playerName, amount, stats.getTotalExperienceGained());
        }
    }
}
