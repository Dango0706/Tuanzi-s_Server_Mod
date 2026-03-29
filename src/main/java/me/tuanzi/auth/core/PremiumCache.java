package me.tuanzi.auth.core;

import me.tuanzi.TemplateMod;

import java.util.Collection;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * 正版玩家缓存管理类
 * 缓存正版玩家的验证结果，减少对 Mojang API 的请求
 */
public class PremiumCache {
    private static final long DEFAULT_TTL = TimeUnit.HOURS.toMillis(24);
    private static final long CLEANUP_INTERVAL = TimeUnit.MINUTES.toMillis(30);
    
    private final Map<String, CachedPlayer> cacheByUsername;
    private final Map<String, CachedPlayer> cacheByUuid;
    private final ScheduledExecutorService cleanupExecutor;
    private final long ttl;
    
    private static PremiumCache instance;
    
    /**
     * 获取 PremiumCache 单例实例
     *
     * @return PremiumCache 实例
     */
    public static synchronized PremiumCache getInstance() {
        if (instance == null) {
            instance = new PremiumCache();
        }
        return instance;
    }
    
    /**
     * 创建 PremiumCache 实例
     * 使用默认的 TTL（24小时）
     */
    public PremiumCache() {
        this(DEFAULT_TTL);
    }
    
