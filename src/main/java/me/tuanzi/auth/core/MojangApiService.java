package me.tuanzi.auth.core;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import me.tuanzi.TuanzisServerMod;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.concurrent.CompletableFuture;

/**
 * Mojang API 服务类
 * 封装 Mojang API 调用，用于验证正版玩家
 */
public class MojangApiService {
    private static final String API_MOJANG_USERS_PROFILES = "https://api.mojang.com/users/profiles/minecraft/";
    private static final String SESSION_SERVER_HAS_JOINED = "https://sessionserver.mojang.com/session/minecraft/hasJoined";
    
    private static final HttpClient HTTP_CLIENT = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .build();
    
    private static final Gson GSON = new Gson();
    private static final int TIMEOUT_SECONDS = 10;
    
    /**
     * Mojang 用户资料响应
     */
    public static class UserProfile {
        private String id;
        private String name;
        
        public String getId() {
            return id;
        }
        
        public String getName() {
            return name;
        }
        
        public boolean isValid() {
            return id != null && !id.isEmpty() && name != null && !name.isEmpty();
        }
    }
    
    /**
     * Session 验证响应
     */
    public static class SessionProfile {
        private String id;
        private String name;
        private JsonObject properties;
        
        public String getId() {
            return id;
        }
        
        public String getName() {
            return name;
        }
        
        public JsonObject getProperties() {
            return properties;
        }
        
        public boolean isValid() {
            return id != null && !id.isEmpty() && name != null && !name.isEmpty();
        }
    }
    
    /**
     * 根据用户名查询 Mojang UUID
     * 同步方法
     *
     * @param username 玩家用户名
     * @return UserProfile 对象，如果查询失败返回 null
     */
    public static UserProfile fetchUuidByUsername(String username) {
        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(API_MOJANG_USERS_PROFILES + username))
                    .timeout(Duration.ofSeconds(TIMEOUT_SECONDS))
                    .GET()
                    .build();
            
            HttpResponse<String> response = HTTP_CLIENT.send(request, HttpResponse.BodyHandlers.ofString());
            
