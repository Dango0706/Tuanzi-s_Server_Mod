package me.tuanzi.economy.events;

import net.fabricmc.fabric.api.event.Event;
import net.fabricmc.fabric.api.event.EventFactory;

public interface TransactionCallback {
    Event<TransactionCallback> EVENT = EventFactory.createArrayBacked(
        TransactionCallback.class,
        (listeners) -> (record) -> {
            for (TransactionCallback listener : listeners) {
                listener.onTransaction(record);
            }
        }
    );
    
    void onTransaction(TransactionRecord record);
}
