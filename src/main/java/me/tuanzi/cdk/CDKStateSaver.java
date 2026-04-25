package me.tuanzi.cdk;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.datafix.DataFixTypes;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;

public class CDKStateSaver extends SavedData {
    private static final String MOD_ID = "cdk-module";
    
    private static final Codec<CDKData> CDK_DATA_CODEC = RecordCodecBuilder.create(instance ->
            instance.group(
                    Codec.unboundedMap(Codec.STRING, CDKEntry.CODEC).fieldOf("cdks").forGetter(CDKData::getCdks)
            ).apply(instance, cdks -> {
                CDKData data = new CDKData();
                data.getCdks().putAll(cdks);
                return data;
            })
    );

    private static final Codec<CDKStateSaver> CODEC = RecordCodecBuilder.create(instance ->
            instance.group(
                    CDK_DATA_CODEC.fieldOf("data").forGetter(saver -> saver.data)
            ).apply(instance, CDKStateSaver::new)
    );

    private static final SavedDataType<CDKStateSaver> TYPE = new SavedDataType<>(
            Identifier.fromNamespaceAndPath(MOD_ID, "cdk_data"),
            CDKStateSaver::new,
            CODEC,
            DataFixTypes.SAVED_DATA_MAP_DATA
    );

    private final CDKData data;

    public CDKStateSaver() {
        this.data = new CDKData();
    }

    public CDKStateSaver(CDKData data) {
        this.data = data;
    }

    public static CDKStateSaver getServerState(MinecraftServer server) {
        CDKStateSaver state = server.getDataStorage().computeIfAbsent(TYPE);
        state.setDirty();
        return state;
    }

    public CDKData getData() {
        return data;
    }
}
