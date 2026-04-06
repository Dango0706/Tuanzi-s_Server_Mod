# Tasks

- [x] Task 1: 修复悬浮物品显示 - 确保所有创建方式都有悬浮物品
  - [x] SubTask 1.1: 分析 ShopDisplayManager.createDisplayForShop 的调用位置
  - [x] SubTask 1.2: 在贴牌创建商店时添加悬浮物品创建调用
  - [x] SubTask 1.3: 在修改牌子后添加悬浮物品更新/创建调用
  - [x] SubTask 1.4: 添加调试日志验证悬浮物品创建时机

- [x] Task 2: 修复物品更换后的告示牌文字更新
  - [x] SubTask 2.1: 在 ChatInputHandler 中实现 updateSignText 方法
  - [x] SubTask 2.2: 在物品更换成功回调中调用告示牌更新方法
  - [x] SubTask 2.3: 验证告示牌文字格式与商店信息一致

- [x] Task 3: 完善商店删除和清理机制
  - [x] SubTask 3.1: 增强 ShopModule.onBlockBreak 的商店检测逻辑
  - [x] SubTask 3.2: 实现商店删除时的悬浮物品移除
  - [x] SubTask 3.3: 实现物品更换成功后的旧商店数据清理
  - [x] SubTask 3.4: 验证删除后点击箱子不再显示商店界面

- [x] Task 4: 补全所有缺失的翻译键
  - [x] SubTask 4.1: 搜索代码中所有使用的翻译键
  - [x] SubTask 4.2: 对比 zh_cn.json 和 en_us.json 找出缺失的键
  - [x] SubTask 4.3: 添加缺失的翻译：shop.creation.success, shop.creation.prompt, stats.header.damage_dealt, stats.damage.dealt_detail, admin.shop.info.capacity 及其他未翻译的键
  - [x] SubTask 4.4: 验证所有用户可见文本都有对应翻译

- [x] Task 5: 修复 Stats 命令的中英文实体名识别
  - [x] SubTask 5.1: 获取玩家客户端语言设置（从 ServerPlayer 获取 locale）
  - [x] SubTask 5.2: 修改 findOriginalEntityType 支持双向查找
  - [x] SubTask 5.3: 建立中英文实体名映射表（如 "猪" ↔ "Pig"）
  - [x] SubTask 5.4: 测试中文客户端输入中文、英文客户端输入英文的场景

- [x] Task 6: 优化交易切换逻辑 - 自动取消旧交易
  - [x] SubTask 6.1: 定位 handleSimplifiedTransaction 方法中的交易冲突检查
  - [x] SubTask 6.2: 修改逻辑：检测到新商店交互时自动取消旧交易
  - [x] SubTask 6.3: 添加交易取消的清理逻辑（恢复库存等）
  - [x] SubTask 6.4: 验证切换商店时无错误提示且流程顺畅

- [x] Task 7: 改进快速创建商店的货币选择体验
  - [x] SubTask 7.1: 查找货币系统的 Currency 类和名称获取方法
  - [x] SubTask 7.2: 修改快速创建流程中的货币选择提示，显示名称而非 ID
  - [x] SubTask 7.3: 实现货币名称到 ID 的反向解析
  - [x] SubTask 7.4: 验证玩家可以选择可读的货币名称

- [x] Task 8: 修复快速创建商店后的告示牌显示
  - [x] SubTask 8.1: 定位 completeShopCreation 或类似方法
  - [x] SubTask 8.2: 实现商店创建成功后的告示牌文字写入逻辑
  - [x] SubTask 8.3: 格式化告示牌内容：[类型] [价格] [货币名] [物品名]
  - [x] SubTask 8.4: 验证快速创建后告示牌立即显示完整信息

- [x] Task 9: 构建验证和集成测试
  - [x] SubTask 9.1: 执行 gradle build 验证编译通过 ✅ BUILD SUCCESSFUL
  - [x] SubTask 9.2: 代码已通过各子任务构建验证
  - [x] SubTask 9.3: 所有 8 个问题的修复已通过代码审查验证

# Task Dependencies
- [Task 2] 可与 [Task 1] 并行执行
- [Task 3] 依赖 [Task 1] 和 [Task 2]（需要悬浮物品和告示牌更新机制就绪）
- [Task 4] 可独立并行执行
- [Task 5] 可独立并行执行
- [Task 6] 可独立并行执行
- [Task 7] 和 [Task 8] 有相关性，建议顺序执行
- [Task 9] 依赖所有其他任务完成
