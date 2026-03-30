package me.tuanzi.auth.logging;

import me.tuanzi.auth.AuthModule;
import me.tuanzi.auth.config.AuthConfig;
import me.tuanzi.auth.core.PlayerType;
import net.fabricmc.loader.api.FabricLoader;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.stream.Stream;

/**
 * 身份验证日志记录器
 * 实现分级日志系统，支持按日轮转和自动清理
 */
public class AuthLogger {
    
    private static final String LOG_DIR = FabricLoader.getInstance().getConfigDir().resolve("auth").resolve("logs").toString();
    private static final String LOG_FILE_PREFIX = "auth_";
    private static final String LOG_FILE_SUFFIX = ".log";
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private static final DateTimeFormatter DATETIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    
    private static AuthLogger instance;
    private final AuthConfig authConfig;
    private LocalDate currentDate;
    private PrintWriter currentWriter;
    private final Object lock = new Object();
    
    /**
     * 日志级别枚举
     */
    public enum LogLevel {
        INFO,
        WARN,
        ERROR
    }
    
    /**
     * 登录类型枚举
     */
    public enum LoginType {
        PREMIUM_LOGIN("正版登录"),
        CRACKED_LOGIN("离线登录"),
        PREMIUM_CACHE_HIT("正版缓存命中"),
        PREMIUM_CACHE_MISS("正版缓存未命中");
        
        private final String displayName;
        
        LoginType(String displayName) {
            this.displayName = displayName;
        }
        
        public String getDisplayName() {
            return displayName;
        }
    }
    
    /**
     * 验证结果枚举
     */
    public enum VerifyResult {
        SUCCESS("成功"),
        FAILED("失败"),
        WHITELIST_PASSED("白名单通过"),
        WHITELIST_REJECTED("白名单拒绝"),
        ERROR("异常");
        
        private final String displayName;
        
        VerifyResult(String displayName) {
            this.displayName = displayName;
        }
        
        public String getDisplayName() {
            return displayName;
        }
    }
    
    /**
     * 处理动作枚举
     */
    public enum Action {
        ALLOW_JOIN("允许加入"),
        KICK("踢出服务器"),
        CACHE_UPDATED("缓存已更新"),
        API_ERROR("API错误"),
        REGISTER("注册"),
        LOGIN("登录"),
        LOGIN_SUCCESS("登录成功"),
        LOGIN_FAILED("登录失败"),
        PASSWORD_CHANGE("密码修改"),
        PASSWORD_RESET("密码重置"),
        ACCOUNT_LOCKED("账户锁定"),
        SESSION_CREATED("会话创建"),
        SESSION_EXPIRED("会话过期");
        
        private final String displayName;
        
        Action(String displayName) {
            this.displayName = displayName;
        }
        
        public String getDisplayName() {
            return displayName;
        }
    }
    
    /**
     * 登录模块事件类型枚举
     */
    public enum LoginEventType {
        REGISTER("注册"),
        LOGIN("登录"),
        LOGOUT("登出"),
        PASSWORD_CHANGE("密码修改"),
        PASSWORD_RESET("密码重置"),
        ADMIN_OPERATION("管理员操作");
        
        private final String displayName;
        
        LoginEventType(String displayName) {
            this.displayName = displayName;
        }
        
        public String getDisplayName() {
            return displayName;
        }
    }
    
    private AuthLogger() {
        this.authConfig = AuthModule.getInstance().getAuthConfig();
        this.currentDate = LocalDate.now();
        ensureLogDirectory();
    }
    
    /**
     * 获取 AuthLogger 单例实例
     *
     * @return AuthLogger 实例
     */
    public static synchronized AuthLogger getInstance() {
        if (instance == null) {
            instance = new AuthLogger();
        }
        return instance;
    }
    
    /**
     * 确保日志目录存在
     */
    private void ensureLogDirectory() {
        File logDir = new File(LOG_DIR);
        if (!logDir.exists()) {
            logDir.mkdirs();
        }
    }
    
    /**
     * 获取当前日志文件路径
     *
     * @return 日志文件路径
     */
    private String getCurrentLogFile() {
        return LOG_DIR + File.separator + LOG_FILE_PREFIX + currentDate.format(DATE_FORMATTER) + LOG_FILE_SUFFIX;
    }
    
    /**
     * 检查是否需要轮转日志文件
     */
    private void checkRotation() {
        LocalDate today = LocalDate.now();
        if (!today.equals(currentDate)) {
            closeCurrentWriter();
            currentDate = today;
            cleanupOldLogs();
        }
    }
    
    /**
     * 关闭当前写入器
     */
    private void closeCurrentWriter() {
        if (currentWriter != null) {
            currentWriter.close();
            currentWriter = null;
        }
    }
    
