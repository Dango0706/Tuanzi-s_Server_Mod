package me.tuanzi.warp;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.datafix.DataFixTypes;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;

public class WarpStateSaver extends SavedData {
    private static final String MOD_ID = "warp-module";
    
    private static final Codec<WarpData> WARP_DATA_CODEC = RecordCodecBuilder.create(instance ->
            instance.group(
                    Codec.unboundedMap(Codec.STRING, WarpEntry.CODEC).fieldOf("warps").forGetter(WarpData::getWarps)
            ).apply(instance, warps -> {
                WarpData data = new WarpData();
                data.getWarps().putAll(warps);
                return data;
            })
    );

    private static final Codec<WarpStateSaver> CODEC = RecordCodecBuilder.create(instance ->
            instance.group(
                    WARP_DATA_CODEC.fieldOf("data").forGetter(saver -> saver.data)
            ).apply(instance, WarpStateSaver::new)
    );

    private static final SavedDataType<WarpStateSaver> TYPE = new SavedDataType<>(
            Identifier.fromNamespaceAndPath(MOD_ID, "warp_data"),
            WarpStateSaver::new,
            CODEC,
            DataFixTypes.SAVED_DATA_MAP_DATA
    );

    private final WarpData data;

    public WarpStateSaver() {
        this.data = new WarpData();
    }

    public WarpStateSaver(WarpData data) {
        this.data = data;
    }

    public static WarpStateSaver getServerState(MinecraftServer server) {
        WarpStateSaver state = server.getDataStorage().computeIfAbsent(TYPE);
        state.setDirty();
        return state;
    }

    public WarpData getData() {
        return data;
    }
}
