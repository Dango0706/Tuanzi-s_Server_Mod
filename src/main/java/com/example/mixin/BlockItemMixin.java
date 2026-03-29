package com.example.mixin;

import com.example.statistics.StatisticsModule;
import com.example.statistics.data.PlayerStatistics;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.level.block.Block;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(BlockItem.class)
public abstract class BlockItemMixin {
    
    @Inject(method = "place", at = @At("RETURN"))
    private void onBlockPlaced(BlockPlaceContext placeContext, CallbackInfoReturnable<InteractionResult> cir) {
        InteractionResult result = cir.getReturnValue();
        
        if (result == InteractionResult.SUCCESS && placeContext.getPlayer() instanceof ServerPlayer player) {
            Block block = ((BlockItem)(Object)this).getBlock();
            String playerName = player.getName().getString();
            String blockType = block.getName().getString();
            
            PlayerStatistics stats = StatisticsModule.getInstance().getDataManager().getPlayerStatistics(playerName);
            stats.addBlockPlaced(blockType);
            StatisticsModule.LOGGER.debug("Player {} placed block: {}, total: {}", playerName, blockType, stats.getBlocksPlaced());
        }
    }
}
