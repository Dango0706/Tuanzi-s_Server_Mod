# 玩家登录验证系统 Spec

## Why
在现有白名单验证系统基础上，需要为盗版玩家添加注册/登录验证层，防止账号被盗用，并提供完善的会话管理和密码管理功能。

## What Changes
- 新增玩家注册/登录系统，盗版玩家必须注册后才能进入服务器
- 新增密码管理功能，支持管理员重置和玩家自助修改
- 新增会话管理功能，支持超时踢出和 IP 持久化
- 新增配置选项，支持自定义超时时间和 IP 会话持续时间

## Impact
- Affected specs: player_authentication（扩展现有验证流程）
- Affected code: 扩展 `me.tuanzi.auth` 包，新增登录验证相关类

## ADDED Requirements

### Requirement: 玩家注册流程
系统 SHALL 提供盗版玩家注册功能。

#### Scenario: 新玩家注册
- **WHEN** 白名单内盗版玩家首次连接服务器
- **AND** 玩家未注册
- **THEN** 系统提示玩家使用 /register 命令注册
- **AND** 玩家执行 /register <密码> <确认密码>
- **THEN** 系统验证密码格式并创建账户
- **AND** 自动完成登录

#### Scenario: 已注册玩家尝试注册
- **WHEN** 已注册玩家执行 /register 命令
- **THEN** 系统提示玩家已注册，请使用 /login 登录

### Requirement: 玩家登录流程
系统 SHALL 提供盗版玩家登录功能。

#### Scenario: 已注册玩家登录
- **WHEN** 已注册玩家连接服务器
- **AND** 玩家未在 IP 会话持久化范围内
- **THEN** 系统提示玩家使用 /login 命令登录
- **AND** 玩家执行 /login <密码>
- **THEN** 系统验证密码
- **AND** 验证成功后允许进入服务器

#### Scenario: 登录失败
- **WHEN** 玩家输入错误密码
- **THEN** 系统提示密码错误
- **AND** 记录失败尝试次数

### Requirement: 登录超时踢出
系统 SHALL 对未在规定时间内完成登录的玩家执行踢出操作。

#### Scenario: 超时踢出
- **WHEN** 玩家连接后未在配置时间内完成注册或登录
- **THEN** 系统自动踢出该玩家
- **AND** 显示超时提示信息

### Requirement: IP 会话持久化
系统 SHALL 支持基于 IP 的会话持久化。

#### Scenario: IP 会话持久化
- **WHEN** 玩家从相同 IP 地址再次连接
- **AND** 距离上次登录在配置时间内
- **THEN** 系统自动完成登录
- **AND** 跳过密码验证

#### Scenario: IP 变化要求重新登录
- **WHEN** 玩家从不同 IP 地址连接
- **THEN** 系统要求玩家重新输入密码登录

### Requirement: 密码管理
系统 SHALL 提供密码管理功能。

#### Scenario: 玩家自助修改密码
- **WHEN** 已登录玩家执行 /changepassword <旧密码> <新密码>
- **AND** 旧密码验证正确
- **THEN** 系统更新密码
- **AND** 提示修改成功

#### Scenario: 管理员重置密码
- **WHEN** 管理员执行 /auth password reset <玩家名> <新密码>
- **THEN** 系统重置指定玩家的密码
- **AND** 记录操作日志

### Requirement: 登录尝试限制
系统 SHALL 实现登录尝试频率限制。

#### Scenario: 频率限制
- **WHEN** 玩家连续多次登录失败
- **AND** 失败次数超过配置阈值
- **THEN** 系统暂时禁止该玩家登录尝试
- **AND** 显示等待时间提示

### Requirement: 配置管理
系统 SHALL 提供可配置的参数。

#### Scenario: 配置项
- 登录超时时间（秒）
- IP 会话持久化时间（秒）
- 最大登录尝试次数
- 登录失败锁定时间（秒）

## 技术规范

### 数据存储格式
```json
{
  "players": {
    "playerName": {
      "passwordHash": "bcrypt_hash",
      "salt": "random_salt",
      "registerTime": 1234567890,
      "lastLoginTime": 1234567890,
      "lastLoginIp": "127.0.0.1",
      "failedAttempts": 0,
      "lockedUntil": 0
    }
  }
}
```

### 配置文件格式
```json
{
  "loginTimeoutSeconds": 60,
  "ipSessionPersistenceSeconds": 3600,
  "maxLoginAttempts": 5,
  "lockoutDurationSeconds": 300,
  "minPasswordLength": 6,
  "maxPasswordLength": 32
}
```

### 密码安全
- 使用 BCrypt 算法加密存储
- 密码长度限制：6-32 字符
- 禁止常见弱密码

### 日志格式
```
[时间戳] [级别] [LoginAuth] 玩家名 | 操作类型 | 结果 | IP地址
```

## 约束
- Minecraft 版本：26.1
- Java 版本：25
- Fabric Loader：0.18.4
- Fabric API：0.144.3+26.1
- 仅适用于盗版玩家（正版玩家跳过此验证）

## 验收标准

### AC-1：新玩家注册流程
- **给定**：白名单内盗版玩家首次连接
- **当**：执行 /register 命令
- **然后**：成功注册并自动登录

### AC-2：已注册玩家登录
- **给定**：已注册玩家连接服务器
- **当**：执行 /login 命令
- **然后**：验证成功后进入服务器

### AC-3：超时踢出
- **给定**：玩家未在配置时间内完成登录
- **当**：超时时间到达
- **然后**：玩家被踢出

### AC-4：IP 会话持久化
- **给定**：玩家从相同 IP 再次连接
- **当**：在配置时间内
- **然后**：自动登录

### AC-5：密码修改
- **给定**：玩家已登录
- **当**：执行 /changepassword 命令
- **然后**：密码成功更新

### AC-6：登录尝试限制
- **给定**：玩家多次登录失败
- **当**：超过阈值
- **然后**：临时锁定登录
