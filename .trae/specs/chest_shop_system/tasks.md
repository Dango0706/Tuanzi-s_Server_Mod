# Tasks

- [ ] Task 1: 创建核心数据模型
  - [ ] SubTask 1.1: 创建 `ShopType` 枚举类（SELL, BUY）
  - [ ] SubTask 1.2: 创建 `ShopInstance` 商店实例数据类（包含 walletTypeId 字段）
  - [ ] SubTask 1.3: 创建 `ShopData` 商店数据模型类
  - [ ] SubTask 1.4: 创建 `ShopConfig` 配置类

- [ ] Task 2: 实现数据持久化层
  - [ ] SubTask 2.1: 创建 `ShopStateSaver` 继承 SavedData
  - [ ] SubTask 2.2: 实现 NBT 序列化/反序列化（包含 ItemStack 的 DataComponent）
  - [ ] SubTask 2.3: 创建 `ShopRegistry` 商店注册表（坐标索引）
  - [ ] SubTask 2.4: 实现高效的坐标到商店的映射查找

- [ ] Task 3: 实现商店管理核心逻辑
  - [ ] SubTask 3.1: 创建 `ShopManager` 商店管理器
  - [ ] SubTask 3.2: 实现商店创建逻辑（解析告示牌、验证物品、验证货币显示名称）
  - [ ] SubTask 3.3: 实现商店删除逻辑
  - [ ] SubTask 3.4: 实现商店查询逻辑（按坐标、按ID、按所有者）
  - [ ] SubTask 3.5: 实现获取玩家面对/指向的商店逻辑
  - [ ] SubTask 3.6: 集成 Economy API 进行货币操作（使用指定的 walletTypeId）
  - [ ] SubTask 3.7: 实现根据货币显示名称查找货币ID的逻辑

- [ ] Task 4: 实现告示牌事件处理
  - [ ] SubTask 4.1: 创建 `SignChangeHandler` 监听告示牌放置事件
  - [ ] SubTask 4.2: 实现告示牌模式匹配（本地化支持）
  - [ ] SubTask 4.3: 验证告示牌是否附着在箱子上
  - [ ] SubTask 4.4: 解析物品ID/名称并获取 ItemStack
  - [ ] SubTask 4.5: 解析第三行的价格和货币显示名称
  - [ ] SubTask 4.6: 验证货币显示名称是否存在
  - [ ] SubTask 4.7: 创建商店实例并发送确认消息
  - [ ] SubTask 4.8: 实现告示牌自动涂蜡功能

- [ ] Task 5: 实现交易系统
  - [ ] SubTask 5.1: 创建 `BlockInteractionHandler` 处理方块交互（支持左键和右键）
  - [ ] SubTask 5.2: 实现聊天消息交互界面（ClickEvent/HoverEvent）
  - [ ] SubTask 5.3: 实现出售商店交易逻辑（玩家购买，使用指定货币）
  - [ ] SubTask 5.4: 实现收购商店交易逻辑（玩家出售，检查所有者余额）
  - [ ] SubTask 5.5: 实现收购者余额不足时向双方发送提示（显示货币 displayName）
  - [ ] SubTask 5.6: 实现 Shift+左键/右键批量交易（64个物品）
  - [ ] SubTask 5.7: 创建 `ShopTransactionEvent` 交易事件
  - [ ] SubTask 5.8: 所有消息显示货币 displayName 而非 ID

- [ ] Task 6: 实现所有权保护
  - [ ] SubTask 6.1: 创建 `BlockProtectionHandler` 处理方块保护
  - [ ] SubTask 6.2: 拦截非所有者打开商店箱子
  - [ ] SubTask 6.3: 拦截非所有者破坏商店方块
  - [ ] SubTask 6.4: 实现爆炸保护（Mixin 或事件监听）
  - [ ] SubTask 6.5: 实现活塞保护（Mixin 或事件监听）

- [ ] Task 7: 实现动态定价算法
  - [ ] SubTask 7.1: 创建 `DynamicPricing` 类
  - [ ] SubTask 7.2: 实现供需比例计算：`Price = BasePrice * (TotalSold / TotalBought)`
  - [ ] SubTask 7.3: 实现价格边界限制（minPrice, maxPrice）
  - [ ] SubTask 7.4: 实现除零保护和默认值处理
  - [ ] SubTask 7.5: 在交易完成后更新统计数据

- [ ] Task 8: 实现管理员商店功能
  - [ ] SubTask 8.1: 创建 `ShopAdminCommand` 管理员命令类（基于玩家面对的商店）
  - [ ] SubTask 8.2: 实现 `/shopadmin create` 创建管理员商店
  - [ ] SubTask 8.3: 实现 `/shopadmin set-infinite <true/false>` 设置无限模式（基于面对的商店）
  - [ ] SubTask 8.4: 实现 `/shopadmin dynamic-toggle` 切换动态定价（基于面对的商店）
  - [ ] SubTask 8.5: 实现 `/shopadmin delete` 删除商店（二次确认机制）
  - [ ] SubTask 8.6: 实现 `/shopadmin info` 查看商店信息（基于面对的商店，显示货币 displayName）
  - [ ] SubTask 8.7: 实现 `/shopadmin set-price <价格>` 设置价格（基于面对的商店）
  - [ ] SubTask 8.8: 实现 `/shopadmin set-currency <货币显示名称>` 设置货币类型（基于面对的商店）
  - [ ] SubTask 8.9: 添加权限检查（需要管理员权限）
  - [ ] SubTask 8.10: 实现删除确认超时机制（30秒）

