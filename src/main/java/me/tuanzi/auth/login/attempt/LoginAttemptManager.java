package me.tuanzi.auth.login.attempt;

import me.tuanzi.auth.login.LoginConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class LoginAttemptManager {
    
    private static final Logger LOGGER = LoggerFactory.getLogger("LoginAttemptManager");
    
    private final LoginConfig config;
    private final Map<String, Integer> failedAttempts;
    private final Map<String, Long> lockoutStartTimes;
    
    public LoginAttemptManager(LoginConfig config) {
        this.config = config;
        this.failedAttempts = new ConcurrentHashMap<>();
        this.lockoutStartTimes = new ConcurrentHashMap<>();
    }
    
    public void recordFailedAttempt(String playerName) {
        int attempts = failedAttempts.getOrDefault(playerName, 0) + 1;
        failedAttempts.put(playerName, attempts);
        
        LOGGER.warn("玩家 {} 登录失败，当前失败次数: {}/{}", playerName, attempts, config.getMaxLoginAttempts());
        
        if (attempts >= config.getMaxLoginAttempts()) {
            lockoutStartTimes.put(playerName, System.currentTimeMillis());
            LOGGER.warn("玩家 {} 登录失败次数达到上限，账户已被锁定 {} 秒", 
                    playerName, config.getLockoutDurationSeconds());
        }
    }
    
    public boolean isLocked(String playerName) {
        Long lockoutStart = lockoutStartTimes.get(playerName);
        if (lockoutStart == null) {
            return false;
        }
        
        long elapsed = System.currentTimeMillis() - lockoutStart;
        long lockoutDurationMs = config.getLockoutDurationSeconds() * 1000L;
        
        if (elapsed >= lockoutDurationMs) {
            lockoutStartTimes.remove(playerName);
            failedAttempts.remove(playerName);
            LOGGER.info("玩家 {} 的账户锁定已自动解除", playerName);
            return false;
        }
        
        return true;
    }
    
    public int getRemainingLockTime(String playerName) {
        Long lockoutStart = lockoutStartTimes.get(playerName);
        if (lockoutStart == null) {
            return 0;
        }
        
        long elapsed = System.currentTimeMillis() - lockoutStart;
        long lockoutDurationMs = config.getLockoutDurationSeconds() * 1000L;
        long remainingMs = lockoutDurationMs - elapsed;
        
        if (remainingMs <= 0) {
            lockoutStartTimes.remove(playerName);
            failedAttempts.remove(playerName);
            return 0;
        }
        
        return (int) (remainingMs / 1000);
    }
    
    public void resetAttempts(String playerName) {
        failedAttempts.remove(playerName);
        lockoutStartTimes.remove(playerName);
        LOGGER.info("已重置玩家 {} 的登录尝试记录", playerName);
    }
    
    public int getFailedAttempts(String playerName) {
        return failedAttempts.getOrDefault(playerName, 0);
    }
    
    public int getMaxAttempts() {
        return config.getMaxLoginAttempts();
    }
    
    public int getRemainingAttempts(String playerName) {
        int failed = failedAttempts.getOrDefault(playerName, 0);
        return Math.max(0, config.getMaxLoginAttempts() - failed);
    }
    
    public void clearAllAttempts() {
        failedAttempts.clear();
        lockoutStartTimes.clear();
        LOGGER.info("已清除所有登录尝试记录");
    }
    
    public String getLockMessage(String playerName) {
        int remainingSeconds = getRemainingLockTime(playerName);
        if (remainingSeconds <= 0) {
            return null;
        }
        
        int minutes = remainingSeconds / 60;
        int seconds = remainingSeconds % 60;
        
        if (minutes > 0) {
            return String.format("§c您的账户已被锁定，请等待 %d 分 %d 秒后再试。", minutes, seconds);
        } else {
            return String.format("§c您的账户已被锁定，请等待 %d 秒后再试。", seconds);
        }
    }
}
