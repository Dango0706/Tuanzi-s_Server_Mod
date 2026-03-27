# 悬浮文字管理指令集 Spec

## Why
当前服务器统计模块已实现丰富的玩家统计数据，但缺乏直观的世界内展示方式。管理员需要一个仅限管理员使用的悬浮文字管理功能，作为 statistics 模块的子模块（与 scoreboard 同级），在指定位置创建和管理悬浮文字，用于展示统计排行榜数据，提升服务器的信息展示能力和玩家体验。

## What Changes
- 新增悬浮文字管理功能，作为 `com.example.statistics.floatingtext` 子包
- 新增悬浮文字创建功能，支持在三维空间指定坐标位置创建悬浮文字
- 新增悬浮文字内容配置功能，与 `StatisticsDataManager` 强绑定，支持所有统计类型的排行榜显示
- 新增悬浮文字管理指令集，包括更新时间设置、位置调整、颜色设置、删除功能
- 新增自动避障功能，确保悬浮文字不被地面方块遮挡
- 所有指令严格限制为管理员权限（OP等级2及以上）

## Impact
- Affected specs: 统计模块（statistics_module）- 悬浮文字模块作为其子模块
- Affected code: 
  - 新增 `com.example.statistics.floatingtext` 包及相关类（与 `com.example.statistics.scoreboard` 同级）
  - 在 `StatisticsModule.java` 中注册悬浮文字管理器和命令
  - 数据存储路径：`config/statistics/floating_texts.json`（与统计模块数据同目录）
  - 使用 Minecraft 26.1 的 `TextDisplay` 实体实现悬浮文字

## 模块结构
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

## ADDED Requirements

### Requirement: 悬浮文字创建功能
系统 SHALL 提供管理员专用指令，支持在三维空间指定坐标位置创建悬浮文字，每个悬浮文字实例具有唯一标识符(ID)。

#### Scenario: 成功创建悬浮文字
- **WHEN** 管理员执行 `/floatingtext create <id> <x> <y> <z>` 指令
- **THEN** 系统在指定位置创建 TextDisplay 实体，并返回创建成功消息

#### Scenario: 坐标格式验证
- **WHEN** 管理员输入的坐标格式不正确
- **THEN** 系统返回坐标格式错误提示

#### Scenario: ID 唯一性验证
- **WHEN** 管理员使用的 ID 已存在
- **THEN** 系统返回 ID 已存在的错误提示

### Requirement: 悬浮文字内容配置功能（与统计模块强绑定）
系统 SHALL 支持将悬浮文字内容与 `StatisticsDataManager` 实时同步，显示排行榜数据。

#### Scenario: 绑定统计数据
- **WHEN** 管理员执行 `/floatingtext setcontent <id> <statType> <displayName>` 指令
- **THEN** 系统将该悬浮文字绑定到指定统计数据类型，并显示排行榜格式

#### Scenario: 排行榜格式显示
- **WHEN** 悬浮文字绑定统计数据后
- **THEN** 系统按以下格式显示排行榜（居中对齐）：
  ```
  xxx排行榜
  1 玩家名字  数据(如12345)
  2 玩家名字  数据(如2222)
  3 玩家名字  数据(如1111)
  ```

#### Scenario: 支持的统计数据类型（与 PlayerStatistics 字段强绑定）
- **WHEN** 管理员设置内容时
- **THEN** 系统支持以下统计类型（与 ScoreboardManager 支持的类型一致）：
  - `playTime` - 在线时间（秒）
  - `distanceTraveled` - 移动距离
  - `blocksPlaced` - 放置方块
  - `blocksBroken` - 破坏方块
  - `kills` - 击杀数
  - `deaths` - 死亡数
  - `damageDealt` - 造成伤害
  - `damageTaken` - 受到伤害
  - `fishingAttempts` - 钓鱼次数
  - `itemsCrafted` - 合成物品数
  - `anvilUses` - 铁砧使用次数
  - `itemsEnchanted` - 附魔物品数
  - `villagerTrades` - 村民交易次数
  - `chatMessagesSent` - 发送聊天消息数
  - `itemsDropped` - 丢弃物品次数
  - `loginDays` - 登录天数

#### Scenario: 数据实时更新
- **WHEN** 悬浮文字绑定统计数据后
- **THEN** 系统按照设定的更新间隔从 `StatisticsDataManager` 获取最新数据并刷新显示

### Requirement: 更新时间设置指令
系统 SHALL 允许管理员设置悬浮文字数据的刷新频率。

#### Scenario: 设置刷新频率
- **WHEN** 管理员执行 `/floatingtext updateinterval <id> <ticks>` 指令
- **THEN** 系统将该悬浮文字的刷新频率设置为指定 tick 数（默认 20 tick，即 1 秒）

