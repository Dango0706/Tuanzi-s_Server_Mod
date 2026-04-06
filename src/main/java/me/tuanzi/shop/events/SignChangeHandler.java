package me.tuanzi.shop.events;

import me.tuanzi.economy.currency.WalletType;
import me.tuanzi.shop.ShopModule;
import me.tuanzi.shop.display.ShopDisplayManager;
import me.tuanzi.shop.shop.ShopInstance;
import me.tuanzi.shop.shop.ShopManager;
import me.tuanzi.shop.shop.ShopType;
import me.tuanzi.shop.utils.ItemUtils;
import me.tuanzi.shop.utils.ShopTranslationHelper;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.network.FilteredText;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.ChestBlock;
import net.minecraft.world.level.block.entity.SignBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Locale;
import java.util.Optional;

public class SignChangeHandler {
    private static final Logger LOGGER = LoggerFactory.getLogger(ShopModule.MOD_ID);
    private final ShopManager shopManager;

    public SignChangeHandler(ShopManager shopManager) {
        this.shopManager = shopManager;
    }

    public void handleSignChange(ServerPlayer player, SignBlockEntity signEntity, List<FilteredText> filteredLines, boolean frontText) {
        LOGGER.info("[商店调试] 告示牌编辑事件触发 - 玩家: {}, 位置: {}, 正面: {}", 
                player.getName().getString(), signEntity.getBlockPos(), frontText);

        String[] lines = new String[4];
        for (int i = 0; i < Math.min(4, filteredLines.size()); i++) {
            lines[i] = filteredLines.get(i).raw();
        }
        for (int i = filteredLines.size(); i < 4; i++) {
            lines[i] = "";
        }

        LOGGER.info("[商店调试] 告示牌内容: [0]='{}', [1]='{}', [2]='{}', [3]='{}'", 
                lines[0], lines[1], lines[2], lines[3]);

        handleSignChange(player, signEntity, lines);
    }

