package com.example.mixin;

import com.example.statistics.StatisticsModule;
import com.example.statistics.data.PlayerStatistics;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.FishingHook;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(FishingHook.class)
public abstract class FishingHookMixin {

    @Shadow
    public abstract Player getPlayerOwner();

    @Inject(method = "retrieve", at = @At("RETURN"))
    private void onRetrieveEnd(CallbackInfoReturnable<Integer> cir) {
        int result = cir.getReturnValueI();
        Player player = getPlayerOwner();
        if (player instanceof net.minecraft.server.level.ServerPlayer) {
            String playerName = player.getName().getString();
            PlayerStatistics stats = StatisticsModule.getInstance().getDataManager().getPlayerStatistics(playerName);
            stats.addFishingAttempt();
            if (result > 0) {
                stats.addFishingSuccess("item");
                StatisticsModule.LOGGER.debug("Player {} fishing success, caught: {}", playerName, result);
            } else {
                stats.addFishingFailure();
                StatisticsModule.LOGGER.debug("Player {} fishing failure", playerName);
            }
        }
    }
}
