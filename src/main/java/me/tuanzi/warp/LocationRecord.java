package me.tuanzi.warp;

import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

public class LocationRecord {
    public final ResourceKey<Level> dimension;
    public final Vec3 pos;
    public final float yaw;
    public final float pitch;

    public LocationRecord(ResourceKey<Level> dimension, Vec3 pos, float yaw, float pitch) {
        this.dimension = dimension;
        this.pos = pos;
        this.yaw = yaw;
        this.pitch = pitch;
    }
}
