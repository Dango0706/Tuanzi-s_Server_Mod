package me.tuanzi.auth.login.listener;

import me.tuanzi.auth.AuthModule;
import me.tuanzi.auth.login.LoginStateManager;
import me.tuanzi.auth.login.session.SessionManager;
import me.tuanzi.auth.login.timeout.LoginTimeoutManager;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.ServerGamePacketListenerImpl;

import java.util.UUID;

public class PlayerQuitListener implements ServerPlayConnectionEvents.Disconnect {
    
    @Override
    public void onPlayDisconnect(ServerGamePacketListenerImpl handler, MinecraftServer server) {
        ServerPlayer player = handler.player;
        String playerName = player.getName().getString();
        UUID playerUUID = player.getUUID();
        
        AuthModule.LOGGER.info("========================================");
        AuthModule.LOGGER.info("[AuthModule] 玩家离开服务器: {} (UUID: {})", playerName, playerUUID);
        
        AuthModule authModule = AuthModule.getInstance();
        if (authModule == null) {
            AuthModule.LOGGER.error("[AuthModule] AuthModule 未初始化");
            return;
        }
        
        LoginTimeoutManager timeoutManager = authModule.getLoginTimeoutManager();
        if (timeoutManager != null) {
            timeoutManager.cancelLoginTimer(playerName);
            AuthModule.LOGGER.info("[AuthModule] 已取消玩家 {} 的登录超时计时器", playerName);
        }
        
        SessionManager sessionManager = authModule.getSessionManager();
        if (sessionManager != null && sessionManager.getSession(playerName) != null) {
            AuthModule.LOGGER.info("[AuthModule] 玩家 {} 的会话保留，等待自然过期", playerName);
        }
        
        LoginStateManager stateManager = LoginStateManager.getInstance();
        stateManager.setLoggedOut(playerUUID);
        
        PlayerRestrictionListener.clearPlayer(playerUUID);
        
        AuthModule.LOGGER.info("[AuthModule] 玩家 {} 的登录状态已清理", playerName);
        AuthModule.LOGGER.info("========================================");
    }
}
