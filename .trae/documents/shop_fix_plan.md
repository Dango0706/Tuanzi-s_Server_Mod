# 商店系统修复与优化计划

## 问题分析

### 1. 命令未注册
**原因**: `ShopModule.onInitialize()` 中注册命令时，`shopManager` 和 `interactionHandler` 为 null，因为它们是在 `initializeForServer()` 中初始化的，而该方法只在服务器启动时调用。

**解决方案**: 将命令注册移到 `ServerLifecycleEvents.SERVER_STARTING` 之后，或者使用静态方法注册命令，在命令执行时获取 ShopManager 实例。

### 2. 翻译键显示问题
**原因**: `ShopTranslationHelper.translatable()` 返回的是翻译键格式 `tuanzis-server-mod:key`，但语言文件中没有对应的商店相关翻译键。

**解决方案**: 在 `zh_cn.json` 和 `en_us.json` 中添加所有商店相关的翻译键，参考其他模块使用 `§` 颜色代码。

### 3. 增加文本色彩
**解决方案**: 在翻译文件中使用 Minecraft 颜色代码：
- `§a` 绿色 (成功)
- `§c` 红色 (错误)
- `§e` 黄色 (提示)
- `§b` 青色 (信息)
- `§6` 金色 (标题)
- `§f` 白色 (内容)

### 4. 简化购买流程
**需求**: 点击牌子后提示输入数量，8秒超时取消。

**解决方案**: 
- 创建 `PendingTransactionManager` 管理待处理交易
- 使用 `ServerChatEvent` 监听玩家聊天输入
- 添加配置项 `inputTimeoutSeconds` (默认8秒)
- 添加管理员命令 `/shopadmin set-timeout <秒>`

### 5. 物品识别无法识别中文
**原因**: `parseItemStack()` 只支持物品ID格式 (如 `minecraft:diamond` 或 `diamond`)。

**解决方案**: 
- 使用 `ItemUtils` 通过物品显示名称查找物品
- 遍历物品注册表，匹配中文名称
- 支持模糊匹配

### 6. 商店所有者更换物品
**需求**: 手持物品点击牌子更换物品，确认/取消按钮，8秒超时。

**解决方案**:
- 在 `BlockInteractionHandler` 中检测商店所有者手持物品点击
- 创建 `PendingItemChangeManager` 管理待确认更换
- 发送带点击按钮的消息
- 监听 `yes`/`no` 聊天输入

---

## 实施步骤

### 步骤 1: 修复命令注册
**文件**: `ShopModule.java`, `ShopCommand.java`, `ShopAdminCommand.java`

1. 修改命令注册方式，使用静态注册，在命令执行时获取实例
2. 将命令注册逻辑移到 `onInitialize()` 中，不依赖实例变量

### 步骤 2: 添加翻译键
**文件**: `zh_cn.json`, `en_us.json`

1. 添加所有商店相关翻译键
2. 使用颜色代码美化输出

### 步骤 3: 修复 ShopTranslationHelper
**文件**: `ShopTranslationHelper.java`

1. 添加颜色常量
2. 添加带颜色的组件创建方法
3. 确保翻译正确显示

### 步骤 4: 实现简化购买流程
**新文件**: `PendingTransactionManager.java`, `ShopConfig.java`

1. 创建配置类存储超时时间
2. 创建待处理交易管理器
3. 修改 `BlockInteractionHandler` 发送提示而非直接交易
4. 创建聊天监听器处理数量输入

### 步骤 5: 支持中文物品识别
**文件**: `ItemUtils.java`, `SignChangeHandler.java`

1. 添加 `findItemByDisplayName()` 方法
2. 修改 `parseItemStack()` 支持中文名称

### 步骤 6: 实现物品更换功能
**新文件**: `PendingItemChangeManager.java`

1. 创建待确认更换管理器
2. 修改 `BlockInteractionHandler` 处理所有者点击
3. 添加确认/取消按钮和聊天监听

