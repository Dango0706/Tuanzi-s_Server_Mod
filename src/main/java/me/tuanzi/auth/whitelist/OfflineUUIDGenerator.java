package me.tuanzi.auth.whitelist;

import java.nio.charset.StandardCharsets;
import java.util.UUID;

public final class OfflineUUIDGenerator {

    private OfflineUUIDGenerator() {
    }

    public static UUID generateOfflineUUID(String playerName) {
        return UUID.nameUUIDFromBytes(
            ("OfflinePlayer:" + playerName).getBytes(StandardCharsets.UTF_8)
        );
    }
}
