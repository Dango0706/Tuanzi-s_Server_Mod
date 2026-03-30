# Economy API 模块检查清单

## 核心功能检查

- [x] WalletType Record 正确定义，包含 id 和 displayName 字段
- [x] WalletTypeRegistry 支持注册、注销和查询钱包类型
- [x] PlayerAccount 正确存储玩家的多种钱包余额
- [x] EconomyAPI 接口定义完整，包含所有必需方法

## 数据持久化检查

- [x] EconomyStateSaver 正确继承 SavedData (PersistentState 在 MC 26.1 中已重命名)
- [x] NBT 序列化正确保存钱包类型数据
- [x] NBT 序列化正确保存玩家账户数据
- [x] 数据在服务器重启后正确恢复
- [x] 自动保存机制正常工作

## API 实现检查

- [x] EconomyAPIImpl 正确实现所有接口方法
- [x] getBalance 返回正确余额，无记录时返回 0.0
- [x] deposit 正确增加余额并触发事件
- [x] withdraw 余额不足时抛出 InsufficientBalanceException
- [x] withdraw 成功时正确扣除余额并触发事件
- [x] setBalance 正确设置余额
- [x] transfer 正确完成转账操作
- [x] API 操作是线程安全的

## 事件系统检查

- [x] TransactionCallback 接口正确定义
- [x] 交易事件在存款时正确触发
- [x] 交易事件在取款时正确触发
- [x] 交易事件在转账时正确触发
- [x] 事件包含完整的交易信息

## 管理员命令检查

- [x] `/econ-admin type add <id> <display_name>` 正确创建钱包类型
- [x] `/econ-admin type remove <id>` 正确删除钱包类型
- [x] `/econ-admin balance <player> <id> set <amount>` 正确设置余额
- [x] `/econ-admin balance <player> <id> add <amount>` 正确增加余额
- [x] `/econ-admin balance <player> <id> remove <amount>` 正确减少余额
- [x] `/econ-admin view <player> [id]` 正确显示余额信息
- [x] 管理员命令需要适当的权限级别

## 玩家命令检查

- [x] `/balance [id]` 正确显示玩家个人余额
- [x] `/balance` 无参数时显示所有钱包类型余额
- [x] `/pay <player> <amount> <id>` 正确完成转账
- [x] `/pay` 余额不足时显示错误消息
- [x] `/pay` 目标玩家不存在时显示错误消息

## 模块集成检查

- [x] EconomyModule 正确实现 ModInitializer
- [x] 模块在服务器启动时正确初始化
- [x] 模块在服务器关闭时正确保存数据
- [x] 命令正确注册到 Brigadier
- [x] fabric.mod.json 包含正确的模块入口

## 构建验证检查

- [x] gradle build 成功完成，无编译错误
- [x] gradle runClient 成功启动游戏
- [x] 模组在游戏中正确加载
- [x] 无运行时异常或警告
