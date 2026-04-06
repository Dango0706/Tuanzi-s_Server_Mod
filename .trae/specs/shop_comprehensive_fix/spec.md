# 商店系统综合修复 Spec

## Why
商店系统存在多个关键功能缺陷，影响用户体验和系统稳定性：
1. 悬浮物品显示不一致
2. 物品更换后告示牌文字不更新
3. 商店删除/破坏机制失效
4. 翻译键缺失导致显示异常
5. Stats 命令无法正确识别中英文实体名
6. 交易切换流程不合理
7. 快速创建商店用户体验差（需输入ID而非名称）
8. 快速创建后告示牌无显示

## What Changes
- **修复悬浮物品显示逻辑**：确保所有创建方式（快速创建、贴牌、修改牌子）都能正确显示悬浮物品
- **修复物品更换后的告示牌更新**：手持物品修改商店物品后，立即更新告示牌文字
- **完善商店删除机制**：确保告示牌破坏或物品更换成功后，商店能被正确删除，悬浮物品消失
- **补全所有缺失的翻译键**：查找并修复所有未被正确翻译的键值对
- **优化 Stats 命令的中英文识别**：根据玩家客户端语言设置自动识别中英文实体名
- **优化交易切换逻辑**：点击新商店时自动取消上一个交易，无需手动取消
- **改进快速创建商店体验**：将货币 ID 输入改为货币显示名称选择
- **修复快速创建后的告示牌显示**：确保快速创建商店后，告示牌上正确显示商店信息

## Impact
- Affected specs: chest_shop_system, shop_multi_fixes, shop_quick_create_fix, statistics_module, translation_fix
- Affected code:
  - ShopModule.java (商店核心逻辑)
  - ShopDisplayManager.java (悬浮物品管理)
  - ChatInputHandler.java (聊天输入处理)
  - StatsCommand.java (统计命令)
  - StatsTranslationHelper.java (统计翻译)
  - zh_cn.json / en_us.json (翻译文件)
  - CurrencyManager/Currency 相关类 (货币系统)

## ADDED Requirements

### Requirement: 统一的悬浮物品显示系统
The system SHALL ensure that floating item displays are created for ALL shop creation methods:
- Quick creation (手持物品 + 空告示牌)
- Sign placement on existing chest (贴牌创建)
- Sign modification after creation (修改牌子)

#### Scenario: 所有方式创建商店都有悬浮物品
- **WHEN** player creates a shop using any valid method
- **THEN** a floating item display SHALL appear above the chest showing the sold/bought item

### Requirement: 实时告示牌文字更新
The system SHALL update sign text immediately when shop items are changed via hand-held items.

#### Scenario: 物品更换后告示牌更新
- **WHEN** player uses hand-held item to change shop's sell/buy item
- **THEN** the sign text SHALL be updated to reflect the new item immediately

### Requirement: 可靠的商店删除机制
The system SHALL properly delete shops and clean up associated displays when:
- Shop owner breaks the sign
- Shop item is successfully replaced (creating a new shop instance)
- Admin deletes the shop

#### Scenario: 告示牌破坏后商店删除
- **WHEN** shop owner breaks the shop's sign
- **THEN** the shop SHALL be removed from database AND floating display SHALL disappear
- **AND** clicking the chest SHALL NOT show any shop interface

#### Scenario: 物品更换后旧商店清理
- **WHEN** player successfully replaces shop item
- **THEN** the old shop data SHALL be cleaned up properly
- **AND** a new shop with the new item SHALL be created

### Requirement: 完整的翻译覆盖
The system SHALL provide translations for ALL user-facing strings in both zh_cn and en_us.

#### Scenario: 所有键值对都被翻译
- **WHEN** system displays any message to player
- **THEN** the message SHALL use proper translation key
- **AND** translation SHALL exist in both language files

### Requirement: 智能 Stats 实体名识别
The system SHALL detect and accept both Chinese and English entity names based on player's client language setting.

#### Scenario: 中文客户端使用中文名
- **WHEN** player uses Chinese client and executes `/stats damage dealt "猪"`
- **THEN** system SHALL correctly identify the entity as "Pig" internally

#### Scenario: 英文客户端使用英文名
- **WHEN** player uses English client and executes `/stats damage dealt "Pig"`
- **THEN** system SHALL correctly process the command without errors

### Requirement: 无缝交易切换
The system SHALL automatically cancel previous transaction when player interacts with a different shop.

#### Scenario: 切换商店自动取消旧交易
- **WHEN** player has an active transaction with Shop A
- **AND** player clicks on Shop B
- **THEN** transaction with Shop A SHALL be cancelled automatically
- **AND** new transaction with Shop B SHALL start without error message

### Requirement: 用户友好的货币选择
The system SHALL display currency names instead of IDs during quick shop creation.

#### Scenario: 快速创建时显示货币名称
- **WHEN** player is creating a shop quickly
- **THEN** system SHALL prompt for currency selection by display name
- **AND** player SHALL select from a list of readable currency names

### Requirement: 快速创建后的完整显示
The system SHALL display complete shop information on the sign immediately after quick creation.

#### Scenario: 快速创建后告示牌有内容
- **WHEN** player completes quick shop creation
- **THEN** the sign SHALL show: [Shop Type] [Price] [Currency Name] [Item Name]
- **AND** all information SHALL be correctly formatted and visible

## MODIFIED Requirements

### Requirement: ShopModule.onBlockBreak
Modified to include comprehensive shop detection and cleanup logic that handles all edge cases including:
- Shops with floating displays
- Shops with pending transactions
- Shops being modified vs being deleted

### Requirement: ChatInputHandler.handleItemReplacement
Modified to trigger sign text update after successful item replacement and ensure old shop data cleanup.

### Requirement: StatsCommand.findOriginalEntityType
Modified to support bidirectional translation lookup (CN→EN and EN→CN) based on client locale.

## REMOVED Requirements
None - This is purely additive fixes and improvements.
