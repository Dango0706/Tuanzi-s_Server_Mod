# 商店系统多项修复 Spec

## Why
发现商店系统存在多个功能缺陷，需要修复：
1. 悬浮物品只在快速创建时显示，其他方式创建不显示
2. 更换物品后告示牌文字未更新
3. 破坏告示牌后商店未被正确删除
4. 多个翻译键缺失
5. stats 指令无法正确显示中文
6. 切换商店交易时应自动取消上一个交易

## What Changes
- 修复悬浮物品显示逻辑，确保所有方式创建的商店都显示
- 修复物品更换后更新告示牌文字
- 修复告示牌破坏后正确删除商店
- 补全所有缺失的翻译键
- 修复 stats 指令中英文识别
- 修复切换商店时自动取消上一个交易

## Impact
- Affected code: `ShopDisplayManager`, `ChatInputHandler`, `BlockInteractionHandler`, `ShopModule`, `zh_cn.json`

## ADDED Requirements

### Requirement: 所有商店创建方式都显示悬浮物品
系统应在任何方式创建商店后显示悬浮物品。

#### Scenario: 通过命令/贴牌创建商店
- **WHEN** 使用 `/shopadmin create` 或贴牌方式创建商店
- **THEN** 商店位置上方应显示悬浮物品

### Requirement: 物品更换后更新告示牌文字
系统在物品更换成功后应更新告示牌上的文字。

#### Scenario: 物品更换成功
- **WHEN** 商店主手持新物品点击商店并确认更换
- **THEN** 告示牌文字应更新为新物品信息

### Requirement: 告示牌破坏后删除商店
系统应在告示牌被破坏后正确删除商店数据。

#### Scenario: 商店主破坏告示牌
- **WHEN** 商店主破坏自己商店的告示牌
- **THEN** 商店应从系统中移除，悬浮物品消失

### Requirement: 补全翻译键
系统应包含所有需要的翻译键。

#### Scenario: 显示消息
- **WHEN** 系统显示任何消息
- **THEN** 应使用正确的翻译键

### Requirement: Stats 中英文识别
stats 命令应根据玩家客户端语言识别实体名称。

#### Scenario: 中文玩家输入
- **WHEN** 玩家使用中文客户端输入 `/stats damage dealt "猪"`
- **THEN** 系统应正确识别为 Pig

### Requirement: 自动取消上一个交易
切换到新商店时应自动取消上一个待处理的交易。

#### Scenario: 点击不同商店
- **WHEN** 玩家有进行中的交易时点击另一个商店
- **THEN** 上一个交易应被自动取消，开始新商店的交易流程
