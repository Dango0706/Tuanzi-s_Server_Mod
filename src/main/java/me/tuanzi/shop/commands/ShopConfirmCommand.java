package me.tuanzi.shop.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import me.tuanzi.shop.ShopModule;
import me.tuanzi.shop.events.ChatInputHandler;
import me.tuanzi.shop.utils.ShopTranslationHelper;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.server.level.ServerPlayer;

public class ShopConfirmCommand {
    public static void register(CommandDispatcher<CommandSourceStack> dispatcher, CommandBuildContext registryAccess) {
        dispatcher.register(Commands.literal("shopconfirm")
                .then(Commands.literal("yes")
                        .executes(context -> handleConfirm(context, "yes")))
                .then(Commands.literal("no")
                        .executes(context -> handleConfirm(context, "no")))
                .then(Commands.literal("是")
                        .executes(context -> handleConfirm(context, "yes")))
                .then(Commands.literal("否")
                        .executes(context -> handleConfirm(context, "no")))
                .then(Commands.literal("sell")
                        .executes(context -> handleConfirm(context, "sell")))
                .then(Commands.literal("buy")
                        .executes(context -> handleConfirm(context, "buy")))
                .then(Commands.literal("出售")
                        .executes(context -> handleConfirm(context, "sell")))
                .then(Commands.literal("收购")
                        .executes(context -> handleConfirm(context, "buy")))
        );
    }

    private static int handleConfirm(CommandContext<CommandSourceStack> context, String action) {
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

        return chatHandler.handleConfirmCommand(player, action) ? 1 : 0;
    }
}
