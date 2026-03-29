package me.tuanzi.mixin;

import me.tuanzi.statistics.StatisticsModule;
import me.tuanzi.statistics.data.PlayerStatistics;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LivingEntity.class)
public abstract class LivingEntityJumpMixin {
    
    @Inject(method = "jumpFromGround", at = @At("HEAD"))
    private void onJump(CallbackInfo ci) {
        LivingEntity entity = (LivingEntity)(Object)this;
        if (entity instanceof ServerPlayer player) {
            String playerName = player.getName().getString();
            PlayerStatistics stats = StatisticsModule.getInstance().getDataManager().getPlayerStatistics(playerName);
            stats.addJump();
            StatisticsModule.LOGGER.debug("Player {} jumped, total: {}", playerName, stats.getTotalJumps());
        }
    }
}
