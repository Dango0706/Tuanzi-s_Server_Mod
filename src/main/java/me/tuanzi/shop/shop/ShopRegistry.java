package me.tuanzi.shop.shop;

import me.tuanzi.shop.storage.ShopData;
import me.tuanzi.shop.storage.ShopStateSaver;
import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;

import java.util.Collection;
import java.util.Optional;
import java.util.UUID;

public class ShopRegistry {
    private static ShopRegistry instance;
    private final ShopData shopData;
    private final MinecraftServer server;

    private ShopRegistry(MinecraftServer server) {
        this.server = server;
        this.shopData = ShopStateSaver.getServerState(server).getData();
    }

    public static synchronized ShopRegistry getInstance(MinecraftServer server) {
        if (instance == null) {
            instance = new ShopRegistry(server);
        }
        return instance;
    }

    public static synchronized void resetInstance() {
        instance = null;
    }

    public void registerShop(ShopInstance shop) {
        shopData.addShop(shop);
        markDirty();
    }

    public void unregisterShop(UUID shopId) {
        shopData.removeShop(shopId);
        markDirty();
    }

    public Optional<ShopInstance> getShopById(UUID shopId) {
        return Optional.ofNullable(shopData.getShopById(shopId));
    }

    public Optional<ShopInstance> getShopByPos(BlockPos pos) {
        return Optional.ofNullable(shopData.getShopByPos(pos.getX(), pos.getY(), pos.getZ()));
    }

    public Optional<ShopInstance> getShopByPosKey(long posKey) {
        return Optional.ofNullable(shopData.getShopByPos(posKey));
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

    public boolean hasShopAtPos(BlockPos pos) {
        return shopData.hasShopAtPos(pos.getX(), pos.getY(), pos.getZ());
    }

    public int getShopCount() {
        return shopData.getShopCount();
    }

    public void markDirty() {
        ShopStateSaver.getServerState(server).setDirty();
    }

    public static long posToKey(BlockPos pos) {
        return ShopData.posToKey(pos);
    }
}
