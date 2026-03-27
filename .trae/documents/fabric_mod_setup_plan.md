# Minecraft Fabric 模组开发环境搭建计划

## 目标版本

* **Minecraft**: 26.1（2026年3月24日发布）

* **Fabric Loader**: 0.18.4

* **Fabric API**: 0.144.3+26.1

* **Java**: 25以上

## 实施步骤

### 1. 创建 Gradle 构建配置文件

#### 1.1 创建 `gradle.properties`

配置项目基本属性：

* Minecraft版本: 26.1

* Yarn映射版本

* Fabric Loader版本: 0.18.4

* Fabric API版本: 0.144.3+26.1

* Java版本: 25以上

#### 1.2 创建 `settings.gradle`

配置Gradle设置和插件仓库

#### 1.3 创建 `build.gradle`

配置：

* Fabric Loom插件

* Java工具链

* 依赖项（Minecraft、Fabric Loader、Fabric API）

* Maven发布配置

### 2. 创建 Gradle Wrapper

#### 2.1 创建 `gradle/wrapper/gradle-wrapper.properties`

配置Gradle Wrapper版本（推荐8.10+）

### 3. 创建模组元数据文件

#### 3.1 创建 `src/main/resources/fabric.mod.json`

包含：

* 模组ID

* 版本号

* 模组名称

* 描述

* 依赖声明（Fabric Loader、Fabric API、Minecraft版本）

* 入口点配置

### 4. 创建基础Java代码结构

#### 4.1 创建主类 `src/main/java/com/example/mod/ExampleMod.java`

包含：

* ModInitializer接口实现

* onInitialize()方法

### 5. 创建资源文件

#### 5.1 创建 `src/main/resources/assets/modid/icon.png`

模组图标（可选，后续添加）

### 6. 创建 `.gitignore`

忽略Gradle构建产物和IDE配置文件

### 7. 创建 `gradlew` 和 `gradlew.bat` 脚本

通过运行 `gradle wrapper` 命令生成

## 文件结构预览

```
tuanzis_server_mod/
├── gradle/
│   └── wrapper/
│       └── gradle-wrapper.properties
├── src/
│   └── main/
│       ├── java/
│       │   └── com/
│       │       └── example/
│       │           └── mod/
│       │               └── ExampleMod.java
│       └── resources/
│           ├── fabric.mod.json
│           └── assets/
│               └── modid/
│                   └── icon.png
├── build.gradle
├── settings.gradle
├── gradle.properties
├── gradlew
├── gradlew.bat
└── .gitignore
```

## 执行顺序

1. 创建 `gradle.properties`
2. 创建 `settings.gradle`
3. 创建 `build.gradle`
4. 创建 `gradle/wrapper/gradle-wrapper.properties`
5. 创建 `src/main/resources/fabric.mod.json`
6. 创建 `src/main/java/com/example/mod/ExampleMod.java`
7. 创建 `.gitignore`
8. 运行 `gradle wrapper` 生成Gradle Wrapper脚本
9. 运行 `./gradlew build` 验证环境配置