### 步骤 7: 添加管理员配置命令
**文件**: `ShopAdminCommand.java`

1. 添加 `/shopadmin set-timeout <秒>` 命令
2. 添加 `/shopadmin reload` 命令

### 步骤 8: 测试验证
1. 运行 `gradle build` 验证编译
2. 运行 `gradle runClient` 测试功能

---

## 详细修改清单

### 新增文件
1. `ShopConfig.java` - 配置管理
2. `PendingTransactionManager.java` - 待处理交易管理
3. `PendingItemChangeManager.java` - 待确认物品更换管理
4. `ChatInputListener.java` - 聊天输入监听

### 修改文件
1. `ShopModule.java` - 修复命令注册，添加配置加载
2. `ShopCommand.java` - 修改为静态方法
3. `ShopAdminCommand.java` - 添加配置命令
4. `ShopTranslationHelper.java` - 添加颜色支持
5. `BlockInteractionHandler.java` - 实现简化流程和物品更换
6. `SignChangeHandler.java` - 支持中文物品识别
7. `ItemUtils.java` - 添加中文名称查找
8. `zh_cn.json` - 添加翻译
9. `en_us.json` - 添加翻译

---

## 翻译键列表

```json
{
  "shop.type.sell": "§b出售商店",
  "shop.type.buy": "§b收购商店",
  "shop.created.success": "§a商店创建成功！",
  "shop.created.failed": "§c商店创建失败！",
  "shop.deleted.success": "§a商店已删除！",
  "shop.deleted.confirm": "§e确认要删除此商店吗？请再次执行命令确认。",
  "shop.not_found": "§c未找到商店！",
  "shop.not_looking": "§c您没有面对任何商店！",
  "shop.no_permission": "§c您没有权限操作此商店！",
  "shop.protected": "§c此商店受保护！",
  "shop.sign.not_on_chest": "§c告示牌必须放置在箱子上！",
  "shop.sign.invalid_item": "§c无效的物品！",
  "shop.sign.invalid_price": "§c无效的价格！",
  "shop.sign.invalid_currency": "§c无效的货币显示名称！",
  
  "transaction.input.prompt": "§e请在 §a%d秒 §e内输入购买数量（输入数字或 'cancel' 取消）:",
  "transaction.input.timeout": "§c输入超时，交易已取消！",
  "transaction.input.cancelled": "§e交易已取消！",
  "transaction.input.invalid": "§c无效的数量，请输入正整数！",
  "transaction.buy.success": "§a购买成功！花费: §e%.2f %s",
  "transaction.sell.success": "§a出售成功！获得: §e%.2f %s",
  "transaction.insufficient_balance": "§c余额不足！需要: §e%.2f %s",
  "transaction.insufficient_items": "§c物品不足！",
  "transaction.insufficient_stock": "§c商店库存不足！",
  "transaction.shift_hint": "§7按住Shift点击可批量交易(64个)",
  
  "item.change.prompt": "§e确认要将商店物品更换为 §b%s §e吗？",
  "item.change.confirm": "§a[确认]",
  "item.change.cancel": "§c[取消]",
  "item.change.success": "§a商店物品已更换为: §b%s",
  "item.change.timeout": "§c操作超时，物品更换已取消！",
  "item.change.cancelled": "§e物品更换已取消！",
  
  "admin.shop.info.title": "§6=== 商店信息 ===",
  "admin.shop.set_infinite": "§a已设置商店无限库存模式: §f%s",
  "admin.shop.dynamic_toggled": "§a已切换动态定价: §f%s",
  "admin.shop.price_set": "§a已设置商店价格: §e%.2f",
  "admin.shop.currency_set": "§a已设置商店货币: §f%s",
  "admin.shop.timeout_set": "§a输入超时时间已设置为: §f%d秒",
  "admin.no_permission": "§c您没有管理员权限！"
}
```
