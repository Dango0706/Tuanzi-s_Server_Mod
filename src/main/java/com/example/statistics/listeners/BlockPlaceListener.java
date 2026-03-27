package com.example.statistics.listeners;

import com.example.statistics.StatisticsModule;
import com.example.statistics.data.PlayerStatistics;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.level.Level;

public class BlockPlaceListener implements UseBlockCallback {
    @Override
    public InteractionResult interact(Player player, Level level, InteractionHand hand, BlockHitResult hitResult) {
        ItemStack stack = player.getItemInHand(hand);
        
        if (stack.getItem() instanceof BlockItem) {
            BlockItem blockItem = (BlockItem) stack.getItem();
            Block block = blockItem.getBlock();
            String playerName = player.getName().getString();
            String blockType = block.getName().getString();
            
            PlayerStatistics stats = StatisticsModule.getInstance().getDataManager().getPlayerStatistics(playerName);
            stats.addBlockPlaced(blockType);
        }
        
        return InteractionResult.PASS;
    }
}