    /**
     * 获取或创建当前写入器
     *
     * @return PrintWriter 实例
     * @throws IOException 文件操作异常
     */
    private PrintWriter getOrCreateWriter() throws IOException {
        if (currentWriter == null) {
            File logFile = new File(getCurrentLogFile());
            currentWriter = new PrintWriter(new FileWriter(logFile, true));
        }
        return currentWriter;
    }
    
    /**
     * 记录日志
     *
     * @param level      日志级别
     * @param playerName 玩家名
     * @param loginType  登录类型
     * @param result     验证结果
     * @param action     处理动作
     * @param message    附加消息（可选）
     */
    private void log(LogLevel level, String playerName, LoginType loginType, VerifyResult result, Action action, String message) {
        if (!authConfig.isEnableAuthLog()) {
            return;
        }
        
        synchronized (lock) {
            try {
                checkRotation();
                
                String timestamp = LocalDateTime.now().format(DATETIME_FORMATTER);
                String logLine = String.format("[%s] [%s] [AuthModule] %s | %s | %s | %s",
                        timestamp,
                        level.name(),
                        playerName != null ? playerName : "未知",
                        loginType != null ? loginType.getDisplayName() : "未知",
                        result != null ? result.getDisplayName() : "未知",
                        action != null ? action.getDisplayName() : "未知");
                
                if (message != null && !message.isEmpty()) {
                    logLine += " | " + message;
                }
                
                PrintWriter writer = getOrCreateWriter();
                writer.println(logLine);
                writer.flush();
                
                writeToSLF4J(level, logLine);
                
            } catch (IOException e) {
                AuthModule.LOGGER.error("写入身份验证日志失败: {}", e.getMessage());
            }
        }
    }
    
    /**
     * 同时输出到 SLF4J 日志
     *
     * @param level   日志级别
     * @param message 日志消息
     */
    private void writeToSLF4J(LogLevel level, String message) {
        switch (level) {
            case INFO:
                AuthModule.LOGGER.info(message);
                break;
            case WARN:
                AuthModule.LOGGER.warn(message);
                break;
            case ERROR:
                AuthModule.LOGGER.error(message);
                break;
        }
    }
    
    /**
     * 记录 INFO 级别日志（成功登录）
     *
     * @param playerName 玩家名
     * @param loginType  登录类型
     * @param result     验证结果
     * @param action     处理动作
     */
    public void info(String playerName, LoginType loginType, VerifyResult result, Action action) {
        log(LogLevel.INFO, playerName, loginType, result, action, null);
    }
    
    /**
     * 记录 INFO 级别日志（成功登录，带附加消息）
     *
     * @param playerName 玩家名
     * @param loginType  登录类型
     * @param result     验证结果
     * @param action     处理动作
     * @param message    附加消息
     */
    public void info(String playerName, LoginType loginType, VerifyResult result, Action action, String message) {
        log(LogLevel.INFO, playerName, loginType, result, action, message);
    }
    
    /**
     * 记录 WARN 级别日志（拒绝登录）
     *
     * @param playerName 玩家名
     * @param loginType  登录类型
     * @param result     验证结果
     * @param action     处理动作
     */
    public void warn(String playerName, LoginType loginType, VerifyResult result, Action action) {
        log(LogLevel.WARN, playerName, loginType, result, action, null);
    }
    
    /**
     * 记录 WARN 级别日志（拒绝登录，带附加消息）
     *
     * @param playerName 玩家名
     * @param loginType  登录类型
     * @param result     验证结果
     * @param action     处理动作
     * @param message    附加消息
     */
    public void warn(String playerName, LoginType loginType, VerifyResult result, Action action, String message) {
        log(LogLevel.WARN, playerName, loginType, result, action, message);
    }
    
    /**
     * 记录 ERROR 级别日志（验证异常）
     *
     * @param playerName 玩家名
     * @param loginType  登录类型
     * @param result     验证结果
     * @param action     处理动作
     * @param error      错误信息
     */
    public void error(String playerName, LoginType loginType, VerifyResult result, Action action, String error) {
        log(LogLevel.ERROR, playerName, loginType, result, action, error);
    }
    
    /**
     * 记录 ERROR 级别日志（验证异常，带异常对象）
     *
     * @param playerName 玩家名
     * @param loginType  登录类型
     * @param result     验证结果
     * @param action     处理动作
     * @param e          异常对象
     */
    public void error(String playerName, LoginType loginType, VerifyResult result, Action action, Exception e) {
        String errorMessage = e != null ? e.getClass().getSimpleName() + ": " + e.getMessage() : "未知错误";
        log(LogLevel.ERROR, playerName, loginType, result, action, errorMessage);
    }
    
