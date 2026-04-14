# Tuanzi's Server Mod

A comprehensive server-side enhancement mod developed for Fabric 1.21.1, integrating advanced authentication, multi-currency economy, smart shops, and an all-dimensional player statistics system. **No client-side mod required.**

## Core Features

### 1. Smart Shop System
*   **Guided Creation**: Right-click an empty sign to start an interactive guide with chat button selection and numerical input.
*   **Typed Dynamic Pricing**:
    *   **BUY Mode (Purchasing)**: $P = x + (y - x) \cdot \frac{K}{K + S}$ (Price converges to floor $x$ as stock increases)
    *   **SELL Mode (Selling)**: $P = x + (y - x) \cdot \frac{S}{K + S}$ (Price rises as sales volume increases)
*   **Visual Display**: `ItemEntity`-based floating item displays with anti-pickup, physical stillness, and non-persistence features (auto-restored on reboot).
*   **Multi-Dimension Support**: Create and maintain shops in the Overworld, Nether, and End automatically.

### 2. Advanced Economy
*   **Multi-Wallet Architecture**: Supports custom currencies (e.g., Gold, Credits).
*   **Dynamic Translation**: Currency names and transaction feedback fully adapt to the player's client language.
*   **Offline Transactions**: Supports transfers to registered offline players.

### 3. Mixed Authentication
*   **Mixed Verification**: Automatically identifies Mojang premium accounts for password-less login; offline players must register.
*   **Whitelist Management**: Enhanced whitelist system integrated with UUID mapping.
*   **Security Protection**: Restricts all physical and interactive operations before login to prevent information leaks or unauthorized damage.

### 4. Total Statistics System
*   **Cross-Dimension Tracking**: Precisely records 20+ metrics including distance traveled, kills, mining, online time, etc.
*   **Real-time Leaderboards**: Supports automatic sidebar scoreboard rotation and dynamic floating text displays.
*   **Extended Survival Data**: Includes fun data like longest session, total jumps, and farthest death drop distance.

## Command Index

### Common Player Commands
| Command | Description | Note |
| :--- | :--- | :--- |
| `/login <password>` | Account login | Auto-login for Premium |
| `/register <pass> <confirm>` | Account registration | Required only once |
| `/pay <player> <walletId> <qty>` | Transfer currency | Tab-completion supported |
| `/balance [walletId]` | Check balances | Shows all wallets by default |
| `/stats [player] [type]` | View player stats | Supports 20+ metrics |
| `/shop help` | Shop functionality guide | Includes Buy/Sell instructions |

### Admin Commands (`Level 4`)
| Command | Description | Note |
| :--- | :--- | :--- |
| `/auth whitelist <add/remove/list>` | Whitelist management | Supports UUID generation |
| `/econ-admin balance <set/add/rem>` | Manage player balances | Multi-currency support |
| `/shopadmin info` | Shop debug data | Shows S, K, Decay rate, etc. |
| `/shopadmin setupDynamic` | Guided dynamic pricing setup | 3-step core variable setup |
| `/scoreboard <start/stop/interval>` | Scoreboard rotation control | Adjust rotation speed |
| `/floatingtext <create/delete/set>` | Floating text leaderboards | Color and position adjustment |

## Technical Specifications
*   **Platform**: Minecraft 1.21.1 (Fabric)
*   **Java Version**: Java 25 (Zulu-25 recommended)
*   **Performance Optimization**: 
    *   Shop displays maintained using a 5-tick throttle.
    *   Statistics data saved using an asynchronous mechanism.
    *   Display entities use a non-persistent scheme, zero save bloat.

## Internationalization (i18n)
This mod fully supports English and Chinese. The system automatically switches based on the player's client language:
*   **Chinese**: `zh_cn` (Includes automatic normalization for `zh_tw`, `zh_hk`)
*   **English**: `en_us` (Default fallback)

---
*Created by Tuanzi - Built for modern Minecraft server communities.*
