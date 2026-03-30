package me.tuanzi.auth.login.listener;

import me.tuanzi.auth.login.LoginStateManager;
import me.tuanzi.auth.utils.TranslationHelper;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class PlayerRestrictionListener {

    private static final Map<UUID, Vec3> lastPositions = new ConcurrentHashMap<>();
    private static final Map<UUID, Long> restrictionMessages = new ConcurrentHashMap<>();
    private static final long MESSAGE_COOLDOWN = 3000;

    public static void register() {
        net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents.END_SERVER_TICK.register(PlayerRestrictionListener::onServerTick);
        net.fabricmc.fabric.api.event.player.AttackBlockCallback.EVENT.register(PlayerRestrictionListener::onAttackBlock);
        net.fabricmc.fabric.api.event.player.UseBlockCallback.EVENT.register(PlayerRestrictionListener::onUseBlock);
        net.fabricmc.fabric.api.event.player.UseItemCallback.EVENT.register(PlayerRestrictionListener::onUseItem);
        net.fabricmc.fabric.api.message.v1.ServerMessageEvents.ALLOW_CHAT_MESSAGE.register(PlayerRestrictionListener::onChatMessage);
    }

    private static void onServerTick(MinecraftServer server) {
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            if (!LoginStateManager.getInstance().isLoggedIn(player.getUUID())) {
                checkAndRestrictMovement(player);
            }
        }
    }

    private static void checkAndRestrictMovement(ServerPlayer player) {
        ServerGamePacketListenerImpl connection = player.connection;
        if (connection == null) return;

        Vec3 currentPos = player.position();
        Vec3 lastPos = lastPositions.get(player.getUUID());

        if (lastPos == null) {
            lastPositions.put(player.getUUID(), currentPos);
            return;
        }

        double distance = currentPos.distanceTo(lastPos);
        if (distance > 0.5) {
            player.teleportTo(lastPos.x, lastPos.y, lastPos.z);
            sendMessageWithCooldown(player, "auth.restriction.no_move");
        }
    }

    private static InteractionResult onAttackBlock(net.minecraft.world.entity.player.Player player, net.minecraft.world.level.Level world, InteractionHand hand, BlockPos pos, Direction direction) {
        if (player instanceof ServerPlayer serverPlayer && !LoginStateManager.getInstance().isLoggedIn(serverPlayer.getUUID())) {
            sendMessageWithCooldown(serverPlayer, "auth.restriction.no_action");
            return InteractionResult.FAIL;
        }
        return InteractionResult.PASS;
    }

    private static InteractionResult onUseBlock(net.minecraft.world.entity.player.Player player, net.minecraft.world.level.Level world, InteractionHand hand, BlockHitResult hitResult) {
        if (player instanceof ServerPlayer serverPlayer && !LoginStateManager.getInstance().isLoggedIn(serverPlayer.getUUID())) {
            sendMessageWithCooldown(serverPlayer, "auth.restriction.no_action");
            return InteractionResult.FAIL;
        }
        return InteractionResult.PASS;
    }

    private static InteractionResult onUseItem(net.minecraft.world.entity.player.Player player, net.minecraft.world.level.Level world, InteractionHand hand) {
        if (player instanceof ServerPlayer serverPlayer && !LoginStateManager.getInstance().isLoggedIn(serverPlayer.getUUID())) {
            ItemStack stack = player.getItemInHand(hand);
            if (!stack.isEmpty()) {
                sendMessageWithCooldown(serverPlayer, "auth.restriction.no_item");
                return InteractionResult.FAIL;
            }
        }
        return InteractionResult.PASS;
    }

    private static boolean onChatMessage(net.minecraft.network.chat.PlayerChatMessage message, net.minecraft.server.level.ServerPlayer sender, net.minecraft.network.chat.ChatType.Bound boundChatType) {
        if (!LoginStateManager.getInstance().isLoggedIn(sender.getUUID())) {
            sendMessageWithCooldown(sender, "auth.restriction.no_chat");
            return false;
        }
        return true;
    }

    private static void sendMessageWithCooldown(ServerPlayer player, String key) {
        long currentTime = System.currentTimeMillis();
        Long lastMessageTime = restrictionMessages.get(player.getUUID());

        if (lastMessageTime == null || currentTime - lastMessageTime > MESSAGE_COOLDOWN) {
            TranslationHelper.sendMessage(player, key);
            restrictionMessages.put(player.getUUID(), currentTime);
        }
    }

    public static void clearPlayer(UUID playerId) {
        lastPositions.remove(playerId);
        restrictionMessages.remove(playerId);
    }
}