- [ ] Task 9: 实现可视化展示系统
  - [ ] SubTask 9.1: 创建 `ShopDisplayManager` 管理 ItemDisplayEntity
  - [ ] SubTask 9.2: 在商店创建时召唤 ItemDisplayEntity
  - [ ] SubTask 9.3: 设置实体位置（箱子上方悬浮）
  - [ ] SubTask 9.4: 设置实体显示的交易物品
  - [ ] SubTask 9.5: 在商店删除时移除实体
  - [ ] SubTask 9.6: 在服务器重启时重建所有展示实体

- [ ] Task 10: 实现交易日志系统
  - [ ] SubTask 10.1: 创建 `TransactionLogger` 类
  - [ ] SubTask 10.2: 实现日志文件写入（`shop_transactions.log`）
  - [ ] SubTask 10.3: 定义日志格式：`[时间戳] [商店ID] 玩家:名 | 类型:买/卖 | 物品:名 | 数量:N | 金额:N 货币显示名称 | 卖方:xxx | 交易箱id:xxx,位置:x,y,z`
  - [ ] SubTask 10.4: 在交易完成后记录日志
  - [ ] SubTask 10.5: 实现日志文件轮转（可选）

- [ ] Task 11: 实现物品NBT支持
  - [ ] SubTask 11.1: 创建 `ItemUtils` 工具类
  - [ ] SubTask 11.2: 实现物品序列化（保留所有 DataComponent）
  - [ ] SubTask 11.3: 实现物品反序列化
  - [ ] SubTask 11.4: 实现物品精确匹配（包含 NBT/DataComponent）
  - [ ] SubTask 11.5: 测试附魔物品、耐久度物品、自定义NBT物品

- [ ] Task 12: 创建模块入口和集成
  - [ ] SubTask 12.1: 创建 `ShopModule` 实现 ModInitializer
  - [ ] SubTask 12.2: 注册服务器生命周期事件
  - [ ] SubTask 12.3: 注册命令回调
  - [ ] SubTask 12.4: 创建必要的 Mixin 配置
  - [ ] SubTask 12.5: 更新 fabric.mod.json 添加模块入口
  - [ ] SubTask 12.6: 创建翻译文件（本地化支持）

- [ ] Task 13: 测试和验证
  - [ ] SubTask 13.1: 使用 gradle build 验证编译
  - [ ] SubTask 13.2: 使用 gradle runClient 验证运行时功能
  - [ ] SubTask 13.3: 测试商店创建流程（含货币显示名称指定）
  - [ ] SubTask 13.4: 测试告示牌自动涂蜡功能
  - [ ] SubTask 13.5: 测试交易流程（出售商店和收购商店）
  - [ ] SubTask 13.6: 测试收购者余额不足提示（双方收到消息，显示货币 displayName）
  - [ ] SubTask 13.7: 测试所有权保护
  - [ ] SubTask 13.8: 测试管理员命令（基于面对的商店）
  - [ ] SubTask 13.9: 测试删除商店二次确认
  - [ ] SubTask 13.10: 测试动态定价
  - [ ] SubTask 13.11: 测试可视化展示
  - [ ] SubTask 13.12: 测试防破坏保护
  - [ ] SubTask 13.13: 测试数据持久化（重启后数据保留）
  - [ ] SubTask 13.14: 验证交易日志记录（含卖方、交易箱ID、位置、货币显示名称）
  - [ ] SubTask 13.15: 测试左键/右键交互
  - [ ] SubTask 13.16: 验证货币显示名称正确显示（不显示ID）

# Task Dependencies
- [Task 2] depends on [Task 1]
- [Task 3] depends on [Task 1, Task 2]
- [Task 4] depends on [Task 3]
- [Task 5] depends on [Task 3]
- [Task 6] depends on [Task 3]
- [Task 7] depends on [Task 3]
- [Task 8] depends on [Task 3, Task 7]
- [Task 9] depends on [Task 3]
- [Task 10] depends on [Task 5]
- [Task 11] depends on [Task 1]
- [Task 12] depends on [Task 3, Task 4, Task 5, Task 6, Task 8, Task 9, Task 10, Task 11]
- [Task 13] depends on [Task 12]

# Parallelizable Tasks
以下任务可以并行执行：
- Task 4, Task 5, Task 6, Task 7, Task 9, Task 10, Task 11（在 Task 3 完成后）
- Task 8 的部分子任务可以与其他任务并行
