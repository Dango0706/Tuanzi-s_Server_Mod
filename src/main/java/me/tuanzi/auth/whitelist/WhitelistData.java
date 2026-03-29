package me.tuanzi.auth.whitelist;

import java.util.ArrayList;
import java.util.List;

public class WhitelistData {
    private List<WhitelistEntry> whitelist;

    public WhitelistData() {
        this.whitelist = new ArrayList<>();
    }

    public List<WhitelistEntry> getWhitelist() {
        return whitelist;
    }

    public void setWhitelist(List<WhitelistEntry> whitelist) {
        this.whitelist = whitelist;
    }
}
