# 统计模块功能文档

## 概述

本文档总结了 Minecraft Fabric Mod 统计模块的所有新增功能，包括数据模型、监听器、命令系统和计分板功能。

## 项目环境

- **Minecraft 版本**: 26.1
- **Java 版本**: 25 (Zulu 25)
- **Fabric Loader**: 0.18.4
- **Fabric API**: 0.144.3+26.1

---

## 一、统计项列表

### 1. 基础统计

| 统计项 | 数据类型 | 说明 |
|--------|----------|------|
| 在线时间 | long (秒) | 玩家累计在线时长 |
| 移动距离 | double (米) | 玩家累计移动距离 |
| 放置方块 | int | 累计放置方块数量 |
| 破坏方块 | int | 累计破坏方块数量 |
| 击杀数 | int | 累计击杀生物数量 |
| 死亡数 | int | 累计死亡次数 |
| 耐久消耗 | int | 累计消耗耐久次数 |
| 造成伤害 | long | 累计造成伤害总量 |
| 受到伤害 | long | 累计受到伤害总量 |

### 2. 新增统计项

| 统计项 | 数据类型 | 说明 |
|--------|----------|------|
| 首次加入时间 | long (时间戳) | 玩家第一次进入服务器的时间 |
| 登录天数 | int | 玩家累计登录天数（每日首次登录记录） |
| 钓鱼次数 | int | 累计钓鱼尝试次数 |
| 钓鱼成功 | int | 成功钓到物品的次数 |
| 钓鱼失败 | int | 钓鱼失败的次数 |
| 合成物品数 | int | 累计合成物品次数 |
| 使用铁砧次数 | int | 累计使用铁砧次数（附魔与修复） |
| 附魔物品数 | int | 累计在附魔台附魔的次数 |
| 村民交易次数 | int | 累计与村民交易的次数 |
| 发送聊天消息数 | int | 累计发送聊天消息数量 |
| 丢弃物品次数 | int | 累计丢弃物品次数 |

### 3. 详细分类统计

| 分类 | 数据结构 | 说明 |
|------|----------|------|
| 方块放置分类 | Map<String, Integer> | 按方块类型统计放置数量 |
| 方块破坏分类 | Map<String, Integer> | 按方块类型统计破坏数量 |
| 击杀生物分类 | Map<String, Integer> | 按生物类型统计击杀数量 |
| 死亡来源分类 | Map<String, Integer> | 按生物类型统计死亡来源 |
| 造成伤害分类 | Map<String, Long> | 按生物类型统计造成伤害 |
| 受到伤害分类 | Map<String, Long> | 按生物类型统计受到伤害 |
| 钓获物品分类 | Map<String, Integer> | 按物品类型统计钓获数量 |
| 合成物品分类 | Map<String, Integer> | 按物品类型统计合成数量 |
| 丢弃物品分类 | Map<String, Integer> | 按物品类型统计丢弃数量 |

---

## 二、命令系统

### 1. 基础命令

| 命令 | 说明 |
|------|------|
| `/stats` | 显示自己的统计概览 |
| `/stats <玩家名>` | 查看指定玩家的统计概览 |
| `/stats server` | 显示服务器统计信息 |

### 2. 击杀/死亡统计

| 命令 | 说明 |
|------|------|
| `/stats kills` | 显示击杀统计详情 |
| `/stats kills <生物类型>` | 显示对特定生物的击杀数 |
| `/stats deaths` | 显示死亡统计详情 |
| `/stats deaths <生物类型>` | 显示来自特定生物的死亡数 |

### 3. 方块统计

| 命令 | 说明 |
|------|------|
| `/stats blocks` | 显示方块统计概览 |
| `/stats blocks placed` | 显示放置方块详情 |
| `/stats blocks placed <方块类型>` | 显示特定方块放置数 |
| `/stats blocks broken` | 显示破坏方块详情 |
| `/stats blocks broken <方块类型>` | 显示特定方块破坏数 |

### 4. 伤害统计

| 命令 | 说明 |
|------|------|
| `/stats damage` | 显示伤害统计概览 |
| `/stats damage dealt` | 显示造成伤害详情 |
| `/stats damage dealt <生物类型>` | 显示对特定生物造成的伤害 |
| `/stats damage taken` | 显示受到伤害详情 |
| `/stats damage taken <生物类型>` | 显示来自特定生物的伤害 |

### 5. 钓鱼统计

| 命令 | 说明 |
|------|------|
| `/stats fishing` | 显示钓鱼统计概览 |
| `/stats fishing <物品类型>` | 显示特定物品钓获数 |

