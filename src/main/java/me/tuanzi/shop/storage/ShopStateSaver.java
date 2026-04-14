package me.tuanzi.shop.storage;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import me.tuanzi.shop.shop.ShopInstance;
import me.tuanzi.shop.shop.ShopType;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.NbtOps;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.datafix.DataFixTypes;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class ShopStateSaver extends SavedData {
    private static final String MOD_ID = "shop-module";
    private final ShopData data;
    private int accumulatedTicks = 0;

    public ShopStateSaver() {
        this.data = new ShopData();
    }

    public ShopData getData() {
        return data;
    }

    public int getAccumulatedTicks() {
        return accumulatedTicks;
    }

    public void setAccumulatedTicks(int accumulatedTicks) {
        this.accumulatedTicks = accumulatedTicks;
        this.setDirty();
    }

    public void addTicks(int count) {
        this.accumulatedTicks += count;
        this.setDirty();
    }

    private record BlockPosDto(int x, int y, int z) {
        private static final Codec<BlockPosDto> CODEC = RecordCodecBuilder.create(instance ->
                instance.group(
                        Codec.INT.fieldOf("x").forGetter(BlockPosDto::x),
                        Codec.INT.fieldOf("y").forGetter(BlockPosDto::y),
                        Codec.INT.fieldOf("z").forGetter(BlockPosDto::z)
                ).apply(instance, BlockPosDto::new)
        );

        static BlockPosDto fromBlockPos(BlockPos pos) {
            return new BlockPosDto(pos.getX(), pos.getY(), pos.getZ());
        }

        BlockPos toBlockPos() {
            return new BlockPos(x(), y(), z());
        }
    }

    private record ShopCoreDto(
            String shopId,
            String ownerId,
            BlockPosDto shopPos,
            BlockPosDto signPos,
            String shopType,
            ItemStack tradeItem,
            String walletTypeId,
            long createdTime
    ) {
        private static final Codec<ShopCoreDto> CODEC = RecordCodecBuilder.create(instance ->
                instance.group(
                        Codec.STRING.fieldOf("shopId").forGetter(ShopCoreDto::shopId),
                        Codec.STRING.fieldOf("ownerId").forGetter(ShopCoreDto::ownerId),
                        BlockPosDto.CODEC.fieldOf("shopPos").forGetter(ShopCoreDto::shopPos),
                        BlockPosDto.CODEC.fieldOf("signPos").forGetter(ShopCoreDto::signPos),
                        Codec.STRING.fieldOf("shopType").forGetter(ShopCoreDto::shopType),
                        ItemStack.CODEC.fieldOf("tradeItem").forGetter(ShopCoreDto::tradeItem),
                        Codec.STRING.fieldOf("walletTypeId").forGetter(ShopCoreDto::walletTypeId),
                        Codec.LONG.fieldOf("createdTime").forGetter(ShopCoreDto::createdTime)
                ).apply(instance, ShopCoreDto::new)
        );

        static ShopCoreDto fromShop(ShopInstance shop) {
            return new ShopCoreDto(
                    shop.getShopId().toString(),
                    shop.getOwnerId().toString(),
                    BlockPosDto.fromBlockPos(shop.getShopPos()),
                    BlockPosDto.fromBlockPos(shop.getSignPos()),
                    shop.getShopType().name(),
                    shop.getTradeItem(),
                    shop.getWalletTypeId(),
                    shop.getCreatedTime()
            );
        }
    }

    private record ShopPriceDto(
            double basePrice,
            double currentPrice,
            double minPrice,
            double maxPrice,
            boolean dynamicPricing,
            double halfLifeConstant,
            double systemStock,
            double decayRate
    ) {
        private static final Codec<ShopPriceDto> CODEC = RecordCodecBuilder.create(instance ->
                instance.group(
                        Codec.DOUBLE.fieldOf("basePrice").forGetter(ShopPriceDto::basePrice),
                        Codec.DOUBLE.fieldOf("currentPrice").forGetter(ShopPriceDto::currentPrice),
                        Codec.DOUBLE.fieldOf("minPrice").forGetter(ShopPriceDto::minPrice),
                        Codec.DOUBLE.fieldOf("maxPrice").forGetter(ShopPriceDto::maxPrice),
                        Codec.BOOL.fieldOf("dynamicPricing").forGetter(ShopPriceDto::dynamicPricing),
                        Codec.DOUBLE.fieldOf("halfLifeConstant").orElse(500.0).forGetter(ShopPriceDto::halfLifeConstant),
                        Codec.DOUBLE.fieldOf("systemStock").orElse(0.0).forGetter(ShopPriceDto::systemStock),
                        Codec.DOUBLE.fieldOf("decayRate").orElse(0.01).forGetter(ShopPriceDto::decayRate)
                ).apply(instance, ShopPriceDto::new)
        );

        static ShopPriceDto fromShop(ShopInstance shop) {
            return new ShopPriceDto(
                    shop.getBasePrice(),
                    shop.getCurrentPrice(),
                    shop.getMinPrice(),
                    shop.getMaxPrice(),
                    shop.isDynamicPricing(),
                    shop.getHalfLifeConstant(),
                    shop.getSystemStock(),
                    shop.getDecayRate()
            );
        }
    }

    private record ShopStatsDto(
            boolean isAdminShop,
            boolean isInfinite,
            long totalSold,
            long totalBought,
            String description
    ) {
        private static final Codec<ShopStatsDto> CODEC = RecordCodecBuilder.create(instance ->
                instance.group(
                        Codec.BOOL.fieldOf("isAdminShop").forGetter(ShopStatsDto::isAdminShop),
                        Codec.BOOL.fieldOf("isInfinite").forGetter(ShopStatsDto::isInfinite),
                        Codec.LONG.fieldOf("totalSold").forGetter(ShopStatsDto::totalSold),
                        Codec.LONG.fieldOf("totalBought").forGetter(ShopStatsDto::totalBought),
                        Codec.STRING.fieldOf("description").forGetter(ShopStatsDto::description)
                ).apply(instance, ShopStatsDto::new)
        );

        static ShopStatsDto fromShop(ShopInstance shop) {
            return new ShopStatsDto(
                    shop.isAdminShop(),
                    shop.isInfinite(),
                    shop.getTotalSold(),
                    shop.getTotalBought(),
                    shop.getDescription()
            );
        }
    }

    private record ShopDataDto(
            ShopCoreDto core,
            ShopPriceDto price,
            ShopStatsDto stats
    ) {
        private static final Codec<ShopDataDto> CODEC = RecordCodecBuilder.create(instance ->
                instance.group(
                        ShopCoreDto.CODEC.fieldOf("core").forGetter(ShopDataDto::core),
                        ShopPriceDto.CODEC.fieldOf("price").forGetter(ShopDataDto::price),
                        ShopStatsDto.CODEC.fieldOf("stats").forGetter(ShopDataDto::stats)
                ).apply(instance, ShopDataDto::new)
        );

        static ShopDataDto fromShop(ShopInstance shop) {
            return new ShopDataDto(
                    ShopCoreDto.fromShop(shop),
                    ShopPriceDto.fromShop(shop),
                    ShopStatsDto.fromShop(shop)
            );
        }

        ShopInstance toShop() {
            UUID shopId = UUID.fromString(core().shopId());
            UUID ownerId = UUID.fromString(core().ownerId());
            BlockPos shopPos = core().shopPos().toBlockPos();
            BlockPos signPos = core().signPos().toBlockPos();
            ShopType type = ShopType.valueOf(core().shopType());

            ShopInstance shop = new ShopInstance(shopId, ownerId, shopPos, signPos, type,
                    core().tradeItem(), price().basePrice(), core().walletTypeId(), core().createdTime());

            shop.setCurrentPrice(price().currentPrice());
            shop.setAdminShop(stats().isAdminShop());
            shop.setInfinite(stats().isInfinite());
            shop.setDynamicPricing(price().dynamicPricing());
            shop.setMinPrice(price().minPrice());
            shop.setMaxPrice(price().maxPrice());
            shop.setHalfLifeConstant(price().halfLifeConstant());
            shop.setSystemStock(price().systemStock());
            shop.setDecayRate(price().decayRate());
            for (long i = 0; i < stats().totalSold(); i++) {
                shop.incrementTotalSold(1);
            }
            for (long i = 0; i < stats().totalBought(); i++) {
                shop.incrementTotalBought(1);
            }
            shop.setDescription(stats().description());
            return shop;
        }
    }

    private static final Codec<List<ShopDataDto>> SHOP_LIST_CODEC = Codec.list(ShopDataDto.CODEC);

    private record RootDto(List<ShopDataDto> shops, int accumulatedTicks) {
        private static final Codec<RootDto> CODEC = RecordCodecBuilder.create(instance ->
                instance.group(
                        SHOP_LIST_CODEC.fieldOf("shops").forGetter(RootDto::shops),
                        Codec.INT.fieldOf("accumulatedTicks").orElse(0).forGetter(RootDto::accumulatedTicks)
                ).apply(instance, RootDto::new)
        );
    }

    private static final Codec<ShopStateSaver> CODEC = RootDto.CODEC.xmap(
            rootDto -> {
                ShopStateSaver saver = new ShopStateSaver();
                for (ShopDataDto dto : rootDto.shops()) {
                    ShopInstance shop = dto.toShop();
                    if (shop != null) {
                        saver.data.addShop(shop);
                    }
                }
                saver.accumulatedTicks = rootDto.accumulatedTicks();
                return saver;
            },
            saver -> {
                List<ShopDataDto> shops = new ArrayList<>();
                for (ShopInstance shop : saver.data.getAllShops()) {
                    shops.add(ShopDataDto.fromShop(shop));
                }
                return new RootDto(shops, saver.accumulatedTicks);
            }
    );

    private static final SavedDataType<ShopStateSaver> TYPE = new SavedDataType<>(
            Identifier.fromNamespaceAndPath(MOD_ID, "shop_data"),
            ShopStateSaver::new,
            CODEC,
            DataFixTypes.SAVED_DATA_MAP_DATA
    );

    public static ShopStateSaver getServerState(MinecraftServer server) {
        ShopStateSaver state = server.getDataStorage().computeIfAbsent(TYPE);
        state.setDirty();
        return state;
    }
}
