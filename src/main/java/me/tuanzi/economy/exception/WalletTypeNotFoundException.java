package me.tuanzi.economy.exception;

public class WalletTypeNotFoundException extends RuntimeException {
    private final String walletTypeId;

    public WalletTypeNotFoundException(String walletTypeId) {
        super(String.format("Wallet type '%s' not found", walletTypeId));
        this.walletTypeId = walletTypeId;
    }

    public String getWalletTypeId() {
        return walletTypeId;
    }
}
