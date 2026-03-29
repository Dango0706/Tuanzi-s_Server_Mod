package me.tuanzi.auth.core;

/**
 * 缓存的玩家信息
 * 用于存储玩家的验证状态和相关信息
 */
public class CachedPlayer {
    private final String username;
    private final String uuid;
    private final PlayerType playerType;
    private final long cacheTime;
    private final long expireTime;
    
    /**
     * 创建缓存玩家信息
     *
     * @param username   玩家用户名
     * @param uuid       玩家 UUID（正版玩家为 Mojang UUID，盗版玩家为离线 UUID）
     * @param playerType 玩家类型
     * @param cacheTime  缓存时间（毫秒时间戳）
     * @param ttl        缓存有效期（毫秒）
     */
    public CachedPlayer(String username, String uuid, PlayerType playerType, long cacheTime, long ttl) {
        this.username = username;
        this.uuid = uuid;
        this.playerType = playerType;
        this.cacheTime = cacheTime;
        this.expireTime = cacheTime + ttl;
    }
    
    /**
     * 获取玩家用户名
     *
     * @return 玩家用户名
     */
    public String getUsername() {
        return username;
    }
    
    /**
     * 获取玩家 UUID
     *
     * @return 玩家 UUID
     */
    public String getUuid() {
        return uuid;
    }
    
    /**
     * 获取玩家类型
     *
     * @return 玩家类型
     */
    public PlayerType getPlayerType() {
        return playerType;
    }
    
    /**
     * 获取缓存时间
     *
     * @return 缓存时间（毫秒时间戳）
     */
    public long getCacheTime() {
        return cacheTime;
    }
    
    /**
     * 获取过期时间
     *
     * @return 过期时间（毫秒时间戳）
     */
    public long getExpireTime() {
        return expireTime;
    }
    
    /**
     * 检查缓存是否已过期
     *
     * @return 如果缓存已过期返回 true，否则返回 false
     */
    public boolean isExpired() {
        return System.currentTimeMillis() > expireTime;
    }
    
    /**
     * 检查玩家是否为正版玩家
     *
     * @return 如果是正版玩家返回 true，否则返回 false
     */
    public boolean isPremium() {
        return playerType == PlayerType.PREMIUM;
    }
    
    @Override
    public String toString() {
        return "CachedPlayer{" +
                "username='" + username + '\'' +
                ", uuid='" + uuid + '\'' +
                ", playerType=" + playerType +
                ", cacheTime=" + cacheTime +
                ", expireTime=" + expireTime +
                '}';
    }
}
