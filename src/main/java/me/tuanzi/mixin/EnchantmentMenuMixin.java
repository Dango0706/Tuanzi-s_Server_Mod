package me.tuanzi.mixin;

import me.tuanzi.statistics.StatisticsModule;
import me.tuanzi.statistics.data.PlayerStatistics;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.EnchantmentMenu;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(EnchantmentMenu.class)
public abstract class EnchantmentMenuMixin {

    @Inject(method = "clickMenuButton", at = @At("RETURN"))
    private void onEnchant(Player player, int buttonId, CallbackInfoReturnable<Boolean> cir) {
        if (cir.getReturnValueZ() && player instanceof ServerPlayer) {
            String playerName = player.getName().getString();
            PlayerStatistics stats = StatisticsModule.getInstance().getDataManager().getPlayerStatistics(playerName);
            stats.addItemEnchanted();
            StatisticsModule.LOGGER.debug("Player {} enchanted item, total: {}", playerName, stats.getItemsEnchanted());
        }
    }
}