#### Scenario: 查询刷新频率
- **WHEN** 管理员执行 `/floatingtext info <id>` 指令
- **THEN** 系统显示该悬浮文字的当前配置，包括刷新频率、绑定统计类型、位置等

### Requirement: 位置调整指令
系统 SHALL 支持绝对坐标和相对坐标两种定位方式调整悬浮文字位置。

#### Scenario: 使用绝对坐标调整位置
- **WHEN** 管理员执行 `/floatingtext move <id> <x> <y> <z>` 指令
- **THEN** 系统将悬浮文字移动到指定绝对坐标位置

#### Scenario: 使用相对坐标调整位置
- **WHEN** 管理员执行 `/floatingtext move <id> ~<x> ~<y> ~<z>` 指令（使用 ~ 符号）
- **THEN** 系统将悬浮文字相对于当前位置移动指定偏移量

#### Scenario: 坐标精度
- **WHEN** 管理员输入坐标时
- **THEN** 系统支持精确到 0.1 单位的坐标调整粒度

### Requirement: 颜色设置指令
系统 SHALL 允许管理员自定义悬浮文字的显示颜色。

#### Scenario: 使用预定义颜色名称设置颜色
- **WHEN** 管理员执行 `/floatingtext color <id> <colorName>` 指令
- **THEN** 系统将悬浮文字颜色设置为预定义颜色（如 red、green、blue、yellow、white、gold、gray、aqua、dark_red、dark_green、dark_blue、dark_aqua、dark_purple、light_purple 等）

#### Scenario: 使用十六进制颜色值设置颜色
- **WHEN** 管理员执行 `/floatingtext color <id> #RRGGBB` 指令
- **THEN** 系统将悬浮文字颜色设置为指定的十六进制颜色值

### Requirement: 删除功能
系统 SHALL 提供安全的悬浮文字删除机制。

#### Scenario: 删除单个悬浮文字
- **WHEN** 管理员执行 `/floatingtext delete <id>` 指令
- **THEN** 系统删除指定的悬浮文字并返回成功消息

#### Scenario: 删除不存在的悬浮文字
- **WHEN** 管理员尝试删除不存在的悬浮文字
- **THEN** 系统返回悬浮文字不存在的错误提示

#### Scenario: 列出所有悬浮文字
- **WHEN** 管理员执行 `/floatingtext list` 指令
- **THEN** 系统显示所有悬浮文字的 ID、位置、绑定统计类型信息

### Requirement: 自动避障功能
系统 SHALL 实现悬浮文字自动调整高度的算法，确保其不会被地面方块遮挡。

#### Scenario: 检测到遮挡时自动调整
- **WHEN** 悬浮文字位置被地面方块遮挡
- **THEN** 系统自动向上调整悬浮文字位置，调整幅度不超过 2 个方块高度

#### Scenario: 无法完全避障时提示
- **WHEN** 调整幅度超过 2 个方块高度仍无法避障
- **THEN** 系统记录警告日志并保持当前位置

### Requirement: 权限控制
系统 SHALL 确保所有悬浮文字管理指令仅限管理员使用。

#### Scenario: 管理员执行指令
- **WHEN** 具有 OP 等级 2 及以上权限的玩家执行悬浮文字管理指令
- **THEN** 指令正常执行

#### Scenario: 非管理员执行指令
- **WHEN** 不具有管理员权限的玩家执行悬浮文字管理指令
- **THEN** 系统返回权限不足提示

### Requirement: 数据持久化
系统 SHALL 实现悬浮文字配置的持久化存储。

#### Scenario: 服务器重启后恢复
- **WHEN** 服务器重启后
- **THEN** 所有悬浮文字配置从存储文件恢复，悬浮文字重新显示

#### Scenario: 配置实时保存
- **WHEN** 管理员修改悬浮文字配置后
- **THEN** 配置立即保存到存储文件

### Requirement: 与统计模块集成
系统 SHALL 作为 statistics 模块的子模块，与统计模块强绑定，确保数据一致性。

#### Scenario: 模块层级结构
- **WHEN** 悬浮文字模块初始化时
- **THEN** 系统作为 `com.example.statistics.floatingtext` 子包存在，与 `com.example.statistics.scoreboard` 同级

#### Scenario: 获取统计管理器实例
- **WHEN** 悬浮文字模块需要获取统计数据时
- **THEN** 系统通过 `StatisticsModule.getInstance().getDataManager()` 获取统计管理器实例

#### Scenario: 排行榜数据排序
- **WHEN** 生成排行榜时
- **THEN** 系统从 `StatisticsDataManager.getAllPlayerStatistics()` 获取所有玩家数据，按指定统计类型降序排序，取前 10 名

#### Scenario: 数据存储路径
- **WHEN** 保存悬浮文字配置时
- **THEN** 系统将数据保存到 `config/statistics/floating_texts.json`，与统计模块数据同目录

## MODIFIED Requirements
无

## REMOVED Requirements
无
