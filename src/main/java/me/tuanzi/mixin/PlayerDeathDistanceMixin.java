package me.tuanzi.mixin;

import me.tuanzi.statistics.StatisticsModule;
import me.tuanzi.statistics.data.PlayerStatistics;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.level.storage.LevelData;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ServerPlayer.class)
public abstract class PlayerDeathDistanceMixin {
    
    @Inject(method = "die", at = @At("HEAD"))
    private void onDie(DamageSource source, CallbackInfo ci) {
        StatisticsModule.LOGGER.debug("[PlayerDeathDistanceMixin] onDie called!");
        
        ServerPlayer serverPlayer = (ServerPlayer)(Object)this;
        String playerName = serverPlayer.getName().getString();
        PlayerStatistics stats = StatisticsModule.getInstance().getDataManager().getPlayerStatistics(playerName);
        
        BlockPos deathPos = serverPlayer.blockPosition();
        
        long lastRespawnTime = stats.getLastRespawnTime();
        StatisticsModule.LOGGER.debug("Player {} died, lastRespawnTime: {}", playerName, lastRespawnTime);
        
        stats.recordDeath();
        
        long shortestLife = stats.getShortestLifeSeconds();
        long longestLife = stats.getLongestLifeSeconds();
        StatisticsModule.LOGGER.debug("Player {} after recordDeath: shortestLife={}, longestLife={}", playerName, shortestLife, longestLife);
        
        ServerPlayer.RespawnConfig respawnConfig = serverPlayer.getRespawnConfig();
        if (respawnConfig != null) {
            LevelData.RespawnData respawnData = respawnConfig.respawnData();
            BlockPos respawnPos = respawnData.pos();
            if (respawnPos != null) {
                double distance = Math.sqrt(deathPos.distSqr(respawnPos));
                stats.updateFarthestDeathDistance(distance);
                StatisticsModule.LOGGER.debug("Player {} died {} blocks from respawn, farthest: {}", playerName, String.format("%.1f", distance), String.format("%.1f", stats.getFarthestDeathDistance()));
            }
        } else {
            double distance = Math.sqrt(deathPos.distSqr(new BlockPos(0, 64, 0)));
            stats.updateFarthestDeathDistance(distance);
            StatisticsModule.LOGGER.debug("Player {} died {} blocks from world spawn (no respawn point), farthest: {}", playerName, String.format("%.1f", distance), String.format("%.1f", stats.getFarthestDeathDistance()));
        }
    }
}
