# Advanced Chest Shop 模块 Spec

## Why
服务器需要一个基于告示牌的箱子商店系统，让玩家能够安全地买卖物品，并与现有的 Economy API 无缝集成，支持多种货币、动态定价、可视化和防破坏等高级功能。

## What Changes
- 新增基于告示牌的箱子商店系统，支持出售店(Sell Shop)和收购店(Buy Shop)
- 新增多货币支持，玩家创建商店时需指定货币显示名称
- 新增商店所有权管理和权限保护机制
- 新增聊天界面交互系统，支持左键/右键点击
- 新增管理员商店功能，支持无限库存和动态定价
- 新增 ItemDisplayEntity 可视化展示系统
- 新增防破坏保护机制（爆炸、活塞）
- 新增交易日志审计系统
- 新增 Shift+点击批量交易功能
- 新增告示牌自动涂蜡功能

## Impact
- Affected specs: economy_api（依赖 Economy API 进行货币交易）
- Affected code: 新增 `me.tuanzi.shop` 包及其子包

## ADDED Requirements

### Requirement: 商店创建与识别
系统 SHALL 支持玩家通过告示牌创建商店。

#### Scenario: 创建出售商店
- **WHEN** 玩家在箱子上放置告示牌
- **AND** 告示牌第一行写入 "[Sell]"（本地化）
- **AND** 告示牌第二行写入物品 ID 或名称
- **AND** 告示牌第三行写入 `价格 货币显示名称`（如 `10 金币` 或 `0.5 软妹币`）
- **AND** 告示牌第四行写入备注（可选）
- **THEN** 系统识别为出售商店
- **AND** 初始化 ShopInstance
- **AND** 记录放置者为商店所有者
- **AND** 根据货币显示名称查找对应的货币ID并记录
- **AND** 自动对告示牌涂蜡，防止手动修改

#### Scenario: 创建收购商店
- **WHEN** 玩家在箱子上放置告示牌
- **AND** 告示牌第一行写入 "[Buy]"（本地化）
- **AND** 告示牌第二行写入物品 ID 或名称
- **AND** 告示牌第三行写入 `价格 货币显示名称`（如 `10 金币` 或 `0.5 软妹币`）
- **AND** 告示牌第四行写入备注（可选）
- **THEN** 系统识别为收购商店
- **AND** 初始化 ShopInstance
- **AND** 记录放置者为商店所有者
- **AND** 根据货币显示名称查找对应的货币ID并记录
- **AND** 自动对告示牌涂蜡，防止手动修改

#### Scenario: 告示牌格式验证失败
- **WHEN** 玩家在箱子上放置告示牌
- **AND** 告示牌格式不符合商店模式
- **THEN** 系统不创建商店
- **AND** 告示牌保持普通状态

#### Scenario: 货币显示名称验证
- **WHEN** 玩家创建商店时指定货币显示名称
- **AND** 指定的货币显示名称不存在
- **THEN** 系统拒绝创建商店
- **AND** 发送错误提示告知货币显示名称无效

#### Scenario: 告示牌自动涂蜡
- **WHEN** 商店创建成功
- **THEN** 告示牌自动被涂蜡
- **AND** 玩家无法手动编辑告示牌内容
- **AND** 保护商店信息不被篡改

### Requirement: 商店所有权保护
系统 SHALL 保护商店免受非所有者的非法操作。

#### Scenario: 非所有者尝试打开商店箱子
- **WHEN** 非所有者玩家尝试打开商店箱子
- **THEN** 阻止打开操作
- **AND** 发送提示消息告知这是受保护的商店

#### Scenario: 非所有者尝试破坏商店方块
- **WHEN** 非所有者玩家尝试破坏商店箱子或告示牌
- **THEN** 阻止破坏操作
- **AND** 发送提示消息告知只有所有者才能破坏

#### Scenario: 所有者操作商店
- **WHEN** 商店所有者尝试打开箱子或破坏方块
- **THEN** 允许操作正常执行

