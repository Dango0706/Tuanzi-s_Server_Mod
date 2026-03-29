package me.tuanzi.mixin;

import me.tuanzi.statistics.StatisticsModule;
import me.tuanzi.statistics.data.PlayerStatistics;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.players.PlayerList;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(PlayerList.class)
public abstract class PlayerListRespawnMixin {

    @Inject(method = "respawn", at = @At("RETURN"))
    private void onRespawn(ServerPlayer serverPlayer, boolean keepAllPlayerData, 
                           net.minecraft.world.entity.Entity.RemovalReason removalReason,
                           CallbackInfoReturnable<ServerPlayer> cir) {
        ServerPlayer respawnedPlayer = cir.getReturnValue();
        if (respawnedPlayer != null) {
            String playerName = respawnedPlayer.getName().getString();
            PlayerStatistics stats = StatisticsModule.getInstance().getDataManager().getPlayerStatistics(playerName);

            stats.setLastRespawnTime(System.currentTimeMillis() / 1000);
            StatisticsModule.LOGGER.debug("Player {} respawned, reset life timer", playerName);
        }
    }
}
