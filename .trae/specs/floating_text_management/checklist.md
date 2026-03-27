# 悬浮文字管理指令集 - 验证清单

## 模块结构验证
- [ ] 包结构正确：验证 `com.example.statistics.floatingtext` 包存在，与 `com.example.statistics.scoreboard` 同级
- [ ] 命令位置正确：验证 `FloatingTextCommand.java` 位于 `com.example.statistics.commands` 包下
- [ ] 数据存储路径正确：验证数据保存到 `config/statistics/floating_texts.json`

## 核心功能验证
- [ ] 悬浮文字创建功能：验证管理员能够在指定坐标创建悬浮文字，ID 唯一性验证有效
- [ ] 悬浮文字内容配置：验证悬浮文字能够正确绑定统计数据并显示排行榜格式
- [ ] 排行榜格式显示：验证排行榜文本格式符合规范，居中对齐正确
- [ ] 更新时间设置：验证刷新频率设置功能正常，悬浮文字按设置间隔更新
- [ ] 位置调整功能：验证绝对坐标和相对坐标移动功能正常，坐标精度达到 0.1 单位
- [ ] 颜色设置功能：验证预定义颜色名称和十六进制颜色值设置功能正常
- [ ] 删除功能：验证悬浮文字删除功能正常，删除不存在的悬浮文字时返回正确提示
- [ ] 列表功能：验证能够列出所有悬浮文字信息

## 统计模块集成验证
- [ ] 数据绑定：验证悬浮文字能够正确绑定到 PlayerStatistics 的所有支持字段
- [ ] 数据获取：验证通过 `StatisticsModule.getInstance().getDataManager()` 正确获取统计管理器
- [ ] 排行榜排序：验证排行榜数据按指定统计类型降序排序正确
- [ ] 数据同步：验证悬浮文字显示的数据与统计模块数据实时同步
- [ ] 统计类型一致性：验证支持的统计类型与 ScoreboardManager 一致

## 自动避障功能验证
- [ ] 碰撞检测：验证悬浮文字与地形碰撞检测算法正确
- [ ] 自动调整：验证悬浮文字被遮挡时能够自动向上调整（不超过 2 个方块）

## 权限控制验证
- [ ] 管理员权限：验证具有 OP 等级 2 及以上权限的玩家能够执行所有悬浮文字管理指令
- [ ] 非管理员权限：验证非管理员玩家无法执行悬浮文字管理指令，返回权限不足提示

## 数据持久化验证
- [ ] 数据保存：验证悬浮文字配置能够正确保存到 `config/statistics/floating_texts.json`
- [ ] 数据加载：验证服务器重启后悬浮文字配置能够正确加载
- [ ] 实体重建：验证服务器重启后 TextDisplay 实体能够正确重建
- [ ] 实时保存：验证修改配置后数据立即保存

## 指令验证
- [ ] `/floatingtext create <id> <x> <y> <z>`：验证创建指令功能正常
- [ ] `/floatingtext setcontent <id> <statType> <displayName>`：验证内容设置指令功能正常
- [ ] `/floatingtext updateinterval <id> <ticks>`：验证更新间隔设置指令功能正常
- [ ] `/floatingtext move <id> <x> <y> <z>`：验证位置移动指令功能正常
- [ ] `/floatingtext color <id> <color>`：验证颜色设置指令功能正常
- [ ] `/floatingtext delete <id>`：验证删除指令功能正常
- [ ] `/floatingtext list`：验证列表指令功能正常
- [ ] `/floatingtext info <id>`：验证信息查询指令功能正常

## Tab 补全验证
- [ ] 统计类型补全：验证 `/floatingtext setcontent` 指令的统计类型 Tab 补全正确
- [ ] 颜色补全：验证 `/floatingtext color` 指令的颜色 Tab 补全正确
- [ ] ID 补全：验证各指令的悬浮文字 ID Tab 补全正确

## 代码质量验证
- [ ] 代码编译：验证代码能够通过 `gradle build` 编译
- [ ] 运行验证：验证代码能够通过 `gradle runClient` 正常运行
- [ ] 代码结构：验证代码结构清晰，符合项目规范
- [ ] 错误处理：验证系统具有适当的错误处理和日志记录

## 兼容性验证
- [ ] Minecraft 26.1 兼容：验证模块与 Minecraft 26.1 兼容
- [ ] Fabric API 0.144.3+26.1 兼容：验证模块与 Fabric API 0.144.3+26.1 兼容
- [ ] TextDisplay 实体：验证 TextDisplay 实体在 Minecraft 26.1 中正常工作
