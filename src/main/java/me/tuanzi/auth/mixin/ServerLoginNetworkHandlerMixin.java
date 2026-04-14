package me.tuanzi.auth.mixin;

import com.mojang.authlib.GameProfile;
import com.mojang.authlib.exceptions.AuthenticationUnavailableException;
import com.mojang.authlib.yggdrasil.ProfileResult;
import me.tuanzi.auth.AuthModule;
import me.tuanzi.auth.config.AuthConfig;
import me.tuanzi.auth.core.MojangApiService;
import me.tuanzi.auth.whitelist.OfflineUUIDGenerator;
import me.tuanzi.auth.whitelist.WhitelistManager;
import net.minecraft.DefaultUncaughtExceptionHandler;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.login.ClientboundHelloPacket;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerLoginPacketListenerImpl;
import net.minecraft.util.CryptException;
import org.jspecify.annotations.Nullable;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import java.math.BigInteger;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.security.PrivateKey;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

@Mixin(ServerLoginPacketListenerImpl.class)
public abstract class ServerLoginNetworkHandlerMixin {

    @Shadow
    @Final
    private static AtomicInteger UNIQUE_THREAD_ID;

    @Shadow
    @Final
    private static org.slf4j.Logger LOGGER;

    @Shadow
    @Final
    private byte[] challenge;

    @Shadow
    @Final
    private MinecraftServer server;

    @Shadow
    @Final
    private net.minecraft.network.Connection connection;

    @Shadow
    private String requestedUsername;

    @Shadow
    @Final
    private net.minecraft.server.notifications.ServerActivityMonitor serverActivityMonitor;

    @Shadow
    @Mutable
    private ServerLoginPacketListenerImpl.State state;

    @Shadow
    public abstract void disconnect(Component component);

    @Shadow
    protected abstract void startClientVerification(GameProfile profile);

    @Inject(
            method = "handleHello",
            at = @At("HEAD"),
            cancellable = true
    )
    private void onHandleHello(net.minecraft.network.protocol.login.ServerboundHelloPacket packet, CallbackInfo ci) {
        ci.cancel();

        String playerName = packet.name();
        AuthModule.LOGGER.debug("========================================");
        AuthModule.LOGGER.debug("[身份验证] 玩家连接: {}", playerName);

        this.requestedUsername = playerName;

        AuthModule module = AuthModule.getInstance();
        if (module == null) {
            AuthModule.LOGGER.error("[身份验证] AuthModule 未初始化，拒绝登录");
            disconnect(Component.literal("§c服务器验证模块未初始化，请联系管理员"));
            return;
        }

        WhitelistManager whitelistManager = module.getWhitelistManager();
        AuthConfig config = module.getAuthConfig();

        UUID offlineUuid = OfflineUUIDGenerator.generateOfflineUUID(playerName);
        boolean inWhitelist = whitelistManager.isInWhitelist(offlineUuid);

        AuthModule.LOGGER.debug("[身份验证] 白名单预检查: {}", inWhitelist);

        if (inWhitelist) {
            AuthModule.LOGGER.debug("[身份验证] 玩家 {} 在白名单中，跳过正版验证", playerName);
            AuthModule.LOGGER.debug("[身份验证] 盗版UUID: {}", offlineUuid);
            AuthModule.LOGGER.debug("[身份验证] >>> 允许离线登录 <<<");
            AuthModule.LOGGER.debug("========================================");
            
            GameProfile offlineProfile = net.minecraft.core.UUIDUtil.createOfflineProfile(playerName);
            startClientVerification(offlineProfile);
            return;
        }

        AuthModule.LOGGER.debug("[身份验证] 玩家不在白名单中，检查是否为正版玩家...");

        Thread thread = new Thread("Auth-Checker-" + playerName + "-" + UNIQUE_THREAD_ID.incrementAndGet()) {
            public void run() {
                try {
                    AuthModule.LOGGER.debug("[身份验证] 正在查询 Mojang API: {}", playerName);
                    
                    MojangApiService.UserProfile profile = MojangApiService.fetchUuidByUsername(playerName);
                    
                    if (profile != null && profile.isValid()) {
                        String uuidStr = profile.getId();
                        String formattedUuid = MojangApiService.formatUuid(uuidStr);
                        UUID premiumUuid = UUID.fromString(formattedUuid);
                        
                        AuthModule.LOGGER.debug("[身份验证] >>> Mojang 查询成功! <<<");
                        AuthModule.LOGGER.debug("[身份验证] 玩家名: {}", profile.getName());
                        AuthModule.LOGGER.debug("[身份验证] 正版UUID: {}", premiumUuid);
                        AuthModule.LOGGER.debug("[身份验证] UUID版本: {}", premiumUuid.version());
                        AuthModule.LOGGER.debug("[身份验证] >>> 发送加密请求进行验证 <<<");
                        AuthModule.LOGGER.debug("========================================");
                        
                        LOGGER.info("玩家 {} 的 UUID 是 {}", playerName, premiumUuid);
                        
                        sendEncryptionRequestAndVerify(playerName);
                    } else {
                        AuthModule.LOGGER.debug("[身份验证] Mojang 查询无结果 - 玩家名不在正版数据库");
                        AuthModule.LOGGER.debug("[身份验证] >>> 拒绝登录：不在白名单且非正版玩家 <<<");
                        AuthModule.LOGGER.debug("========================================");
                        
                        String kickMessage = config != null ? config.getKickMessage() : "§c您不在服务器白名单中，请联系管理员申请访问权限。";
                        disconnect(Component.literal(kickMessage));
                    }
                } catch (Exception e) {
                    AuthModule.LOGGER.error("[身份验证] 验证异常: {} - {}", e.getClass().getName(), e.getMessage());
                    AuthModule.LOGGER.debug("[身份验证] >>> 拒绝登录：验证异常 <<<");
                    AuthModule.LOGGER.debug("========================================");
                    
                    String kickMessage = config != null ? config.getKickMessage() : "§c您不在服务器白名单中，请联系管理员申请访问权限。";
                    disconnect(Component.literal(kickMessage));
                }
            }
        };
        thread.setUncaughtExceptionHandler(new DefaultUncaughtExceptionHandler(LOGGER));
        thread.start();
    }

