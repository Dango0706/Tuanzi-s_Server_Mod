package me.tuanzi.economy.api;

import me.tuanzi.economy.account.AccountManager;
import me.tuanzi.economy.currency.WalletType;
import me.tuanzi.economy.currency.WalletTypeRegistry;
import me.tuanzi.economy.storage.EconomyData;
import me.tuanzi.economy.storage.EconomyStateSaver;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;

import java.util.Collection;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.locks.ReentrantLock;

public class EconomyAPIImpl implements EconomyAPI {
    private static EconomyAPIImpl instance;
    private final MinecraftServer server;
    private final EconomyStateSaver stateSaver;
    private final ReentrantLock lock = new ReentrantLock();

    private EconomyAPIImpl(MinecraftServer server) {
        this.server = server;
        this.stateSaver = EconomyStateSaver.getServerState(server);
    }

    public static synchronized EconomyAPIImpl getInstance(MinecraftServer server) {
        if (instance == null) {
            instance = new EconomyAPIImpl(server);
        }
        return instance;
    }

    public static synchronized void resetInstance() {
        instance = null;
    }

    @Override
    public double getBalance(UUID player, String walletTypeId) {
        AccountManager accountManager = AccountManager.getInstance(server);
        return accountManager.getBalance(player, walletTypeId);
    }

    @Override
    public void deposit(UUID player, String walletTypeId, double amount) {
        AccountManager accountManager = AccountManager.getInstance(server);
        accountManager.deposit(player, walletTypeId, amount);
    }

    @Override
    public void withdraw(UUID player, String walletTypeId, double amount) {
        AccountManager accountManager = AccountManager.getInstance(server);
        accountManager.withdraw(player, walletTypeId, amount);
    }

    @Override
    public boolean hasEnough(UUID player, String walletTypeId, double amount) {
        AccountManager accountManager = AccountManager.getInstance(server);
        return accountManager.hasEnough(player, walletTypeId, amount);
    }

    @Override
    public void setBalance(UUID player, String walletTypeId, double amount) {
        AccountManager accountManager = AccountManager.getInstance(server);
        accountManager.setBalance(player, walletTypeId, amount);
    }

    @Override
    public void registerWalletType(String id, Component displayName) {
        lock.lock();
        try {
            WalletType walletType = new WalletType(id, displayName);
            WalletTypeRegistry.register(walletType);
            EconomyData data = stateSaver.getData();
            data.registerWalletType(walletType);
            stateSaver.setDirty();
        } finally {
            lock.unlock();
        }
    }

    @Override
    public void unregisterWalletType(String id) {
        lock.lock();
        try {
            WalletTypeRegistry.unregister(id);
            EconomyData data = stateSaver.getData();
            data.unregisterWalletType(id);
            stateSaver.setDirty();
        } finally {
            lock.unlock();
        }
    }

    @Override
    public Optional<WalletType> getWalletType(String id) {
        return WalletTypeRegistry.get(id);
    }

    @Override
    public Collection<WalletType> getAllWalletTypes() {
        return WalletTypeRegistry.getAll();
    }

    @Override
    public void transfer(UUID from, UUID to, String walletTypeId, double amount) {
        AccountManager accountManager = AccountManager.getInstance(server);
        accountManager.transfer(from, to, walletTypeId, amount);
    }
}
