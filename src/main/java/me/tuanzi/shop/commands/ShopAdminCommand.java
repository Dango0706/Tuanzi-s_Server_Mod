package me.tuanzi.shop.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.arguments.DoubleArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import me.tuanzi.economy.currency.WalletType;
import me.tuanzi.shop.ShopModule;
import me.tuanzi.shop.config.ShopConfig;
import me.tuanzi.shop.display.ShopDisplayManager;
import me.tuanzi.shop.shop.ShopInstance;
import me.tuanzi.shop.shop.ShopManager;
import me.tuanzi.shop.shop.ShopType;
import me.tuanzi.shop.util.SignUpdateHelper;
import me.tuanzi.shop.utils.ShopTranslationHelper;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.permissions.Permission;
import net.minecraft.server.permissions.PermissionLevel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class ShopAdminCommand {
    private static final Logger LOGGER = LoggerFactory.getLogger(ShopModule.MOD_ID);
    private static final Map<UUID, Long> deleteConfirmations = new ConcurrentHashMap<>();
    private static final long CONFIRM_TIMEOUT_MS = 30000;

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher, CommandBuildContext registryAccess) {
        dispatcher.register(Commands.literal("shopadmin")
                .requires(source -> source.permissions().hasPermission(new Permission.HasCommandLevel(PermissionLevel.GAMEMASTERS)))
                .then(Commands.literal("delete")
                        .executes(ShopAdminCommand::deleteShop)
                )
                .then(Commands.literal("setInfinite")
                        .then(Commands.argument("value", BoolArgumentType.bool())
                                .executes(context -> setInfinite(context, BoolArgumentType.getBool(context, "value")))
                        )
                )
                .then(Commands.literal("toggleDynamic")
                        .executes(ShopAdminCommand::toggleDynamicPricing)
                )
                .then(Commands.literal("setPrice")
                        .then(Commands.argument("price", DoubleArgumentType.doubleArg(0.01))
                                .executes(context -> setPrice(context, DoubleArgumentType.getDouble(context, "price")))
                        )
                )
                .then(Commands.literal("setCurrency")
                        .then(Commands.argument("currency", StringArgumentType.word())
                                .executes(context -> setCurrency(context, StringArgumentType.getString(context, "currency")))
                        )
                )
                .then(Commands.literal("setMinPrice")
                        .then(Commands.argument("price", DoubleArgumentType.doubleArg(0))
                                .executes(context -> setMinPrice(context, DoubleArgumentType.getDouble(context, "price")))
                        )
                )
                .then(Commands.literal("setMaxPrice")
                        .then(Commands.argument("price", DoubleArgumentType.doubleArg(0.01))
                                .executes(context -> setMaxPrice(context, DoubleArgumentType.getDouble(context, "price")))
                        )
                )
                .then(Commands.literal("setTimeout")
                        .then(Commands.argument("seconds", IntegerArgumentType.integer(1, 60))
                                .executes(context -> setTimeout(context, IntegerArgumentType.getInteger(context, "seconds")))
                        )
                )
                .then(Commands.literal("info")
                        .executes(ShopAdminCommand::showShopInfo)
                )
                .then(Commands.literal("reload")
                        .executes(ShopAdminCommand::reloadConfig)
                )
                .then(Commands.literal("list")
                        .executes(ShopAdminCommand::listShops)
                )
                .then(Commands.literal("tp")
                        .then(Commands.argument("shopId", StringArgumentType.word())
                                .executes(context -> teleportToShop(context, StringArgumentType.getString(context, "shopId")))
                        )
                )
                .then(Commands.literal("setOwner")
                        .then(Commands.argument("player", EntityArgument.player())
                                .executes(context -> setOwner(context, EntityArgument.getPlayer(context, "player")))
                        )
                )
                .then(Commands.literal("help")
                        .executes(ShopAdminCommand::showHelp)
                )
        );
    }

    private static ServerPlayer getPlayer(CommandSourceStack source) {
        try {
            return source.getPlayerOrException();
        } catch (CommandSyntaxException e) {
            return null;
        }
    }

    private static int deleteShop(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();
        ServerPlayer player = getPlayer(source);
        if (player == null) {
            source.sendFailure(ShopTranslationHelper.literal("cThis command can only be used by players"));
            return 0;
        }

        ShopManager shopManager = ShopManager.getInstance(player.level().getServer());
        if (shopManager == null) {
            source.sendFailure(ShopTranslationHelper.translatable("shop.not_found"));
            return 0;
        }

        Optional<ShopInstance> shopOpt = shopManager.getShopPlayerLookingAt(player);
        if (shopOpt.isEmpty()) {
            source.sendFailure(ShopTranslationHelper.translatable("shop.not_looking"));
            return 0;
        }

        ShopInstance shop = shopOpt.get();
        UUID shopId = shop.getShopId();
        UUID playerId = player.getUUID();

        Long lastConfirmTime = deleteConfirmations.get(playerId);
        long currentTime = System.currentTimeMillis();

        if (lastConfirmTime == null || (currentTime - lastConfirmTime) > CONFIRM_TIMEOUT_MS) {
            deleteConfirmations.put(playerId, currentTime);
            source.sendSuccess(() -> ShopTranslationHelper.translatable("shop.deleted.confirm"), false);
            return 0;
        }

        shopManager.deleteShop(shopId);
        deleteConfirmations.remove(playerId);

        ShopModule moduleInstance = ShopModule.getInstance(player.level().getServer());
        if (moduleInstance != null && moduleInstance.getDisplayManager() != null) {
            if (player.level() instanceof net.minecraft.server.level.ServerLevel adminServerLevel) {
                moduleInstance.getDisplayManager().removeDisplayForShop(shopId, adminServerLevel);
                LOGGER.info("[商店管理] 管理员 {} 删除了商店 {}，并移除了悬浮展示实体",
                        player.getName().getString(), shopId);
            }
        }

        LOGGER.info("[商店管理] 管理员 {} 删除了商店 {}，位置 {}",
                player.getName().getString(), shopId, shop.getShopPos());
        source.sendSuccess(() -> ShopTranslationHelper.translatable("shop.deleted.success"), false);
        return 1;
    }

    private static int setInfinite(CommandContext<CommandSourceStack> context, boolean value) {
        CommandSourceStack source = context.getSource();
        ServerPlayer player = getPlayer(source);
        if (player == null) {
            source.sendFailure(ShopTranslationHelper.literal("cThis command can only be used by players"));
            return 0;
        }

        ShopManager shopManager = ShopManager.getInstance(player.level().getServer());
        if (shopManager == null) {
            source.sendFailure(ShopTranslationHelper.translatable("shop.not_found"));
            return 0;
        }

        Optional<ShopInstance> shopOpt = shopManager.getShopPlayerLookingAt(player);
        if (shopOpt.isEmpty()) {
            source.sendFailure(ShopTranslationHelper.translatable("shop.not_looking"));
            return 0;
        }

        ShopInstance shop = shopOpt.get();
        shop.setInfinite(value);
        shopManager.markDirty();

        LOGGER.info("[商店管理] 管理员 {} 设置商店 {} 的无限模式为: {}",
                player.getName().getString(), shop.getShopId(), value);

        boolean finalValue = value;
        source.sendSuccess(() -> ShopTranslationHelper.translatable("admin.shop.set_infinite", finalValue ? "aYes" : "cNo"), false);
        return 1;
    }

    private static int toggleDynamicPricing(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();
        ServerPlayer player = getPlayer(source);
        if (player == null) {
            source.sendFailure(ShopTranslationHelper.literal("cThis command can only be used by players"));
            return 0;
        }

        ShopManager shopManager = ShopManager.getInstance(player.level().getServer());
        if (shopManager == null) {
            source.sendFailure(ShopTranslationHelper.translatable("shop.not_found"));
            return 0;
        }

        Optional<ShopInstance> shopOpt = shopManager.getShopPlayerLookingAt(player);
        if (shopOpt.isEmpty()) {
            source.sendFailure(ShopTranslationHelper.translatable("shop.not_looking"));
            return 0;
        }

        ShopInstance shop = shopOpt.get();
        shop.setDynamicPricing(!shop.isDynamicPricing());
        shopManager.markDirty();

        LOGGER.info("[商店管理] 管理员 {} 切换商店 {} 的动态定价状态为: {}",
                player.getName().getString(), shop.getShopId(), shop.isDynamicPricing());

        if (player.level() instanceof net.minecraft.server.level.ServerLevel serverLevel) {
            SignUpdateHelper.updateSignForShop(shop, serverLevel);
            LOGGER.info("[商店管理] 告示牌已同步更新（动态定价切换） - 商店ID: {}", shop.getShopId());
        }

        boolean newValue = shop.isDynamicPricing();
        source.sendSuccess(() -> ShopTranslationHelper.translatable("admin.shop.dynamic_toggled", newValue ? "aEnabled" : "cDisabled"), false);
        return 1;
    }

    private static int setPrice(CommandContext<CommandSourceStack> context, double price) {
        CommandSourceStack source = context.getSource();
        ServerPlayer player = getPlayer(source);
        if (player == null) {
            source.sendFailure(ShopTranslationHelper.literal("cThis command can only be used by players"));
            return 0;
        }

        ShopManager shopManager = ShopManager.getInstance(player.level().getServer());
        if (shopManager == null) {
            source.sendFailure(ShopTranslationHelper.translatable("shop.not_found"));
            return 0;
        }

        Optional<ShopInstance> shopOpt = shopManager.getShopPlayerLookingAt(player);
        if (shopOpt.isEmpty()) {
            source.sendFailure(ShopTranslationHelper.translatable("shop.not_looking"));
            return 0;
        }

        ShopInstance shop = shopOpt.get();
        shop.setBasePrice(price);
        shop.setCurrentPrice(price);
        shopManager.markDirty();

        LOGGER.info("[商店管理] 管理员 {} 设置商店 {} 的价格为: {}",
                player.getName().getString(), shop.getShopId(), price);

        if (player.level() instanceof net.minecraft.server.level.ServerLevel serverLevel) {
            SignUpdateHelper.updateSignForShop(shop, serverLevel);
            LOGGER.info("[商店管理] 告示牌已同步更新 - 商店ID: {}", shop.getShopId());
        }

        source.sendSuccess(() -> ShopTranslationHelper.translatable("admin.shop.price_set", price), false);
        return 1;
    }

    private static int setCurrency(CommandContext<CommandSourceStack> context, String currencyId) {
        CommandSourceStack source = context.getSource();
        ServerPlayer player = getPlayer(source);
        if (player == null) {
            source.sendFailure(ShopTranslationHelper.literal("cThis command can only be used by players"));
            return 0;
        }

        ShopManager shopManager = ShopManager.getInstance(player.level().getServer());
        if (shopManager == null) {
            source.sendFailure(ShopTranslationHelper.translatable("shop.not_found"));
            return 0;
        }

        Optional<ShopInstance> shopOpt = shopManager.getShopPlayerLookingAt(player);
        if (shopOpt.isEmpty()) {
            source.sendFailure(ShopTranslationHelper.translatable("shop.not_looking"));
            return 0;
        }

        Optional<WalletType> walletTypeOpt = shopManager.findWalletTypeById(currencyId);
        if (walletTypeOpt.isEmpty()) {
            source.sendFailure(ShopTranslationHelper.colored("cCurrency ID not found: " + currencyId));
            return 0;
        }

        ShopInstance shop = shopOpt.get();
        shop.setWalletTypeId(currencyId);
        shopManager.markDirty();

        String currencyName = walletTypeOpt.get().displayName().getString();
        LOGGER.info("[商店管理] 管理员 {} 设置商店 {} 的货币为: {}",
                player.getName().getString(), shop.getShopId(), currencyName);

        if (player.level() instanceof net.minecraft.server.level.ServerLevel serverLevel) {
            SignUpdateHelper.updateSignForShop(shop, serverLevel);
            LOGGER.info("[商店管理] 告示牌已同步更新 - 商店ID: {}", shop.getShopId());
        }

        source.sendSuccess(() -> ShopTranslationHelper.translatable("admin.shop.currency_set", currencyName), false);
        return 1;
    }

    private static int setMinPrice(CommandContext<CommandSourceStack> context, double price) {
        CommandSourceStack source = context.getSource();
        ServerPlayer player = getPlayer(source);
        if (player == null) {
            source.sendFailure(ShopTranslationHelper.literal("cThis command can only be used by players"));
            return 0;
        }

        ShopManager shopManager = ShopManager.getInstance(player.level().getServer());
        if (shopManager == null) {
            source.sendFailure(ShopTranslationHelper.translatable("shop.not_found"));
            return 0;
        }

        Optional<ShopInstance> shopOpt = shopManager.getShopPlayerLookingAt(player);
        if (shopOpt.isEmpty()) {
            source.sendFailure(ShopTranslationHelper.translatable("shop.not_looking"));
            return 0;
        }

        ShopInstance shop = shopOpt.get();
        shop.setMinPrice(price);
        shopManager.markDirty();

        source.sendSuccess(() -> ShopTranslationHelper.colored("aSet shop min price: e" + price), false);
        return 1;
    }

    private static int setMaxPrice(CommandContext<CommandSourceStack> context, double price) {
        CommandSourceStack source = context.getSource();
        ServerPlayer player = getPlayer(source);
        if (player == null) {
            source.sendFailure(ShopTranslationHelper.literal("cThis command can only be used by players"));
            return 0;
        }

        ShopManager shopManager = ShopManager.getInstance(player.level().getServer());
        if (shopManager == null) {
            source.sendFailure(ShopTranslationHelper.translatable("shop.not_found"));
            return 0;
        }

        Optional<ShopInstance> shopOpt = shopManager.getShopPlayerLookingAt(player);
        if (shopOpt.isEmpty()) {
            source.sendFailure(ShopTranslationHelper.translatable("shop.not_looking"));
            return 0;
        }

        ShopInstance shop = shopOpt.get();
        shop.setMaxPrice(price);
        shopManager.markDirty();

        source.sendSuccess(() -> ShopTranslationHelper.colored("aSet shop max price: e" + price), false);
        return 1;
    }

    private static int setTimeout(CommandContext<CommandSourceStack> context, int seconds) {
        CommandSourceStack source = context.getSource();
        
        ShopConfig config = ShopModule.getConfig();
        if (config == null) {
            source.sendFailure(ShopTranslationHelper.colored("cConfig not initialized"));
            return 0;
        }

        config.setInputTimeoutSeconds(seconds);
        source.sendSuccess(() -> ShopTranslationHelper.translatable("admin.shop.timeout_set", seconds), false);
        return 1;
    }

    private static int showShopInfo(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();
        ServerPlayer player = getPlayer(source);
        if (player == null) {
            source.sendFailure(ShopTranslationHelper.literal("cThis command can only be used by players"));
            return 0;
        }

        ShopManager shopManager = ShopManager.getInstance(player.level().getServer());
        if (shopManager == null) {
            source.sendFailure(ShopTranslationHelper.translatable("shop.not_found"));
            return 0;
        }

        Optional<ShopInstance> shopOpt = shopManager.getShopPlayerLookingAt(player);
        if (shopOpt.isEmpty()) {
            source.sendFailure(ShopTranslationHelper.translatable("shop.not_looking"));
            return 0;
        }

        ShopInstance shop = shopOpt.get();
        sendDetailedShopInfo(player, shop, shopManager);
        return 1;
    }

    private static void sendDetailedShopInfo(ServerPlayer player, ShopInstance shop, ShopManager shopManager) {
        player.sendSystemMessage(ShopTranslationHelper.translatable("admin.shop.info.title"));
        player.sendSystemMessage(ShopTranslationHelper.translatable("admin.shop.info.id", shop.getShopId().toString().substring(0, 8)));
        player.sendSystemMessage(ShopTranslationHelper.translatable("admin.shop.info.owner", shop.getOwnerId().toString().substring(0, 8)));
        player.sendSystemMessage(ShopTranslationHelper.translatable("admin.shop.info.type", 
                shop.getShopType() == ShopType.SELL ? "bSell Shop" : "bBuy Shop"));
        player.sendSystemMessage(ShopTranslationHelper.translatable("admin.shop.info.item", 
                shop.getTradeItem().getDisplayName().getString()));
        
        String currencyName = shopManager.getCurrencyDisplayName(shop.getWalletTypeId());
        player.sendSystemMessage(ShopTranslationHelper.translatable("admin.shop.info.price", 
                shop.getCurrentPrice(), currencyName));
        player.sendSystemMessage(ShopTranslationHelper.translatable("admin.shop.info.currency", currencyName));
        player.sendSystemMessage(ShopTranslationHelper.translatable("admin.shop.info.stock", 
                shop.isInfinite() ? -1 : 0));
        player.sendSystemMessage(ShopTranslationHelper.translatable("admin.shop.info.stats", 
                shop.getTotalSold(), shop.getTotalBought()));
        player.sendSystemMessage(ShopTranslationHelper.translatable("admin.shop.info.infinite", 
                shop.isInfinite() ? "aYes" : "cNo"));
        player.sendSystemMessage(ShopTranslationHelper.translatable("admin.shop.info.dynamic", 
                shop.isDynamicPricing() ? "aEnabled" : "cDisabled"));
    }

    private static int reloadConfig(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();
        
        ShopConfig config = ShopModule.getConfig();
        if (config == null) {
            source.sendFailure(ShopTranslationHelper.colored("cConfig not initialized"));
            return 0;
        }

        config.load();
        source.sendSuccess(() -> ShopTranslationHelper.colored("aShop config reloaded"), false);
        return 1;
    }

    public static void cleanupExpiredConfirmations() {
        long currentTime = System.currentTimeMillis();
        deleteConfirmations.entrySet().removeIf(entry -> 
                (currentTime - entry.getValue()) > CONFIRM_TIMEOUT_MS);
    }

    private static int listShops(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();
        ServerPlayer player = getPlayer(source);
        if (player == null) {
            source.sendFailure(ShopTranslationHelper.literal("cThis command can only be used by players"));
            return 0;
        }

        ShopManager shopManager = ShopManager.getInstance(player.level().getServer());
        if (shopManager == null) {
            source.sendFailure(ShopTranslationHelper.translatable("shop.not_found"));
            return 0;
        }

        Collection<ShopInstance> shopsCollection = shopManager.getAllShops();
        if (shopsCollection.isEmpty()) {
            source.sendSuccess(() -> ShopTranslationHelper.colored("eNo shops found"), false);
            return 1;
        }

        List<ShopInstance> shops = new ArrayList<>(shopsCollection);
        source.sendSuccess(() -> ShopTranslationHelper.colored("a=== Shop List (" + shops.size() + ") ==="), false);
        for (int i = 0; i < Math.min(shops.size(), 10); i++) {
            ShopInstance shop = shops.get(i);
            String shopId = shop.getShopId().toString().substring(0, 8);
            String itemName = shop.getTradeItem().getDisplayName().getString();
            String type = shop.getShopType() == ShopType.SELL ? "Sell" : "Buy";
            BlockPos pos = shop.getShopPos();
            source.sendSuccess(() -> ShopTranslationHelper.colored(
                    String.format("7[%s] f%s 7(%s) at x:%d y:%d z:%d", 
                            shopId, itemName, type, pos.getX(), pos.getY(), pos.getZ())), false);
        }
        
        if (shops.size() > 10) {
            int remaining = shops.size() - 10;
            source.sendSuccess(() -> ShopTranslationHelper.colored("7... and " + remaining + " more shops"), false);
        }
        
        return 1;
    }

    private static int teleportToShop(CommandContext<CommandSourceStack> context, String shopIdStr) {
        CommandSourceStack source = context.getSource();
        ServerPlayer player = getPlayer(source);
        if (player == null) {
            source.sendFailure(ShopTranslationHelper.literal("cThis command can only be used by players"));
            return 0;
        }

        ShopManager shopManager = ShopManager.getInstance(player.level().getServer());
        if (shopManager == null) {
            source.sendFailure(ShopTranslationHelper.translatable("shop.not_found"));
            return 0;
        }

        ShopInstance targetShop = null;
        for (ShopInstance shop : shopManager.getAllShops()) {
            if (shop.getShopId().toString().startsWith(shopIdStr)) {
                targetShop = shop;
                break;
            }
        }

        if (targetShop == null) {
            source.sendFailure(ShopTranslationHelper.colored("cShop not found with ID: " + shopIdStr));
            return 0;
        }

        BlockPos pos = targetShop.getShopPos();
        player.teleportTo(pos.getX() + 0.5, pos.getY() + 1, pos.getZ() + 0.5);
        source.sendSuccess(() -> ShopTranslationHelper.colored("aTeleported to shop at x:" + pos.getX() + " y:" + pos.getY() + " z:" + pos.getZ()), false);
        return 1;
    }

    private static int setOwner(CommandContext<CommandSourceStack> context, ServerPlayer newOwner) {
        CommandSourceStack source = context.getSource();
        ServerPlayer player = getPlayer(source);
        if (player == null) {
            source.sendFailure(ShopTranslationHelper.literal("cThis command can only be used by players"));
            return 0;
        }

        ShopManager shopManager = ShopManager.getInstance(player.level().getServer());
        if (shopManager == null) {
            source.sendFailure(ShopTranslationHelper.translatable("shop.not_found"));
            return 0;
        }

        Optional<ShopInstance> shopOpt = shopManager.getShopPlayerLookingAt(player);
        if (shopOpt.isEmpty()) {
            source.sendFailure(ShopTranslationHelper.translatable("shop.not_looking"));
            return 0;
        }

        ShopInstance shop = shopOpt.get();
        UUID oldOwner = shop.getOwnerId();
        
        source.sendSuccess(() -> ShopTranslationHelper.colored(
                "cNote: Owner change is not supported. Current owner: " + oldOwner), false);
        source.sendSuccess(() -> ShopTranslationHelper.colored(
                "7To transfer ownership, delete and recreate the shop."), false);
        
        return 1;
    }

    private static int showHelp(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();
        
        source.sendSuccess(() -> ShopTranslationHelper.colored("a=== ShopAdmin Commands ==="), false);
        source.sendSuccess(() -> ShopTranslationHelper.colored("e/shopadmin delete - Delete shop (look at it)"), false);
        source.sendSuccess(() -> ShopTranslationHelper.colored("e/shopadmin setInfinite <true/false> - Set infinite mode"), false);
        source.sendSuccess(() -> ShopTranslationHelper.colored("e/shopadmin toggleDynamic - Toggle dynamic pricing"), false);
        source.sendSuccess(() -> ShopTranslationHelper.colored("e/shopadmin setPrice <price> - Set shop price"), false);
        source.sendSuccess(() -> ShopTranslationHelper.colored("e/shopadmin setCurrency <id> - Set currency type"), false);
        source.sendSuccess(() -> ShopTranslationHelper.colored("e/shopadmin setMinPrice <price> - Set minimum price"), false);
        source.sendSuccess(() -> ShopTranslationHelper.colored("e/shopadmin setMaxPrice <price> - Set maximum price"), false);
        source.sendSuccess(() -> ShopTranslationHelper.colored("e/shopadmin setTimeout <seconds> - Set input timeout"), false);
        source.sendSuccess(() -> ShopTranslationHelper.colored("e/shopadmin info - Show shop info"), false);
        source.sendSuccess(() -> ShopTranslationHelper.colored("e/shopadmin reload - Reload config"), false);
        source.sendSuccess(() -> ShopTranslationHelper.colored("e/shopadmin list - List all shops"), false);
        source.sendSuccess(() -> ShopTranslationHelper.colored("e/shopadmin tp <shopId> - Teleport to shop"), false);
        source.sendSuccess(() -> ShopTranslationHelper.colored("e/shopadmin setOwner <player> - Set shop owner"), false);
        
        return 1;
    }
}
