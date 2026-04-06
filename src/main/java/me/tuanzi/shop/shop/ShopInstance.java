package me.tuanzi.shop.shop;

import net.minecraft.core.BlockPos;
import net.minecraft.world.item.ItemStack;

import java.util.UUID;

public class ShopInstance {
    private final UUID shopId;
    private final UUID ownerId;
    private final BlockPos shopPos;
    private final BlockPos signPos;
    private final ShopType shopType;
    private ItemStack tradeItem;
    private double basePrice;
    private double currentPrice;
    private boolean isAdminShop;
    private boolean isInfinite;
    private boolean dynamicPricing;
    private double minPrice;
    private double maxPrice;
    private long totalSold;
    private long totalBought;
    private String walletTypeId;
    private final long createdTime;
    private String description;
    private volatile boolean deleted;

    public ShopInstance(UUID shopId, UUID ownerId, BlockPos shopPos, BlockPos signPos,
                        ShopType shopType, ItemStack tradeItem, double basePrice,
                        String walletTypeId, long createdTime) {
        this.shopId = shopId;
        this.ownerId = ownerId;
        this.shopPos = shopPos;
        this.signPos = signPos;
        this.shopType = shopType;
        this.tradeItem = tradeItem.copy();
        this.basePrice = basePrice;
        this.currentPrice = basePrice;
        this.isAdminShop = false;
        this.isInfinite = false;
        this.dynamicPricing = false;
        this.minPrice = basePrice * 0.1;
        this.maxPrice = basePrice * 10.0;
        this.totalSold = 0;
        this.totalBought = 0;
        this.walletTypeId = walletTypeId;
        this.createdTime = createdTime;
        this.description = "";
    }

    public UUID getShopId() {
        return shopId;
    }

    public UUID getOwnerId() {
        return ownerId;
    }

    public BlockPos getShopPos() {
        return shopPos;
    }

    public BlockPos getSignPos() {
        return signPos;
    }

    public ShopType getShopType() {
        return shopType;
    }

    public ItemStack getTradeItem() {
        return tradeItem.copy();
    }

    public void setTradeItem(ItemStack tradeItem) {
        this.tradeItem = tradeItem.copy();
    }

    public double getBasePrice() {
        return basePrice;
    }

    public void setBasePrice(double basePrice) {
        this.basePrice = basePrice;
    }

    public double getCurrentPrice() {
        return currentPrice;
    }

    public void setCurrentPrice(double currentPrice) {
        this.currentPrice = currentPrice;
    }

    public boolean isAdminShop() {
        return isAdminShop;
    }

    public void setAdminShop(boolean adminShop) {
        isAdminShop = adminShop;
    }

    public boolean isInfinite() {
        return isInfinite;
    }

    public void setInfinite(boolean infinite) {
        isInfinite = infinite;
    }

    public boolean isDynamicPricing() {
        return dynamicPricing;
    }

    public void setDynamicPricing(boolean dynamicPricing) {
        this.dynamicPricing = dynamicPricing;
    }

    public double getMinPrice() {
        return minPrice;
    }

    public void setMinPrice(double minPrice) {
        this.minPrice = minPrice;
    }

    public double getMaxPrice() {
        return maxPrice;
    }

    public void setMaxPrice(double maxPrice) {
        this.maxPrice = maxPrice;
    }

    public long getTotalSold() {
        return totalSold;
    }

    public void incrementTotalSold(long amount) {
        this.totalSold += amount;
    }

    public long getTotalBought() {
        return totalBought;
    }

    public void incrementTotalBought(long amount) {
        this.totalBought += amount;
    }

    public String getWalletTypeId() {
        return walletTypeId;
    }

    public void setWalletTypeId(String walletTypeId) {
        this.walletTypeId = walletTypeId;
    }

    public long getCreatedTime() {
        return createdTime;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public boolean isOwner(UUID playerId) {
        return ownerId.equals(playerId);
    }

    public boolean isValid() {
        return !deleted;
    }

    public boolean isDeleted() {
        return deleted;
    }

    public void markAsDeleted() {
        this.deleted = true;
    }
}
