# Tuanzi's Server Mod

## Project Overview

Tuanzi's Server Mod (`tuanzis_server_mod`) is a comprehensive server-side modification for Minecraft built on the Fabric modding toolchain. It is designed to provide essential server administration features, player management, and a robust economy system without requiring client-side mods.

### Key Technologies
* **Platform:** Minecraft 26.1
* **Mod Loader:** Fabric Loader (0.18.4)
* **API:** Fabric API (0.144.3+26.1)
* **Language:** Java 25+
* **Build System:** Gradle (with Fabric Loom)

## Architecture & Modules

The codebase is highly modular, organized by feature under the `me.tuanzi` package:

* **`auth`**: Handles player authentication, login/register systems, offline UUID generation, and server whitelisting. It manages sessions and login timeouts.
* **`economy`**: A complete economy API managing player balances, transactions, and multiple wallet types.
* **`shop`**: Implements a physical shop system (e.g., chest shops, sign interactions) with dynamic pricing capabilities.
* **`statistics`**: Tracks extensive player and server statistics (e.g., block breaks, kills, playtime), and manages floating text displays and scoreboards for leaderboards.
* **`mixin`**: Contains Fabric Mixins used to hook into and modify core Minecraft server logic.

## Building and Running

The project uses the Gradle wrapper (`gradlew`). Standard Fabric Loom tasks apply:

* **Build the mod (.jar):**
  ```bash
  ./gradlew build
  ```
  The compiled artifact will be placed in `build/libs/`.

* **Run a local development server:**
  ```bash
  ./gradlew runServer
  ```

## Development Conventions

* **Java Version:** The project strictly targets Java 25. 环境变量的Java为21，请手动选择25版本的Java（非 Path 环境变量版本，需手动配置，路径：`C:/Program Files/Zulu/zulu-25`）。
* **Language Requirement:** Always communicate and output responses in Chinese (中文).
* **Source Code Reference:** 请参考 Minecraft 反编译后的本地源码来进行开发（路径详见根目录源码 Jar 包）。
* **Cache Directory:** 所有查看源码或开发过程中产生的临时缓存文件必须放置在 `cache/` 文件夹下。该文件夹已被列入 Git 忽略名单。
* **Git Exclusions:** 以下内容已被移除 Git 追踪且永远不应提交：
    - 运行缓存与 IDE 配置文件 (`.gradle-user-home`, `.vs`, `.idea`, `.trae`)
    - 文档与临时源码 (`docs`, `net`, `temp_sources*`)
    - Gradle 安装包与二进制文件 (`gradle-9.4.1`, `gradle/`)
    - NBT/日志文件 (`*.log`)
    - 核心指令上下文 (`GEMINI.md`)