    @Inject(
            method = "handleKey",
            at = @At("HEAD"),
            cancellable = true
    )
    private void onHandleKey(net.minecraft.network.protocol.login.ServerboundKeyPacket packet, CallbackInfo ci) throws CryptException {
        ci.cancel();

        AuthModule.LOGGER.debug("========================================");
        AuthModule.LOGGER.debug("[身份验证] 收到加密响应");
        AuthModule.LOGGER.debug("[身份验证] 玩家名: {}", this.requestedUsername);

        PrivateKey serverPrivateKey = this.server.getKeyPair().getPrivate();
        
        if (!packet.isChallengeValid(this.challenge, serverPrivateKey)) {
            AuthModule.LOGGER.error("[身份验证] 协议错误: 挑战验证失败");
            throw new IllegalStateException("Protocol error");
        }

        SecretKey secretKey = packet.getSecretKey(serverPrivateKey);
        Cipher decryptCipher = net.minecraft.util.Crypt.getCipher(2, secretKey);
        Cipher encryptCipher = net.minecraft.util.Crypt.getCipher(1, secretKey);
        String digest = new BigInteger(net.minecraft.util.Crypt.digestData("", this.server.getKeyPair().getPublic(), secretKey)).toString(16);

        state = ServerLoginPacketListenerImpl.State.AUTHENTICATING;
        this.connection.setEncryptionKey(decryptCipher, encryptCipher);

        AuthModule.LOGGER.debug("[身份验证] 加密已建立，开始 Mojang Session 验证...");

        final String playerName = this.requestedUsername;

        Thread thread = new Thread("User Authenticator #" + UNIQUE_THREAD_ID.incrementAndGet()) {
            public void run() {
                String name = Objects.requireNonNull(playerName, "Player name not initialized");

                try {
                    AuthModule.LOGGER.debug("[身份验证] 正在向 Mojang Session 服务器验证: {}", name);
                    
                    InetAddress address = getAddress();
                    ProfileResult result = server.services().sessionService().hasJoinedServer(name, digest, address);
                    
                    if (result != null) {
                        GameProfile profile = result.profile();
                        AuthModule.LOGGER.debug("[身份验证] >>> Mojang Session 验证成功! <<<");
                        AuthModule.LOGGER.debug("[身份验证] 玩家名: {}", profile.name());
                        AuthModule.LOGGER.debug("[身份验证] 正版UUID: {}", profile.id());
                        AuthModule.LOGGER.debug("[身份验证] >>> 确认为正版玩家，允许登录 <<<");
                        AuthModule.LOGGER.debug("========================================");
                        
                        LOGGER.info("玩家 {} 的 UUID 是 {}", profile.name(), profile.id());
                        serverActivityMonitor.reportLoginActivity();
                        startClientVerification(profile);
                        
                    } else {
                        AuthModule.LOGGER.debug("[身份验证] Mojang Session 验证失败");
                        AuthModule.LOGGER.debug("[身份验证] 可能原因: 盗版玩家冒充正版用户名");
                        AuthModule.LOGGER.debug("[身份验证] >>> 拒绝登录 <<<");
                        AuthModule.LOGGER.debug("========================================");
                        
                        AuthModule module = AuthModule.getInstance();
                        AuthConfig config = module != null ? module.getAuthConfig() : null;
                        String kickMessage = config != null ? config.getKickMessage() : "§c您不在服务器白名单中，请联系管理员申请访问权限。";
                        disconnect(Component.literal(kickMessage));
                    }
                } catch (AuthenticationUnavailableException e) {
                    AuthModule.LOGGER.error("[身份验证] Mojang 服务器不可用: {}", e.getMessage());
                    AuthModule.LOGGER.debug("[身份验证] >>> 拒绝登录 <<<");
                    AuthModule.LOGGER.debug("========================================");
                    
                    disconnect(Component.literal("§cMojang 服务器暂时不可用，请稍后再试"));
                } catch (Exception e) {
                    AuthModule.LOGGER.error("[身份验证] 验证异常: {} - {}", e.getClass().getName(), e.getMessage());
                    AuthModule.LOGGER.debug("[身份验证] >>> 拒绝登录 <<<");
                    AuthModule.LOGGER.debug("========================================");
                    
                    disconnect(Component.literal("§c验证过程中发生错误，请联系管理员"));
                }
            }

            @Nullable
            private InetAddress getAddress() {
                SocketAddress remoteAddress = connection.getRemoteAddress();
                return server.getPreventProxyConnections() && remoteAddress instanceof InetSocketAddress
                        ? ((InetSocketAddress) remoteAddress).getAddress()
                        : null;
            }
        };
        thread.setUncaughtExceptionHandler(new DefaultUncaughtExceptionHandler(LOGGER));
        thread.start();
    }

    private void sendEncryptionRequestAndVerify(String playerName) {
        AuthModule.LOGGER.debug("[身份验证] 发送加密请求: {}", playerName);
        
        state = ServerLoginPacketListenerImpl.State.KEY;
        this.connection.send(new ClientboundHelloPacket("", this.server.getKeyPair().getPublic().getEncoded(), this.challenge, true));
        
        AuthModule.LOGGER.debug("[身份验证] 加密请求已发送，等待客户端响应...");
    }
}
