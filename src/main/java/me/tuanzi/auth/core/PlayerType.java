package me.tuanzi.auth.core;

/**
 * 玩家类型枚举
 * 用于区分正版玩家和盗版玩家
 */
public enum PlayerType {
    /**
     * 正版玩家 - 拥有 Mojang 账户
     */
    PREMIUM,
    
    /**
     * 盗版玩家 - 离线模式玩家
     */
    CRACKED
}
