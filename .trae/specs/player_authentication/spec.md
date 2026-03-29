# 玩家身份验证模块 Spec

## Why
在盗版模式(online_mode=false)服务器中，需要一套安全可靠的身份验证系统来控制服务器访问权限，区分正版与盗版玩家，并通过白名单机制管理盗版玩家的访问权限。

## What Changes
- 新增玩家身份验证模块，实现混合验证(Hybrid Authentication)机制
- 新增白名单管理系统，支持管理员实时配置
- 新增分级日志系统，记录所有登录验证事件
- 新增配置文件系统，支持自定义踢出提示信息
- 新增管理员指令集，用于白名单管理

## Impact
- Affected specs: 无（独立模块）
- Affected code: 新增 `me.tuanzi.auth` 包及其子包

## 核心实现原理：混合验证 (Hybrid Authentication)

### 验证流程概述
1. **拦截登录过程**：通过 Mixin 注入 `ServerLoginNetworkHandler`，在玩家连接时拦截登录流程
2. **强制加密请求**：无论服务器 `online-mode` 设置如何，模组强制向客户端发送加密请求
3. **处理响应**：
   - **正版客户端**：能够正确响应加密请求，并与 Mojang 服务器进行握手验证
   - **盗版客户端**：无法响应加密请求（或由于未登录 Launcher 导致验证失败）
4. **降级处理**：如果 Mojang 验证失败，模组捕获异常，允许该连接以"离线模式"继续，然后检查白名单

### 技术实现要点
- 使用 Mixin 注入 `ServerLoginNetworkHandler` 的登录流程
- 调用 Mojang Session Server API 验证玩家身份
- 异步处理网络请求，避免阻塞主线程
- 缓存验证结果以提高性能

## ADDED Requirements

### Requirement: 混合验证机制
系统 SHALL 在玩家连接时执行混合验证流程。

#### Scenario: 正版玩家验证成功
- **WHEN** 玩家连接服务器
- **AND** 模组发送加密请求
- **AND** 客户端正确响应加密请求
- **AND** Mojang 服务器验证通过
- **THEN** 系统识别该玩家为正版玩家
- **AND** 自动授予游戏进入权限
- **AND** 记录正版玩家登录日志

#### Scenario: 盗版玩家验证失败降级
- **WHEN** 玩家连接服务器
- **AND** 模组发送加密请求
- **AND** 客户端无法响应加密请求或 Mojang 验证失败
- **THEN** 系统识别该玩家为盗版玩家
- **AND** 触发白名单验证流程

### Requirement: 白名单验证
系统 SHALL 对盗版玩家执行白名单验证。

#### Scenario: 白名单玩家通过验证
- **WHEN** 盗版玩家连接服务器
- **AND** 玩家的盗版 UUID 存在于白名单中
- **THEN** 授予游戏进入权限
- **AND** 记录白名单玩家登录日志

#### Scenario: 非白名单玩家被拒绝
- **WHEN** 盗版玩家连接服务器
- **AND** 玩家的盗版 UUID 不存在于白名单中
- **THEN** 立即踢出该玩家
- **AND** 显示管理员配置的自定义提示信息
- **AND** 记录拒绝登录日志

### Requirement: 白名单管理指令
系统 SHALL 提供管理员专用白名单管理指令。

#### Scenario: 添加白名单
- **WHEN** 管理员执行 `/auth whitelist add <玩家名>`
- **THEN** 根据玩家名生成盗版 UUID
- **AND** 将玩家名和盗版 UUID 添加到白名单
- **AND** 持久化保存白名单数据
- **AND** 显示操作成功提示

#### Scenario: 删除白名单
- **WHEN** 管理员执行 `/auth whitelist remove <玩家名>`
- **THEN** 根据玩家名生成盗版 UUID
- **AND** 从白名单移除对应的玩家记录
- **AND** 持久化保存白名单数据
- **AND** 显示操作成功提示

#### Scenario: 查询白名单
- **WHEN** 管理员执行 `/auth whitelist list`
- **THEN** 显示当前白名单中所有玩家名及其 UUID

#### Scenario: 重载配置
- **WHEN** 管理员执行 `/auth reload`
- **THEN** 重新加载配置文件和白名单数据

### Requirement: 异步验证处理
系统 SHALL 采用异步处理机制执行身份验证逻辑。

#### Scenario: 异步验证不阻塞主线程
- **WHEN** 玩家连接触发身份验证
- **THEN** Mojang API 请求在独立线程中执行
- **AND** 单次验证处理时间不超过 100ms（不含网络延迟）

#### Scenario: 验证结果缓存
- **WHEN** 玩家验证完成
- **THEN** 缓存验证结果
- **AND** 后续连接使用缓存结果

### Requirement: 白名单数据存储
系统 SHALL 使用高效数据结构存储白名单。

#### Scenario: 内存缓存与持久化
- **WHEN** 白名单数据被修改
- **THEN** 内存中使用 HashMap 存储（UUID -> 玩家名，O(1) 查询复杂度）
- **AND** 同步持久化到 JSON 文件