    /**
     * 创建 PremiumCache 实例
     *
     * @param ttl 缓存有效期（毫秒）
     */
    public PremiumCache(long ttl) {
        this.ttl = ttl;
        this.cacheByUsername = new ConcurrentHashMap<>();
        this.cacheByUuid = new ConcurrentHashMap<>();
        this.cleanupExecutor = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "PremiumCache-Cleanup");
            t.setDaemon(true);
            return t;
        });
        
        startCleanupTask();
        TemplateMod.LOGGER.info("PremiumCache 已初始化，TTL: {} 毫秒", ttl);
    }
    
    /**
     * 启动定期清理过期缓存的任务
     */
    private void startCleanupTask() {
        cleanupExecutor.scheduleAtFixedRate(this::cleanup, CLEANUP_INTERVAL, CLEANUP_INTERVAL, TimeUnit.MILLISECONDS);
    }
    
    /**
     * 清理过期的缓存条目
     */
    public void cleanup() {
        int removedCount = 0;
        for (Map.Entry<String, CachedPlayer> entry : cacheByUsername.entrySet()) {
            if (entry.getValue().isExpired()) {
                remove(entry.getKey());
                removedCount++;
            }
        }
        if (removedCount > 0) {
            TemplateMod.LOGGER.debug("已清理 {} 个过期的缓存条目", removedCount);
        }
    }
    
    /**
     * 添加玩家到缓存
     *
     * @param username   玩家用户名
     * @param uuid       玩家 UUID
     * @param playerType 玩家类型
     */
    public void put(String username, String uuid, PlayerType playerType) {
        long cacheTime = System.currentTimeMillis();
        CachedPlayer player = new CachedPlayer(username, uuid, playerType, cacheTime, ttl);
        
        cacheByUsername.put(username.toLowerCase(), player);
        if (uuid != null && !uuid.isEmpty()) {
            cacheByUuid.put(uuid, player);
        }
        
        TemplateMod.LOGGER.debug("已缓存玩家: {} ({}), 类型: {}", username, uuid, playerType);
    }
    
    /**
     * 添加正版玩家到缓存
     *
     * @param username 玩家用户名
     * @param uuid     玩家 Mojang UUID
     */
    public void putPremium(String username, String uuid) {
        put(username, uuid, PlayerType.PREMIUM);
    }
    
    /**
     * 添加盗版玩家到缓存
     *
     * @param username 玩家用户名
     */
    public void putCracked(String username) {
        put(username, null, PlayerType.CRACKED);
    }
    
    /**
     * 根据用户名获取缓存的玩家信息
     *
     * @param username 玩家用户名
     * @return CachedPlayer 对象，如果不存在或已过期返回 null
     */
    public CachedPlayer getByUsername(String username) {
        CachedPlayer player = cacheByUsername.get(username.toLowerCase());
        if (player != null && !player.isExpired()) {
            return player;
        }
        if (player != null) {
            remove(username);
        }
        return null;
    }
    
    /**
     * 根据 UUID 获取缓存的玩家信息
     *
     * @param uuid 玩家 UUID
     * @return CachedPlayer 对象，如果不存在或已过期返回 null
     */
    public CachedPlayer getByUuid(String uuid) {
        CachedPlayer player = cacheByUuid.get(uuid);
        if (player != null && !player.isExpired()) {
            return player;
        }
        if (player != null) {
            removeByUuid(uuid);
        }
        return null;
    }
    
    /**
     * 检查用户名是否在缓存中且有效
     *
     * @param username 玩家用户名
     * @return 如果存在且有效返回 true
     */
    public boolean containsUsername(String username) {
        return getByUsername(username) != null;
    }
    
    /**
     * 检查 UUID 是否在缓存中且有效
     *
     * @param uuid 玩家 UUID
     * @return 如果存在且有效返回 true
     */
    public boolean containsUuid(String uuid) {
        return getByUuid(uuid) != null;
    }
    
    /**
     * 检查玩家是否为正版玩家（从缓存中）
     *
     * @param username 玩家用户名
     * @return 如果是正版玩家返回 true，如果不存在或不是正版返回 false
     */
    public boolean isPremium(String username) {
        CachedPlayer player = getByUsername(username);
        return player != null && player.isPremium();
    }
    
    /**
     * 根据用户名移除缓存
     *
     * @param username 玩家用户名
     */
    public void remove(String username) {
        CachedPlayer player = cacheByUsername.remove(username.toLowerCase());
        if (player != null && player.getUuid() != null) {
            cacheByUuid.remove(player.getUuid());
        }
    }
    
    /**
     * 根据 UUID 移除缓存
     *
     * @param uuid 玩家 UUID
     */
    public void removeByUuid(String uuid) {
        CachedPlayer player = cacheByUuid.remove(uuid);
        if (player != null) {
            cacheByUsername.remove(player.getUsername().toLowerCase());
        }
    }
    
    /**
     * 清空所有缓存
     */
    public void clear() {
        cacheByUsername.clear();
        cacheByUuid.clear();
        TemplateMod.LOGGER.info("已清空所有缓存");
    }
    
    /**
     * 获取缓存大小
     *
     * @return 缓存中的玩家数量
     */
    public int size() {
        return cacheByUsername.size();
    }
    
    /**
     * 获取所有缓存的玩家
     *
     * @return 所有缓存的玩家集合
     */
    public Collection<CachedPlayer> getAllCached() {
        return cacheByUsername.values();
    }
    
    /**
     * 获取正版玩家数量
     *
     * @return 正版玩家数量
     */
    public long getPremiumCount() {
        return cacheByUsername.values().stream()
                .filter(CachedPlayer::isPremium)
                .filter(p -> !p.isExpired())
                .count();
    }
    
    /**
     * 获取盗版玩家数量
     *
     * @return 盗版玩家数量
     */
    public long getCrackedCount() {
        return cacheByUsername.values().stream()
                .filter(p -> !p.isPremium())
                .filter(p -> !p.isExpired())
                .count();
    }
    
    /**
     * 关闭缓存，释放资源
     */
    public void shutdown() {
        cleanupExecutor.shutdown();
        try {
            if (!cleanupExecutor.awaitTermination(5, TimeUnit.SECONDS)) {
                cleanupExecutor.shutdownNow();
            }
        } catch (InterruptedException e) {
            cleanupExecutor.shutdownNow();
            Thread.currentThread().interrupt();
        }
        clear();
        TemplateMod.LOGGER.info("PremiumCache 已关闭");
    }
    
    /**
     * 检查并缓存玩家类型
     * 如果缓存中不存在，则调用 Mojang API 查询并缓存结果
     *
     * @param username 玩家用户名
     * @return 玩家类型
     */
    public PlayerType checkAndCachePlayerType(String username) {
        CachedPlayer cached = getByUsername(username);
        if (cached != null) {
            return cached.getPlayerType();
        }
        
        MojangApiService.UserProfile profile = MojangApiService.fetchUuidByUsername(username);
        if (profile != null && profile.isValid()) {
            String formattedUuid = MojangApiService.formatUuid(profile.getId());
            putPremium(username, formattedUuid);
            return PlayerType.PREMIUM;
        } else {
            putCracked(username);
            return PlayerType.CRACKED;
        }
    }
}
