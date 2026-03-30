# Tasks

- [x] Task 1: 创建登录验证模块基础架构
  - [x] SubTask 1.1: 创建 `me.tuanzi.auth.login` 包结构
  - [x] SubTask 1.2: 创建 `LoginConfig` 配置类，支持登录相关配置
  - [x] SubTask 1.3: 创建 `PlayerAccount` 数据类，存储玩家账户信息

- [x] Task 2: 实现账户数据管理
  - [x] SubTask 2.1: 创建 `AccountManager` 类，管理玩家账户数据
  - [x] SubTask 2.2: 实现 `loadAccounts()` 方法，从 JSON 文件加载账户数据
  - [x] SubTask 2.3: 实现 `saveAccounts()` 方法，持久化账户数据到 JSON 文件
  - [x] SubTask 2.4: 实现 `registerPlayer(String playerName, String password)` 方法
  - [x] SubTask 2.5: 实现 `isRegistered(String playerName)` 方法
  - [x] SubTask 2.6: 实现 `verifyPassword(String playerName, String password)` 方法
  - [x] SubTask 2.7: 实现 `changePassword(String playerName, String oldPassword, String newPassword)` 方法
  - [x] SubTask 2.8: 实现 `resetPassword(String playerName, String newPassword)` 方法

- [x] Task 3: 实现密码加密服务
  - [x] SubTask 3.1: 创建 `PasswordService` 类
  - [x] SubTask 3.2: 实现 `hashPassword(String password)` 方法，使用 SHA-256 + 盐值加密
  - [x] SubTask 3.3: 实现 `verifyPassword(String plainPassword, String hashedPassword)` 方法
  - [x] SubTask 3.4: 实现 `validatePasswordStrength(String password)` 方法

- [x] Task 4: 实现会话管理
  - [x] SubTask 4.1: 创建 `LoginSession` 类，存储会话状态
  - [x] SubTask 4.2: 创建 `SessionManager` 类，管理玩家会话
  - [x] SubTask 4.3: 实现 `createSession(String playerName, String ipAddress)` 方法
  - [x] SubTask 4.4: 实现 `validateSession(String playerName, String ipAddress)` 方法
  - [x] SubTask 4.5: 实现 `invalidateSession(String playerName)` 方法
  - [x] SubTask 4.6: 实现 IP 会话持久化检查逻辑
  - [x] SubTask 4.7: 实现会话过期清理机制

- [x] Task 5: 实现登录超时管理
  - [x] SubTask 5.1: 创建 `LoginTimeoutManager` 类
  - [x] SubTask 5.2: 实现 `startLoginTimer(String playerName)` 方法
  - [x] SubTask 5.3: 实现超时自动踢出逻辑
  - [x] SubTask 5.4: 实现 `cancelLoginTimer(String playerName)` 方法

- [x] Task 6: 实现登录尝试限制
  - [x] SubTask 6.1: 创建 `LoginAttemptManager` 类
  - [x] SubTask 6.2: 实现 `recordFailedAttempt(String playerName)` 方法
  - [x] SubTask 6.3: 实现 `isLocked(String playerName)` 方法
  - [x] SubTask 6.4: 实现 `getRemainingLockTime(String playerName)` 方法
  - [x] SubTask 6.5: 实现 `resetAttempts(String playerName)` 方法

- [x] Task 7: 实现玩家注册命令
  - [x] SubTask 7.1: 创建 `RegisterCommand` 类
  - [x] SubTask 7.2: 实现 `/register <密码> <确认密码>` 命令
  - [x] SubTask 7.3: 实现密码格式验证
  - [x] SubTask 7.4: 实现注册成功后自动登录

- [x] Task 8: 实现玩家登录命令
  - [x] SubTask 8.1: 创建 `LoginCommand` 类
  - [x] SubTask 8.2: 实现 `/login <密码>` 命令
  - [x] SubTask 8.3: 实现登录成功后的处理逻辑
  - [x] SubTask 8.4: 实现登录失败处理和提示

- [x] Task 9: 实现密码管理命令
  - [x] SubTask 9.1: 创建 `ChangePasswordCommand` 类
  - [x] SubTask 9.2: 实现 `/changepassword <旧密码> <新密码>` 命令
  - [x] SubTask 9.3: 创建管理员密码重置命令 `/auth password reset <玩家名> <新密码>`

- [x] Task 10: 实现登录状态拦截
  - [x] SubTask 10.1: 创建 `PlayerJoinListener` 类监听玩家加入事件
  - [x] SubTask 10.2: 实现正版玩家跳过登录验证
  - [x] SubTask 10.3: 实现盗版玩家登录状态检查
  - [x] SubTask 10.4: 实现未登录玩家限制（禁止移动、交互等）
  - [x] SubTask 10.5: 实现登录提示消息显示

- [x] Task 11: 实现配置文件系统
  - [x] SubTask 11.1: 扩展 `AuthConfig` 添加登录相关配置项
  - [x] SubTask 11.2: 实现配置文件加载和保存
  - [x] SubTask 11.3: 实现配置热重载功能

- [x] Task 12: 实现日志记录
  - [x] SubTask 12.1: 扩展 `AuthLogger` 添加登录相关日志
  - [x] SubTask 12.2: 实现注册事件日志
  - [x] SubTask 12.3: 实现登录事件日志（成功/失败）
  - [x] SubTask 12.4: 实现密码修改日志
  - [x] SubTask 12.5: 实现管理员操作日志

- [x] Task 13: 模块集成与测试
  - [x] SubTask 13.1: 在 `AuthModule` 中注册登录相关组件
  - [x] SubTask 13.2: 在 `AuthCommand` 中集成密码管理命令
  - [x] SubTask 13.3: 执行 `gradle build` 验证编译通过
  - [x] SubTask 13.4: 执行 `gradle runClient` 验证功能正常

# Task Dependencies
- [Task 2] depends on [Task 1, Task 3]
- [Task 4] depends on [Task 1]
- [Task 5] depends on [Task 1]
- [Task 6] depends on [Task 1]
- [Task 7] depends on [Task 2, Task 3, Task 5]
- [Task 8] depends on [Task 2, Task 3, Task 5, Task 6]
- [Task 9] depends on [Task 2, Task 3]
- [Task 10] depends on [Task 4, Task 5]
- [Task 11] depends on [Task 1]
- [Task 12] depends on [Task 1]
- [Task 13] depends on [Task 7, Task 8, Task 9, Task 10, Task 11, Task 12]
