package com.example.statistics.listeners;

import com.example.statistics.StatisticsModule;
import com.example.statistics.data.PlayerStatistics;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.server.level.ServerPlayer;

public class DamageListener implements ServerLivingEntityEvents.AfterDamage {
    @Override
    public void afterDamage(LivingEntity entity, DamageSource damageSource, float damageAmount, float actualDamageAmount, boolean blocked) {
        long damage = (long) actualDamageAmount;
        
        if (damageSource.getEntity() instanceof ServerPlayer) {
            ServerPlayer attacker = (ServerPlayer) damageSource.getEntity();
            String attackerName = attacker.getName().getString();
            String entityType = entity.getType().getDescriptionId();
            
            PlayerStatistics stats = StatisticsModule.getInstance().getDataManager().getPlayerStatistics(attackerName);
            stats.addDamageDealt(entityType, damage);
        }
        
        if (entity instanceof ServerPlayer) {
            ServerPlayer player = (ServerPlayer) entity;
            String playerName = player.getName().getString();
            String attackerType = "unknown";
            
            if (damageSource.getEntity() != null) {
                attackerType = damageSource.getEntity().getType().getDescriptionId();
            }
            
            PlayerStatistics stats = StatisticsModule.getInstance().getDataManager().getPlayerStatistics(playerName);
            stats.addDamageTaken(attackerType, damage);
        }
    }
}
