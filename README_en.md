# Tuanzi's Server Mod

A comprehensive server-side enhancement mod developed for Minecraft 26.1 (1.22) Fabric, integrating advanced authentication, multi-currency economy, smart shops, title system, CDK gift codes, and an all-dimensional player statistics system. **No client-side mod required.**

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

### 4. Title System
*   **Multi-location Display**: Titles are synchronized in the chat prefix, Tab player list, and above the player's head.
*   **Color Optimization**: Supports `&` color codes with special rendering optimization to prevent color bleeding to the username.
*   **Activation Methods**: Supports direct issuance by administrators or activation via special "Title Name Tags" by players.
*   **Offline Compatibility**: Supports issuing titles to offline players, who will automatically receive a notification upon logging in.

### 5. CDK System (Gift Codes)
*   **Backend Command Execution**: Commands are executed by the console upon redemption, allowing for complex rewards without elevating player permissions.
*   **Multi-dimensional Restrictions**: Supports global one-time or player one-time redemption, with precise expiration time down to the second.
*   **Batch Generation & Export**: Supports batch generation of random codes based on templates and exporting to CSV, JSON, or TXT formats.
*   **Detailed Auditing**: Independently records all redemption history, including timestamps, command snapshots, and player info.

### 6. Total Statistics System
*   **All-round Metrics**: Records 25+ metrics including distance traveled, kills, mining, online time, title ownership, CDK redemptions, etc.
*   **Real-time Leaderboards**: Supports automatic sidebar scoreboard rotation and dynamic floating text displays.
*   **Data Export**: Supports one-click exporting and resetting of statistical data.

### 7. Backup & Rollback System
*   **Automated Backups**: Periodically packages all core data (Economy, Shop, Stats, CDK, Titles, Whitelist) into a ZIP file.
*   **Secure Recovery**: One-click rollback to a specific timestamp, followed by an automatic safe shutdown to ensure data consistency.
*   **Manual Control**: Admins can trigger backups manually and adjust the auto-backup interval via commands.

## Command Index

### Common Player Commands
| Command | Description | Note |
| :--- | :--- | :--- |
| `/login <password>` | Account login | Auto-login for Premium |
| `/register <pass> <confirm>` | Account registration | Required only once |
| `/pay <player> <walletId> <qty>` | Transfer currency | Tab-completion supported |
| `/balance [walletId]` | Check balances | Shows all wallets by default |
| `/stats [player] [type]` | View player stats | Supports 25+ metrics |
| `/titles <list/set/clear>` | Title management | View, wear, or remove titles (with expiry display) |
| `/cdk <code>` | Redeem gift code | Executes reward commands on success |
| `/shop help` | Shop functionality guide | Includes Buy/Sell instructions |

### Admin Commands (`Level 4`)
| Command | Description | Note |
| :--- | :--- | :--- |
| `/auth whitelist <add/remove/list>` | Whitelist management | Supports UUID generation |
| `/econ-admin balance <set/add/rem>` | Manage player balances | Multi-currency support |
| `/eco backup` | Perform full backup | Stored in `./mod_backups/` |
| `/eco restore <timestamp>` | Restore from backup | Automatically stops server |
| `/eco interval <hours>` | Set backup interval | Default is 12 hours |
| `/titleadmin <create/delete/give/getitem>` | Title repository | Now supports `[days]` time parameter |
| `/titleadmin setexpiry <id> <date>` | Global title expiry | Set end date for a title for all players |
| `/titleadmin modifyplayer <targets> <id> <set/add/rem> <days>` | Player title duration | Set/extend/shorten holding time |
| `/cdkadmin <create/batch/list/export>` | CDK management | Batch generation and history export |
| `/shopadmin info` | Shop debug data | Shows S, K, Decay rate, etc. |
| `/shopadmin setupDynamic` | Guided dynamic pricing setup | 3-step core variable setup |
| `/scoreboard <start/stop/interval>` | Scoreboard rotation control | Adjust rotation speed |
| `/floatingtext <create/delete/set>` | Floating text leaderboards | Color and position adjustment |

## Technical Specifications
*   **Platform**: Minecraft 26.1 (1.22) Fabric
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
