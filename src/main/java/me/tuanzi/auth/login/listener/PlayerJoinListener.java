package me.tuanzi.auth.login.listener;

import me.tuanzi.auth.AuthModule;
import me.tuanzi.auth.core.CachedPlayer;
import me.tuanzi.auth.core.PlayerType;
import me.tuanzi.auth.core.PremiumCache;
import me.tuanzi.auth.login.LoginConfig;
import me.tuanzi.auth.login.LoginStateManager;
import me.tuanzi.auth.login.data.AccountManager;
import me.tuanzi.auth.login.session.SessionManager;
import me.tuanzi.auth.login.timeout.LoginTimeoutManager;
import me.tuanzi.auth.utils.TranslationHelper;
import me.tuanzi.auth.whitelist.WhitelistManager;
import net.fabricmc.fabric.api.networking.v1.PacketSender;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.ServerGamePacketListenerImpl;

import java.util.UUID;

public class PlayerJoinListener implements ServerPlayConnectionEvents.Join {
    
    @Override
    public void onPlayReady(ServerGamePacketListenerImpl handler, PacketSender sender, MinecraftServer server) {
        ServerPlayer player = handler.player;
        String playerName = player.getName().getString();
        UUID playerUUID = player.getUUID();
        
        AuthModule.LOGGER.info("========================================");
        AuthModule.LOGGER.info("[AuthModule] 玩家加入服务器: {} (UUID: {})", playerName, playerUUID);
        
        AuthModule authModule = AuthModule.getInstance();
        if (authModule == null) {
            AuthModule.LOGGER.error("[AuthModule] AuthModule 未初始化");
            return;
        }
        
        WhitelistManager whitelistManager = authModule.getWhitelistManager();
        LoginConfig loginConfig = authModule.getLoginConfig();
        AccountManager accountManager = authModule.getAccountManager();
        LoginTimeoutManager timeoutManager = authModule.getLoginTimeoutManager();
        SessionManager sessionManager = authModule.getSessionManager();
        LoginStateManager stateManager = LoginStateManager.getInstance();
        
        boolean isPremium = checkIfPremiumPlayer(playerUUID, playerName);
        
        if (isPremium) {
            AuthModule.LOGGER.info("[AuthModule] 玩家 {} 是正版玩家，跳过登录验证", playerName);
            stateManager.setLoggedIn(playerUUID);
            AuthModule.LOGGER.info("========================================");
            return;
        }
        
        UUID offlineUuid = me.tuanzi.auth.whitelist.OfflineUUIDGenerator.generateOfflineUUID(playerName);
        boolean inWhitelist = whitelistManager.isInWhitelist(offlineUuid);
        
        if (!inWhitelist) {
            AuthModule.LOGGER.warn("[AuthModule] 盗版玩家 {} 不在白名单中，拒绝登录", playerName);
            handler.disconnect(TranslationHelper.translatable("auth.module.not_in_whitelist"));
            AuthModule.LOGGER.info("========================================");
            return;
        }
        
        AuthModule.LOGGER.info("[AuthModule] 玩家 {} 是盗版玩家，需要登录验证", playerName);
        
        String ipAddress = getPlayerIpAddress(handler);
        
        if (sessionManager != null && sessionManager.checkIpPersistence(playerName, ipAddress)) {
            AuthModule.LOGGER.info("[AuthModule] 玩家 {} 通过 IP 持久化自动登录", playerName);
            stateManager.setLoggedIn(playerUUID);
            TranslationHelper.sendMessage(player, "auth.restriction.auto_login");
            AuthModule.LOGGER.info("========================================");
            return;
        }
        
        boolean isRegistered = accountManager != null && accountManager.isRegistered(playerName);
        
        if (isRegistered) {
            TranslationHelper.sendMessage(player, "auth.restriction.please_login");
        } else {
            TranslationHelper.sendMessage(player, "auth.restriction.please_register");
        }
        
        if (timeoutManager != null) {
            timeoutManager.startLoginTimer(playerName);
            int timeoutSeconds = loginConfig != null ? loginConfig.getLoginTimeoutSeconds() : 60;
            TranslationHelper.sendMessage(player, "auth.restriction.login_timeout", timeoutSeconds);
        }
        
        AuthModule.LOGGER.info("[AuthModule] 等待玩家 {} 登录...", playerName);
        AuthModule.LOGGER.info("========================================");
    }
    
    private boolean checkIfPremiumPlayer(UUID playerUuid, String playerName) {
        int uuidVersion = playerUuid.version();
        
        AuthModule.LOGGER.info("[AuthModule] 玩家 {} UUID版本: {}", playerName, uuidVersion);
        
        if (uuidVersion == 4) {
            AuthModule.LOGGER.info("[AuthModule] UUID版本为4，判定为正版玩家");
            return true;
        } else if (uuidVersion == 3) {
            AuthModule.LOGGER.info("[AuthModule] UUID版本为3，判定为盗版玩家");
            return false;
        }
        
        PremiumCache premiumCache = PremiumCache.getInstance();
        CachedPlayer cached = premiumCache.getByUsername(playerName);
        
        if (cached != null) {
            return cached.isPremium();
        }
        
        PlayerType playerType = premiumCache.checkAndCachePlayerType(playerName);
        return playerType == PlayerType.PREMIUM;
    }
    
    private String getPlayerIpAddress(ServerGamePacketListenerImpl handler) {
        try {
            ServerPlayer player = handler.player;
            if (player != null) {
                return player.getIpAddress();
            }
        } catch (Exception e) {
            AuthModule.LOGGER.debug("无法获取玩家 IP 地址: {}", e.getMessage());
        }
        return "unknown";
    }
}
