package me.tuanzi.economy.account;

import me.tuanzi.economy.events.TransactionCallback;
import me.tuanzi.economy.events.TransactionRecord;
import me.tuanzi.economy.events.TransactionType;
import me.tuanzi.economy.exception.InsufficientBalanceException;
import me.tuanzi.economy.exception.WalletTypeNotFoundException;
import me.tuanzi.economy.storage.EconomyData;
import me.tuanzi.economy.storage.EconomyStateSaver;
import net.minecraft.server.MinecraftServer;

import java.util.Collection;
import java.util.UUID;
import java.util.concurrent.locks.ReentrantLock;

public class AccountManager {
    private static AccountManager instance;
    private final MinecraftServer server;
    private final EconomyStateSaver stateSaver;
    private final ReentrantLock lock = new ReentrantLock();

    private AccountManager(MinecraftServer server) {
        this.server = server;
        this.stateSaver = EconomyStateSaver.getServerState(server);
    }

    public static synchronized AccountManager getInstance(MinecraftServer server) {
        if (instance == null) {
            instance = new AccountManager(server);
        }
        return instance;
    }

    public static synchronized void resetInstance() {
        instance = null;
    }

    public PlayerAccount getOrCreateAccount(UUID playerId) {
        lock.lock();
        try {
            EconomyData data = stateSaver.getData();
            return data.getOrCreatePlayerAccount(playerId);
        } finally {
            lock.unlock();
        }
    }

    public PlayerAccount getAccount(UUID playerId) {
        lock.lock();
        try {
            EconomyData data = stateSaver.getData();
            return data.getPlayerAccount(playerId);
        } finally {
            lock.unlock();
        }
    }

    public boolean hasAccount(UUID playerId) {
        lock.lock();
        try {
            EconomyData data = stateSaver.getData();
            return data.getPlayerAccount(playerId) != null;
        } finally {
            lock.unlock();
        }
    }

    public void removeAccount(UUID playerId) {
        lock.lock();
        try {
            EconomyData data = stateSaver.getData();
            data.removePlayerAccount(playerId);
            stateSaver.setDirty();
        } finally {
            lock.unlock();
        }
    }

    public Collection<PlayerAccount> getAllAccounts() {
        lock.lock();
        try {
            EconomyData data = stateSaver.getData();
            return data.getAllPlayerAccounts();
        } finally {
            lock.unlock();
        }
    }

    public double getBalance(UUID playerId, String walletTypeId) {
        lock.lock();
        try {
            EconomyData data = stateSaver.getData();
            if (!data.hasWalletType(walletTypeId)) {
                throw new WalletTypeNotFoundException(walletTypeId);
            }
            PlayerAccount account = data.getPlayerAccount(playerId);
            if (account == null) {
                return 0.0;
            }
            return account.getBalance(walletTypeId);
        } finally {
            lock.unlock();
        }
    }

    public void deposit(UUID playerId, String walletTypeId, double amount) {
        lock.lock();
        try {
            EconomyData data = stateSaver.getData();
            if (!data.hasWalletType(walletTypeId)) {
                throw new WalletTypeNotFoundException(walletTypeId);
            }
            PlayerAccount account = data.getOrCreatePlayerAccount(playerId);
            double balanceBefore = account.getBalance(walletTypeId);
            account.deposit(walletTypeId, amount);
            double balanceAfter = account.getBalance(walletTypeId);
            stateSaver.setDirty();
            TransactionRecord record = new TransactionRecord(
                    playerId,
                    walletTypeId,
                    amount,
                    balanceBefore,
                    balanceAfter,
                    new TransactionType.Deposit(),
                    System.currentTimeMillis()
            );
            TransactionCallback.EVENT.invoker().onTransaction(record);
        } finally {
            lock.unlock();
        }
    }

