package me.tuanzi.mixin;

import me.tuanzi.statistics.StatisticsModule;
import me.tuanzi.statistics.data.PlayerStatistics;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.EntityHitResult;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Projectile.class)
public abstract class ProjectileHitMixin {
    
    @Inject(method = "onHitEntity", at = @At("RETURN"))
    private void onProjectileHit(EntityHitResult entityHitResult, CallbackInfo ci) {
        Projectile projectile = (Projectile)(Object)this;
        Entity owner = projectile.getOwner();
        
        if (owner instanceof ServerPlayer player && entityHitResult.getEntity() instanceof LivingEntity) {
            ItemStack weapon = player.getMainHandItem();
            String playerName = player.getName().getString();
            PlayerStatistics stats = StatisticsModule.getInstance().getDataManager().getPlayerStatistics(playerName);
            
            if (weapon.getItem() == Items.BOW) {
                stats.addBowHit();
                StatisticsModule.LOGGER.debug("Player {} hit with bow, total: {}", playerName, stats.getBowHits());
            } else if (weapon.getItem() == Items.CROSSBOW) {
                stats.addCrossbowHit();
                StatisticsModule.LOGGER.debug("Player {} hit with crossbow, total: {}", playerName, stats.getCrossbowHits());
            }
        }
    }
}
