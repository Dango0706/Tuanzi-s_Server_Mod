package me.tuanzi.auth.login.session;

import me.tuanzi.auth.login.LoginConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class SessionManager {
    private static final Logger LOGGER = LoggerFactory.getLogger("SessionManager");
    private static SessionManager instance;
    private final Map<String, LoginSession> sessions = new ConcurrentHashMap<>();
    private final LoginConfig config;
    private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();

    private SessionManager(LoginConfig config) {
        this.config = config;
        startCleanupTask();
    }

    public static synchronized SessionManager getInstance(LoginConfig config) {
        if (instance == null) {
            instance = new SessionManager(config);
        }
        return instance;
    }

    public static synchronized SessionManager getInstance() {
        if (instance == null) {
            throw new IllegalStateException("SessionManager 尚未初始化，请先调用 getInstance(LoginConfig)");
        }
        return instance;
    }

    public LoginSession createSession(String playerName, String ipAddress) {
        long sessionDurationMs = config.getIpSessionPersistenceSeconds() * 1000L;
        LoginSession session = new LoginSession(playerName, ipAddress, sessionDurationMs);
        sessions.put(playerName.toLowerCase(), session);
        LOGGER.info("玩家 {} 的会话已创建，IP: {}", playerName, ipAddress);
        return session;
    }

    public boolean validateSession(String playerName, String ipAddress) {
        String key = playerName.toLowerCase();
        LoginSession session = sessions.get(key);
        
        if (session == null) {
            return false;
        }
        
        if (!session.isValid()) {
            sessions.remove(key);
            return false;
        }
        
        if (!session.isSameIp(ipAddress)) {
            LOGGER.warn("玩家 {} 的 IP 地址不匹配，会话无效", playerName);
            return false;
        }
        
        return true;
    }

    public void invalidateSession(String playerName) {
        String key = playerName.toLowerCase();
        LoginSession session = sessions.remove(key);
        if (session != null) {
            LOGGER.info("玩家 {} 的会话已失效", playerName);
        }
    }

    public boolean hasValidSession(String playerName) {
        String key = playerName.toLowerCase();
        LoginSession session = sessions.get(key);
        return session != null && session.isValid();
    }

    public boolean checkIpPersistence(String playerName, String ipAddress) {
        String key = playerName.toLowerCase();
        LoginSession session = sessions.get(key);
        
        if (session == null) {
            return false;
        }
        
        if (session.isSameIp(ipAddress) && !session.isExpired()) {
            session.refresh(config.getIpSessionPersistenceSeconds() * 1000L);
            LOGGER.info("玩家 {} 通过 IP 持久化自动登录，IP: {}", playerName, ipAddress);
            return true;
        }
        
        return false;
    }

    public LoginSession getSession(String playerName) {
        return sessions.get(playerName.toLowerCase());
    }

    public int getActiveSessionCount() {
        return sessions.size();
    }

    public void clearAllSessions() {
        sessions.clear();
        LOGGER.info("所有会话已清除");
    }

    private void startCleanupTask() {
        long cleanupIntervalMs = Math.max(config.getIpSessionPersistenceSeconds() / 2, 60) * 1000L;
        
        scheduler.scheduleAtFixedRate(() -> {
            try {
                cleanupExpiredSessions();
            } catch (Exception e) {
                LOGGER.error("清理过期会话时发生错误: {}", e.getMessage());
            }
        }, cleanupIntervalMs, cleanupIntervalMs, TimeUnit.MILLISECONDS);
        
        LOGGER.info("会话清理任务已启动，清理间隔: {} 毫秒", cleanupIntervalMs);
    }

    private void cleanupExpiredSessions() {
        int removedCount = 0;
        for (Map.Entry<String, LoginSession> entry : sessions.entrySet()) {
            if (entry.getValue().isExpired()) {
                sessions.remove(entry.getKey());
                removedCount++;
            }
        }
        if (removedCount > 0) {
            LOGGER.info("已清理 {} 个过期会话", removedCount);
        }
    }

    public void shutdown() {
        scheduler.shutdown();
        try {
            if (!scheduler.awaitTermination(5, TimeUnit.SECONDS)) {
                scheduler.shutdownNow();
            }
        } catch (InterruptedException e) {
            scheduler.shutdownNow();
            Thread.currentThread().interrupt();
        }
        LOGGER.info("SessionManager 已关闭");
    }
}