### Requirement: 聊天界面交互
系统 SHALL 通过聊天消息提供交互式购买/出售界面。

#### Scenario: 玩家左键点击商店
- **WHEN** 玩家左键点击商店告示牌或箱子
- **THEN** 发送包含商店信息的聊天消息
- **AND** 消息包含可点击的购买/出售按钮
- **AND** 按钮使用 ClickEvent 执行交易命令
- **AND** 按钮使用 HoverEvent 显示详细信息

#### Scenario: 玩家右键点击商店
- **WHEN** 玩家右键点击商店告示牌或箱子
- **THEN** 发送包含商店信息的聊天消息
- **AND** 消息包含可点击的购买/出售按钮
- **AND** 按钮使用 ClickEvent 执行交易命令
- **AND** 按钮使用 HoverEvent 显示详细信息

#### Scenario: 聊天消息显示商店信息
- **WHEN** 商店交互消息发送给玩家
- **THEN** 消息包含：商店类型、物品名称、单价、库存数量、货币显示名称
- **AND** 不显示货币ID，始终显示货币的 displayName
- **AND** 提供可点击的交易按钮

### Requirement: 出售商店交易逻辑
系统 SHALL 实现出售商店(Sell Shop)的完整交易流程。

#### Scenario: 玩家购买物品成功
- **WHEN** 玩家点击出售商店的购买按钮
- **AND** 玩家有足够余额（指定货币类型）
- **AND** 商店箱子有足够物品
- **THEN** 从玩家账户扣除金额（指定货币）
- **AND** 向所有者账户存入金额（指定货币）
- **AND** 物品从商店箱子移动到玩家背包
- **AND** 记录交易日志
- **AND** 更新商店统计（用于动态定价）

#### Scenario: 玩家余额不足
- **WHEN** 玩家点击购买按钮
- **AND** 玩家在指定货币类型下余额不足
- **THEN** 发送余额不足提示（显示货币 displayName）
- **AND** 不执行交易

#### Scenario: 商店库存不足
- **WHEN** 玩家点击购买按钮
- **AND** 商店箱子库存不足
- **THEN** 发送库存不足提示
- **AND** 不执行交易

### Requirement: 收购商店交易逻辑
系统 SHALL 实现收购商店(Buy Shop)的完整交易流程。

#### Scenario: 玩家出售物品成功
- **WHEN** 玩家点击收购商店的出售按钮
- **AND** 玩家背包有足够物品
- **AND** 所有者在指定货币类型下有足够余额
- **AND** 商店箱子有足够空间
- **THEN** 从所有者账户扣除金额（指定货币）
- **AND** 向玩家账户存入金额（指定货币）
- **AND** 物品从玩家背包移动到商店箱子
- **AND** 记录交易日志
- **AND** 更新商店统计（用于动态定价）

#### Scenario: 玩家物品不足
- **WHEN** 玩家点击出售按钮
- **AND** 玩家背包物品不足
- **THEN** 发送物品不足提示
- **AND** 不执行交易

#### Scenario: 收购者（所有者）余额不足
- **WHEN** 玩家点击出售按钮
- **AND** 所有者在指定货币类型下余额不足
- **THEN** 不执行交易
- **AND** 向玩家发送提示：商店所有者余额不足，无法完成交易（显示货币 displayName）
- **AND** 向所有者发送提示：您的收购商店余额不足，请及时补充（显示货币 displayName）

#### Scenario: 商店箱子空间不足
- **WHEN** 玩家点击出售按钮
- **AND** 商店箱子空间不足
- **THEN** 发送空间不足提示
- **AND** 不执行交易

### Requirement: 管理员商店功能
系统 SHALL 支持管理员商店(Admin Shop)的特殊功能。

#### Scenario: 创建管理员商店
- **WHEN** 管理员执行 `/shopadmin create` 命令
- **THEN** 创建管理员商店
- **AND** 商店标记为 Admin Shop
- **AND** 商店拥有无限库存和资金

