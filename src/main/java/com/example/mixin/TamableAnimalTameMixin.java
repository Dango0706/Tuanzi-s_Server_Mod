package com.example.mixin;

import com.example.statistics.StatisticsModule;
import com.example.statistics.data.PlayerStatistics;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(TamableAnimal.class)
public abstract class TamableAnimalTameMixin {
    
    @Inject(method = "tame", at = @At("HEAD"))
    private void onTame(Player player, CallbackInfo ci) {
        if (player instanceof ServerPlayer serverPlayer) {
            TamableAnimal animal = (TamableAnimal)(Object)this;
            String playerName = serverPlayer.getName().getString();
            String animalType = animal.getType().getDescriptionId();
            
            PlayerStatistics stats = StatisticsModule.getInstance().getDataManager().getPlayerStatistics(playerName);
            stats.addAnimalTamed(animalType);
            StatisticsModule.LOGGER.debug("Player {} tamed {}, total: {}", 
                playerName, animalType, stats.getAnimalsTamed());
        }
    }
}
