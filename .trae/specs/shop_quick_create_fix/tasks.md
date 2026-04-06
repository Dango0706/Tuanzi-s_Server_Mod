# Tasks

- [x] Task 1: 修复快速创建商店的容器检测逻辑
  - [x] SubTask 1.1: 修改 `ShopModule.handleQuickShopCreation` 中的容器检测，使用正确的 BlockEntity 类型检测
  - [x] SubTask 1.2: 检测 `BaseContainerBlockEntity` 或 `Container` 接口
  - [x] SubTask 1.3: 添加水平方向容器检测作为后备

- [x] Task 2: 商店创建后创建悬浮物品显示
  - [x] SubTask 2.1: 在 `ChatInputHandler.completeShopCreation` 中获取 ShopDisplayManager
  - [x] SubTask 2.2: 商店创建成功后调用 `displayManager.createDisplayForShop`
  - [x] SubTask 2.3: 使用玩家当前所在维度的 ServerLevel

# Task Dependencies
- [Task 2] 依赖 [Task 1]