            if (response.statusCode() == 200) {
                UserProfile profile = GSON.fromJson(response.body(), UserProfile.class);
                if (profile != null && profile.isValid()) {
                    TuanzisServerMod.LOGGER.debug("成功查询到玩家 {} 的 Mojang UUID: {}", username, profile.getId());
                    return profile;
                }
            } else if (response.statusCode() == 204 || response.statusCode() == 404) {
                TuanzisServerMod.LOGGER.debug("玩家 {} 不是正版玩家", username);
                return null;
            } else {
                TuanzisServerMod.LOGGER.warn("查询玩家 {} 的 Mojang UUID 失败，状态码: {}", username, response.statusCode());
            }
        } catch (Exception e) {
            TuanzisServerMod.LOGGER.error("查询玩家 {} 的 Mojang UUID 时发生异常: {}", username, e.getMessage());
        }
        return null;
    }
    
    /**
     * 根据用户名查询 Mojang UUID
     * 异步方法
     *
     * @param username 玩家用户名
     * @return CompletableFuture 包含 UserProfile 对象
     */
    public static CompletableFuture<UserProfile> fetchUuidByUsernameAsync(String username) {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(API_MOJANG_USERS_PROFILES + username))
                .timeout(Duration.ofSeconds(TIMEOUT_SECONDS))
                .GET()
                .build();
        
        return HTTP_CLIENT.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                .thenApply(response -> {
                    if (response.statusCode() == 200) {
                        UserProfile profile = GSON.fromJson(response.body(), UserProfile.class);
                        if (profile != null && profile.isValid()) {
                            TuanzisServerMod.LOGGER.debug("成功查询到玩家 {} 的 Mojang UUID: {}", username, profile.getId());
                            return profile;
                        }
                    } else if (response.statusCode() == 204 || response.statusCode() == 404) {
                        TuanzisServerMod.LOGGER.debug("玩家 {} 不是正版玩家", username);
                    } else {
                        TuanzisServerMod.LOGGER.warn("查询玩家 {} 的 Mojang UUID 失败，状态码: {}", username, response.statusCode());
                    }
                    return null;
                })
                .exceptionally(e -> {
                    TuanzisServerMod.LOGGER.error("查询玩家 {} 的 Mojang UUID 时发生异常: {}", username, e.getMessage());
                    return null;
                });
    }
    
    /**
     * 验证玩家是否已加入 Mojang Session 服务器
     * 同步方法
     *
     * @param username 玩家用户名
     * @param serverId 服务器 ID（由服务器生成）
     * @return SessionProfile 对象，如果验证失败返回 null
     */
    public static SessionProfile verifySession(String username, String serverId) {
        try {
            String url = SESSION_SERVER_HAS_JOINED + "?username=" + username + "&serverId=" + serverId;
            
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .timeout(Duration.ofSeconds(TIMEOUT_SECONDS))
                    .GET()
                    .build();
            
            HttpResponse<String> response = HTTP_CLIENT.send(request, HttpResponse.BodyHandlers.ofString());
            
            if (response.statusCode() == 200) {
                SessionProfile profile = GSON.fromJson(response.body(), SessionProfile.class);
                if (profile != null && profile.isValid()) {
                    TuanzisServerMod.LOGGER.debug("玩家 {} Session 验证成功", username);
                    return profile;
                }
            } else {
                TuanzisServerMod.LOGGER.debug("玩家 {} Session 验证失败，状态码: {}", username, response.statusCode());
            }
        } catch (Exception e) {
            TuanzisServerMod.LOGGER.error("验证玩家 {} Session 时发生异常: {}", username, e.getMessage());
        }
        return null;
    }
    
    /**
     * 验证玩家是否已加入 Mojang Session 服务器
     * 异步方法
     *
     * @param username 玩家用户名
     * @param serverId 服务器 ID（由服务器生成）
     * @return CompletableFuture 包含 SessionProfile 对象
     */
    public static CompletableFuture<SessionProfile> verifySessionAsync(String username, String serverId) {
        String url = SESSION_SERVER_HAS_JOINED + "?username=" + username + "&serverId=" + serverId;
        
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .timeout(Duration.ofSeconds(TIMEOUT_SECONDS))
                .GET()
                .build();
        
        return HTTP_CLIENT.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                .thenApply(response -> {
                    if (response.statusCode() == 200) {
                        SessionProfile profile = GSON.fromJson(response.body(), SessionProfile.class);
                        if (profile != null && profile.isValid()) {
                            TuanzisServerMod.LOGGER.debug("玩家 {} Session 验证成功", username);
                            return profile;
                        }
                    } else {
                        TuanzisServerMod.LOGGER.debug("玩家 {} Session 验证失败，状态码: {}", username, response.statusCode());
                    }
                    return null;
                })
                .exceptionally(e -> {
                    TuanzisServerMod.LOGGER.error("验证玩家 {} Session 时发生异常: {}", username, e.getMessage());
                    return null;
                });
    }
    
    /**
     * 检查用户名是否为正版玩家
     * 同步方法
     *
     * @param username 玩家用户名
     * @return 如果是正版玩家返回 true，否则返回 false
     */
    public static boolean isPremiumPlayer(String username) {
        return fetchUuidByUsername(username) != null;
    }
    
    /**
     * 检查用户名是否为正版玩家
     * 异步方法
     *
     * @param username 玩家用户名
     * @return CompletableFuture 包含布尔值结果
     */
    public static CompletableFuture<Boolean> isPremiumPlayerAsync(String username) {
        return fetchUuidByUsernameAsync(username)
                .thenApply(profile -> profile != null);
    }
    
    /**
     * 将无破折号的 UUID 转换为带破折号的标准格式
     *
     * @param uuid 无破折号的 UUID 字符串
     * @return 带破折号的标准格式 UUID
     */
    public static String formatUuid(String uuid) {
        if (uuid == null || uuid.isEmpty()) {
            return uuid;
        }
        
        if (uuid.contains("-")) {
            return uuid;
        }
        
        if (uuid.length() != 32) {
            return uuid;
        }
        
        return uuid.substring(0, 8) + "-" +
               uuid.substring(8, 12) + "-" +
               uuid.substring(12, 16) + "-" +
               uuid.substring(16, 20) + "-" +
               uuid.substring(20);
    }
}