    public void withdraw(UUID playerId, String walletTypeId, double amount) {
        lock.lock();
        try {
            EconomyData data = stateSaver.getData();
            if (!data.hasWalletType(walletTypeId)) {
                throw new WalletTypeNotFoundException(walletTypeId);
            }
            PlayerAccount account = data.getOrCreatePlayerAccount(playerId);
            double balanceBefore = account.getBalance(walletTypeId);
            if (balanceBefore < amount) {
                throw new InsufficientBalanceException(walletTypeId, balanceBefore, amount);
            }
            account.withdraw(walletTypeId, amount);
            double balanceAfter = account.getBalance(walletTypeId);
            stateSaver.setDirty();
            TransactionRecord record = new TransactionRecord(
                    playerId,
                    walletTypeId,
                    amount,
                    balanceBefore,
                    balanceAfter,
                    new TransactionType.Withdraw(),
                    System.currentTimeMillis()
            );
            TransactionCallback.EVENT.invoker().onTransaction(record);
        } finally {
            lock.unlock();
        }
    }

    public boolean hasEnough(UUID playerId, String walletTypeId, double amount) {
        lock.lock();
        try {
            EconomyData data = stateSaver.getData();
            if (!data.hasWalletType(walletTypeId)) {
                throw new WalletTypeNotFoundException(walletTypeId);
            }
            PlayerAccount account = data.getPlayerAccount(playerId);
            if (account == null) {
                return false;
            }
            return account.hasEnough(walletTypeId, amount);
        } finally {
            lock.unlock();
        }
    }

    public void setBalance(UUID playerId, String walletTypeId, double amount) {
        lock.lock();
        try {
            EconomyData data = stateSaver.getData();
            if (!data.hasWalletType(walletTypeId)) {
                throw new WalletTypeNotFoundException(walletTypeId);
            }
            PlayerAccount account = data.getOrCreatePlayerAccount(playerId);
            double balanceBefore = account.getBalance(walletTypeId);
            account.setBalance(walletTypeId, amount);
            double balanceAfter = account.getBalance(walletTypeId);
            stateSaver.setDirty();
            TransactionRecord record = new TransactionRecord(
                    playerId,
                    walletTypeId,
                    amount - balanceBefore,
                    balanceBefore,
                    balanceAfter,
                    new TransactionType.Deposit(),
                    System.currentTimeMillis()
            );
            TransactionCallback.EVENT.invoker().onTransaction(record);
        } finally {
            lock.unlock();
        }
    }

    public void transfer(UUID fromPlayerId, UUID toPlayerId, String walletTypeId, double amount) {
        lock.lock();
        try {
            EconomyData data = stateSaver.getData();
            if (!data.hasWalletType(walletTypeId)) {
                throw new WalletTypeNotFoundException(walletTypeId);
            }
            PlayerAccount fromAccount = data.getPlayerAccount(fromPlayerId);
            if (fromAccount == null) {
                throw new InsufficientBalanceException(walletTypeId, 0.0, amount);
            }
            double fromBalanceBefore = fromAccount.getBalance(walletTypeId);
            if (fromBalanceBefore < amount) {
                throw new InsufficientBalanceException(walletTypeId, fromBalanceBefore, amount);
            }
            PlayerAccount toAccount = data.getOrCreatePlayerAccount(toPlayerId);
            double toBalanceBefore = toAccount.getBalance(walletTypeId);
            fromAccount.withdraw(walletTypeId, amount);
            toAccount.deposit(walletTypeId, amount);
            double fromBalanceAfter = fromAccount.getBalance(walletTypeId);
            double toBalanceAfter = toAccount.getBalance(walletTypeId);
            stateSaver.setDirty();
            TransactionType.Transfer transferType = new TransactionType.Transfer(fromPlayerId, toPlayerId);
            TransactionRecord fromRecord = new TransactionRecord(
                    fromPlayerId,
                    walletTypeId,
                    -amount,
                    fromBalanceBefore,
                    fromBalanceAfter,
                    transferType,
                    System.currentTimeMillis()
            );
            TransactionCallback.EVENT.invoker().onTransaction(fromRecord);
            TransactionRecord toRecord = new TransactionRecord(
                    toPlayerId,
                    walletTypeId,
                    amount,
                    toBalanceBefore,
                    toBalanceAfter,
                    transferType,
                    System.currentTimeMillis()
            );
            TransactionCallback.EVENT.invoker().onTransaction(toRecord);
        } finally {
            lock.unlock();
        }
    }
}
