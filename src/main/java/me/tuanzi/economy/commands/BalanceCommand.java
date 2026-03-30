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

public class BalanceCommand {
    public static void register(CommandDispatcher<CommandSourceStack> dispatcher, CommandBuildContext registryAccess) {
        dispatcher.register(Commands.literal("balance")
            .executes(ctx -> {
                ServerPlayer player = ctx.getSource().getPlayer();
                if (player == null) {
                    ServerTranslationHelper.sendFailure(ctx.getSource(), "economy.balance.player_only");
                    return 0;
                }
                return showAllBalances(ctx, player);
            })
            .then(Commands.argument("wallet", StringArgumentType.word())
                .suggests((ctx, builder) -> suggestWalletTypes(ctx, builder))
                .executes(ctx -> {
                    ServerPlayer player = ctx.getSource().getPlayer();
                    if (player == null) {
                        ServerTranslationHelper.sendFailure(ctx.getSource(), "economy.balance.player_only");
                        return 0;
                    }
                    String walletId = StringArgumentType.getString(ctx, "wallet");
                    return showSpecificBalance(ctx, player, walletId);
                }))
            .then(Commands.argument("player", EntityArgument.player())
                .executes(ctx -> {
                    ServerPlayer target = EntityArgument.getPlayer(ctx, "player");
                    return showAllBalances(ctx, target);
                })
                .then(Commands.argument("wallet", StringArgumentType.word())
                    .suggests((ctx, builder) -> suggestWalletTypes(ctx, builder))
                    .executes(ctx -> {
                        ServerPlayer target = EntityArgument.getPlayer(ctx, "player");
                        String walletId = StringArgumentType.getString(ctx, "wallet");
                        return showSpecificBalance(ctx, target, walletId);
                    }))));
    }

    private static CompletableFuture<Suggestions> suggestWalletTypes(CommandContext<CommandSourceStack> ctx, SuggestionsBuilder builder) {
        EconomyAPI api = EconomyAPIImpl.getInstance(ctx.getSource().getServer());
        Collection<WalletType> types = api.getAllWalletTypes();
        return SharedSuggestionProvider.suggest(
            types.stream().map(WalletType::id),
            builder
        );
    }

    private static int showAllBalances(CommandContext<CommandSourceStack> ctx, ServerPlayer player) {
        EconomyAPI api = EconomyAPIImpl.getInstance(ctx.getSource().getServer());
        Collection<WalletType> types = api.getAllWalletTypes();

        String playerName = player.getName().getString();
        ServerTranslationHelper.sendSuccess(ctx.getSource(), "economy.balance.player_header", playerName);

        if (types.isEmpty()) {
            ServerTranslationHelper.sendSuccess(ctx.getSource(), "economy.balance.no_wallets");
            return 1;
        }

        for (WalletType type : types) {
            double balance = api.getBalance(player.getUUID(), type.id());
            String displayName = type.displayName().getString();
            ServerTranslationHelper.sendSuccess(ctx.getSource(), "economy.balance.line", displayName, balance);
        }

        return 1;
    }

    private static int showSpecificBalance(CommandContext<CommandSourceStack> ctx, ServerPlayer player, String walletId) {
        EconomyAPI api = EconomyAPIImpl.getInstance(ctx.getSource().getServer());

        if (api.getWalletType(walletId).isEmpty()) {
            ServerTranslationHelper.sendFailure(ctx.getSource(), "economy.balance.wallet_not_found", walletId);
            return 0;
        }

        WalletType type = api.getWalletType(walletId).get();
        double balance = api.getBalance(player.getUUID(), walletId);
        String displayName = type.displayName().getString();

        ServerTranslationHelper.sendSuccess(ctx.getSource(), "economy.balance.line", displayName, balance);
        return 1;
    }
}
