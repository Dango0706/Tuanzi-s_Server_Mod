package me.tuanzi.shop.storage;

import me.tuanzi.shop.shop.ShopInstance;

import java.util.Collection;
import java.util.Collections;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class ShopRegistry {
    private static ShopRegistry instance;
    private final ShopData shopData;

    private ShopRegistry() {
        this.shopData = new ShopData();
    }

    public static synchronized ShopRegistry getInstance(Object server) {
        if (instance == null) {
            instance = new ShopRegistry();
        }
        return instance;
    }

    public static synchronized void resetInstance() {
        instance = null;
    }

    public void registerShop(ShopInstance shop) {
        shopData.addShop(shop);
    }

    public void unregisterShop(UUID shopId) {
        shopData.removeShop(shopId);
    }

    public Optional<ShopInstance> getShopById(UUID shopId) {
        return Optional.ofNullable(shopData.getShopById(shopId));
    }

    public Optional<ShopInstance> getShopByPos(net.minecraft.core.BlockPos pos) {
        return Optional.ofNullable(shopData.getShopByPos(pos.getX(), pos.getY(), pos.getZ()));
    }

    public Collection<ShopInstance> getShopsByOwner(UUID ownerId) {
        return shopData.getShopsByOwner(ownerId);
    }

    public Collection<ShopInstance> getAllShops() {
        return shopData.getAllShops();
    }

    public boolean hasShop(UUID shopId) {
        return shopData.hasShop(shopId);
    }

    public boolean hasShopAtPos(net.minecraft.core.BlockPos pos) {
        return shopData.hasShopAtPos(pos.getX(), pos.getY(), pos.getZ());
    }

    public int getShopCount() {
        return shopData.getShopCount();
    }

    public ShopData getShopData() {
        return shopData;
    }
}
