# Tasks

- [x] Task 1: 创建核心数据模型和 API 接口
  - [x] SubTask 1.1: 创建 `WalletType` Record 类
  - [x] SubTask 1.2: 创建 `WalletTypeRegistry` 钱包类型注册表
  - [x] SubTask 1.3: 创建 `PlayerAccount` 玩家账户数据类
  - [x] SubTask 1.4: 创建 `EconomyAPI` 接口定义
  - [x] SubTask 1.5: 创建异常类（InsufficientBalanceException, WalletTypeNotFoundException）

- [x] Task 2: 实现数据持久化层
  - [x] SubTask 2.1: 创建 `EconomyData` 数据模型类
  - [x] SubTask 2.2: 创建 `EconomyStateSaver` 继承 PersistentState
  - [x] SubTask 2.3: 实现 NBT 序列化/反序列化
  - [x] SubTask 2.4: 实现自动保存机制

- [x] Task 3: 实现 API 核心逻辑
  - [x] SubTask 3.1: 创建 `AccountManager` 账户管理器
  - [x] SubTask 3.2: 创建 `EconomyAPIImpl` 实现 EconomyAPI 接口
  - [x] SubTask 3.3: 实现线程安全的余额操作
  - [x] SubTask 3.4: 实现钱包类型注册/注销逻辑

- [x] Task 4: 实现事件系统
  - [x] SubTask 4.1: 创建 `TransactionType` Sealed Class
  - [x] SubTask 4.2: 创建 `TransactionRecord` Record
  - [x] SubTask 4.3: 创建 `TransactionCallback` 回调接口
  - [x] SubTask 4.4: 在 API 实现中集成事件触发

- [x] Task 5: 实现管理员命令
  - [x] SubTask 5.1: 创建 `EconAdminCommand` 类
  - [x] SubTask 5.2: 实现 `/econ-admin type add <id> <display_name>` 子命令
  - [x] SubTask 5.3: 实现 `/econ-admin type remove <id>` 子命令
  - [x] SubTask 5.4: 实现 `/econ-admin balance <player> <id> set/add/remove <amount>` 子命令
  - [x] SubTask 5.5: 实现 `/econ-admin view <player> [id]` 子命令
  - [x] SubTask 5.6: 添加权限检查（需要管理员权限）

- [x] Task 6: 实现玩家命令
  - [x] SubTask 6.1: 创建 `BalanceCommand` 类
  - [x] SubTask 6.2: 实现 `/balance [id]` 命令
  - [x] SubTask 6.3: 创建 `PayCommand` 类
  - [x] SubTask 6.4: 实现 `/pay <player> <amount> <id>` 命令
  - [x] SubTask 6.5: 添加参数验证和错误提示

- [x] Task 7: 创建模块入口和集成
  - [x] SubTask 7.1: 创建 `EconomyModule` 实现 ModInitializer
  - [x] SubTask 7.2: 注册服务器生命周期事件
  - [x] SubTask 7.3: 注册命令回调
  - [x] SubTask 7.4: 创建 mixin 配置文件（如需要）
  - [x] SubTask 7.5: 更新 fabric.mod.json 添加模块入口

- [x] Task 8: 测试和验证
  - [x] SubTask 8.1: 使用 gradle build 验证编译
  - [x] SubTask 8.2: 使用 gradle runClient 验证运行时功能
  - [x] SubTask 8.3: 测试所有管理员命令
  - [x] SubTask 8.4: 测试所有玩家命令
  - [x] SubTask 8.5: 验证数据持久化（重启后数据保留）

# Task Dependencies
- [Task 2] depends on [Task 1]
- [Task 3] depends on [Task 1, Task 2]
- [Task 4] depends on [Task 1]
- [Task 5] depends on [Task 3]
- [Task 6] depends on [Task 3]
- [Task 7] depends on [Task 3, Task 4, Task 5, Task 6]
- [Task 8] depends on [Task 7]
