package me.tuanzi.economy.api;

import me.tuanzi.economy.currency.WalletType;
import net.minecraft.network.chat.Component;

import java.util.Collection;
import java.util.Optional;
import java.util.UUID;

public interface EconomyAPI {
    double getBalance(UUID player, String walletTypeId);

    void deposit(UUID player, String walletTypeId, double amount);

    void withdraw(UUID player, String walletTypeId, double amount);

    boolean hasEnough(UUID player, String walletTypeId, double amount);

    void setBalance(UUID player, String walletTypeId, double amount);

    void registerWalletType(String id, Component displayName);

    void unregisterWalletType(String id);

    Optional<WalletType> getWalletType(String id);

    Collection<WalletType> getAllWalletTypes();

    void transfer(UUID from, UUID to, String walletTypeId, double amount);
}
