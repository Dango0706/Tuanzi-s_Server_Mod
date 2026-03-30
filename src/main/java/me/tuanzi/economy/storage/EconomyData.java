package me.tuanzi.economy.storage;

import me.tuanzi.economy.account.PlayerAccount;
import me.tuanzi.economy.currency.WalletType;

import java.util.Collection;
import java.util.Collections;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class EconomyData {
    private final ConcurrentHashMap<String, WalletType> walletTypes = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<UUID, PlayerAccount> playerAccounts = new ConcurrentHashMap<>();

    public EconomyData() {
    }

    public PlayerAccount getOrCreatePlayerAccount(UUID playerId) {
        return playerAccounts.computeIfAbsent(playerId, PlayerAccount::new);
    }

    public PlayerAccount getPlayerAccount(UUID playerId) {
        return playerAccounts.get(playerId);
    }

    public void removePlayerAccount(UUID playerId) {
        playerAccounts.remove(playerId);
    }

    public Collection<PlayerAccount> getAllPlayerAccounts() {
        return Collections.unmodifiableCollection(playerAccounts.values());
    }

    public void registerWalletType(WalletType walletType) {
        walletTypes.put(walletType.id(), walletType);
    }

    public void unregisterWalletType(String id) {
        walletTypes.remove(id);
    }

    public WalletType getWalletType(String id) {
        return walletTypes.get(id);
    }

    public Collection<WalletType> getAllWalletTypes() {
        return Collections.unmodifiableCollection(walletTypes.values());
    }

    public boolean hasWalletType(String id) {
        return walletTypes.containsKey(id);
    }

    ConcurrentHashMap<String, WalletType> getWalletTypesMap() {
        return walletTypes;
    }

    ConcurrentHashMap<UUID, PlayerAccount> getPlayerAccountsMap() {
        return playerAccounts;
    }
}
