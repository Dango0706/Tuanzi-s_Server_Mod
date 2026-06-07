package me.tuanzi.warp;

import net.minecraft.server.MinecraftServer;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;
import java.util.Collection;

public class WarpManager {
    private static WarpManager instance;
    private final MinecraftServer server;

    private WarpManager(MinecraftServer server) {
        this.server = server;
    }

    public static void init(MinecraftServer server) {
        instance = new WarpManager(server);
    }

    public static WarpManager getInstance() {
        return instance;
    }

    public WarpData getWarpData() {
        return WarpStateSaver.getServerState(server).getData();
    }

    public boolean createWarp(String name, ResourceKey<Level> dimension, double x, double y, double z, float yaw, float pitch) {
        if (getWarpData().getWarp(name) != null) {
            return false;
        }
        WarpEntry entry = new WarpEntry(name, dimension, x, y, z, yaw, pitch);
        getWarpData().addWarp(entry);
        WarpStateSaver.getServerState(server).setDirty();
        return true;
    }

    public boolean deleteWarp(String name) {
        if (getWarpData().getWarp(name) == null) {
            return false;
        }
        getWarpData().removeWarp(name);
        WarpStateSaver.getServerState(server).setDirty();
        return true;
    }

    public WarpEntry getWarp(String name) {
        return getWarpData().getWarp(name);
    }

    public Collection<WarpEntry> getAllWarps() {
        return getWarpData().getWarps().values();
    }
}
