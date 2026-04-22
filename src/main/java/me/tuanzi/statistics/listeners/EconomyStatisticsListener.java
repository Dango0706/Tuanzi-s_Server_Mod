package me.tuanzi.statistics.listeners;

import me.tuanzi.economy.events.TransactionCallback;
import me.tuanzi.economy.events.TransactionType;
import me.tuanzi.shop.events.ShopTransactionCallback;
import me.tuanzi.statistics.data.PlayerStatistics;
import me.tuanzi.statistics.data.StatisticsDataManager;

public class EconomyStatisticsListener {
    private final StatisticsDataManager dataManager;
    private net.minecraft.server.MinecraftServer server;

    public EconomyStatisticsListener(StatisticsDataManager dataManager) {
        this.dataManager = dataManager;
    }

    public void register(net.minecraft.server.MinecraftServer server) {
        this.server = server;
        // 监听商店交易
        ShopTransactionCallback.EVENT.register((player, shop, item, quantity, totalPrice, isBuy) -> {
            PlayerStatistics stats = dataManager.getPlayerStatistics(player.getName().getString());
            String itemType = item.getItem().toString();
            if (isBuy) {
                stats.addItemsBought(itemType, quantity, shop.getWalletTypeId(), totalPrice);
            } else {
                stats.addItemsSold(itemType, quantity, shop.getWalletTypeId(), totalPrice);
            }
        });

        // 监听经济转账
        TransactionCallback.EVENT.register((record) -> {
            if (record.type() instanceof TransactionType.Transfer transfer) {
                // 获取当前这条交易记录所属的玩家名
                String playerName = getPlayerName(record.playerId());
                if (playerName == null) return;

                double absAmount = Math.abs(record.amount());

                // 判断当前玩家是发送者还是接收者，分别记录
                if (record.playerId().equals(transfer.fromPlayer())) {
                    dataManager.getPlayerStatistics(playerName).addMoneySent(record.walletTypeId(), absAmount);
                } else if (record.playerId().equals(transfer.toPlayer())) {
                    dataManager.getPlayerStatistics(playerName).addMoneyReceived(record.walletTypeId(), absAmount);
                }
            }
        });
    }

    private String getPlayerName(java.util.UUID uuid) {
        // 1. 尝试从在线玩家获取
        net.minecraft.server.level.ServerPlayer player = server.getPlayerList().getPlayer(uuid);
        if (player != null) return player.getName().getString();

        // 2. 尝试从白名单获取
        me.tuanzi.auth.whitelist.WhitelistManager whitelistManager = me.tuanzi.auth.AuthModule.getInstance().getWhitelistManager();
        if (whitelistManager != null) {
            String name = whitelistManager.getPlayerName(uuid);
            if (name != null) return name;
        }

        return null;
    }
}
