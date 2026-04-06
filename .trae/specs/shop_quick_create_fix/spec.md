# 商店快速创建与显示修复 Spec

## Why

快速创建商店功能无法触发，且创建成功后没有悬浮物品显示。

## What Changes

* 修复快速创建商店的容器检测逻辑

* 商店创建成功后调用显示管理器创建悬浮物品

## Impact

* Affected code: `ShopModule.java`, `ChatInputHandler.java`

## ADDED Requirements

### Requirement: 快速创建商店触发检测

系统应当正确检测告示牌下方的容器方块。

#### Scenario: 玩家手持物品点击箱子上的空告示牌

* **WHEN** 玩家手持物品右键点击放置在箱子上的空告示牌

* **THEN** 系统应触发商店创建流程并显示提示信息

### Requirement: 商店创建后显示悬浮物品

系统应当在商店创建成功后立即创建悬浮物品显示。

#### Scenario: 商店创建成功

* **WHEN** 商店创建流程完成

* **THEN** 系统应在商店位置上方显示悬浮物品

