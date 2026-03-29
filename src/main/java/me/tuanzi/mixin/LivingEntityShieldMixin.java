package me.tuanzi.mixin;

import me.tuanzi.statistics.StatisticsModule;
import me.tuanzi.statistics.data.PlayerStatistics;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(LivingEntity.class)
public abstract class LivingEntityShieldMixin {
    
    @Inject(method = "applyItemBlocking", at = @At("RETURN"))
    private void onApplyItemBlocking(ServerLevel level, DamageSource source, float damage, 
                                      CallbackInfoReturnable<Float> cir) {
        float damageBlocked = cir.getReturnValueF();
        if (damageBlocked > 0.0F) {
            LivingEntity entity = (LivingEntity)(Object)this;
            if (entity instanceof ServerPlayer player) {
                String playerName = player.getName().getString();
                PlayerStatistics stats = StatisticsModule.getInstance().getDataManager().getPlayerStatistics(playerName);
                stats.addShieldBlock();
                StatisticsModule.LOGGER.debug("Player {} blocked damage with shield, total blocks: {}", 
                    playerName, stats.getShieldBlocks());
            }
        }
    }
}
