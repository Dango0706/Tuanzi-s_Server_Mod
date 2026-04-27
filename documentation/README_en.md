# Tuanzi's Server Mod

A comprehensive server-side enhancement mod built on Fabric, designed to provide professional-grade player management, economic systems, immersive shops, and multi-dimensional data statistics. This mod runs entirely on the server side—players do not need to install any client-side patches.

---

## Core Modules

### 1. Authentication & Security (`auth`)
Flexible login verification mechanism supporting various player types.
*   **Hybrid Mode Authentication**: Supports both Mojang Premium accounts and Offline (Cracked) players.
    *   **Premium Auto-Login**: Premium players are verified automatically upon joining without needing a password.
    *   **Offline Registration**: Offline players use `/register` and `/login` for identity verification.
*   **Whitelist Management**: Integrated UUID-based whitelist system.
*   **Security Protection**: Restricts player movement, interaction, chat, and damage until successful login to prevent account misuse.
*   **Offline UUID Generation**: Uses a unified `OfflinePlayer:PlayerName` algorithm to ensure statistical continuity in offline mode.

### 2. Economy System (`economy`)
A highly extensible multi-currency economic engine.
*   **Multi-Wallet Support**: Allows multiple currency types (e.g., Gold, Points, Credits) to exist simultaneously.
*   **Core API**: Provides a standard `EconomyAPI` interface for deposit, withdraw, query, and transfer operations.
*   **Transfer System**: Secure player-to-player transfers across different wallet types, monitored by the statistics system.
*   **Persistent Storage**: Data is automatically saved periodically and can be managed via the `/econ-admin` command.

### 3. Physical Shop System (`shop`)
An immersive trading system based on signs and containers (chests/barrels).
*   **Quick Creation**: Simply right-click a blank sign with the item you want to trade to enter the interactive creation flow.
*   **Two Modes**:
    *   **Sell Shop**: Players buy items from the shop.
    *   **Buy Shop**: Players sell items to the shop.
*   **Infinite Shops**: Admins can set infinite stock shops; the headers ([Buy]/[Sell]) for these shops are displayed in **Bright Red**.
*   **Dynamic Pricing**:
    *   Exponential price curves based on supply and demand.
    *   **Auto-Decay**: System pressure (S) decays smoothly every minute (1200 ticks), ensuring prices return to base values over time.
*   **Bulk Trading**: Supports Shift-Clicking to trade a full stack (64 items) instantly.

### 4. Multi-Dimensional Statistics (`statistics`)
Deeply tracks every player action within the server.
*   **Basic Stats**: Playtime, distance traveled, blocks placed/broken, kills, deaths, damage, durability usage, etc.
*   **Advanced Tracking**: Fishing details, crafting stats, enchantment records, sneak time, longest session, and even the farthest death distance from the drop point.
*   **Economy Statistics (Deeply Optimized)**:
    *   Records total items bought/sold for every specific item type.
    *   Records **detailed currency breakdowns** (amount spent/earned per currency) for each item type.
    *   **Offline Support**: Transfers and economic changes are accurately recorded in the target player's statistics file even if they are offline.
*   **Data Visualization**:
    *   **Sidebar Scoreboard**: Supports rotating display of multiple stat types.
    *   **Floating Text**: Create dynamically updated leaderboards in the world.

---

## Command Overview

### Player Commands
| Command | Description |
| :--- | :--- |
| `/login <password>` | Login for offline players |
| `/register <pw> <confirm>` | Register for offline players |
| `/pay <player> <wallet> <qty>` | Transfer currency to another player |
| `/balance` | Check your current balances |
| `/stats` | View your personal statistics briefing |
| `/stats economy` | View detailed personal economy stats (including item breakdown) |
| `/shop info` | View detailed data of the shop you are interacting with |

### Admin Commands (Permission Level 2)
| Command | Description |
| :--- | :--- |
| `/auth whitelist <add/remove>` | Manage server whitelist |
| `/econ-admin` | Manage player balances and currency types |
| `/shopadmin` | Manage shop attributes (infinite mode, dynamic pricing parameters, etc.) |
| `/stats <PlayerName>` | View the statistics briefing of a specific player |
| `/stats <PlayerName> <Sub>` | View specific category stats for a player (e.g., blocks, economy) |
| `/statsboard` | Configure sidebar scoreboard rotation and display types |
| `/floatingtext` | Create and manage holographic leaderboards in the world |

---

## Technical Specifications

*   **Platform**: Minecraft 1.21.1 / Fabric Loader
*   **Language**: Java 25
*   **Build Tool**: Gradle 9.4.1 (Fabric Loom)
*   **Requirements**:
    *   Compilation and execution must specify the Java 25 path (Recommended: Zulu JDK 25).
    *   Recommended Memory: At least 2GB (depending on player count).

---
*Document Version: 2026.04.22*
