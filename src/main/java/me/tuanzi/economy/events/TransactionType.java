package me.tuanzi.economy.events;

import java.util.UUID;

public sealed interface TransactionType
    permits TransactionType.Deposit, TransactionType.Withdraw, TransactionType.Transfer {
    
    record Deposit() implements TransactionType {}
    record Withdraw() implements TransactionType {}
    record Transfer(UUID fromPlayer, UUID toPlayer) implements TransactionType {}
}