#### Scenario: 管理员商店交易
- **WHEN** 玩家与管理员商店交易
- **THEN** 不检查箱子库存
- **AND** 不检查所有者余额
- **AND** 物品直接生成或消失
- **AND** 资金直接从虚空扣除或存入虚空

#### Scenario: 设置商店无限模式（基于玩家面对的商店）
- **WHEN** 管理员面对/指向一个商店
- **AND** 管理员执行 `/shopadmin set-infinite <true/false>`
- **THEN** 更新玩家面对的商店的无限库存状态
- **AND** 发送操作成功提示

### Requirement: 动态定价算法
系统 SHALL 为管理员商店实现基于供需的动态定价。

#### Scenario: 动态定价计算
- **WHEN** 管理员商店启用动态定价
- **THEN** 价格按公式计算：`Price = BasePrice * (TotalSold / TotalBought)`
- **AND** 价格受最小值和最大值边界限制
- **AND** 防止除零错误（当 TotalBought = 0 时使用默认值）

#### Scenario: 启用/禁用动态定价（基于玩家面对的商店）
- **WHEN** 管理员面对/指向一个商店
- **AND** 管理员执行 `/shopadmin dynamic-toggle`
- **THEN** 切换玩家面对的商店的动态定价状态
- **AND** 发送操作成功提示

#### Scenario: 价格边界保护
- **WHEN** 动态定价计算结果超出边界
- **THEN** 价格限制在 minPrice 和 maxPrice 之间

### Requirement: 管理员命令
系统 SHALL 提供完整的管理员命令集（基于玩家面对/指向的商店）。

#### Scenario: 删除商店第一次确认（基于玩家面对的商店）
- **WHEN** 管理员面对/指向一个商店
- **AND** 管理员执行 `/shopadmin delete`
- **THEN** 发送确认提示：确认要删除此商店吗？请再次执行命令确认
- **AND** 记录删除确认状态

#### Scenario: 删除商店第二次确认
- **WHEN** 管理员已执行第一次删除命令
- **AND** 管理员在确认超时时间内再次执行 `/shopadmin delete`
- **THEN** 删除玩家面对的商店
- **AND** 清理相关数据
- **AND** 移除 ItemDisplayEntity
- **AND** 发送操作成功提示

#### Scenario: 删除确认超时
- **WHEN** 管理员执行第一次删除命令
- **AND** 超过确认超时时间（如30秒）未执行第二次命令
- **THEN** 取消删除确认状态
- **AND** 需要重新执行删除命令

#### Scenario: 查看商店信息（基于玩家面对的商店）
- **WHEN** 管理员面对/指向一个商店
- **AND** 管理员执行 `/shopadmin info`
- **THEN** 显示玩家面对的商店详细信息
- **AND** 包含：所有者、物品、价格、货币显示名称、库存、统计数据
- **AND** 不显示货币ID

#### Scenario: 设置商店价格（基于玩家面对的商店）
- **WHEN** 管理员面对/指向一个商店
- **AND** 管理员执行 `/shopadmin set-price <价格>`
- **THEN** 更新玩家面对的商店的价格
- **AND** 发送操作成功提示

#### Scenario: 设置商店货币类型（基于玩家面对的商店）
- **WHEN** 管理员面对/指向一个商店
- **AND** 管理员执行 `/shopadmin set-currency <货币显示名称>`
- **THEN** 根据货币显示名称查找对应的货币ID
- **AND** 更新玩家面对的商店的货币类型
- **AND** 发送操作成功提示

### Requirement: 数据持久化
系统 SHALL 持久化存储所有商店数据。

#### Scenario: 商店数据存储
- **WHEN** 商店创建或更新
- **THEN** 数据保存到 `shop_registry.json` 或 PersistentState
- **AND** 包含：坐标、UUID、物品NBT、价格、货币ID、统计数据

