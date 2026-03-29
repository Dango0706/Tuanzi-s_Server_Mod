package com.example.statistics.listeners;

import com.example.statistics.StatisticsModule;
import com.example.statistics.data.PlayerStatistics;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;

public class EntityDeathListener implements ServerLivingEntityEvents.AllowDeath {
    @Override
    public boolean allowDeath(LivingEntity entity, DamageSource damageSource, float damageAmount) {
        if (damageSource.getEntity() instanceof ServerPlayer killer) {
            String killerName = killer.getName().getString();
            String entityType = entity.getType().getDescriptionId();

            PlayerStatistics stats = StatisticsModule.getInstance().getDataManager().getPlayerStatistics(killerName);
            stats.addKill(entityType);
        }

        if (entity instanceof ServerPlayer player) {
            String playerName = player.getName().getString();
            String killerType = "unknown";

            if (damageSource.getEntity() != null) {
                killerType = damageSource.getEntity().getType().getDescriptionId();
            }

            PlayerStatistics stats = StatisticsModule.getInstance().getDataManager().getPlayerStatistics(playerName);
            stats.addDeath(killerType);
        }

        return true;
    }
}
