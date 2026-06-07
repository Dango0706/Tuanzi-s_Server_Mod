package me.tuanzi.warp;

import java.util.HashMap;
import java.util.Map;

public class WarpData {
    private final Map<String, WarpEntry> warps = new HashMap<>();

    public Map<String, WarpEntry> getWarps() {
        return warps;
    }

    public void addWarp(WarpEntry entry) {
        warps.put(entry.getName().toLowerCase(), entry);
    }

    public void removeWarp(String name) {
        warps.remove(name.toLowerCase());
    }

    public WarpEntry getWarp(String name) {
        return warps.get(name.toLowerCase());
    }
}