#### Scenario: 服务器启动加载数据
- **WHEN** 服务器启动
- **THEN** 自动加载所有商店数据
- **AND** 重建商店实例
- **AND** 重新生成 ItemDisplayEntity

#### Scenario: 高效商店查找
- **WHEN** 玩家与方块交互
- **THEN** 使用坐标索引快速查找商店
- **AND** 查找时间复杂度为 O(1)

### Requirement: 可视化展示
系统 SHALL 自动在商店上方显示交易物品。

#### Scenario: 创建 ItemDisplayEntity
- **WHEN** 商店创建成功
- **THEN** 在箱子上方召唤 ItemDisplayEntity
- **AND** 实体显示商店交易的物品
- **AND** 实体位置正确悬浮

#### Scenario: 更新展示物品
- **WHEN** 商店交易物品变更
- **THEN** 更新 ItemDisplayEntity 显示的物品

#### Scenario: 移除展示实体
- **WHEN** 商店被删除
- **THEN** 移除对应的 ItemDisplayEntity

### Requirement: 防破坏保护
系统 SHALL 保护商店方块免受破坏。

#### Scenario: 爆炸保护
- **WHEN** 爆炸事件（Creeper/TNT）发生
- **AND** 爆炸范围包含商店方块
- **THEN** 商店方块不受爆炸影响
- **AND** 商店保持完整

#### Scenario: 活塞保护
- **WHEN** 活塞尝试推动或拉动商店方块
- **THEN** 阻止活塞操作
- **AND** 商店方块保持原位

### Requirement: Shift批量交易
系统 SHALL 支持 Shift+点击进行批量交易。

#### Scenario: Shift+左键/右键购买
- **WHEN** 玩家 Shift+左键或 Shift+右键点击出售商店
- **AND** 玩家有足够余额
- **AND** 商店有足够库存
- **THEN** 购买整组物品（64个）
- **AND** 执行完整的交易流程

#### Scenario: Shift+左键/右键出售
- **WHEN** 玩家 Shift+左键或 Shift+右键点击收购商店
- **AND** 玩家有足够物品
- **AND** 商店有足够空间
- **AND** 所有者有足够余额
- **THEN** 出售整组物品（64个）
- **AND** 执行完整的交易流程

### Requirement: 交易日志
系统 SHALL 记录所有交易到独立日志文件。

#### Scenario: 记录交易日志
- **WHEN** 任何交易完成
- **THEN** 记录到 `shop_transactions.log` 文件
- **AND** 包含：时间戳、商店ID、买家、卖家、物品、数量、金额、货币显示名称、交易箱ID、位置

#### Scenario: 日志格式
- **WHEN** 日志写入
- **THEN** 格式为：`[时间戳] [商店ID] 玩家:玩家名 | 类型:买/卖 | 物品:物品名 | 数量:N | 金额:N 货币显示名称 | 卖方:xxx | 交易箱id:xxx,位置:x:123,y:64,z:-123`

### Requirement: 物品NBT支持
系统 SHALL 完整支持物品的 NBT/DataComponent 数据。

#### Scenario: 附魔物品交易
- **WHEN** 商店交易附魔物品
- **THEN** 正确保留所有附魔信息
- **AND** 交易后物品附魔不变

#### Scenario: 耐久度物品交易
- **WHEN** 商店交易有耐久度的物品
- **THEN** 正确保留耐久度信息
- **AND** 交易后物品耐久度不变

#### Scenario: 自定义NBT数据
- **WHEN** 商店交易包含自定义NBT的物品
- **THEN** 完整保留所有NBT数据
- **AND** 交易后物品数据完整

## 技术规范

### 核心类结构

