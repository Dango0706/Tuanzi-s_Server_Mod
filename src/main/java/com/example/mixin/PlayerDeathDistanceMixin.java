package com.example.mixin;

import com.example.statistics.StatisticsModule;
import com.example.statistics.data.PlayerStatistics;
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
        System.out.println("[DEBUG] PlayerDeathDistanceMixin.onDie called!");
        
        ServerPlayer serverPlayer = (ServerPlayer)(Object)this;
        String playerName = serverPlayer.getName().getString();
        PlayerStatistics stats = StatisticsModule.getInstance().getDataManager().getPlayerStatistics(playerName);
        
        BlockPos deathPos = serverPlayer.blockPosition();
        
        long lastRespawnTime = stats.getLastRespawnTime();
        System.out.println("[DEBUG] Player " + playerName + " died, lastRespawnTime: " + lastRespawnTime);
        
        stats.recordDeath();
        
        long shortestLife = stats.getShortestLifeSeconds();
        long longestLife = stats.getLongestLifeSeconds();
        System.out.println("[DEBUG] Player " + playerName + " after recordDeath: shortestLife=" + shortestLife + ", longestLife=" + longestLife);
        
        ServerPlayer.RespawnConfig respawnConfig = serverPlayer.getRespawnConfig();
        if (respawnConfig != null) {
            LevelData.RespawnData respawnData = respawnConfig.respawnData();
            BlockPos respawnPos = respawnData.pos();
            if (respawnPos != null) {
                double distance = Math.sqrt(deathPos.distSqr(respawnPos));
                stats.updateFarthestDeathDistance(distance);
                System.out.println("[DEBUG] Player " + playerName + " died " + String.format("%.1f", distance) + " blocks from respawn, farthest: " + String.format("%.1f", stats.getFarthestDeathDistance()));
            }
        } else {
            double distance = Math.sqrt(deathPos.distSqr(new BlockPos(0, 64, 0)));
            stats.updateFarthestDeathDistance(distance);
            System.out.println("[DEBUG] Player " + playerName + " died " + String.format("%.1f", distance) + " blocks from world spawn (no respawn point), farthest: " + String.format("%.1f", stats.getFarthestDeathDistance()));
        }
    }
}
