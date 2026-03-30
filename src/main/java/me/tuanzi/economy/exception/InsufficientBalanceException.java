package me.tuanzi.economy.exception;

public class InsufficientBalanceException extends RuntimeException {
    private final String walletTypeId;
    private final double currentBalance;
    private final double requiredAmount;

    public InsufficientBalanceException(String walletTypeId, double currentBalance, double requiredAmount) {
        super(String.format("Insufficient balance in wallet '%s'. Required: %.2f, Available: %.2f",
                walletTypeId, requiredAmount, currentBalance));
        this.walletTypeId = walletTypeId;
        this.currentBalance = currentBalance;
        this.requiredAmount = requiredAmount;
    }

    public String getWalletTypeId() {
        return walletTypeId;
    }

    public double getCurrentBalance() {
        return currentBalance;
    }

    public double getRequiredAmount() {
        return requiredAmount;
    }
}
