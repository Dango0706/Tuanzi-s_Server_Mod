package me.tuanzi.shop.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import me.tuanzi.shop.ShopModule;
import me.tuanzi.shop.events.BlockInteractionHandler;
import me.tuanzi.shop.events.ChatInputHandler;
import me.tuanzi.shop.shop.ShopInstance;
import me.tuanzi.shop.shop.ShopManager;
import me.tuanzi.shop.shop.ShopType;
import me.tuanzi.shop.utils.ShopTranslationHelper;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;

import java.util.Optional;

public class ShopCommand {
    private static final int DEFAULT_QUANTITY = 1;

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher, CommandBuildContext registryAccess) {
        dispatcher.register(Commands.literal("shop")
                .then(Commands.literal("buy")
                        .then(Commands.argument("quantity", IntegerArgumentType.integer(1))
                                .executes(context -> executeShopCommand(context, "buy", IntegerArgumentType.getInteger(context, "quantity")))
                        )
                        .executes(context -> executeShopCommand(context, "buy", DEFAULT_QUANTITY))
                )
                .then(Commands.literal("sell")
                        .then(Commands.argument("quantity", IntegerArgumentType.integer(1))
                                .executes(context -> executeShopCommand(context, "sell", IntegerArgumentType.getInteger(context, "quantity")))
                        )
                        .executes(context -> executeShopCommand(context, "sell", DEFAULT_QUANTITY))
                )
                .then(Commands.literal("info")
                        .executes(ShopCommand::showShopInfo)
                )
                .then(Commands.literal("confirm")
                        .then(Commands.argument("confirm", StringArgumentType.word())
                                .executes(context -> handleConfirm(context, StringArgumentType.getString(context, "confirm")))
                        )
                )
                .then(Commands.literal("cancel")
                        .executes(ShopCommand::handleCancel)
                )
                .then(Commands.literal("help")
                        .executes(ShopCommand::showHelp)
                )
        );
    }

    private static int executeShopCommand(CommandContext<CommandSourceStack> context, String type, int quantity) {
        CommandSourceStack source = context.getSource();
        ServerPlayer player = source.getPlayer();
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
        if (chatHandler != null && chatHandler.hasPendingTransaction(player.getUUID())) {
            source.sendFailure(ShopTranslationHelper.translatable(source, "transaction.pending_exists"));
            return 0;
        }

        BlockInteractionHandler interactionHandler = ShopModule.getInstance(player.level().getServer()).getInteractionHandler();
        if (interactionHandler == null) {
            return 0;
        }

        return interactionHandler.executeTransaction(player, shop.getShopId(), type, quantity) ? 1 : 0;
    }

    private static int showShopInfo(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();
        ServerPlayer player = source.getPlayer();
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
        sendShopInfo(player, shop);
        return 1;
    }

    private static void sendShopInfo(ServerPlayer player, ShopInstance shop) {
        ShopManager shopManager = ShopManager.getInstance(player.level().getServer());
        String currencyName = shopManager.getCurrencyDisplayName(shop.getWalletTypeId());
        ItemStack stack = shop.getTradeItem();
        
        player.sendSystemMessage(ShopTranslationHelper.header(player, "admin.shop.info.title_fmt"));
        
        // 1. 基础信息
        player.sendSystemMessage(ShopTranslationHelper.colored("§b" + ShopTranslationHelper.getRawTranslation(player, "common.item") + ": §f" + stack.getDisplayName().getString()));
        String itemId = net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(stack.getItem()).toString();
        player.sendSystemMessage(ShopTranslationHelper.colored("§b" + ShopTranslationHelper.getRawTranslation(player, "common.item_id") + ": §7" + itemId));
        
        String typeKey = shop.getShopType() == ShopType.SELL ? "shop.type.sell" : "shop.type.buy";
        player.sendSystemMessage(ShopTranslationHelper.colored("§b" + ShopTranslationHelper.getRawTranslation(player, "common.type") + ": §f" + ShopTranslationHelper.getRawTranslation(player, typeKey)));
        
        player.sendSystemMessage(ShopTranslationHelper.colored("§b" + ShopTranslationHelper.getRawTranslation(player, "admin.shop.info.current_price") + ": §6" + 
                String.format("%.2f %s", shop.getCurrentPrice(), currencyName)));

        // 2. 附魔信息
        var enchants = stack.get(net.minecraft.core.component.DataComponents.ENCHANTMENTS);
        if (enchants != null && !enchants.isEmpty()) {
            player.sendSystemMessage(ShopTranslationHelper.colored("§b" + ShopTranslationHelper.getRawTranslation(player, "item.info.enchants") + ":"));
            enchants.entrySet().forEach(entry -> {
                String enchantName = entry.getKey().getRegisteredName();
                int level = entry.getIntValue();
                player.sendSystemMessage(ShopTranslationHelper.colored("  §7- §f" + enchantName + " §e" + level));
            });
        }

        // 3. Lore 信息
        var lore = stack.get(net.minecraft.core.component.DataComponents.LORE);
        if (lore != null && !lore.lines().isEmpty()) {
            player.sendSystemMessage(ShopTranslationHelper.colored("§b" + ShopTranslationHelper.getRawTranslation(player, "item.info.lore") + ":"));
            lore.lines().forEach(line -> player.sendSystemMessage(Component.literal("  §d").append(line)));
        }

        // 4. 耐久度
        if (stack.isDamageableItem()) {
            int maxDamage = stack.getMaxDamage();
            int currentDamage = stack.getDamageValue();
            player.sendSystemMessage(ShopTranslationHelper.colored("§b" + ShopTranslationHelper.getRawTranslation(player, "item.info.durability") + ": §f" + (maxDamage - currentDamage) + " / " + maxDamage));
        }

        if (shop.isDynamicPricing()) {
            player.sendSystemMessage(ShopTranslationHelper.colored("§b" + ShopTranslationHelper.getRawTranslation(player, "common.dynamic_pricing") + ": §a" + ShopTranslationHelper.getRawTranslation(player, "common.enabled")));
        }

        if (shop.getDescription() != null && !shop.getDescription().isEmpty()) {
            player.sendSystemMessage(ShopTranslationHelper.colored("§b" + ShopTranslationHelper.getRawTranslation(player, "common.note") + ": §f" + shop.getDescription()));
        }
        
        player.sendSystemMessage(ShopTranslationHelper.header(player, "admin.shop.info.title_fmt"));
    }

    private static int handleConfirm(CommandContext<CommandSourceStack> context, String confirm) {
        ServerPlayer player = context.getSource().getPlayer();
        if (player == null) return 0;
        
        ChatInputHandler chatHandler = ShopModule.getInstance(player.level().getServer()).getChatInputHandler();
        if (chatHandler == null) return 0;

        return chatHandler.handleConfirmCommand(player, confirm) ? 1 : 0;
    }

    private static int handleCancel(CommandContext<CommandSourceStack> context) {
        ServerPlayer player = context.getSource().getPlayer();
        if (player == null) return 0;
        
        ChatInputHandler chatHandler = ShopModule.getInstance(player.level().getServer()).getChatInputHandler();
        if (chatHandler != null) {
            return chatHandler.handleConfirmCommand(player, "no") ? 1 : 0;
        }
        return 0;
    }

    private static int showHelp(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();
        
        source.sendSuccess(() -> ShopTranslationHelper.header(source, "shop.help.header"), false);
        source.sendSuccess(() -> ShopTranslationHelper.translatable(source, "shop.help.buy"), false);
        source.sendSuccess(() -> ShopTranslationHelper.translatable(source, "shop.help.sell"), false);
        source.sendSuccess(() -> ShopTranslationHelper.translatable(source, "shop.help.info"), false);
        source.sendSuccess(() -> ShopTranslationHelper.translatable(source, "shop.help.cancel"), false);
        source.sendSuccess(() -> ShopTranslationHelper.translatable(source, "shop.help.help"), false);
        source.sendSuccess(() -> ShopTranslationHelper.header(source, "shop.help.header"), false);
        
        return 1;
    }
}
