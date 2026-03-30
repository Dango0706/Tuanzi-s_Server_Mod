package me.tuanzi.economy.events;

import java.util.UUID;

public record TransactionRecord(
    UUID playerId,
    String walletTypeId,
    double amount,
    double balanceBefore,
    double balanceAfter,
    TransactionType type,
    long timestamp
) {}
