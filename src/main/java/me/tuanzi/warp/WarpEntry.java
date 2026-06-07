package me.tuanzi.warp;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

public class WarpEntry {
    private final String name;
    private final ResourceKey<Level> dimension;
    private final double x, y, z;
    private final float yaw, pitch;

    public WarpEntry(String name, ResourceKey<Level> dimension, double x, double y, double z, float yaw, float pitch) {
        this.name = name;
        this.dimension = dimension;
        this.x = x;
        this.y = y;
        this.z = z;
        this.yaw = yaw;
        this.pitch = pitch;
    }

    public static final Codec<WarpEntry> CODEC = RecordCodecBuilder.create(instance ->
            instance.group(
                    Codec.STRING.fieldOf("name").forGetter(WarpEntry::getName),
                    ResourceKey.codec(Registries.DIMENSION).fieldOf("dimension").forGetter(WarpEntry::getDimension),
                    Codec.DOUBLE.fieldOf("x").forGetter(WarpEntry::getX),
                    Codec.DOUBLE.fieldOf("y").forGetter(WarpEntry::getY),
                    Codec.DOUBLE.fieldOf("z").forGetter(WarpEntry::getZ),
                    Codec.FLOAT.fieldOf("yaw").forGetter(WarpEntry::getYaw),
                    Codec.FLOAT.fieldOf("pitch").forGetter(WarpEntry::getPitch)
            ).apply(instance, WarpEntry::new)
    );

    public String getName() { return name; }
    public ResourceKey<Level> getDimension() { return dimension; }
    public double getX() { return x; }
    public double getY() { return y; }
    public double getZ() { return z; }
    public float getYaw() { return yaw; }
    public float getPitch() { return pitch; }
    
    public LocationRecord toLocationRecord() {
        return new LocationRecord(dimension, new Vec3(x, y, z), yaw, pitch);
    }
}