### 6. 合成统计

| 命令 | 说明 |
|------|------|
| `/stats crafting` | 显示合成统计概览 |
| `/stats crafting <物品类型>` | 显示特定物品合成数 |

### 7. 丢弃物品统计

| 命令 | 说明 |
|------|------|
| `/stats drops` | 显示丢弃物品统计概览 |
| `/stats drops <物品类型>` | 显示特定物品丢弃数 |

### 8. 玩家信息

| 命令 | 说明 |
|------|------|
| `/stats info` | 显示首次加入时间和登录天数 |

### 9. 活动统计

| 命令 | 说明 |
|------|------|
| `/stats activity` | 显示铁砧使用、附魔、交易、聊天统计 |

---

## 三、计分板系统

### 管理员命令

| 命令 | 说明 |
|------|------|
| `/statsboard create <类型>` | 创建指定类型的计分板 |
| `/statsboard startrotation` | 开始计分板轮播 |
| `/statsboard stoprotation` | 停止计分板轮播 |
| `/statsboard interval <tick>` | 设置更新间隔（默认20tick=1秒） |
| `/statsboard update` | 手动更新计分板 |
| `/statsboard remove` | 移除计分板 |

### 支持的计分板类型

| 类型 | 显示名称 | 说明 |
|------|----------|------|
| `playTime` | 在线时间 (秒) | 显示玩家在线时间（秒） |
| `playTimeMinutes` | 在线时间 (分钟) | 显示玩家在线时间（分钟） |
| `playTimeHours` | 在线时间 (小时) | 显示玩家在线时间（小时） |
| `distanceTraveled` | 移动距离 (米) | 显示玩家移动距离 |
| `blocksPlaced` | 放置方块 | 显示放置方块数量 |
| `blocksBroken` | 破坏方块 | 显示破坏方块数量 |
| `kills` | 击杀数 | 显示击杀生物数量 |
| `deaths` | 死亡数 | 显示死亡次数 |
| `damageDealt` | 造成伤害 | 显示造成伤害总量 |
| `damageTaken` | 受到伤害 | 显示受到伤害总量 |
| `fishingAttempts` | 钓鱼次数 | 显示钓鱼尝试次数 |
| `itemsCrafted` | 合成物品 | 显示合成物品数量 |
| `anvilUses` | 使用铁砧 | 显示使用铁砧次数 |
| `itemsEnchanted` | 附魔物品 | 显示附魔物品数量 |
| `villagerTrades` | 村民交易 | 显示村民交易次数 |
| `chatMessagesSent` | 聊天消息 | 显示发送聊天消息数 |
| `itemsDropped` | 丢弃物品 | 显示丢弃物品次数 |
| `loginDays` | 登录天数 | 显示登录天数 |

---

## 四、文件结构

### 1. 数据模型

```
src/main/java/com/example/statistics/data/
├── PlayerStatistics.java      # 玩家统计数据模型
└── StatisticsDataManager.java  # 数据管理器（加载/保存）
```

### 2. 监听器

```
src/main/java/com/example/statistics/listeners/
├── PlayerJoinListener.java    # 玩家加入事件（首次加入时间、登录天数）
├── PlayerLeaveListener.java   # 玩家离开事件（保存在线时间）
├── PlayerMoveListener.java    # 玩家移动事件（移动距离）
├── BlockBreakListener.java    # 方块破坏事件
├── BlockPlaceListener.java    # 方块放置事件
├── EntityDeathListener.java   # 生物死亡事件（击杀/死亡统计）
├── DamageListener.java        # 伤害事件（造成/受到伤害）
├── ItemDurabilityListener.java # 物品耐久消耗事件
└── ChatListener.java          # 聊天消息事件
```

### 3. Mixin 注入

```
src/main/java/com/example/mixin/
├── FishingHookMixin.java      # 钓鱼事件
├── ServerPlayerMixin.java     # 玩家丢弃物品事件
├── AnvilMenuMixin.java        # 铁砧使用事件
├── EnchantmentMenuMixin.java  # 附魔台事件
├── MerchantMenuMixin.java     # 村民交易事件
└── ResultSlotMixin.java       # 合成物品事件
```

### 4. 命令系统

```
src/main/java/com/example/statistics/commands/
├── StatsCommand.java          # 玩家统计命令
└── ScoreboardCommand.java     # 计分板管理命令
```

### 5. 计分板系统

```
src/main/java/com/example/statistics/scoreboard/
└── ScoreboardManager.java     # 计分板管理器
```

