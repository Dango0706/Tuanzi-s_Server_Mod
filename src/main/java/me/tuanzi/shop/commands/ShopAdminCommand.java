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
import me.tuanzi.shop.events.ChatInputHandler;
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
                .then(Commands.literal("setHalfLife")
                        .then(Commands.argument("value", DoubleArgumentType.doubleArg(1.0))
                                .executes(context -> setHalfLife(context, DoubleArgumentType.getDouble(context, "value")))
                        )
                )
                .then(Commands.literal("setSystemStock")
                        .then(Commands.argument("value", DoubleArgumentType.doubleArg(0.0))
                                .executes(context -> setSystemStock(context, DoubleArgumentType.getDouble(context, "value")))
                        )
                )
                .then(Commands.literal("adjustSystemStock")
                        .then(Commands.argument("percentage", DoubleArgumentType.doubleArg(0.0))
                                .executes(context -> adjustSystemStock(context, DoubleArgumentType.getDouble(context, "percentage")))
                        )
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
            source.sendFailure(ShopTranslationHelper.translatable(source, "auth.command.player_only"));
            return 0;
        }

        ShopManager shopManager = ShopManager.getInstance(player.level().getServer());
        if (shopManager == null) {
            source.sendFailure(ShopTranslationHelper.translatable(source, "shop.not_found"));
            return 0;
        }

        Optional<ShopInstance> shopOpt = shopManager.getShopPlayerLookingAt(player);
        if (shopOpt.isEmpty()) {
            source.sendFailure(ShopTranslationHelper.translatable(source, "shop.not_looking"));
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

        // 1. 先移除世界中的悬浮展示实体 (必须在删除内存/存档数据前)
        ShopModule moduleInstance = ShopModule.getInstance(player.level().getServer());
        if (moduleInstance != null && moduleInstance.getDisplayManager() != null) {
            if (player.level() instanceof net.minecraft.server.level.ServerLevel adminServerLevel) {
                moduleInstance.getDisplayManager().removeDisplayForShop(shopId, adminServerLevel);
                LOGGER.info("[商店管理] 管理员 {} 正在删除商店 {}，已触发悬浮展示实体清理",
                        player.getName().getString(), shopId);
            }
        }

        // 2. 然后删除商店数据记录
        shopManager.deleteShop(shopId);
        deleteConfirmations.remove(playerId);

        LOGGER.info("[商店管理] 管理员 {} 已成功删除商店 {}，位置 {}",
                player.getName().getString(), shopId, shop.getShopPos());
        source.sendSuccess(() -> ShopTranslationHelper.translatable("shop.deleted.success"), false);
        return 1;
    }

    private static int setInfinite(CommandContext<CommandSourceStack> context, boolean value) {
        CommandSourceStack source = context.getSource();
        ServerPlayer player = getPlayer(source);
        if (player == null) {
            source.sendFailure(ShopTranslationHelper.translatable(source, "auth.command.player_only"));
            return 0;
        }

        ShopManager shopManager = ShopManager.getInstance(player.level().getServer());
        if (shopManager == null) {
            source.sendFailure(ShopTranslationHelper.translatable(source, "shop.not_found"));
            return 0;
        }

        Optional<ShopInstance> shopOpt = shopManager.getShopPlayerLookingAt(player);
        if (shopOpt.isEmpty()) {
            source.sendFailure(ShopTranslationHelper.translatable(source, "shop.not_looking"));
            return 0;
        }

        ShopInstance shop = shopOpt.get();
        shop.setInfinite(value);
        shopManager.markDirty();

        LOGGER.info("[商店管理] 管理员 {} 设置商店 {} 的无限模式为: {}",
                player.getName().getString(), shop.getShopId(), value);

        source.sendSuccess(() -> ShopTranslationHelper.translatable("admin.shop.set_infinite", 
                value ? ShopTranslationHelper.getRawTranslation("common.yes") : ShopTranslationHelper.getRawTranslation("common.no")), false);
        return 1;
    }

    private static int toggleDynamicPricing(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();
        ServerPlayer player = getPlayer(source);
        if (player == null) {
            source.sendFailure(ShopTranslationHelper.translatable(source, "auth.command.player_only"));
            return 0;
        }

        ShopManager shopManager = ShopManager.getInstance(player.level().getServer());
        if (shopManager == null) {
            source.sendFailure(ShopTranslationHelper.translatable(source, "shop.not_found"));
            return 0;
        }

        Optional<ShopInstance> shopOpt = shopManager.getShopPlayerLookingAt(player);
        if (shopOpt.isEmpty()) {
            source.sendFailure(ShopTranslationHelper.translatable(source, "shop.not_looking"));
            return 0;
        }

        ShopInstance shop = shopOpt.get();
        ChatInputHandler chatHandler = ShopModule.getInstance(player.level().getServer()).getChatInputHandler();

        // 开启动态定价前进行变量检查
        if (!shop.isDynamicPricing()) {
            // 如果变量为初始值 -1.0，则引导设置
            if (shop.getMinPrice() < 0 || shop.getMaxPrice() < 0 || shop.getHalfLifeConstant() < 0) {
                if (chatHandler != null) {
                    chatHandler.startDynamicSetup(player, shop);
                    return 1;
                }
            }
        }

        shop.setDynamicPricing(!shop.isDynamicPricing());
        
        // 实时更新价格
        shop.setCurrentPrice(me.tuanzi.shop.pricing.DynamicPricing.calculatePrice(shop));
        shopManager.markDirty();

        if (player.level() instanceof net.minecraft.server.level.ServerLevel serverLevel) {
            SignUpdateHelper.updateSignForShop(shop, serverLevel);
        }

        boolean newValue = shop.isDynamicPricing();
        source.sendSuccess(() -> ShopTranslationHelper.translatable("admin.shop.dynamic_toggled", 
                newValue ? ShopTranslationHelper.getRawTranslation("common.enabled") : ShopTranslationHelper.getRawTranslation("common.disabled")), false);
        return 1;
    }

    private static int setHalfLife(CommandContext<CommandSourceStack> context, double value) {
        CommandSourceStack source = context.getSource();
        ServerPlayer player = getPlayer(source);
        if (player == null) return 0;

        ShopManager shopManager = ShopManager.getInstance(player.level().getServer());
        Optional<ShopInstance> shopOpt = shopManager.getShopPlayerLookingAt(player);
        if (shopOpt.isEmpty()) return 0;

        ShopInstance shop = shopOpt.get();
        shop.setHalfLifeConstant(value);
        
        // 实时更新价格和告示牌
        shop.setCurrentPrice(me.tuanzi.shop.pricing.DynamicPricing.calculatePrice(shop));
        shopManager.markDirty();
        if (player.level() instanceof net.minecraft.server.level.ServerLevel serverLevel) {
            SignUpdateHelper.updateSignForShop(shop, serverLevel);
        }

        source.sendSuccess(() -> ShopTranslationHelper.translatable("admin.shop.halflife_set", value), false);
        return 1;
    }

    private static int setSystemStock(CommandContext<CommandSourceStack> context, double value) {
        CommandSourceStack source = context.getSource();
        ServerPlayer player = getPlayer(source);
        if (player == null) return 0;

        ShopManager shopManager = ShopManager.getInstance(player.level().getServer());
        Optional<ShopInstance> shopOpt = shopManager.getShopPlayerLookingAt(player);
        if (shopOpt.isEmpty()) return 0;

        ShopInstance shop = shopOpt.get();
        shop.setSystemStock(value);
        
        // 实时更新价格和告示牌
        shop.setCurrentPrice(me.tuanzi.shop.pricing.DynamicPricing.calculatePrice(shop));
        shopManager.markDirty();
        if (player.level() instanceof net.minecraft.server.level.ServerLevel serverLevel) {
            SignUpdateHelper.updateSignForShop(shop, serverLevel);
        }

        source.sendSuccess(() -> ShopTranslationHelper.translatable("admin.shop.system_stock_set", value), false);
        return 1;
    }

    private static int adjustSystemStock(CommandContext<CommandSourceStack> context, double percentage) {
        CommandSourceStack source = context.getSource();
        ServerPlayer player = getPlayer(source);
        if (player == null) return 0;

        ShopManager shopManager = ShopManager.getInstance(player.level().getServer());
        Optional<ShopInstance> shopOpt = shopManager.getShopPlayerLookingAt(player);
        if (shopOpt.isEmpty()) {
            source.sendFailure(ShopTranslationHelper.translatable("shop.not_looking"));
            return 0;
        }

        ShopInstance shop = shopOpt.get();
        shop.setDecayRate(percentage);
        shopManager.markDirty();

        source.sendSuccess(() -> ShopTranslationHelper.colored(
                String.format("§a已将该商店的每日系统库存衰减率设置为: §e%.1f%%", percentage * 100)), false);
        return 1;
    }

    private static int setPrice(CommandContext<CommandSourceStack> context, double price) {
        CommandSourceStack source = context.getSource();
        ServerPlayer player = getPlayer(source);
        if (player == null) {
            source.sendFailure(ShopTranslationHelper.translatable(source, "auth.command.player_only"));
            return 0;
        }

        ShopManager shopManager = ShopManager.getInstance(player.level().getServer());
        if (shopManager == null) {
            source.sendFailure(ShopTranslationHelper.translatable(source, "shop.not_found"));
            return 0;
        }

        Optional<ShopInstance> shopOpt = shopManager.getShopPlayerLookingAt(player);
        if (shopOpt.isEmpty()) {
            source.sendFailure(ShopTranslationHelper.translatable(source, "shop.not_looking"));
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
        }

        source.sendSuccess(() -> ShopTranslationHelper.translatable("admin.shop.price_set", price), false);
        return 1;
    }

    private static int setCurrency(CommandContext<CommandSourceStack> context, String currencyId) {
        CommandSourceStack source = context.getSource();
        ServerPlayer player = getPlayer(source);
        if (player == null) {
            source.sendFailure(ShopTranslationHelper.translatable(source, "auth.command.player_only"));
            return 0;
        }

        ShopManager shopManager = ShopManager.getInstance(player.level().getServer());
        if (shopManager == null) {
            source.sendFailure(ShopTranslationHelper.translatable(source, "shop.not_found"));
            return 0;
        }

        Optional<ShopInstance> shopOpt = shopManager.getShopPlayerLookingAt(player);
        if (shopOpt.isEmpty()) {
            source.sendFailure(ShopTranslationHelper.translatable(source, "shop.not_looking"));
            return 0;
        }

        Optional<WalletType> walletTypeOpt = shopManager.findWalletTypeById(currencyId);
        if (walletTypeOpt.isEmpty()) {
            source.sendFailure(ShopTranslationHelper.colored("§cCurrency ID not found: " + currencyId));
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
        }

        source.sendSuccess(() -> ShopTranslationHelper.translatable("admin.shop.currency_set", currencyName), false);
        return 1;
    }

    private static int setMinPrice(CommandContext<CommandSourceStack> context, double price) {
        CommandSourceStack source = context.getSource();
        ServerPlayer player = getPlayer(source);
        if (player == null) {
            source.sendFailure(ShopTranslationHelper.translatable(source, "auth.command.player_only"));
            return 0;
        }

        ShopManager shopManager = ShopManager.getInstance(player.level().getServer());
        if (shopManager == null) {
            source.sendFailure(ShopTranslationHelper.translatable(source, "shop.not_found"));
            return 0;
        }

        Optional<ShopInstance> shopOpt = shopManager.getShopPlayerLookingAt(player);
        if (shopOpt.isEmpty()) {
            source.sendFailure(ShopTranslationHelper.translatable(source, "shop.not_looking"));
            return 0;
        }

        ShopInstance shop = shopOpt.get();
        shop.setMinPrice(price);
        
        // 实时更新价格和告示牌
        shop.setCurrentPrice(me.tuanzi.shop.pricing.DynamicPricing.calculatePrice(shop));
        shopManager.markDirty();
        if (player.level() instanceof net.minecraft.server.level.ServerLevel serverLevel) {
            SignUpdateHelper.updateSignForShop(shop, serverLevel);
        }

        source.sendSuccess(() -> ShopTranslationHelper.translatable("admin.shop.min_price_set", price), false);
        return 1;
    }

    private static int setMaxPrice(CommandContext<CommandSourceStack> context, double price) {
        CommandSourceStack source = context.getSource();
        ServerPlayer player = getPlayer(source);
        if (player == null) {
            source.sendFailure(ShopTranslationHelper.translatable(source, "auth.command.player_only"));
            return 0;
        }

        ShopManager shopManager = ShopManager.getInstance(player.level().getServer());
        if (shopManager == null) {
            source.sendFailure(ShopTranslationHelper.translatable(source, "shop.not_found"));
            return 0;
        }

        Optional<ShopInstance> shopOpt = shopManager.getShopPlayerLookingAt(player);
        if (shopOpt.isEmpty()) {
            source.sendFailure(ShopTranslationHelper.translatable(source, "shop.not_looking"));
            return 0;
        }

        ShopInstance shop = shopOpt.get();
        shop.setMaxPrice(price);
        
        // 实时更新价格和告示牌
        shop.setCurrentPrice(me.tuanzi.shop.pricing.DynamicPricing.calculatePrice(shop));
        shopManager.markDirty();
        if (player.level() instanceof net.minecraft.server.level.ServerLevel serverLevel) {
            SignUpdateHelper.updateSignForShop(shop, serverLevel);
        }

        source.sendSuccess(() -> ShopTranslationHelper.translatable("admin.shop.max_price_set", price), false);
        return 1;
    }

    private static int setTimeout(CommandContext<CommandSourceStack> context, int seconds) {
        CommandSourceStack source = context.getSource();
        
        ShopConfig config = ShopModule.getConfig();
        if (config == null) {
            source.sendFailure(ShopTranslationHelper.colored("§cConfig not initialized"));
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
            source.sendFailure(ShopTranslationHelper.translatable(source, "auth.command.player_only"));
            return 0;
        }

        ShopManager shopManager = ShopManager.getInstance(player.level().getServer());
        if (shopManager == null) {
            source.sendFailure(ShopTranslationHelper.translatable(source, "shop.not_found"));
            return 0;
        }

        Optional<ShopInstance> shopOpt = shopManager.getShopPlayerLookingAt(player);
        if (shopOpt.isEmpty()) {
            source.sendFailure(ShopTranslationHelper.translatable(source, "shop.not_looking"));
            return 0;
        }

        ShopInstance shop = shopOpt.get();
        sendDetailedShopInfo(player, shop, shopManager);
        return 1;
    }

    private static void sendDetailedShopInfo(ServerPlayer player, ShopInstance shop, ShopManager shopManager) {
        String currencyName = shopManager.getCurrencyDisplayName(shop.getWalletTypeId());
        BlockPos pos = shop.getShopPos();

        player.sendSystemMessage(ShopTranslationHelper.colored("§e================ [商店详细信息] ================"));
        player.sendSystemMessage(ShopTranslationHelper.colored("§bID: §f" + shop.getShopId().toString().substring(0, 8)));
        player.sendSystemMessage(ShopTranslationHelper.colored("§b位置: §f" + String.format("x:%d y:%d z:%d", pos.getX(), pos.getY(), pos.getZ())));
        player.sendSystemMessage(ShopTranslationHelper.colored("§b所有者: §f" + shop.getOwnerId().toString().substring(0, 8)));
        player.sendSystemMessage(ShopTranslationHelper.colored("§b类型: §f" + (shop.getShopType() == ShopType.SELL ? "§e出售 (玩家买)" : "§a收购 (玩家卖)")));
        player.sendSystemMessage(ShopTranslationHelper.colored("§b物品: §f" + shop.getTradeItem().getDisplayName().getString()));
        player.sendSystemMessage(ShopTranslationHelper.colored("§b基础单价: §e" + String.format("%.2f %s", shop.getBasePrice(), currencyName)));
        player.sendSystemMessage(ShopTranslationHelper.colored("§b当前成交价: §6" + String.format("%.2f %s", shop.getCurrentPrice(), currencyName)));

        // 动态定价相关
        player.sendSystemMessage(ShopTranslationHelper.colored("§b动态定价: " + (shop.isDynamicPricing() ? "§a开启" : "§7关闭")));
        if (shop.isDynamicPricing()) {
            player.sendSystemMessage(ShopTranslationHelper.colored("  §7- 最低价(x): §f" + String.format("%.2f", shop.getMinPrice())));
            player.sendSystemMessage(ShopTranslationHelper.colored("  §7- 最高价(y): §f" + String.format("%.2f", shop.getMaxPrice())));
            player.sendSystemMessage(ShopTranslationHelper.colored("  §7- 半衰常数(K): §f" + String.format("%.2f", shop.getHalfLifeConstant())));
            player.sendSystemMessage(ShopTranslationHelper.colored("  §7- 系统压力(S): §f" + String.format("%.2f", shop.getSystemStock())));
            player.sendSystemMessage(ShopTranslationHelper.colored("  §7- 每日衰减率: §f" + String.format("%.1f%%", shop.getDecayRate() * 100)));
        }

        // 统计数据
        player.sendSystemMessage(ShopTranslationHelper.colored("§b累计交易统计:"));
        player.sendSystemMessage(ShopTranslationHelper.colored("  §7- 累计售出(玩家买入): §f" + shop.getTotalSold()));
        player.sendSystemMessage(ShopTranslationHelper.colored("  §7- 累计收购(玩家卖出): §f" + shop.getTotalBought()));

        // 库存/模式状态
        player.sendSystemMessage(ShopTranslationHelper.colored("§b商店模式: " + (shop.isInfinite() ? "§d无限库存" : "§f标准")));
        if (!shop.isInfinite()) {
            ShopModule module = ShopModule.getInstance(player.level().getServer());
            if (module != null && module.getInteractionHandler() != null) {
                int stock = module.getInteractionHandler().getShopStock(shop);
                int capacity = module.getInteractionHandler().getChestAvailableSpace(shop);
                if (shop.getShopType() == ShopType.SELL) {
                    player.sendSystemMessage(ShopTranslationHelper.colored("  §7- 当前库存: §f" + stock));
                } else {
                    player.sendSystemMessage(ShopTranslationHelper.colored("  §7- 剩余容量: §f" + capacity));
                }
            }
        }
        
        player.sendSystemMessage(ShopTranslationHelper.colored("§e============================================"));
    }

    private static int reloadConfig(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();
        
        ShopConfig config = ShopModule.getConfig();
        if (config == null) {
            source.sendFailure(ShopTranslationHelper.colored("§cConfig not initialized"));
            return 0;
        }

        config.load();
        source.sendSuccess(() -> ShopTranslationHelper.translatable("admin.shop.config_reloaded"), false);
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
            source.sendFailure(ShopTranslationHelper.translatable("auth.command.player_only"));
            return 0;
        }

        ShopManager shopManager = ShopManager.getInstance(player.level().getServer());
        if (shopManager == null) {
            source.sendFailure(ShopTranslationHelper.translatable("shop.not_found"));
            return 0;
        }

        Collection<ShopInstance> shopsCollection = shopManager.getAllShops();
        if (shopsCollection.isEmpty()) {
            source.sendSuccess(() -> ShopTranslationHelper.colored("§eNo shops found"), false);
            return 1;
        }

        List<ShopInstance> shops = new ArrayList<>(shopsCollection);
        source.sendSuccess(() -> ShopTranslationHelper.translatable("admin.shop.list.header", shops.size()), false);
        for (int i = 0; i < Math.min(shops.size(), 10); i++) {
            ShopInstance shop = shops.get(i);
            String shopId = shop.getShopId().toString().substring(0, 8);
            String itemName = shop.getTradeItem().getDisplayName().getString();
            String type = shop.getShopType() == ShopType.SELL ? "Sell" : "Buy";
            BlockPos pos = shop.getShopPos();
            source.sendSuccess(() -> ShopTranslationHelper.colored(
                    String.format("§7[%s] §f%s §7(%s) at x:%d y:%d z:%d", 
                            shopId, itemName, type, pos.getX(), pos.getY(), pos.getZ())), false);
        }
        
        if (shops.size() > 10) {
            int remaining = shops.size() - 10;
            source.sendSuccess(() -> ShopTranslationHelper.colored("§7... and " + remaining + " more shops"), false);
        }
        
        return 1;
    }

    private static int teleportToShop(CommandContext<CommandSourceStack> context, String shopIdStr) {
        CommandSourceStack source = context.getSource();
        ServerPlayer player = getPlayer(source);
        if (player == null) {
            source.sendFailure(ShopTranslationHelper.translatable("auth.command.player_only"));
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
            source.sendFailure(ShopTranslationHelper.translatable("admin.shop.tp.not_found", shopIdStr));
            return 0;
        }

        BlockPos pos = targetShop.getShopPos();
        player.teleportTo(pos.getX() + 0.5, pos.getY() + 1, pos.getZ() + 0.5);
        source.sendSuccess(() -> ShopTranslationHelper.translatable("admin.shop.tp.success", (double)pos.getX(), (double)pos.getY(), (double)pos.getZ()), false);
        return 1;
    }

    private static int setOwner(CommandContext<CommandSourceStack> context, ServerPlayer newOwner) {
        CommandSourceStack source = context.getSource();
        ServerPlayer player = getPlayer(source);
        if (player == null) {
            source.sendFailure(ShopTranslationHelper.translatable(source, "auth.command.player_only"));
            return 0;
        }

        ShopManager shopManager = ShopManager.getInstance(player.level().getServer());
        if (shopManager == null) {
            source.sendFailure(ShopTranslationHelper.translatable(source, "shop.not_found"));
            return 0;
        }

        Optional<ShopInstance> shopOpt = shopManager.getShopPlayerLookingAt(player);
        if (shopOpt.isEmpty()) {
            source.sendFailure(ShopTranslationHelper.translatable(source, "shop.not_looking"));
            return 0;
        }

        ShopInstance shop = shopOpt.get();
        UUID oldOwner = shop.getOwnerId();
        
        source.sendSuccess(() -> ShopTranslationHelper.translatable("admin.shop.set_owner.not_supported", oldOwner.toString()), false);
        source.sendSuccess(() -> ShopTranslationHelper.translatable("admin.shop.set_owner.hint"), false);
        
        return 1;
    }

    private static int showHelp(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();
        
        source.sendSuccess(() -> ShopTranslationHelper.colored("§a=== ShopAdmin Commands ==="), false);
        source.sendSuccess(() -> ShopTranslationHelper.colored("§e/shopadmin delete - Delete shop (look at it)"), false);
        source.sendSuccess(() -> ShopTranslationHelper.colored("§e/shopadmin setInfinite <true/false> - Set infinite mode"), false);
        source.sendSuccess(() -> ShopTranslationHelper.colored("§e/shopadmin toggleDynamic - Toggle dynamic pricing"), false);
        source.sendSuccess(() -> ShopTranslationHelper.colored("§e/shopadmin setHalfLife <K> - Set half-life constant"), false);
        source.sendSuccess(() -> ShopTranslationHelper.colored("§e/shopadmin setSystemStock <S> - Set system stock"), false);
        source.sendSuccess(() -> ShopTranslationHelper.colored("§e/shopadmin adjustSystemStock <%> - Set daily system stock decay rate"), false);
        source.sendSuccess(() -> ShopTranslationHelper.colored("§e/shopadmin setPrice <price> - Set shop price"), false);
        source.sendSuccess(() -> ShopTranslationHelper.colored("§e/shopadmin setCurrency <id> - Set currency type"), false);
        source.sendSuccess(() -> ShopTranslationHelper.colored("§e/shopadmin setMinPrice <price> - Set minimum price"), false);
        source.sendSuccess(() -> ShopTranslationHelper.colored("§e/shopadmin setMaxPrice <price> - Set maximum price"), false);
        source.sendSuccess(() -> ShopTranslationHelper.colored("§e/shopadmin setTimeout <seconds> - Set input timeout"), false);
        source.sendSuccess(() -> ShopTranslationHelper.colored("§e/shopadmin info - Show shop info"), false);
        source.sendSuccess(() -> ShopTranslationHelper.colored("§e/shopadmin reload - Reload config"), false);
        source.sendSuccess(() -> ShopTranslationHelper.colored("§e/shopadmin list - List all shops"), false);
        source.sendSuccess(() -> ShopTranslationHelper.colored("§e/shopadmin tp <shopId> - Teleport to shop"), false);
        source.sendSuccess(() -> ShopTranslationHelper.colored("§e/shopadmin setOwner <player> - Set shop owner"), false);
        
        return 1;
    }
}