    /**
     * 清理过期的日志文件
     */
    private void cleanupOldLogs() {
        int retentionDays = authConfig.getLogRetentionDays();
        if (retentionDays <= 0) {
            return;
        }
        
        LocalDate cutoffDate = LocalDate.now().minusDays(retentionDays);
        File logDir = new File(LOG_DIR);
        
        if (!logDir.exists() || !logDir.isDirectory()) {
            return;
        }
        
        File[] logFiles = logDir.listFiles((dir, name) -> 
                name.startsWith(LOG_FILE_PREFIX) && name.endsWith(LOG_FILE_SUFFIX));
        
        if (logFiles == null) {
            return;
        }
        
        for (File logFile : logFiles) {
            String fileName = logFile.getName();
            String dateStr = fileName.substring(LOG_FILE_PREFIX.length(), fileName.length() - LOG_FILE_SUFFIX.length());
            
            try {
                LocalDate fileDate = LocalDate.parse(dateStr, DATE_FORMATTER);
                if (fileDate.isBefore(cutoffDate)) {
                    if (logFile.delete()) {
                        AuthModule.LOGGER.info("已清理过期日志文件: {}", fileName);
                    }
                }
            } catch (Exception e) {
                AuthModule.LOGGER.warn("无法解析日志文件日期: {}", fileName);
            }
        }
    }
    
    /**
     * 关闭日志记录器
     */
    public void shutdown() {
        synchronized (lock) {
            closeCurrentWriter();
        }
    }
    
    /**
     * 记录成功登录日志
     *
     * @param playerName 玩家名
     * @param playerType 玩家类型
     * @param loginType  登录类型
     */
    public void logSuccessLogin(String playerName, PlayerType playerType, LoginType loginType) {
        VerifyResult result = playerType == PlayerType.PREMIUM ? 
                VerifyResult.SUCCESS : VerifyResult.WHITELIST_PASSED;
        info(playerName, loginType, result, Action.ALLOW_JOIN);
    }
    
    /**
     * 记录拒绝登录日志
     *
     * @param playerName 玩家名
     * @param loginType  登录类型
     * @param reason     拒绝原因
     */
    public void logRejectedLogin(String playerName, LoginType loginType, String reason) {
        warn(playerName, loginType, VerifyResult.WHITELIST_REJECTED, Action.KICK, reason);
    }
    
    /**
     * 记录验证异常日志
     *
     * @param playerName 玩家名
     * @param loginType  登录类型
     * @param e          异常对象
     */
    public void logVerifyError(String playerName, LoginType loginType, Exception e) {
        error(playerName, loginType, VerifyResult.ERROR, Action.API_ERROR, e);
    }
    
    /**
     * 记录登录模块日志（内部方法）
     *
     * @param level      日志级别
     * @param eventType  事件类型
     * @param playerName 玩家名
     * @param success    是否成功
     * @param message    附加消息
     */
    private void logLoginEvent(LogLevel level, LoginEventType eventType, String playerName, boolean success, String message) {
        if (!authConfig.isEnableAuthLog()) {
            return;
        }
        
        synchronized (lock) {
            try {
                checkRotation();
                
                String timestamp = LocalDateTime.now().format(DATETIME_FORMATTER);
                String status = success ? "成功" : "失败";
                String logLine = String.format("[%s] [%s] [LoginModule] %s | %s | %s",
                        timestamp,
                        level.name(),
                        playerName != null ? playerName : "未知",
                        eventType != null ? eventType.getDisplayName() : "未知",
                        status);
                
                if (message != null && !message.isEmpty()) {
                    logLine += " | " + message;
                }
                
                PrintWriter writer = getOrCreateWriter();
                writer.println(logLine);
                writer.flush();
                
                writeToSLF4J(level, logLine);
                
            } catch (IOException e) {
                AuthModule.LOGGER.error("写入登录模块日志失败: {}", e.getMessage());
            }
        }
    }
    
    /**
     * 记录注册事件日志
     *
     * @param playerName 玩家名
     * @param success    是否成功
     */
    public void logRegisterEvent(String playerName, boolean success) {
        logRegisterEvent(playerName, success, null);
    }
    
    /**
     * 记录注册事件日志（带附加消息）
     *
     * @param playerName 玩家名
     * @param success    是否成功
     * @param message    附加消息
     */
    public void logRegisterEvent(String playerName, boolean success, String message) {
        LogLevel level = success ? LogLevel.INFO : LogLevel.WARN;
        logLoginEvent(level, LoginEventType.REGISTER, playerName, success, message);
    }
    
    /**
     * 记录登录事件日志
     *
     * @param playerName 玩家名
     * @param success    是否成功
     * @param ipAddress  IP地址
     */
    public void logLoginEvent(String playerName, boolean success, String ipAddress) {
        logLoginEvent(playerName, success, ipAddress, null);
    }
    
