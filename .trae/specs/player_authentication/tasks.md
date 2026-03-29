# Tasks

- [x] Task 1: 创建身份验证模块基础架构
  - [x] SubTask 1.1: 创建 `me.tuanzi.auth` 包结构
  - [x] SubTask 1.2: 创建 `AuthModule` 主类，实现 ModInitializer 接口
  - [x] SubTask 1.3: 创建 `AuthConfig` 配置类，支持 JSON 配置文件加载

- [x] Task 2: 实现白名单数据管理
  - [x] SubTask 2.1: 创建 `WhitelistManager` 类，使用 HashMap<UUID, String> 存储白名单
  - [x] SubTask 2.2: 创建 `OfflineUUIDGenerator` 工具类，实现盗版 UUID 生成算法
  - [x] SubTask 2.3: 实现 `generateOfflineUUID(String playerName)` 方法
  - [x] SubTask 2.4: 实现 `loadWhitelist()` 方法，从 JSON 文件加载白名单
  - [x] SubTask 2.5: 实现 `saveWhitelist()` 方法，持久化白名单到 JSON 文件
  - [x] SubTask 2.6: 实现 `addToWhitelist(String playerName)` 方法，自动生成 UUID
  - [x] SubTask 2.7: 实现 `removeFromWhitelist(String playerName)` 方法
  - [x] SubTask 2.8: 实现 `isInWhitelist(UUID playerUUID)` 方法，O(1) 查询
  - [x] SubTask 2.9: 实现 `getWhitelistEntries()` 方法，获取所有白名单条目

- [x] Task 3: 实现混合验证核心逻辑
  - [x] SubTask 3.1: 创建 `PlayerType` 枚举，定义 PREMIUM（正版）和 CRACKED（盗版）
  - [x] SubTask 3.2: 创建 `MojangApiService` 类，封装 Mojang API 调用
  - [x] SubTask 3.3: 实现 `fetchPremiumUuid(String playerName)` 方法，查询玩家正版 UUID
  - [x] SubTask 3.4: 实现 `verifySession(String playerName, String serverId)` 方法，验证会话
  - [x] SubTask 3.5: 创建 `PremiumCache` 类，缓存正版玩家验证结果
  - [x] SubTask 3.6: 实现缓存过期清理机制

- [x] Task 4: 实现 ServerLoginNetworkHandler Mixin
  - [x] SubTask 4.1: 创建 `ServerLoginNetworkHandlerMixin` 类
  - [x] SubTask 4.2: 注入登录流程，拦截玩家连接
  - [x] SubTask 4.3: 实现强制发送加密请求逻辑
  - [x] SubTask 4.4: 实现正版验证成功后的处理逻辑
  - [x] SubTask 4.5: 实现验证失败后的降级处理逻辑
  - [x] SubTask 4.6: 实现盗版玩家白名单检查（基于 UUID）
  - [x] SubTask 4.7: 实现非白名单玩家踢出逻辑
  - [x] SubTask 4.8: 在 mixin 配置文件中注册 Mixin

- [x] Task 5: 实现异步验证处理
  - [x] SubTask 5.1: 创建 `AsyncVerificationTask` 类，实现 Runnable 接口
  - [x] SubTask 5.2: 使用 CompletableFuture 处理异步验证结果
  - [x] SubTask 5.3: 实现验证超时处理机制
  - [x] SubTask 5.4: 确保验证结果正确回调到主线程

- [x] Task 6: 实现白名单管理指令
  - [x] SubTask 6.1: 创建 `AuthCommand` 类
  - [x] SubTask 6.2: 实现 `/auth whitelist add <玩家名>` 指令，自动生成盗版 UUID
  - [x] SubTask 6.3: 实现 `/auth whitelist remove <玩家名>` 指令
  - [x] SubTask 6.4: 实现 `/auth whitelist list` 指令，显示玩家名和 UUID
  - [x] SubTask 6.5: 实现 `/auth reload` 指令，重载配置和白名单
  - [x] SubTask 6.6: 添加权限检查，仅管理员可执行指令

- [x] Task 7: 实现分级日志系统
  - [x] SubTask 7.1: 创建 `AuthLogger` 类
  - [x] SubTask 7.2: 实现 INFO 级别日志记录（成功登录）
  - [x] SubTask 7.3: 实现 WARN 级别日志记录（拒绝登录）
  - [x] SubTask 7.4: 实现 ERROR 级别日志记录（验证异常）
  - [x] SubTask 7.5: 实现日志文件按日轮转机制
  - [x] SubTask 7.6: 实现日志文件自动清理（保留 30 天）

- [x] Task 8: 实现配置文件系统
  - [x] SubTask 8.1: 创建默认配置文件模板
  - [x] SubTask 8.2: 实现配置文件加载逻辑
  - [x] SubTask 8.3: 实现配置文件热重载功能
  - [x] SubTask 8.4: 支持自定义踢出提示信息配置

- [x] Task 9: 模块集成与测试
  - [x] SubTask 9.1: 在 `AuthModule` 中注册所有事件监听器
  - [x] SubTask 9.2: 在 `AuthModule` 中注册所有指令
  - [x] SubTask 9.3: 实现服务器生命周期事件处理（启动加载、关闭保存）
  - [x] SubTask 9.4: 执行 `gradle build` 验证编译通过
  - [x] SubTask 9.5: 执行 `gradle runClient` 验证功能正常

# Task Dependencies
- [Task 2] depends on [Task 1]
- [Task 3] depends on [Task 1]
- [Task 4] depends on [Task 2, Task 3]
- [Task 5] depends on [Task 3]
- [Task 6] depends on [Task 2]
- [Task 7] depends on [Task 1]
- [Task 8] depends on [Task 1]
- [Task 9] depends on [Task 4, Task 5, Task 6, Task 7, Task 8]
