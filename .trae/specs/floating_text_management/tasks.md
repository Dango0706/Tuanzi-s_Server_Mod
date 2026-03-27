# 悬浮文字管理指令集 - 任务列表

## [ ] 任务 1：创建悬浮文字子模块基础结构
- **优先级**：P0
- **依赖**：无
- **描述**：
  - 创建 `com.example.statistics.floatingtext` 包结构（与 `com.example.statistics.scoreboard` 同级）
  - 创建 `FloatingTextManager.java` 管理器类，负责悬浮文字的创建、更新、删除
  - 在 `StatisticsModule.java` 中初始化悬浮文字管理器
- **验收标准**：模块能够正确初始化和加载

## [ ] 任务 2：实现悬浮文字数据模型
- **优先级**：P0
- **依赖**：任务 1
- **描述**：
  - 创建 `FloatingTextData.java` 数据类，包含以下字段：
    - 唯一 ID（String）
    - 世界名称（String）
    - 位置坐标（double x, y, z）
    - 绑定的统计数据类型（String，对应 PlayerStatistics 字段）
    - 显示名称（String）
    - 颜色设置（String，支持颜色名称和十六进制）
    - 更新间隔（int，单位 tick，默认 20）
    - 创建时间（long）
  - 实现 JSON 序列化和反序列化
- **验收标准**：数据类能够正确序列化和反序列化

## [ ] 任务 3：实现悬浮文字数据持久化
- **优先级**：P0
- **依赖**：任务 2
- **描述**：
  - 创建 `FloatingTextDataManager.java` 数据管理类
  - 实现悬浮文字配置的保存和加载功能（JSON 格式）
  - 数据存储路径：`config/statistics/floating_texts.json`（与统计模块数据同目录）
  - 在服务器关闭时保存数据
  - 在服务器启动时加载数据并重建悬浮文字实体
- **验收标准**：
  - 数据能够正确保存和加载
  - 服务器重启后悬浮文字配置能够恢复

## [ ] 任务 4：实现悬浮文字实体创建功能
- **优先级**：P0
- **依赖**：任务 2
- **描述**：
  - 研究 Minecraft 26.1 中 `TextDisplay` 实体的使用方式
  - 实现 `TextDisplay` 实体的创建方法
  - 实现悬浮文字的创建和销毁方法
  - 实现悬浮文字的文本内容更新方法
  - 设置悬浮文字的对齐方式（居中）
- **验收标准**：
  - 能够在指定位置创建 TextDisplay 实体
  - 悬浮文字能够正确显示文本内容

## [ ] 任务 5：实现排行榜数据获取和格式化（与统计模块强绑定）
- **优先级**：P0
- **依赖**：任务 3、任务 4
- **描述**：
  - 创建 `LeaderboardFormatter.java` 类
  - 实现 `StatisticsModule.getInstance().getDataManager()` 获取统计管理器
  - 实现从 `StatisticsDataManager.getAllPlayerStatistics()` 获取所有玩家数据
  - 实现按统计类型排序算法（降序），参考 `ScoreboardManager.updateScores()` 的实现
  - 实现排行榜文本格式化，格式如下：
    ```
    xxx排行榜
    1 玩家名字  数据
    2 玩家名字  数据
    ...
    ```
  - 支持所有 PlayerStatistics 字段的统计类型（与 ScoreboardManager 一致）
- **验收标准**：
  - 能够正确获取排行榜数据
  - 排行榜格式符合规范要求
  - 数据与统计模块实时同步

## [ ] 任务 6：实现悬浮文字创建指令
- **优先级**：P0
- **依赖**：任务 4
- **描述**：
  - 创建 `FloatingTextCommand.java` 命令类（放在 `com.example.statistics.commands` 包下）
  - 实现 `/floatingtext create <id> <x> <y> <z>` 指令
  - 实现坐标参数验证（支持相对坐标 ~ 符号）
  - 实现 ID 唯一性验证
  - 设置管理员权限验证（requires OP level 2）
  - 在 `StatisticsModule.java` 中注册命令
- **验收标准**：
  - 管理员能够创建悬浮文字
  - 非 OP 玩家无法执行指令

## [ ] 任务 7：实现悬浮文字内容设置指令（与统计模块强绑定）
- **优先级**：P0
- **依赖**：任务 5、任务 6
- **描述**：
  - 实现 `/floatingtext setcontent <id> <statType> <displayName>` 指令
  - 实现统计数据类型参数提示和验证（Tab 补全）
  - 支持的统计类型（与 ScoreboardManager 一致）：playTime, distanceTraveled, blocksPlaced, blocksBroken, kills, deaths, damageDealt, damageTaken, fishingAttempts, itemsCrafted, anvilUses, itemsEnchanted, villagerTrades, chatMessagesSent, itemsDropped, loginDays
  - 实现显示名称设置
  - 绑定后立即显示排行榜
- **验收标准**：
  - 能够将悬浮文字绑定到统计数据
  - 排行榜能够正确显示
  - Tab 补全正确显示所有统计类型

## [ ] 任务 8：实现悬浮文字更新间隔设置指令
- **优先级**：P1
- **依赖**：任务 6
- **描述**：
  - 实现 `/floatingtext updateinterval <id> <ticks>` 指令
  - 实现更新间隔验证（范围 1-1200 tick，即 0.05秒-60秒）
  - 实现定时更新机制（使用 ServerTickEvents，参考 ScoreboardManager 的实现）
  - 默认更新间隔为 20 tick（1秒）
