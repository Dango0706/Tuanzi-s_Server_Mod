# 翻译系统修复 Spec

## Why
四个模块（Auth、Economy、Statistics、FloatingText）的翻译系统存在问题，导致翻译键无法正确显示，玩家看到的都是原始的翻译键（如 `auth.restriction.auto_login`）而不是翻译后的文本。

## What Changes
- 统一所有模块的翻译加载路径
- 确保所有翻译系统正确初始化
- 添加缺失的翻译键
- 添加调试日志以验证翻译加载成功

## Impact
- Affected specs: Auth、Economy、Statistics 模块
- Affected code: 
  - `me.tuanzi.economy.utils.ServerTranslationHelper`
  - `me.tuanzi.statistics.util.StatsTranslationHelper`
  - `me.tuanzi.auth.utils.AuthTranslationHelper`

## ADDED Requirements

### Requirement: 统一翻译加载机制
系统 SHALL 使用统一的翻译加载机制。

#### Scenario: 翻译文件正确加载
- **WHEN** 模块初始化时
- **THEN** 翻译文件应从正确的路径加载
- **AND** 加载成功后应输出日志确认

### Requirement: 翻译键查找
系统 SHALL 在翻译键不存在时提供有意义的反馈。

#### Scenario: 翻译键不存在
- **WHEN** 请求的翻译键不存在
- **THEN** 返回翻译键本身（保持现有行为）
- **AND** 输出警告日志

## MODIFIED Requirements
无

## REMOVED Requirements
无

## 技术分析

### 问题根因
1. 翻译文件路径正确 (`/assets/tuanzis-server-mod/lang/zh_cn.json`)
2. 但可能存在类加载器问题 - 翻译文件在 JAR 中时，`getResourceAsStream` 可能无法正确加载
3. 需要添加调试日志确认翻译是否成功加载

### 解决方案
1. 添加翻译加载计数日志
2. 确保 `initialized` 标志正确设置
3. 验证翻译加载后的 Map 大小
