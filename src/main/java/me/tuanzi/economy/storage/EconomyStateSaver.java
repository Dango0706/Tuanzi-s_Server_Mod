package me.tuanzi.economy.storage;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import me.tuanzi.economy.account.PlayerAccount;
import me.tuanzi.economy.currency.WalletType;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentSerialization;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.datafix.DataFixTypes;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class EconomyStateSaver extends SavedData {
    private static final String MOD_ID = "economy-module";
    private final EconomyData data;

    public EconomyStateSaver() {
        this.data = new EconomyData();
    }

    public EconomyStateSaver(EconomyData data) {
        this.data = data;
    }

    public EconomyData getData() {
        return data;
    }

    private static final Codec<WalletType> WALLET_TYPE_CODEC = RecordCodecBuilder.create(instance ->
            instance.group(
                    Codec.STRING.fieldOf("id").forGetter(WalletType::id),
                    ComponentSerialization.CODEC.fieldOf("displayName").forGetter(WalletType::displayName)
            ).apply(instance, WalletType::new)
    );

    private static final Codec<PlayerAccount> PLAYER_ACCOUNT_CODEC = RecordCodecBuilder.create(instance ->
            instance.group(
                    Codec.STRING.xmap(UUID::fromString, UUID::toString).fieldOf("playerId").forGetter(PlayerAccount::getPlayerId),
                    Codec.unboundedMap(Codec.STRING, Codec.DOUBLE).fieldOf("balances").forGetter(account -> {
                        Map<String, Double> balances = new HashMap<>();
                        account.getAllBalances().forEach(balances::put);
                        return balances;
                    })
            ).apply(instance, (uuid, balances) -> {
                PlayerAccount account = new PlayerAccount(uuid);
                balances.forEach(account::setBalance);
                return account;
            })
    );

    private static final Codec<EconomyData> ECONOMY_DATA_CODEC = RecordCodecBuilder.create(instance ->
            instance.group(
                    Codec.unboundedMap(Codec.STRING, WALLET_TYPE_CODEC).fieldOf("walletTypes").forGetter(economyData -> {
                        Map<String, WalletType> map = new HashMap<>();
                        economyData.getWalletTypesMap().forEach(map::put);
                        return map;
                    }),
                    Codec.unboundedMap(Codec.STRING.xmap(UUID::fromString, UUID::toString), PLAYER_ACCOUNT_CODEC).fieldOf("playerAccounts").forGetter(economyData -> {
                        Map<UUID, PlayerAccount> map = new HashMap<>();
                        economyData.getPlayerAccountsMap().forEach(map::put);
                        return map;
                    })
            ).apply(instance, (walletTypes, playerAccounts) -> {
                EconomyData data = new EconomyData();
                walletTypes.values().forEach(data::registerWalletType);
                playerAccounts.values().forEach(account -> data.getPlayerAccountsMap().put(account.getPlayerId(), account));
                return data;
            })
    );

    private static final Codec<EconomyStateSaver> CODEC = RecordCodecBuilder.create(instance ->
            instance.group(
                    ECONOMY_DATA_CODEC.fieldOf("data").forGetter(saver -> saver.data)
            ).apply(instance, EconomyStateSaver::new)
    );

    private static final SavedDataType<EconomyStateSaver> TYPE = new SavedDataType<>(
            Identifier.fromNamespaceAndPath(MOD_ID, "economy_data"),
            EconomyStateSaver::new,
            CODEC,
            DataFixTypes.SAVED_DATA_MAP_DATA
    );

    public static EconomyStateSaver getServerState(MinecraftServer server) {
        EconomyStateSaver state = server.getDataStorage().computeIfAbsent(TYPE);
        state.setDirty();
        return state;
    }
}