    public boolean handleSignChange(ServerPlayer player, SignBlockEntity signEntity, String[] lines) {
        String firstLine = lines[0].trim();
        
        LOGGER.debug("[商店调试] 检查第一行是否匹配商店模式: '{}'", firstLine);
        
        if (!ShopTranslationHelper.isSellPattern(firstLine) && !ShopTranslationHelper.isBuyPattern(firstLine)) {
            LOGGER.debug("[商店调试] 第一行不匹配商店模式，跳过处理");
            return false;
        }

        LOGGER.info("[商店调试] 检测到商店模式: '{}'", firstLine);

        if (!isSignAttachedToChest(signEntity)) {
            LOGGER.warn("[商店调试] 告示牌未附着在箱子上 - 位置: {}", signEntity.getBlockPos());
            player.sendSystemMessage(ShopTranslationHelper.literal(
                    ShopTranslationHelper.getRawTranslation("shop.sign.not_on_chest")));
            return false;
        }

        LOGGER.info("[商店调试] 告示牌已附着在箱子上");

        ShopType shopType = ShopTranslationHelper.isSellPattern(firstLine) ? ShopType.SELL : ShopType.BUY;
        LOGGER.info("[商店调试] 商店类型: {}", shopType == ShopType.SELL ? "出售" : "收购");

        String itemLine = lines[1].trim();
        LOGGER.debug("[商店调试] 解析物品行: '{}'", itemLine);
        
        ItemStack tradeItem = parseItemStack(itemLine, player);
        if (tradeItem.isEmpty()) {
            LOGGER.warn("[商店调试] 无法解析物品: '{}'", itemLine);
            player.sendSystemMessage(ShopTranslationHelper.literal(
                    ShopTranslationHelper.getRawTranslation("shop.sign.invalid_item")));
            return false;
        }

        LOGGER.info("[商店调试] 解析物品成功: {}", tradeItem.getDisplayName().getString());

        String priceLine = lines[2].trim();
        LOGGER.debug("[商店调试] 解析价格行: '{}'", priceLine);
        
        String[] priceParts = priceLine.split("\\s+", 2);
        if (priceParts.length < 2) {
            LOGGER.warn("[商店调试] 价格行格式错误，缺少货币名称: '{}'", priceLine);
            player.sendSystemMessage(ShopTranslationHelper.literal(
                    ShopTranslationHelper.getRawTranslation("shop.sign.invalid_currency")));
            return false;
        }

        double price;
        try {
            price = Double.parseDouble(priceParts[0]);
            LOGGER.debug("[商店调试] 解析价格: {}", price);
        } catch (NumberFormatException e) {
            LOGGER.warn("[商店调试] 价格格式错误: '{}'", priceParts[0]);
            player.sendSystemMessage(ShopTranslationHelper.literal(
                    ShopTranslationHelper.getRawTranslation("shop.sign.invalid_price")));
            return false;
        }

        String currencyDisplayName = priceParts[1];
        LOGGER.debug("[商店调试] 货币显示名称: '{}'", currencyDisplayName);
        
        Optional<WalletType> walletTypeOpt = shopManager.findWalletTypeByDisplayName(currencyDisplayName);
        if (walletTypeOpt.isEmpty()) {
            LOGGER.warn("[商店调试] 未找到匹配的货币: '{}'", currencyDisplayName);
            player.sendSystemMessage(ShopTranslationHelper.literal(
                    ShopTranslationHelper.getRawTranslation("shop.sign.invalid_currency")));
            return false;
        }

        LOGGER.info("[商店调试] 找到货币: 货币ID={}, 显示名={}",
                walletTypeOpt.get().id(), walletTypeOpt.get().displayName().getString());

        String walletTypeId = walletTypeOpt.get().id();
        String description = lines.length > 3 ? lines[3].trim() : "";

        BlockPos signPos = signEntity.getBlockPos();
        BlockPos chestPos = getAttachedChestPos(signEntity);

        LOGGER.info("[商店调试] 准备创建商店 - 箱子位置: {}, 告示牌位置: {}", chestPos, signPos);

        ShopInstance shop = shopManager.createShop(
                player.getUUID(),
                chestPos,
                signPos,
                shopType,
                tradeItem,
                price,
                walletTypeId,
                description
        );

        if (shop == null) {
            LOGGER.error("[商店] 玩家 {} 创建商店失败 - 位置: {}, 类型: {}", 
                    player.getName().getString(), signPos, shopType);
            player.sendSystemMessage(ShopTranslationHelper.translatable("shop.created.failed"));
            return false;
        }

        LOGGER.info("[商店] 玩家 {} 成功创建{}商店 - 商店ID: {}, 位置: {}, 物品: {}, 价格: {} {}",
                player.getName().getString(),
                shopType == ShopType.SELL ? "出售" : "收购",
                shop.getShopId(),
                chestPos,
                tradeItem.getDisplayName().getString(),
                price,
                currencyDisplayName);

        waxSign(signEntity);
        LOGGER.info("[商店调试] 告示牌已自动涂蜡");

        // 立即更新告示牌文本，确保物品名等显示正确（包括客户端本地化）
        if (player.level() instanceof net.minecraft.server.level.ServerLevel serverLevel) {
            me.tuanzi.shop.util.SignUpdateHelper.updateSignForShop(shop, serverLevel);
        }

        MinecraftServer server = player.level().getServer();
        if (server != null) {
            ShopModule instance = ShopModule.getInstance(server);
            if (instance != null && instance.getDisplayManager() != null) {
                ShopDisplayManager displayManager = instance.getDisplayManager();
                if (player.level() instanceof net.minecraft.server.level.ServerLevel serverLevel) {
                    displayManager.createDisplayForShop(shop, serverLevel);
                    LOGGER.info("[商店调试] 贴牌创建商店 - 已为商店 {} 创建悬浮物品显示", shop.getShopId());
                }
            }
        }

        player.sendSystemMessage(ShopTranslationHelper.translatable("shop.created.success"));
        return true;
    }

