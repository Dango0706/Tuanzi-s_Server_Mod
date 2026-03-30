package me.tuanzi.economy.commands;

import me.tuanzi.economy.api.EconomyAPI;
import me.tuanzi.economy.api.EconomyAPIImpl;
import me.tuanzi.economy.currency.WalletType;
import me.tuanzi.economy.utils.ServerTranslationHelper;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

import java.util.Collection;
import java.util.concurrent.CompletableFuture;

import static com.mojang.brigadier.arguments.DoubleArgumentType.getDouble;
import static com.mojang.brigadier.arguments.DoubleArgumentType.doubleArg;

public class PayCommand {
    public static void register(CommandDispatcher<CommandSourceStack> dispatcher, CommandBuildContext registryAccess) {
        dispatcher.register(Commands.literal("pay")
            .then(Commands.argument("player", EntityArgument.player())
                .then(Commands.argument("wallet", StringArgumentType.word())
                    .suggests((ctx, builder) -> suggestWalletTypes(ctx, builder))
                    .then(Commands.argument("amount", doubleArg(0.01))
                        .executes(ctx -> executePay(ctx))))));
    }

    private static CompletableFuture<Suggestions> suggestWalletTypes(CommandContext<CommandSourceStack> ctx, SuggestionsBuilder builder) {
        EconomyAPI api = EconomyAPIImpl.getInstance(ctx.getSource().getServer());
        Collection<WalletType> types = api.getAllWalletTypes();
        return SharedSuggestionProvider.suggest(
            types.stream().map(WalletType::id),
            builder
        );
    }

    private static int executePay(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        ServerPlayer sender = ctx.getSource().getPlayerOrException();
        ServerPlayer target = EntityArgument.getPlayer(ctx, "player");
        String walletId = StringArgumentType.getString(ctx, "wallet");
        double amount = getDouble(ctx, "amount");

        if (sender.getUUID().equals(target.getUUID())) {
            ServerTranslationHelper.sendFailure(ctx.getSource(), "economy.pay.self");
            return 0;
        }

        EconomyAPI api = EconomyAPIImpl.getInstance(ctx.getSource().getServer());

        if (api.getWalletType(walletId).isEmpty()) {
            ServerTranslationHelper.sendFailure(ctx.getSource(), "economy.pay.wallet_not_found", walletId);
            return 0;
        }

        double senderBalance = api.getBalance(sender.getUUID(), walletId);
        if (senderBalance < amount) {
            ServerTranslationHelper.sendFailure(ctx.getSource(), "economy.pay.insufficient_balance", senderBalance, amount);
            return 0;
        }

        api.withdraw(sender.getUUID(), walletId, amount);
        api.deposit(target.getUUID(), walletId, amount);

        String senderName = sender.getName().getString();
        String targetName = target.getName().getString();
        String currencyName = api.getWalletType(walletId).get().displayName().getString();

        ServerTranslationHelper.sendSuccess(ctx.getSource(), "economy.pay.success", targetName, amount, currencyName);
        ServerTranslationHelper.sendMessage(target, "economy.pay.received", senderName, amount, currencyName);

        return 1;
    }
}
