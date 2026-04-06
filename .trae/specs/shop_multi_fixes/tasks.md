# Tasks

- [x] Task 1: 修复悬浮物品显示 - 所有创建方式
  - [x] SubTask 1.1: 在 ShopDisplayManager 添加调试日志
  - [x] SubTask 1.2: 在 ShopModule.onBlockBreak 中添加商店删除逻辑

- [x] Task 2: 修复物品更换后更新告示牌文字
  - [x] SubTask 2.1: 在 ChatInputHandler 添加 updateSignText 方法
  - [x] SubTask 2.2: 在物品更换成功后调用更新方法

- [x] Task 3: 修复告示牌破坏后删除商店
  - [x] SubTask 3.1: 在 onBlockBreak 中检测商店并删除
  - [x] SubTask 3.2: 同时移除悬浮物品显示

- [x] Task 4: 补全翻译键
  - [x] SubTask 4.1: 查找所有缺失的翻译键
  - [x] SubTask 4.2: 在 zh_cn.json 中添加缺失的翻译

- [x] Task 5: 修复 stats 中英文识别
  - [x] SubTask 5.1: 修改 findOriginalEntityType 方法增加直接 key 匹配和反向翻译匹配

- [x] Task 6: 修复切换商店自动取消交易
  - [x] SubTask 6.1: 修改 handleSimplifiedTransaction 自动取消旧交易

# Task Dependencies
- 无特殊依赖，已并行处理完成
