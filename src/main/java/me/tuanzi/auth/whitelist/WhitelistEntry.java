package me.tuanzi.auth.whitelist;

import java.util.UUID;

public class WhitelistEntry {
    private String name;
    private UUID uuid;

    public WhitelistEntry() {
    }

    public WhitelistEntry(String name, UUID uuid) {
        this.name = name;
        this.uuid = uuid;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public UUID getUuid() {
        return uuid;
    }

    public void setUuid(UUID uuid) {
        this.uuid = uuid;
    }
}
