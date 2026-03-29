package com.example.mixin;

import com.example.statistics.StatisticsModule;
import com.example.statistics.data.PlayerStatistics;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(TamableAnimal.class)
public abstract class TamableAnimalMixin {
    
    @Inject(method = "die", at = @At("HEAD"))
    private void onPetDeath(net.minecraft.world.damagesource.DamageSource cause, CallbackInfo ci) {
        TamableAnimal pet = (TamableAnimal)(Object)this;
        if (pet.isTame()) {
            Entity owner = pet.getOwner();
            if (owner instanceof ServerPlayer player) {
                String playerName = player.getName().getString();
                String petType = pet.getType().getDescriptionId();
                
                PlayerStatistics stats = StatisticsModule.getInstance().getDataManager().getPlayerStatistics(playerName);
                stats.addPetDeath(petType);
                StatisticsModule.LOGGER.debug("Player {}'s pet {} died, total: {}", playerName, petType, stats.getPetDeaths());
            }
        }
    }
}
