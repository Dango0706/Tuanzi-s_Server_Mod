package me.tuanzi.auth.login.timeout;

import me.tuanzi.auth.login.LoginConfig;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

public class LoginTimeoutManager {
    
    private static final Logger LOGGER = LoggerFactory.getLogger("LoginTimeoutManager");
    
    private final LoginConfig config;
    private final MinecraftServer server;
    private final ScheduledExecutorService scheduler;
    private final Map<String, ScheduledFuture<?>> loginTimers;
    private final Map<String, Long> loginStartTimes;
    
    public LoginTimeoutManager(LoginConfig config, MinecraftServer server) {
        this.config = config;
        this.server = server;
        this.scheduler = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread thread = new Thread(r, "Login-Timeout-Manager");
            thread.setDaemon(true);
            return thread;
        });
        this.loginTimers = new ConcurrentHashMap<>();
        this.loginStartTimes = new ConcurrentHashMap<>();
    }
    
    public void startLoginTimer(String playerName) {
        cancelLoginTimer(playerName);
        
        int timeoutSeconds = config.getLoginTimeoutSeconds();
        long startTime = System.currentTimeMillis();
        loginStartTimes.put(playerName, startTime);
        
        LOGGER.info("为玩家 {} 启动登录超时计时器，超时时间: {} 秒", playerName, timeoutSeconds);
        
        ScheduledFuture<?> timer = scheduler.schedule(() -> {
            kickPlayerForTimeout(playerName);
            loginTimers.remove(playerName);
            loginStartTimes.remove(playerName);
        }, timeoutSeconds, TimeUnit.SECONDS);
        
        loginTimers.put(playerName, timer);
    }
    
    public void cancelLoginTimer(String playerName) {
        ScheduledFuture<?> timer = loginTimers.remove(playerName);
        if (timer != null) {
            timer.cancel(false);
            loginStartTimes.remove(playerName);
            LOGGER.debug("已取消玩家 {} 的登录超时计时器", playerName);
        }
    }
    
    public boolean hasActiveTimer(String playerName) {
        ScheduledFuture<?> timer = loginTimers.get(playerName);
        return timer != null && !timer.isDone();
    }
    
    public int getRemainingSeconds(String playerName) {
        Long startTime = loginStartTimes.get(playerName);
        if (startTime == null) {
            return 0;
        }
        
        long elapsed = (System.currentTimeMillis() - startTime) / 1000;
        int remaining = config.getLoginTimeoutSeconds() - (int) elapsed;
        return Math.max(0, remaining);
    }
    
    private void kickPlayerForTimeout(String playerName) {
        server.execute(() -> {
            ServerPlayer player = findPlayerByName(playerName);
            if (player != null && player.connection != null) {
                String message = String.format("§c登录超时！您有 %d 秒的时间完成登录。", config.getLoginTimeoutSeconds());
                player.connection.disconnect(Component.literal(message));
                LOGGER.info("玩家 {} 因登录超时被踢出服务器", playerName);
            }
        });
    }
    
    private ServerPlayer findPlayerByName(String playerName) {
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            if (player.getName().getString().equals(playerName)) {
                return player;
            }
        }
        return null;
    }
    
    public void shutdown() {
        LOGGER.info("正在关闭登录超时管理器...");
        scheduler.shutdown();
        try {
            if (!scheduler.awaitTermination(5, TimeUnit.SECONDS)) {
                scheduler.shutdownNow();
            }
        } catch (InterruptedException e) {
            scheduler.shutdownNow();
            Thread.currentThread().interrupt();
        }
        loginTimers.clear();
        loginStartTimes.clear();
        LOGGER.info("登录超时管理器已关闭");
    }
    
    public void clearAllTimers() {
        for (ScheduledFuture<?> timer : loginTimers.values()) {
            timer.cancel(false);
        }
        loginTimers.clear();
        loginStartTimes.clear();
        LOGGER.info("已清除所有登录超时计时器");
    }
}
