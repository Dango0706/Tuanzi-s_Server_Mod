package me.tuanzi.shop.shop;

public class ShopConfig {
    private static final int DEFAULT_DELETE_CONFIRM_TIMEOUT_SECONDS = 30;
    private static final int DEFAULT_MAX_STACK_SIZE = 64;
    private static final double DEFAULT_MIN_PRICE_MULTIPLIER = 0.1;
    private static final double DEFAULT_MAX_PRICE_MULTIPLIER = 10.0;

    private int deleteConfirmTimeoutSeconds = DEFAULT_DELETE_CONFIRM_TIMEOUT_SECONDS;
    private int maxStackSize = DEFAULT_MAX_STACK_SIZE;
    private double minPriceMultiplier = DEFAULT_MIN_PRICE_MULTIPLIER;
    private double maxPriceMultiplier = DEFAULT_MAX_PRICE_MULTIPLIER;
    private boolean enableTransactionLog = true;
    private int logRetentionDays = 30;

    public int getDeleteConfirmTimeoutSeconds() {
        return deleteConfirmTimeoutSeconds;
    }

    public void setDeleteConfirmTimeoutSeconds(int deleteConfirmTimeoutSeconds) {
        this.deleteConfirmTimeoutSeconds = deleteConfirmTimeoutSeconds;
    }

    public int getMaxStackSize() {
        return maxStackSize;
    }

    public void setMaxStackSize(int maxStackSize) {
        this.maxStackSize = maxStackSize;
    }

    public double getMinPriceMultiplier() {
        return minPriceMultiplier;
    }

    public void setMinPriceMultiplier(double minPriceMultiplier) {
        this.minPriceMultiplier = minPriceMultiplier;
    }

    public double getMaxPriceMultiplier() {
        return maxPriceMultiplier;
    }

    public void setMaxPriceMultiplier(double maxPriceMultiplier) {
        this.maxPriceMultiplier = maxPriceMultiplier;
    }

    public boolean isEnableTransactionLog() {
        return enableTransactionLog;
    }

    public void setEnableTransactionLog(boolean enableTransactionLog) {
        this.enableTransactionLog = enableTransactionLog;
    }

    public int getLogRetentionDays() {
        return logRetentionDays;
    }

    public void setLogRetentionDays(int logRetentionDays) {
        this.logRetentionDays = logRetentionDays;
    }
}
