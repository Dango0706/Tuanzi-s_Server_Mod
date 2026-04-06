# 商店模块修复与优化计划

## 问题列表

1. **告示牌被破坏/推动时,上面的文字会消失** - 阻止破坏后文本消失
2. **收购时仍然会显示库存**
3. **新增快速创建商店功能**
4. **商店上方没有悬浮的物品展示**
5. **将所有指令注册完全** - `/shopconfirm` 等指令未注册

***

## 问题 1: 告示牌被破坏后文本消失

### 原因分析

* `BlockProtectionHandler.canBreakBlock()` 返回 `false` 阻止了破坏

* 但 Minecraft 在触发破坏事件前可能已经处理了告示牌的文本清除

* 或者告示牌的 `BlockEntity` 被移除后重新放置时文本丢失

### 解决方案

1. 在阻止破坏前，保存告示牌的文本内容
2. 如果破坏被阻止，恢复告示牌文本
3. 或者在 `PlayerBlockBreakEvents.BEFORE` 事件中更早地拦截

### 实现步骤

1. 修改 `BlockProtectionHandler.canBreakBlock()` 方法
2. 在返回 `false` 前，获取告示牌的 `SignBlockEntity`
3. 保存告示牌文本到临时变量
4. 返回 `false` 阻止破坏后，重新设置告示牌文本
5. 可能需要使用 `ServerLevel` 的方块更新来恢复

### 代码修改

```java
// BlockProtectionHandler.java
public boolean canBreakBlock(ServerPlayer player, BlockPos pos) {
    Optional<ShopInstance> shopOpt = shopManager.getShopByPos(pos);
    if (shopOpt.isEmpty()) {
        return true;
    }

    ShopInstance shop = shopOpt.get();
    if (shop.isOwner(player.getUUID()) || player.hasPermission(...)) {
        // 所有者破坏 - 删除商店数据
        shopManager.deleteShop(shop.getShopId());
        return true;
    }

    // 非所有者尝试破坏 - 阻止并恢复告示牌
    Level level = player.level();
    if (level.getBlockEntity(pos) instanceof SignBlockEntity signEntity) {
        // 保存并恢复告示牌文本的逻辑
        // ...
    }
    
    player.sendSystemMessage(...);
    return false;
}
```

***

## 问题 2: 收购时显示库存

### 原因分析

* `BlockInteractionHandler.sendShopInfo()` 对所有商店显示库存

* 收购商店（BUY）应该显示"收购物品容量"或不显示

### 解决方案

根据商店类型显示不同信息

### 实现步骤

修改 `BlockInteractionHandler.sendShopInfo()` 方法：

```java
if (!shop.isAdminShop() && !shop.isInfinite()) {
    if (shop.getShopType() == ShopType.SELL) {
        // 出售商店显示库存
        player.sendSystemMessage(translatable("admin.shop.info.stock", stock));
    } else {
        // 收购商店显示收购物品容量
        int capacity = getChestAvailableSpace(shop);
        player.sendSystemMessage(translatable("admin.shop.info.capacity", capacity));
    }
}
```

***

## 问题 3: 新增快速创建商店

### 功能描述

当玩家手持物品点击空附着在箱子上的告示牌时：

1. 系统发送信息：是否创建箱子商店？
2. 提供确认/取消按钮，或输入 yes/no
3. 8秒无输入则取消
4. 选择是后，让玩家选择出售/收购（15秒超时）
5. 输入价格和货币种类（15秒超时）
6. 输入备注（15秒超时，输入{None}为无备注，不输入则不创建）

### 实现步骤

1. 在 `ChatInputHandler` 中添加新的状态机
2. 创建 `PendingShopCreation` 类
3. 创建 `ShopCreationState` 枚举
4. 修改 `BlockInteractionHandler` 检测空告示牌点击
5. 实现各阶段的聊天输入处理

### 新增状态

```java
enum ShopCreationState {
    WAITING_CONFIRM,    // 等待确认创建
    SELECTING_TYPE,     // 选择出售/收购
    INPUTTING_PRICE,    // 输入价格
    INPUTTING_CURRENCY, // 输入货币
    INPUTTING_NOTE      // 输入备注
}
```

***

## 问题 4: 商店上方悬浮物品展示

### 解决方案

使用 `ItemDisplay` 实体展示物品

### 实现步骤

1. 在 `ShopDisplayManager.createDisplayForShop()` 中创建 `ItemDisplay` 实体
2. 设置位置为告示牌上方 0.5-1 格
3. 设置物品为商店交易物品
4. 设置 `BillboardConstraints.CENTER` 使其面向玩家
5. 存储实体 UUID 用于后续移除

***

## 问题 5: 指令注册不完全

### 当前问题

* 代码中使用了 `/shopconfirm yes` 和 `/shopconfirm no`

* 但实际注册的是 `/shop confirm`（有空格）

* 导致点击按钮时提示"未知命令"

### 需要注册的指令

1. `/shopconfirm yes` - 确认物品更换
2. `/shopconfirm no` - 取消物品更换
3. `/shopadmin list` - 列出所有商店
4. `/shopadmin tp <shopId>` - 传送到商店
5. `/shopadmin setOwner <player>` - 设置商店所有者
6. `/shop help` - 显示帮助
7. `/shopadmin help` - 显示管理员帮助

### 实现步骤

1. 创建 `ShopConfirmCommand.java` 注册 `/shopconfirm` 指令
2. 或修改 `ShopCommand.java` 使 `/shop confirm` 和 `/shopconfirm` 都可用
3. 在 `ShopAdminCommand` 中添加 list, tp, setOwner, help 子命令
4. 在 `ShopCommand` 中添加 help 子命令

### 代码修改

```java
// 新建 ShopConfirmCommand.java
public class ShopConfirmCommand {
    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("shopconfirm")
            .then(Commands.literal("yes")
                .executes(ctx -> handleConfirm(ctx, true)))
            .then(Commands.literal("no")
                .executes(ctx -> handleConfirm(ctx, false)))
        );
    }
}
```

***

## 实现顺序

1. **问题 5** - 修复指令注册（最紧急，影响现有功能）
2. **问题 2** - 修改库存显示逻辑（简单）
3. **问题 1** - 修复告示牌文本消失
4. **问题 3** - 快速创建商店
5. **问题 4** - 物品展示

***

## 文件修改清单

| 文件                             | 修改内容                            |
| ------------------------------ | ------------------------------- |
| `ShopConfirmCommand.java`      | 新建，注册 `/shopconfirm` 指令         |
| `ShopModule.java`              | 注册新指令，添加活塞事件                    |
| `BlockInteractionHandler.java` | 修改库存显示，添加空告示牌检测                 |
| `BlockProtectionHandler.java`  | 修复告示牌文本消失，添加活塞保护                |
| `ChatInputHandler.java`        | 添加快速创建商店状态机                     |
| `ShopDisplayManager.java`      | 实现物品展示功能                        |
| `ShopCommand.java`             | 添加 help 子命令                     |
| `ShopAdminCommand.java`        | 添加 list, tp, setOwner, help 子命令 |
| `ShopTranslationHelper.java`   | 添加新翻译键                          |
| 语言文件                           | 添加新翻译文本                         |

