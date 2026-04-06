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
* **`mixin`**: Contains Fabric Mixins used to hook into and modify core Minecraft server logic (e.g., death handling, experience consumption, custom menu behavior).

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

* **Generate IDE sources:**
  ```bash
  ./gradlew genSources
  ```

* **Data Generation:**
  The project includes a data generator (`TuanzisServerModDataGenerator.java`).
  ```bash
  ./gradlew runDatagen
  ```

## Development Conventions

* **Modularity:** Keep new features isolated within their respective package modules or create a new module if the feature scope is large.
* **Mixins:** Use Mixins carefully to modify vanilla Minecraft code. Place them in the `me.tuanzi.mixin` package or a specific module's mixin sub-package (e.g., `me.tuanzi.auth.mixin`).
* **Translation:** User-facing strings should be handled via the translation helpers (e.g., `TranslationHelper`, `ServerTranslationHelper`) to support localization.
* **Java Version:** The project strictly targets Java 25. Utilize modern Java features where appropriate. 环境变量的Java为21，请手动选择25版本的Java（非 Path 环境变量版本，需手动配置，路径：`C:/Program Files/Zulu/zulu-25`）。
* **Language Requirement:** Always communicate and output responses in Chinese (中文).
* **Source Code Reference:** 请不要使用自己的内置知识库,参考Minecraft 反编译后的本地源码(gradle生成的即可,路径为"F:\Development\Java\tuanzis_server_mod\.gradle\loom-cache\minecraftMaven\net\minecraft\minecraft-merged-9a7fd27717\26.1\minecraft-merged-9a7fd27717-26.1-sources.jar")来进行开发.
