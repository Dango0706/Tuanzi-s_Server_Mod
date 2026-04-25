package me.tuanzi.title;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.datafix.DataFixTypes;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class TitleStateSaver extends SavedData {
    private static final String MOD_ID = "title-module";
    
    private static final Codec<TitleData> TITLE_DATA_CODEC = RecordCodecBuilder.create(instance ->
            instance.group(
                    Codec.unboundedMap(Codec.STRING, Title.CODEC).fieldOf("titles").forGetter(TitleData::getTitles),
                    Codec.unboundedMap(Codec.STRING.xmap(UUID::fromString, UUID::toString), PlayerTitleData.CODEC).fieldOf("playerTitles").forGetter(TitleData::getPlayerTitles)
            ).apply(instance, (titles, playerTitles) -> {
                TitleData data = new TitleData();
                data.getTitles().putAll(titles);
                data.getPlayerTitles().putAll(playerTitles);
                return data;
            })
    );

    private static final Codec<TitleStateSaver> CODEC = RecordCodecBuilder.create(instance ->
            instance.group(
                    TITLE_DATA_CODEC.fieldOf("data").forGetter(saver -> saver.data)
            ).apply(instance, TitleStateSaver::new)
    );

    private static final SavedDataType<TitleStateSaver> TYPE = new SavedDataType<>(
            Identifier.fromNamespaceAndPath(MOD_ID, "title_data"),
            TitleStateSaver::new,
            CODEC,
            DataFixTypes.SAVED_DATA_MAP_DATA
    );

    private final TitleData data;

    public TitleStateSaver() {
        this.data = new TitleData();
    }

    public TitleStateSaver(TitleData data) {
        this.data = data;
    }

    public static TitleStateSaver getServerState(MinecraftServer server) {
        TitleStateSaver state = server.getDataStorage().computeIfAbsent(TYPE);
        state.setDirty();
        return state;
    }

    public TitleData getData() {
        return data;
    }
}