- **验收标准**：
  - 能够设置和修改更新间隔
  - 悬浮文字按设置的间隔更新数据

## [ ] 任务 9：实现悬浮文字位置调整指令
- **优先级**：P1
- **依赖**：任务 6
- **描述**：
  - 实现 `/floatingtext move <id> <x> <y> <z>` 指令
  - 支持绝对坐标和相对坐标（~ 符号）
  - 实现坐标精度（0.1 单位）
  - 更新悬浮文字实体位置
  - 移动后执行避障检测
- **验收标准**：
  - 能够使用绝对坐标移动悬浮文字
  - 能够使用相对坐标移动悬浮文字

## [ ] 任务 10：实现悬浮文字颜色设置指令
- **优先级**：P1
- **依赖**：任务 6
- **描述**：
  - 实现 `/floatingtext color <id> <color>` 指令
  - 支持预定义颜色名称（red、green、blue、yellow、white、gold、gray、aqua、dark_red、dark_green、dark_blue、dark_aqua、dark_purple、light_purple）
  - 支持十六进制颜色值（#RRGGBB 格式）
  - 更新悬浮文字显示颜色
  - 实现 Tab 补全显示所有可用颜色
- **验收标准**：
  - 能够使用颜色名称设置颜色
  - 能够使用十六进制值设置颜色

## [ ] 任务 11：实现悬浮文字删除和列表指令
- **优先级**：P1
- **依赖**：任务 6
- **描述**：
  - 实现 `/floatingtext delete <id>` 指令
  - 实现 `/floatingtext list` 指令
  - 实现 `/floatingtext info <id>` 指令
  - 删除时同时移除 TextDisplay 实体
- **验收标准**：
  - 能够删除悬浮文字
  - 能够列出所有悬浮文字
  - 能够查看悬浮文字详细信息

## [ ] 任务 12：实现自动避障功能
- **优先级**：P1
- **依赖**：任务 4
- **描述**：
  - 创建 `CollisionDetector.java` 类
  - 实现悬浮文字与地形碰撞检测算法
  - 检测悬浮文字位置是否被方块遮挡
  - 实现高度自动调整机制（最大调整 2 个方块）
  - 在创建和移动悬浮文字时执行避障检测
- **验收标准**：
  - 悬浮文字不会被地面方块遮挡
  - 调整幅度不超过 2 个方块高度

## [ ] 任务 13：实现定时更新机制
- **优先级**：P0
- **依赖**：任务 5、任务 8
- **描述**：
  - 使用 `ServerTickEvents.END_SERVER_TICK` 实现定时更新（参考 ScoreboardManager 的 Timer 实现）
  - 为每个悬浮文字维护独立的更新计时器
  - 每次更新时从 `StatisticsDataManager` 获取最新数据
  - 更新 TextDisplay 实体的文本内容
- **验收标准**：
  - 悬浮文字能够按设定的间隔自动更新
  - 数据与统计模块保持同步

## [ ] 任务 14：集成测试和验证
- **优先级**：P0
- **依赖**：任务 1-13
- **描述**：
  - 使用 `gradle build` 验证代码编译
  - 使用 `gradle runClient` 验证功能正确性
  - 测试所有指令功能
  - 测试数据持久化
  - 测试权限控制
  - 测试与统计模块的数据同步
- **验收标准**：
  - 代码编译成功
  - 所有功能正常工作
  - 数据持久化正确
  - 与统计模块数据同步正确

# 任务依赖关系
- [任务 2] 依赖 [任务 1]
- [任务 3] 依赖 [任务 2]
- [任务 4] 依赖 [任务 2]
- [任务 5] 依赖 [任务 3, 任务 4]
- [任务 6] 依赖 [任务 4]
- [任务 7] 依赖 [任务 5, 任务 6]
- [任务 8] 依赖 [任务 6]
- [任务 9] 依赖 [任务 6]
- [任务 10] 依赖 [任务 6]
- [任务 11] 依赖 [任务 6]
- [任务 12] 依赖 [任务 4]
- [任务 13] 依赖 [任务 5, 任务 8]
- [任务 14] 依赖 [任务 1-13]

# 模块结构
```
com.example.statistics/
├── StatisticsModule.java          # 主模块入口
├── commands/                       # 命令包
│   ├── StatsCommand.java
│   ├── ScoreboardCommand.java
│   └── FloatingTextCommand.java   # 新增
├── data/                           # 数据包
│   ├── PlayerStatistics.java
│   ├── ServerStatistics.java
│   └── StatisticsDataManager.java
├── listeners/                      # 监听器包
│   └── ...
├── scoreboard/                     # 计分板子模块
│   └── ScoreboardManager.java
└── floatingtext/                   # 悬浮文字子模块（新增，与 scoreboard 同级）
    ├── FloatingTextManager.java    # 悬浮文字管理器
    ├── FloatingTextData.java       # 悬浮文字数据模型
    ├── FloatingTextDataManager.java # 悬浮文字数据持久化
    ├── LeaderboardFormatter.java   # 排行榜格式化工具
    └── CollisionDetector.java      # 碰撞检测工具
```