```
me.tuanzi.shop/
├── ShopModule.java                 # 模块入口
├── shop/
│   ├── ShopInstance.java          # 商店实例数据类
│   ├── ShopType.java              # 商店类型枚举 (SELL, BUY)
│   ├── ShopManager.java           # 商店管理器（核心逻辑）
│   └── ShopRegistry.java          # 商店注册表（数据存储）
├── events/
│   ├── SignChangeHandler.java     # 告示牌放置事件处理
│   ├── BlockInteractionHandler.java # 方块交互事件处理（左键/右键）
│   ├── BlockProtectionHandler.java  # 方块保护事件处理
│   └── ShopTransactionEvent.java    # 交易事件
├── pricing/
│   ├── DynamicPricing.java        # 动态定价算法
│   └── PriceConfig.java           # 价格配置
├── display/
│   └── ShopDisplayManager.java    # ItemDisplayEntity 管理
├── commands/
│   ├── ShopAdminCommand.java      # 管理员命令（基于玩家面对的商店）
│   └── ShopCommand.java           # 玩家命令
├── storage/
│   ├── ShopData.java              # 商店数据模型
│   └── ShopStateSaver.java        # PersistentState 存储
├── logging/
│   └── TransactionLogger.java     # 交易日志记录器
└── utils/
    ├── ShopTranslationHelper.java # 翻译助手
    └── ItemUtils.java             # 物品工具类
```

### ShopInstance 数据结构

```java
public class ShopInstance {
    private UUID shopId;           // 商店唯一ID
    private UUID ownerId;          // 所有者UUID
    private BlockPos shopPos;      // 商店位置（箱子）
    private BlockPos signPos;      // 告示牌位置
    private ShopType shopType;     // 商店类型
    private ItemStack tradeItem;   // 交易物品（含NBT）
    private double basePrice;      // 基础价格
    private double currentPrice;   // 当前价格（动态定价）
    private boolean isAdminShop;   // 是否管理员商店
    private boolean isInfinite;    // 是否无限库存
    private boolean dynamicPricing;// 是否启用动态定价
    private double minPrice;       // 最低价格
    private double maxPrice;       // 最高价格
    private long totalSold;        // 总售出数量
    private long totalBought;      // 总收购数量
    private String walletTypeId;   // 货币类型ID（必需）
    private long createdTime;      // 创建时间
}
```

### 动态定价算法

```java
public class DynamicPricing {
    public static double calculatePrice(ShopInstance shop) {
        if (!shop.isDynamicPricing()) {
            return shop.getBasePrice();
        }
        
        double basePrice = shop.getBasePrice();
        long sold = shop.getTotalSold();
        long bought = shop.getTotalBought();
        
        double ratio;
        if (bought == 0) {
            ratio = sold > 0 ? 2.0 : 1.0;
        } else {
            ratio = (double) sold / bought;
        }
        
        double price = basePrice * ratio;
        
        return Math.max(shop.getMinPrice(), 
               Math.min(shop.getMaxPrice(), price));
    }
}
```

### 告示牌模式

```
第一行: [Sell] 或 [Buy]（支持本地化）
第二行: 物品ID（如 minecraft:diamond）或物品名称
第三行: 价格 货币显示名称（如 10 金币 或 0.5 软妹币）
第四行: 备注（可选）
```

### 货币显示名称解析

```java
public String parseCurrencyDisplayName(String line3) {
    String[] parts = line3.trim().split("\\s+", 2);
    if (parts.length < 2) {
        return null;
    }
    return parts[1];
}

public Optional<WalletType> findWalletTypeByDisplayName(String displayName) {
    return EconomyAPI.getAllWalletTypes().stream()
        .filter(wt -> wt.displayName().getString().equals(displayName))
        .findFirst();
}
```

### 告示牌自动涂蜡

```java
public void waxSign(BlockPos signPos, ServerLevel level) {
    BlockState state = level.getBlockState(signPos);
    if (state.getBlock() instanceof SignBlock) {
        Block waxedSign = getWaxedSignVariant(state.getBlock());
        level.setBlock(signPos, waxedSign.defaultBlockState()
            .setValue(SignBlock.FACING, state.getValue(SignBlock.FACING)), 3);
    }
}
```

### 交易日志格式