### 6. 配置文件

```
src/main/resources/
├── fabric.mod.json            # Fabric 模组配置
└── template-mod.mixins.json   # Mixin 配置
```

---

## 五、数据存储

### 存储位置

数据存储在服务器目录下的 `statistics/` 文件夹中：

```
服务器目录/
└── statistics/
    └── <玩家名>.json
```

### 数据格式示例

```json
{
  "playerName": "Steve",
  "playTimeSeconds": 3600,
  "distanceTraveled": 5000.5,
  "blocksPlaced": 100,
  "blocksBroken": 50,
  "kills": 10,
  "deaths": 2,
  "durabilityUsed": 5,
  "damageDealt": 500,
  "damageTaken": 100,
  "firstJoinTime": 1711526400,
  "loginDays": 5,
  "lastLoginDate": "2026-03-27",
  "fishingAttempts": 20,
  "fishingSuccess": 15,
  "fishingFailures": 5,
  "itemsCrafted": 30,
  "anvilUses": 10,
  "itemsEnchanted": 8,
  "villagerTrades": 25,
  "chatMessagesSent": 100,
  "itemsDropped": 15,
  "blocksPlacedByType": {"minecraft:stone": 50, "minecraft:dirt": 50},
  "blocksBrokenByType": {"minecraft:stone": 30, "minecraft:dirt": 20},
  "killsByEntityType": {"minecraft:zombie": 5, "minecraft:skeleton": 5},
  "deathsByEntityType": {"minecraft:zombie": 1, "minecraft:creeper": 1},
  "damageDealtByEntityType": {"minecraft:zombie": 300, "minecraft:skeleton": 200},
  "damageTakenByEntityType": {"minecraft:zombie": 50, "minecraft:creeper": 50},
  "fishCaughtByType": {"minecraft:cod": 10, "minecraft:salmon": 5},
  "itemsCraftedByType": {"minecraft:stone_pickaxe": 5, "minecraft:torch": 25},
  "itemsDroppedByType": {"minecraft:cobblestone": 10, "minecraft:dirt": 5}
}
```

---

## 六、生物类型中文翻译

命令中显示的生物类型会自动翻译为中文：

| 英文 | 中文 |
|------|------|
| zombie | 僵尸 |
| skeleton | 骷髅 |
| creeper | 苦力怕 |
| spider | 蜘蛛 |
| enderman | 末影人 |
| blaze | 烈焰人 |
| ghast | 恶魂 |
| witch | 女巫 |
| slime | 史莱姆 |
| pig | 猪 |
| cow | 牛 |
| sheep | 羊 |
| chicken | 鸡 |
| player | 玩家 |
| villager | 村民 |
| pillager | 掠夺者 |
| drowned | 溺尸 |
| phantom | 幻术师 |
| wither | 凋灵 |
| warden | 监守者 |
| guardian | 守卫者 |
| dragon | 龙 |
| iron_golem | 铁傀儡 |
| snow_golem | 雪傀儡 |

---

## 七、Tab 补全功能

所有涉及实体类型、方块类型、物品类型的命令都支持 Tab 补全，补全内容使用双引号包裹：

- 示例：`/stats kills "僵尸"` 而非 `/stats kills 僵尸`
- 这是为了避免包含空格或特殊字符的类型名称导致命令解析错误

---

## 八、注意事项

1. **数据持久化**：玩家数据在玩家离开服务器时自动保存
2. **计分板更新**：计分板默认每秒更新一次，可通过命令调整
3. **在线时间计算**：计分板显示的在线时间包含当前会话时间，实时更新
4. **伤害统计**：伤害值以整数显示，计分板不支持小数
5. **登录天数**：每日首次登录时记录，同一天多次登录只计一次

---

## 九、版本历史

### v1.0.0
- 初始版本
- 实现基础统计功能（在线时间、移动距离、方块、击杀、死亡）

### v1.1.0
- 新增伤害统计（造成/受到伤害，按生物类型分类）
- 新增计分板系统
- 支持计分板轮播功能

### v1.2.0
- 新增首次加入时间统计
- 新增登录天数统计
- 新增钓鱼统计（成功/失败/按物品分类）
- 新增合成物品统计（按物品分类）
- 新增使用铁砧次数统计
- 新增附魔物品数统计
- 新增村民交易次数统计
- 新增发送聊天消息数统计
- 新增丢弃物品统计（按物品分类）
- 计分板新增多种类型支持
- 所有分类统计支持 Tab 补全（双引号格式）