#### Scenario: 服务器启动加载白名单
- **WHEN** 服务器启动
- **THEN** 自动从 JSON 文件加载白名单数据到内存

#### Scenario: 盗版 UUID 生成
- **WHEN** 添加玩家到白名单
- **THEN** 使用 MD5 算法基于 "OfflinePlayer:" + 玩家名 生成 UUID
- **AND** 生成的 UUID 版本为 3（name-based）

### Requirement: 配置文件管理
系统 SHALL 支持通过配置文件自定义踢出提示信息。

#### Scenario: 自定义踢出提示
- **WHEN** 管理员修改配置文件中的踢出提示信息
- **THEN** 后续被拒绝的玩家将看到新的提示信息

### Requirement: 分级日志系统
系统 SHALL 实现分级日志记录机制。

#### Scenario: INFO 级别日志
- **WHEN** 玩家成功登录
- **THEN** 记录 INFO 级别日志
- **AND** 包含时间戳、玩家ID、登录类型、验证结果

#### Scenario: WARN 级别日志
- **WHEN** 盗版玩家被拒绝登录
- **THEN** 记录 WARN 级别日志
- **AND** 包含玩家名、拒绝原因

#### Scenario: ERROR 级别日志
- **WHEN** 验证过程发生异常
- **THEN** 记录 ERROR 级别日志
- **AND** 包含异常详细信息

#### Scenario: 日志文件轮转
- **WHEN** 日志记录时
- **THEN** 按日期生成日志文件
- **AND** 保留最近 30 天的历史记录

### Requirement: 模块兼容性
系统 SHALL 与现有服务器架构保持松耦合设计。

#### Scenario: 模块独立运行
- **WHEN** 模组加载
- **THEN** 身份验证模块独立初始化
- **AND** 不影响其他模块的正常运行

## 技术规范

### Mixin 注入点
- 目标类：`net.minecraft.server.network.ServerLoginNetworkHandler`
- 注入方法：登录流程开始时的加密请求发送

### Mojang API 调用
- UUID 查询：`https://api.mojang.com/users/profiles/minecraft/<username>`
- Session 验证：`https://sessionserver.mojang.com/session/minecraft/hasJoined`

### 白名单数据结构
```java
// 内存存储：UUID -> 玩家名
Map<UUID, String> whitelist = new HashMap<>();

// 盗版 UUID 生成算法
public static UUID generateOfflineUUID(String playerName) {
    byte[] md5 = MD5.hash(("OfflinePlayer:" + playerName).getBytes(StandardCharsets.UTF_8));
    // 设置版本为 3 (name-based)
    md5[6] &= 0x0f;
    md5[6] |= 0x30;
    // 设置变体
    md5[8] &= 0x3f;
    md5[8] |= 0x80;
    return UUID.nameUUIDFromBytes(md5);
}

// JSON 文件格式
{
  "whitelist": [
    {
      "name": "player1",
      "uuid": "xxxxxxxx-xxxx-3xxx-xxxx-xxxxxxxxxxxx"
    },
    {
      "name": "player2",
      "uuid": "yyyyyyyy-yyyy-3yyy-yyyy-yyyyyyyyyyyy"
    }
  ]
}
```

### 配置文件格式
```json
{
  "kickMessage": "§c您不在服务器白名单中，请联系管理员申请访问权限。",
  "enableAuthLog": true,
  "logRetentionDays": 30,
  "cacheExpiryMinutes": 30,
  "enablePremiumCache": true
}
```

### 日志格式
```
[时间戳] [级别] [AuthModule] 玩家名 | 登录类型 | 验证结果 | 处理动作
```

## 约束
- Minecraft 版本：26.1
- Java 版本：25
- Fabric Loader：0.18.4
- Fabric API：0.144.3+26.1
- 服务器模式：online_mode=false

## 验收标准

### AC-1：正版玩家自动通过
- **给定**：正版玩家连接服务器
- **当**：混合验证流程执行
- **然后**：玩家自动获得游戏进入权限
- **验证**：程序化测试

### AC-2：白名单玩家通过验证
- **给定**：盗版玩家在白名单中
- **当**：混合验证流程执行并降级到白名单检查
- **然后**：玩家获得游戏进入权限
- **验证**：程序化测试

### AC-3：非白名单玩家被踢出
- **给定**：盗版玩家不在白名单中
- **当**：混合验证流程执行并降级到白名单检查
- **然后**：玩家被踢出并看到自定义提示
- **验证**：人工验证

### AC-4：白名单管理指令正常工作
- **给定**：管理员执行白名单管理指令
- **当**：指令正确执行
- **然后**：白名单数据正确更新并持久化
- **验证**：人工验证

### AC-5：验证性能达标
- **给定**：玩家连接触发验证
- **当**：验证流程执行
- **然后**：本地处理时间不超过 100ms
- **验证**：性能测试

### AC-6：日志正确记录
- **给定**：玩家登录事件发生
- **当**：验证流程完成
- **然后**：日志文件包含完整记录
- **验证**：程序化测试
