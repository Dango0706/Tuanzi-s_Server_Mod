package me.tuanzi.shop.storage;

import me.tuanzi.shop.shop.ShopInstance;

import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class ShopData {
    private final ConcurrentHashMap<UUID, ShopInstance> shopsById = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<Long, ShopInstance> shopsByPosKey = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<UUID, ConcurrentHashMap<UUID, ShopInstance>> shopsByOwner = new ConcurrentHashMap<>();

    public ShopData() {
    }

    public void addShop(ShopInstance shop) {
        shopsById.put(shop.getShopId(), shop);
        shopsByPosKey.put(posToKey(shop.getShopPos()), shop);
        shopsByPosKey.put(posToKey(shop.getSignPos()), shop);
        shopsByOwner.computeIfAbsent(shop.getOwnerId(), k -> new ConcurrentHashMap<>())
                .put(shop.getShopId(), shop);
    }

    public void removeShop(UUID shopId) {
        ShopInstance shop = shopsById.remove(shopId);
        if (shop != null) {
            shopsByPosKey.remove(posToKey(shop.getShopPos()));
            shopsByPosKey.remove(posToKey(shop.getSignPos()));
            ConcurrentHashMap<UUID, ShopInstance> ownerShops = shopsByOwner.get(shop.getOwnerId());
            if (ownerShops != null) {
                ownerShops.remove(shopId);
                if (ownerShops.isEmpty()) {
                    shopsByOwner.remove(shop.getOwnerId());
                }
            }
        }
    }

    public ShopInstance getShopById(UUID shopId) {
        return shopsById.get(shopId);
    }

    public ShopInstance getShopByPos(long posKey) {
        return shopsByPosKey.get(posKey);
    }

    public ShopInstance getShopByPos(int x, int y, int z) {
        return shopsByPosKey.get(posToKey(x, y, z));
    }

    public Collection<ShopInstance> getShopsByOwner(UUID ownerId) {
        ConcurrentHashMap<UUID, ShopInstance> ownerShops = shopsByOwner.get(ownerId);
        return ownerShops != null ? Collections.unmodifiableCollection(ownerShops.values()) : Collections.emptyList();
    }

    public Collection<ShopInstance> getAllShops() {
        return Collections.unmodifiableCollection(shopsById.values());
    }

    public boolean hasShop(UUID shopId) {
        return shopsById.containsKey(shopId);
    }

    public boolean hasShopAtPos(long posKey) {
        return shopsByPosKey.containsKey(posKey);
    }

    public boolean hasShopAtPos(int x, int y, int z) {
        return shopsByPosKey.containsKey(posToKey(x, y, z));
    }

    public int getShopCount() {
        return shopsById.size();
    }

    public void clear() {
        shopsById.clear();
        shopsByPosKey.clear();
        shopsByOwner.clear();
    }

    public static long posToKey(int x, int y, int z) {
        return ((long) x & 0xFFFFFFL) | (((long) y & 0xFFFFL) << 24) | (((long) z & 0xFFFFFFL) << 40);
    }

    public static long posToKey(net.minecraft.core.BlockPos pos) {
        return posToKey(pos.getX(), pos.getY(), pos.getZ());
    }

    public static int[] keyToPos(long key) {
        int x = (int) (key & 0xFFFFFFL);
        int y = (int) ((key >> 24) & 0xFFFFL);
        int z = (int) ((key >> 40) & 0xFFFFFFL);
        return new int[]{x, y, z};
    }

    Map<UUID, ShopInstance> getShopsByIdMap() {
        return shopsById;
    }
}
