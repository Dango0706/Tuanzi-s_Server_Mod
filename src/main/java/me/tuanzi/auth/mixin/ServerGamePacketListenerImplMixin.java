package me.tuanzi.auth.mixin;

import com.mojang.brigadier.ParseResults;
import me.tuanzi.auth.login.LoginStateManager;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ServerGamePacketListenerImpl.class)
public abstract class ServerGamePacketListenerImplMixin {

    @Shadow
    public ServerPlayer player;

    @Inject(
            method = "handleChatCommand",
            at = @At("HEAD"),
            cancellable = true
    )
    private void onHandleChatCommand(net.minecraft.network.protocol.game.ServerboundChatCommandPacket packet, CallbackInfo ci) {
        if (!LoginStateManager.getInstance().isLoggedIn(player.getUUID())) {
            String command = packet.command();
            if (!isAllowedCommand(command)) {
                player.sendSystemMessage(Component.literal("§c请先登录后再使用命令！"));
                ci.cancel();
            }
        }
    }

    @Inject(
            method = "handleSignedChatCommand",
            at = @At("HEAD"),
            cancellable = true
    )
    private void onHandleSignedChatCommand(net.minecraft.network.protocol.game.ServerboundChatCommandSignedPacket packet, CallbackInfo ci) {
        if (!LoginStateManager.getInstance().isLoggedIn(player.getUUID())) {
            String command = packet.command();
            if (!isAllowedCommand(command)) {
                player.sendSystemMessage(Component.literal("§c请先登录后再使用命令！"));
                ci.cancel();
            }
        }
    }

    private boolean isAllowedCommand(String command) {
        String lowerCommand = command.toLowerCase().trim();
        return lowerCommand.startsWith("login ") || 
               lowerCommand.startsWith("register ") ||
               lowerCommand.equals("login") ||
               lowerCommand.equals("register");
    }
}
