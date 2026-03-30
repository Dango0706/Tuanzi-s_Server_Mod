package me.tuanzi.auth.login.listener;

import me.tuanzi.auth.AuthModule;
import me.tuanzi.auth.login.LoginStateManager;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.event.player.AttackBlockCallback;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.fabricmc.fabric.api.event.player.UseItemCallback;
import net.fabricmc.fabric.api.message.v1.ServerMessageEvents;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class PlayerRestrictionListener {
    
    private static final Map<UUID, Vec3> lastPositions = new ConcurrentHashMap<>();
    private static final Map<UUID, Boolean> restrictionMessages = new ConcurrentHashMap<>();
    
    public static void register() {
        ServerTickEvents.END_SERVER_TICK.register(PlayerRestrictionListener::onServerTick);
        
        AttackBlockCallback.EVENT.register(PlayerRestrictionListener::onAttackBlock);
        
        UseBlockCallback.EVENT.register(PlayerRestrictionListener::onUseBlock);
        
        UseItemCallback.EVENT.register(PlayerRestrictionListener::onUseItem);
        
        ServerMessageEvents.ALLOW_CHAT_MESSAGE.register(PlayerRestrictionListener::onChatMessage);
        
        AuthModule.LOGGER.info("[AuthModule] 玩家行为限制监听器已注册");
    }
    
    private static void onServerTick(MinecraftServer server) {
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            UUID playerId = player.getUUID();
            
            if (isLoggedIn(playerId)) {
                lastPositions.remove(playerId);
                restrictionMessages.remove(playerId);
                continue;
            }
            
            Vec3 currentPos = player.position();
            Vec3 lastPos = lastPositions.get(playerId);
            
            if (lastPos == null) {
                lastPositions.put(playerId, currentPos);
                continue;
            }
            
            double distance = currentPos.distanceTo(lastPos);
            
            if (distance > 0.5) {
                player.teleportTo(lastPos.x, lastPos.y, lastPos.z);
                
                if (!restrictionMessages.getOrDefault(playerId, false)) {
                    player.sendSystemMessage(Component.literal("§c请先登录后再移动！"));
                    restrictionMessages.put(playerId, true);
                }
            }
        }
    }
    
    private static InteractionResult onAttackBlock(Player player, Level level, InteractionHand hand, BlockPos pos, Direction direction) {
        if (player instanceof ServerPlayer serverPlayer) {
            if (!isLoggedIn(serverPlayer.getUUID())) {
                serverPlayer.sendSystemMessage(Component.literal("§c请先登录后再进行操作！"));
                return InteractionResult.FAIL;
            }
        }
        return InteractionResult.PASS;
    }
    
    private static InteractionResult onUseBlock(Player player, Level level, InteractionHand hand, BlockHitResult hitResult) {
        if (player instanceof ServerPlayer serverPlayer) {
            if (!isLoggedIn(serverPlayer.getUUID())) {
                serverPlayer.sendSystemMessage(Component.literal("§c请先登录后再进行操作！"));
                return InteractionResult.FAIL;
            }
        }
        return InteractionResult.PASS;
    }
    
    private static InteractionResult onUseItem(Player player, Level level, InteractionHand hand) {
        if (player instanceof ServerPlayer serverPlayer) {
            if (!isLoggedIn(serverPlayer.getUUID())) {
                serverPlayer.sendSystemMessage(Component.literal("§c请先登录后再使用物品！"));
                return InteractionResult.FAIL;
            }
        }
        return InteractionResult.PASS;
    }
    
    private static boolean onChatMessage(net.minecraft.network.chat.PlayerChatMessage message, net.minecraft.server.level.ServerPlayer sender, net.minecraft.network.chat.ChatType.Bound boundChatType) {
        if (!isLoggedIn(sender.getUUID())) {
            sender.sendSystemMessage(Component.literal("§c请先登录后再发送消息！"));
            return false;
        }
        return true;
    }
    
    public static void clearPlayer(UUID playerId) {
        lastPositions.remove(playerId);
        restrictionMessages.remove(playerId);
    }
    
    private static boolean isLoggedIn(UUID playerId) {
        return LoginStateManager.getInstance().isLoggedIn(playerId);
    }
}
