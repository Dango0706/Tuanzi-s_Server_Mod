package me.tuanzi.title.listeners;

import me.tuanzi.statistics.StatisticsModule;
import me.tuanzi.statistics.data.PlayerStatistics;
import me.tuanzi.title.PlayerTitleData;
import me.tuanzi.title.TitleManager;
import net.fabricmc.fabric.api.event.player.UseItemCallback;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.CustomData;

import java.util.List;

public class TitleListener {
    public static void register() {
        // Handle Join
        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> {
            ServerPlayer player = handler.getPlayer();
            TitleManager.getInstance().updatePlayerScoreboard(player);
            
            // Check pending notifications
            PlayerTitleData data = TitleManager.getInstance().getTitleData().getOrCreatePlayerTitleData(player.getUUID());
            List<String> pending = data.getPendingNotifications();
            if (!pending.isEmpty()) {
                for (String msg : pending) {
                    player.sendSystemMessage(Component.literal(msg));
                }
                pending.clear();
            }
        });

        // Handle Item Use
        UseItemCallback.EVENT.register((player, world, hand) -> {
            if (world.isClientSide()) return InteractionResult.PASS;

            ItemStack stack = player.getItemInHand(hand);
            if (stack.is(Items.NAME_TAG)) {
                CustomData customData = stack.get(DataComponents.CUSTOM_DATA);
                if (customData != null) {
                    CompoundTag tag = customData.copyTag();
                    if (tag.contains("tuanzi_title")) {
                        String titleId = tag.getString("tuanzi_title").orElse("");
                        
                        if (titleId.isEmpty()) return InteractionResult.PASS;

                        PlayerTitleData data = TitleManager.getInstance().getTitleData().getOrCreatePlayerTitleData(player.getUUID());
                        if (data.getUnlockedTitles().contains(titleId)) {
                            player.sendSystemMessage(Component.literal("§c你已经拥有这个称号了。"));
                            return InteractionResult.FAIL;
                        }

                        if (!TitleManager.getInstance().getTitleData().getTitles().containsKey(titleId)) {
                            player.sendSystemMessage(Component.literal("§c该称号已失效。"));
                            return InteractionResult.FAIL;
                        }

                        // Success
                        data.unlockTitle(titleId);
                        stack.shrink(1);
                        
                        String displayName = TitleManager.getInstance().getTitleData().getTitles().get(titleId).displayName().getString();
                        player.sendSystemMessage(Component.literal("§b成功激活称号: §f" + displayName));
                        
                        // Stats
                        PlayerStatistics stats = StatisticsModule.getInstance().getDataManager().getPlayerStatistics(player.getName().getString());
                        stats.addTitleOwned();
                        stats.addTitleFromItem();
                        
                        return InteractionResult.SUCCESS;
                    }
                }
            }
            return InteractionResult.PASS;
        });
    }
}