```
[2025-03-31 14:30:25] [shop-uuid-here] 玩家:Steve | 类型:买 | 物品:minecraft:diamond | 数量:64 | 金额:640.0 金币 | 卖方:Alex | 交易箱id:chest-uuid,位置:x:123,y:64,z:-123
```

### 管理员命令（基于玩家面对的商店）

```
/shopadmin create                    - 创建管理员商店
/shopadmin set-infinite <true/false> - 设置无限库存模式
/shopadmin dynamic-toggle            - 切换动态定价
/shopadmin delete                    - 删除商店（需二次确认）
/shopadmin info                      - 查看商店信息
/shopadmin set-price <价格>          - 设置价格
/shopadmin set-currency <货币显示名称> - 设置货币类型
```

## 约束
- Minecraft 版本：26.1
- Java 版本：25
- Fabric Loader：0.18.4
- Fabric API：0.144.3+26.1
- 依赖：Economy API 模块
- 所有货币必须由管理员创建，不存在默认货币

## 验收标准

### AC-1：商店创建成功
- **给定**：玩家在箱子上放置正确格式的告示牌
- **当**：告示牌放置完成
- **然后**：商店成功创建并显示确认消息
- **验证**：程序化测试

### AC-2：多货币支持正确
- **给定**：玩家创建商店时指定货币显示名称
- **当**：交易发生
- **然后**：使用正确的货币类型进行交易
- **验证**：程序化测试

### AC-3：交易流程正确
- **给定**：商店已创建且有足够库存/资金
- **当**：玩家执行交易
- **然后**：物品和货币正确转移
- **验证**：程序化测试

### AC-4：收购者余额不足提示
- **给定**：收购商店所有者余额不足
- **当**：玩家尝试出售物品
- **然后**：双方收到提示消息（显示货币 displayName）
- **验证**：人工验证

### AC-5：所有权保护有效
- **给定**：商店已创建
- **当**：非所有者尝试破坏或打开
- **然后**：操作被阻止
- **验证**：人工验证

### AC-6：管理员命令基于面对的商店
- **给定**：管理员面对一个商店
- **当**：执行管理员命令
- **然后**：操作应用于面对的商店
- **验证**：人工验证

### AC-7：动态定价正确计算
- **给定**：管理员商店启用动态定价
- **当**：交易发生
- **然后**：价格按算法更新
- **验证**：程序化测试

### AC-8：可视化展示正常
- **给定**：商店已创建
- **当**：玩家靠近商店
- **然后**：看到悬浮的物品展示
- **验证**：人工验证

### AC-9：防破坏保护有效
- **给定**：商店已创建
- **当**：爆炸或活塞尝试影响商店
- **然后**：商店不受影响
- **验证**：人工验证

### AC-10：交易日志完整
- **给定**：交易完成
- **当**：查看日志文件
- **然后**：包含完整交易记录（含卖方、交易箱ID、位置、货币显示名称）
- **验证**：程序化测试

### AC-11：数据持久化正确
- **给定**：商店数据已保存
- **当**：服务器重启
- **然后**：所有商店数据正确恢复
- **验证**：程序化测试

### AC-12：左键/右键交互正常
- **给定**：商店已创建
- **当**：玩家左键或右键点击
- **然后**：都触发交互界面
- **验证**：人工验证

### AC-13：告示牌自动涂蜡
- **给定**：商店创建成功
- **当**：玩家尝试编辑告示牌
- **然后**：无法编辑（告示牌已涂蜡）
- **验证**：人工验证

### AC-14：删除商店二次确认
- **给定**：管理员执行删除命令
- **当**：第一次执行
- **然后**：提示确认，不立即删除
- **当**：第二次执行（在超时时间内）
- **然后**：商店被删除
- **验证**：人工验证

### AC-15：货币显示名称正确显示
- **给定**：商店使用特定货币
- **当**：玩家查看商店信息或交易
- **然后**：显示货币的 displayName，不显示货币ID
- **验证**：人工验证