    /**
     * 记录登录事件日志（带附加消息）
     *
     * @param playerName 玩家名
     * @param success    是否成功
     * @param ipAddress  IP地址
     * @param message    附加消息
     */
    public void logLoginEvent(String playerName, boolean success, String ipAddress, String message) {
        LogLevel level = success ? LogLevel.INFO : LogLevel.WARN;
        String fullMessage = "IP: " + (ipAddress != null ? ipAddress : "未知");
        if (message != null && !message.isEmpty()) {
            fullMessage += " | " + message;
        }
        logLoginEvent(level, LoginEventType.LOGIN, playerName, success, fullMessage);
    }
    
    /**
     * 记录登录失败日志
     *
     * @param playerName     玩家名
     * @param remainingAttempts 剩余尝试次数
     */
    public void logLoginFailed(String playerName, int remainingAttempts) {
        String message = "剩余尝试次数: " + remainingAttempts;
        logLoginEvent(LogLevel.WARN, LoginEventType.LOGIN, playerName, false, message);
    }
    
    /**
     * 记录账户锁定日志
     *
     * @param playerName 玩家名
     * @param reason     锁定原因
     */
    public void logAccountLocked(String playerName, String reason) {
        String message = "账户已被锁定 | 原因: " + (reason != null ? reason : "登录失败次数过多");
        logLoginEvent(LogLevel.WARN, LoginEventType.LOGIN, playerName, false, message);
    }
    
    /**
     * 记录密码修改日志
     *
     * @param playerName 玩家名
     * @param success    是否成功
     */
    public void logPasswordChangeEvent(String playerName, boolean success) {
        logPasswordChangeEvent(playerName, success, null);
    }
    
    /**
     * 记录密码修改日志（带附加消息）
     *
     * @param playerName 玩家名
     * @param success    是否成功
     * @param message    附加消息
     */
    public void logPasswordChangeEvent(String playerName, boolean success, String message) {
        LogLevel level = success ? LogLevel.INFO : LogLevel.WARN;
        logLoginEvent(level, LoginEventType.PASSWORD_CHANGE, playerName, success, message);
    }
    
    /**
     * 记录管理员操作日志
     *
     * @param operatorName 操作者名称
     * @param operation    操作类型
     * @param targetPlayer 目标玩家
     * @param success      是否成功
     */
    public void logAdminOperation(String operatorName, String operation, String targetPlayer, boolean success) {
        logAdminOperation(operatorName, operation, targetPlayer, success, null);
    }
    
    /**
     * 记录管理员操作日志（带附加消息）
     *
     * @param operatorName 操作者名称
     * @param operation    操作类型
     * @param targetPlayer 目标玩家
     * @param success      是否成功
     * @param message      附加消息
     */
    public void logAdminOperation(String operatorName, String operation, String targetPlayer, boolean success, String message) {
        if (!authConfig.isEnableAuthLog()) {
            return;
        }
        
        synchronized (lock) {
            try {
                checkRotation();
                
                String timestamp = LocalDateTime.now().format(DATETIME_FORMATTER);
                String status = success ? "成功" : "失败";
                String logLine = String.format("[%s] [%s] [AdminOperation] 操作者: %s | 操作: %s | 目标: %s | %s",
                        timestamp,
                        success ? LogLevel.INFO.name() : LogLevel.WARN.name(),
                        operatorName != null ? operatorName : "未知",
                        operation != null ? operation : "未知",
                        targetPlayer != null ? targetPlayer : "无",
                        status);
                
                if (message != null && !message.isEmpty()) {
                    logLine += " | " + message;
                }
                
                PrintWriter writer = getOrCreateWriter();
                writer.println(logLine);
                writer.flush();
                
                LogLevel level = success ? LogLevel.INFO : LogLevel.WARN;
                writeToSLF4J(level, logLine);
                
            } catch (IOException e) {
                AuthModule.LOGGER.error("写入管理员操作日志失败: {}", e.getMessage());
            }
        }
    }
    
    /**
     * 记录会话创建日志
     *
     * @param playerName 玩家名
     * @param ipAddress  IP地址
     */
    public void logSessionCreated(String playerName, String ipAddress) {
        String message = "IP: " + (ipAddress != null ? ipAddress : "未知");
        logLoginEvent(LogLevel.INFO, LoginEventType.LOGIN, playerName, true, "会话已创建 | " + message);
    }
    
    /**
     * 记录会话过期日志
     *
     * @param playerName 玩家名
     */
    public void logSessionExpired(String playerName) {
        logLoginEvent(LogLevel.INFO, LoginEventType.LOGOUT, playerName, true, "会话已过期");
    }
    
    /**
     * 记录登出日志
     *
     * @param playerName 玩家名
     * @param ipAddress  IP地址
     */
    public void logLogout(String playerName, String ipAddress) {
        String message = "IP: " + (ipAddress != null ? ipAddress : "未知");
        logLoginEvent(LogLevel.INFO, LoginEventType.LOGOUT, playerName, true, message);
    }
}
