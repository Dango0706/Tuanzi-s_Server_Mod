package me.tuanzi.shop.logging;

import me.tuanzi.shop.shop.ShopInstance;
import net.minecraft.core.BlockPos;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

public class TransactionLogger {
    private static final String LOG_FILE_NAME = "shop_transactions.log";
    private static final DateTimeFormatter TIMESTAMP_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private final Path logPath;
    private final boolean enabled;

    public TransactionLogger(Path worldPath, boolean enabled) {
        this.logPath = worldPath.resolve(LOG_FILE_NAME);
        this.enabled = enabled;

        if (enabled) {
            try {
                if (!Files.exists(logPath)) {
                    Files.createFile(logPath);
                }
            } catch (IOException e) {
                throw new RuntimeException("Failed to create transaction log file", e);
            }
        }
    }

    public void logTransaction(UUID playerId, String playerName, ShopInstance shop,
                               String transactionType, String itemName, int quantity,
                               double amount, String currencyDisplayName) {
        if (!enabled) {
            return;
        }

        String timestamp = LocalDateTime.now().format(TIMESTAMP_FORMATTER);
        String shopId = shop.getShopId().toString();
        String sellerName = getSellerName(shop);
        String chestId = shop.getShopPos().toString();
        BlockPos pos = shop.getShopPos();

        String logLine = String.format(
                "[%s] [%s] 玩家:%s | 类型:%s | 物品:%s | 数量:%d | 金额:%.2f %s | 卖方:%s | 交易箱id:%s,位置:x:%d,y:%d,z:%d%n",
                timestamp, shopId, playerName, transactionType, itemName, quantity,
                amount, currencyDisplayName, sellerName, chestId,
                pos.getX(), pos.getY(), pos.getZ()
        );

        try {
            Files.writeString(logPath, logLine, StandardOpenOption.APPEND);
        } catch (IOException e) {
            System.err.println("Failed to write transaction log: " + e.getMessage());
        }
    }

    private String getSellerName(ShopInstance shop) {
        if (shop.isAdminShop()) {
            return "Admin Shop";
        }
        return shop.getOwnerId().toString();
    }

    public void logBuy(UUID playerId, String playerName, ShopInstance shop,
                       String itemName, int quantity, double amount, String currencyDisplayName) {
        logTransaction(playerId, playerName, shop, "买", itemName, quantity, amount, currencyDisplayName);
    }

    public void logSell(UUID playerId, String playerName, ShopInstance shop,
                        String itemName, int quantity, double amount, String currencyDisplayName) {
        logTransaction(playerId, playerName, shop, "卖", itemName, quantity, amount, currencyDisplayName);
    }

    public boolean isEnabled() {
        return enabled;
    }

    public Path getLogPath() {
        return logPath;
    }
}
