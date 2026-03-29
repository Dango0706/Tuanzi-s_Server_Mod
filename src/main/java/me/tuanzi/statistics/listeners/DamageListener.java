package me.tuanzi.statistics.listeners;

import me.tuanzi.statistics.StatisticsModule;
import me.tuanzi.statistics.data.PlayerStatistics;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;

public class DamageListener implements ServerLivingEntityEvents.AfterDamage {
    @Override
    public void afterDamage(LivingEntity entity, DamageSource damageSource, float damageAmount, float actualDamageAmount, boolean blocked) {
        long damage = (long) actualDamageAmount;

        if (damageSource.getEntity() instanceof ServerPlayer attacker) {
            String attackerName = attacker.getName().getString();
            String entityType = entity.getType().getDescriptionId();

            PlayerStatistics stats = StatisticsModule.getInstance().getDataManager().getPlayerStatistics(attackerName);
            stats.addDamageDealt(entityType, damage);
            
            if (damage > 0) {
                stats.updateHighestDamageDealt(damage);
            }
        }

        if (entity instanceof ServerPlayer player) {
            String playerName = player.getName().getString();
            String attackerType = "unknown";

            if (damageSource.getEntity() != null) {
                attackerType = damageSource.getEntity().getType().getDescriptionId();
            }

            PlayerStatistics stats = StatisticsModule.getInstance().getDataManager().getPlayerStatistics(playerName);
            stats.addDamageTaken(attackerType, damage);
            
            if (damage > 0) {
                stats.updateHighestDamageTaken(damage);
            }
        }
    }
}
