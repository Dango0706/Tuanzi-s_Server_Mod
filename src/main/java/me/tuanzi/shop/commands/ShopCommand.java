package me.tuanzi.shop.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import me.tuanzi.shop.ShopModule;
import me.tuanzi.shop.events.ChatInputHandler;
import me.tuanzi.shop.shop.ShopInstance;
import me.tuanzi.shop.shop.ShopManager;
import me.tuanzi.shop.shop.ShopType;
import me.tuanzi.shop.utils.ShopTranslationHelper;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.server.level.ServerPlayer;

import java.util.Optional;
import java.util.UUID;

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
        
        if (!source.isPlayer()) {
            source.sendFailure(ShopTranslationHelper.literal("cThis command can only be used by players"));
            return 0;
        }

        ServerPlayer player;
        try {
            player = source.getPlayerOrException();
        } catch (CommandSyntaxException e) {
            source.sendFailure(ShopTranslationHelper.literal("cPlayer not found"));
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
        ChatInputHandler chatHandler = ShopModule.getInstance(player.level().getServer()).getChatInputHandler();
        if (chatHandler != null && chatHandler.hasPendingTransaction(player.getUUID())) {
            source.sendFailure(ShopTranslationHelper.colored("cYou have a pending transaction, please complete or cancel it first"));
            return 0;
        }

        var interactionHandler = ShopModule.getInstance(player.level().getServer()).getInteractionHandler();
        if (interactionHandler == null) {
            source.sendFailure(ShopTranslationHelper.colored("cTransaction handler not initialized"));
            return 0;
        }

        return interactionHandler.executeTransaction(player, shop.getShopId(), type, quantity) ? 1 : 0;
    }

    private static int showShopInfo(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();
        
        if (!source.isPlayer()) {
            source.sendFailure(ShopTranslationHelper.literal("cThis command can only be used by players"));
            return 0;
        }

        ServerPlayer player;
        try {
            player = source.getPlayerOrException();
        } catch (CommandSyntaxException e) {
            source.sendFailure(ShopTranslationHelper.literal("cPlayer not found"));
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
        sendShopInfo(player, shop);
        return 1;
    }

    private static void sendShopInfo(ServerPlayer player, ShopInstance shop) {
        player.sendSystemMessage(ShopTranslationHelper.translatable("admin.shop.info.title"));
        player.sendSystemMessage(ShopTranslationHelper.translatable("admin.shop.info.id", shop.getShopId().toString().substring(0, 8)));
        player.sendSystemMessage(ShopTranslationHelper.translatable("admin.shop.info.type", 
                shop.getShopType() == ShopType.SELL ? "bSell Shop" : "bBuy Shop"));
        player.sendSystemMessage(ShopTranslationHelper.translatable("admin.shop.info.item", 
                shop.getTradeItem().getDisplayName().getString()));
        
        String currencyName = ShopModule.getInstance(player.level().getServer())
                .getShopManager().getCurrencyDisplayName(shop.getWalletTypeId());
        player.sendSystemMessage(ShopTranslationHelper.translatable("admin.shop.info.price", 
                shop.getCurrentPrice(), currencyName));
    }

    private static int handleConfirm(CommandContext<CommandSourceStack> context, String confirm) {
        CommandSourceStack source = context.getSource();
        
        if (!source.isPlayer()) {
            source.sendFailure(ShopTranslationHelper.literal("cThis command can only be used by players"));
            return 0;
        }

        ServerPlayer player;
        try {
            player = source.getPlayerOrException();
        } catch (CommandSyntaxException e) {
            source.sendFailure(ShopTranslationHelper.literal("cPlayer not found"));
            return 0;
        }
        
        ChatInputHandler chatHandler = ShopModule.getInstance(player.level().getServer()).getChatInputHandler();
        if (chatHandler == null) {
            source.sendFailure(ShopTranslationHelper.colored("cChat handler not initialized"));
            return 0;
        }

        boolean confirmed = confirm.equalsIgnoreCase("yes");
        return chatHandler.handleConfirmCommand(player, confirmed) ? 1 : 0;
    }

    private static int handleCancel(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();
        
        if (!source.isPlayer()) {
            source.sendFailure(ShopTranslationHelper.literal("cThis command can only be used by players"));
            return 0;
        }

        ServerPlayer player;
        try {
            player = source.getPlayerOrException();
        } catch (CommandSyntaxException e) {
            source.sendFailure(ShopTranslationHelper.literal("cPlayer not found"));
            return 0;
        }
        
        ChatInputHandler chatHandler = ShopModule.getInstance(player.level().getServer()).getChatInputHandler();
        if (chatHandler == null) {
            source.sendFailure(ShopTranslationHelper.colored("cChat handler not initialized"));
            return 0;
        }

        return chatHandler.handleConfirmCommand(player, false) ? 1 : 0;
    }

    private static int showHelp(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();
        
        source.sendSuccess(() -> ShopTranslationHelper.colored("a=== Shop Commands ==="), false);
        source.sendSuccess(() -> ShopTranslationHelper.colored("e/shop buy [quantity] - Buy items from shop"), false);
        source.sendSuccess(() -> ShopTranslationHelper.colored("e/shop sell [quantity] - Sell items to shop"), false);
        source.sendSuccess(() -> ShopTranslationHelper.colored("e/shop info - Show shop information"), false);
        source.sendSuccess(() -> ShopTranslationHelper.colored("e/shop confirm <yes/no> - Confirm/cancel pending action"), false);
        source.sendSuccess(() -> ShopTranslationHelper.colored("e/shop cancel - Cancel pending action"), false);
        source.sendSuccess(() -> ShopTranslationHelper.colored("e/shop help - Show this help"), false);
        
        return 1;
    }
}
