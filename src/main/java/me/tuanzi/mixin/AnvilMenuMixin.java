package me.tuanzi.mixin;

import me.tuanzi.statistics.StatisticsModule;
import me.tuanzi.statistics.data.PlayerStatistics;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AnvilMenu;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(AnvilMenu.class)
public abstract class AnvilMenuMixin {

    @Inject(method = "onTake", at = @At("HEAD"))
    private void onAnvilUse(Player player, ItemStack stack, CallbackInfo ci) {
        if (player instanceof net.minecraft.server.level.ServerPlayer) {
            String playerName = player.getName().getString();
            PlayerStatistics stats = StatisticsModule.getInstance().getDataManager().getPlayerStatistics(playerName);
            stats.addAnvilUse();

            StatisticsModule.LOGGER.debug("Player {} used anvil, total: {}", playerName, stats.getAnvilUses());
        }
    }
}