    private void waxSign(SignBlockEntity signEntity) {
        signEntity.setWaxed(true);
        signEntity.setChanged();
        
        Level level = signEntity.getLevel();
        if (level instanceof net.minecraft.server.level.ServerLevel serverLevel) {
            BlockPos signPos = signEntity.getBlockPos();
            LOGGER.info("[商店调试] 涂蜡完成 - 位置: {}, 已标记数据为脏数据等待保存", signPos);
        }
    }

    private boolean isSignAttachedToChest(SignBlockEntity signEntity) {
        BlockPos signPos = signEntity.getBlockPos();
        Level level = signEntity.getLevel();
        if (level == null) {
            LOGGER.debug("[商店调试] 告示牌所在世界为空");
            return false;
        }

        BlockState signState = level.getBlockState(signPos);
        Direction facing;
        
        if (signState.getBlock() instanceof net.minecraft.world.level.block.StandingSignBlock) {
            int rotation = signState.getValue(net.minecraft.world.level.block.StandingSignBlock.ROTATION);
            facing = Direction.from2DDataValue(rotation / 4);
            LOGGER.debug("[商店调试] 站立告示牌，旋转值: {}, 面向: {}", rotation, facing);
        } else if (signState.getBlock() instanceof net.minecraft.world.level.block.WallSignBlock) {
            facing = signState.getValue(net.minecraft.world.level.block.WallSignBlock.FACING);
            LOGGER.debug("[商店调试] 墙面告示牌，面向: {}", facing);
        } else {
            LOGGER.warn("[商店调试] 未知的告示牌类型: {}", signState.getBlock());
            return false;
        }

        BlockPos behindPos = signPos.relative(facing.getOpposite());
        BlockState behindState = level.getBlockState(behindPos);
        
        LOGGER.debug("[商店调试] 检查告示牌后方方块 - 位置: {}, 方块: {}", behindPos, behindState.getBlock());

        boolean isChest = isChest(behindState);
        LOGGER.debug("[商店调试] 是否为箱子: {}", isChest);
        return isChest;
    }

    private BlockPos getAttachedChestPos(SignBlockEntity signEntity) {
        BlockPos signPos = signEntity.getBlockPos();
        Level level = signEntity.getLevel();
        if (level == null) return signPos;

        BlockState signState = level.getBlockState(signPos);
        Direction facing;
        
        if (signState.getBlock() instanceof net.minecraft.world.level.block.StandingSignBlock) {
            int rotation = signState.getValue(net.minecraft.world.level.block.StandingSignBlock.ROTATION);
            facing = Direction.from2DDataValue(rotation / 4);
        } else {
            facing = signState.getValue(net.minecraft.world.level.block.WallSignBlock.FACING);
        }

        return signPos.relative(facing.getOpposite());
    }

    private boolean isChest(BlockState state) {
        return state.getBlock() instanceof ChestBlock || state.getBlock() == Blocks.CHEST;
    }

    private ItemStack parseItemStack(String itemLine, ServerPlayer player) {
        if (itemLine.isEmpty()) {
            return ItemStack.EMPTY;
        }

        String language = getPreferredLanguage(player);
        Optional<ItemStack> result = ItemUtils.parseItemStackFlexible(itemLine, player.level(), language);
        if (result.isPresent()) {
            ItemStack stack = result.get();
            LOGGER.info("[商店调试] 解析物品成功: {} -> {}", itemLine, stack.getDisplayName().getString());
            return stack;
        }

        LOGGER.warn("[商店调试] 无法解析物品: '{}'", itemLine);
        return ItemStack.EMPTY;
    }

    private String getPreferredLanguage(ServerPlayer player) {
        String language = player.clientInformation().language();
        if (language == null || language.isBlank()) {
            return "zh_cn";
        }
        String normalized = language.toLowerCase(Locale.ROOT);
        return normalized.startsWith("zh") ? "zh_cn" : "en_us";
    }
}
