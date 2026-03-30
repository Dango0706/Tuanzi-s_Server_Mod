package me.tuanzi.economy.account;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class PlayerAccount {
    private final UUID playerId;
    private final ConcurrentHashMap<String, Double> balances;

    public PlayerAccount(UUID playerId) {
        this.playerId = playerId;
        this.balances = new ConcurrentHashMap<>();
    }

    public UUID getPlayerId() {
        return playerId;
    }

    public double getBalance(String walletTypeId) {
        return balances.getOrDefault(walletTypeId, 0.0);
    }

    public void setBalance(String walletTypeId, double amount) {
        balances.put(walletTypeId, amount);
    }

    public void deposit(String walletTypeId, double amount) {
        balances.merge(walletTypeId, amount, Double::sum);
    }

    public void withdraw(String walletTypeId, double amount) {
        balances.merge(walletTypeId, -amount, Double::sum);
    }

    public boolean hasEnough(String walletTypeId, double amount) {
        return getBalance(walletTypeId) >= amount;
    }

    public boolean hasWallet(String walletTypeId) {
        return balances.containsKey(walletTypeId);
    }

    public Map<String, Double> getAllBalances() {
        return Map.copyOf(balances);
    }

    public void removeWallet(String walletTypeId) {
        balances.remove(walletTypeId);
    }

    public void clearAllBalances() {
        balances.clear();
    }
}
