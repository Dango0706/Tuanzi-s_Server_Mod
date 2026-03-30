package me.tuanzi.economy.currency;

import net.minecraft.network.chat.Component;

import java.util.Collection;
import java.util.Collections;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

public final class WalletTypeRegistry {
    private static final ConcurrentHashMap<String, WalletType> WALLET_TYPES = new ConcurrentHashMap<>();

    private WalletTypeRegistry() {
    }

    public static void register(String id, Component displayName) {
        WalletType walletType = new WalletType(id, displayName);
        WALLET_TYPES.put(id, walletType);
    }

    public static void register(WalletType walletType) {
        WALLET_TYPES.put(walletType.id(), walletType);
    }

    public static void unregister(String id) {
        WALLET_TYPES.remove(id);
    }

    public static Optional<WalletType> get(String id) {
        return Optional.ofNullable(WALLET_TYPES.get(id));
    }

    public static boolean exists(String id) {
        return WALLET_TYPES.containsKey(id);
    }

    public static Collection<WalletType> getAll() {
        return Collections.unmodifiableCollection(WALLET_TYPES.values());
    }

    public static void clear() {
        WALLET_TYPES.clear();
    }

    public static int size() {
        return WALLET_TYPES.size();
    }
}
