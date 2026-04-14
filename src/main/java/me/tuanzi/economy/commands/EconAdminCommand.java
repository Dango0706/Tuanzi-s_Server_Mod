package me.tuanzi.economy.commands;

import me.tuanzi.economy.api.EconomyAPI;
import me.tuanzi.economy.api.EconomyAPIImpl;
import me.tuanzi.economy.currency.WalletType;
import me.tuanzi.economy.exception.WalletTypeNotFoundException;
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
import net.minecraft.server.permissions.Permission;
import net.minecraft.server.permissions.PermissionLevel;

import java.util.Collection;
import java.util.concurrent.CompletableFuture;

import static com.mojang.brigadier.arguments.DoubleArgumentType.getDouble;
import static com.mojang.brigadier.arguments.DoubleArgumentType.doubleArg;

public class EconAdminCommand {
    public static void register(CommandDispatcher<CommandSourceStack> dispatcher, CommandBuildContext registryAccess) {
        dispatcher.register(Commands.literal("econ-admin")
            .requires(source -> source.permissions().hasPermission(new Permission.HasCommandLevel(PermissionLevel.GAMEMASTERS)))
            .then(Commands.literal("type")
                .then(Commands.literal("add")
                    .then(Commands.argument("id", StringArgumentType.word())
                        .then(Commands.argument("display_name", StringArgumentType.greedyString())
                            .executes(ctx -> addWalletType(ctx)))))
                .then(Commands.literal("remove")
                    .then(Commands.argument("id", StringArgumentType.word())
                        .suggests((ctx, builder) -> suggestWalletTypes(ctx, builder))
                        .executes(ctx -> removeWalletType(ctx)))))
            .then(Commands.literal("balance")
                .then(Commands.argument("player", EntityArgument.player())
                    .then(Commands.argument("id", StringArgumentType.word())
                        .suggests((ctx, builder) -> suggestWalletTypes(ctx, builder))
                        .then(Commands.literal("set")
                            .then(Commands.argument("amount", doubleArg(0))
                                .executes(ctx -> setBalance(ctx))))
                        .then(Commands.literal("add")
                            .then(Commands.argument("amount", doubleArg(0))
                                .executes(ctx -> addBalance(ctx))))
                        .then(Commands.literal("remove")
                            .then(Commands.argument("amount", doubleArg(0))
                                .executes(ctx -> removeBalance(ctx)))))))
            .then(Commands.literal("view")
                .then(Commands.argument("player", EntityArgument.player())
                    .executes(ctx -> viewAllBalances(ctx))
                    .then(Commands.argument("id", StringArgumentType.word())
                        .suggests((ctx, builder) -> suggestWalletTypes(ctx, builder))
                        .executes(ctx -> viewBalance(ctx))))));
    }

    private static CompletableFuture<Suggestions> suggestWalletTypes(CommandContext<CommandSourceStack> ctx, SuggestionsBuilder builder) {
        EconomyAPI api = EconomyAPIImpl.getInstance(ctx.getSource().getServer());
        Collection<WalletType> types = api.getAllWalletTypes();
        return SharedSuggestionProvider.suggest(
            types.stream().map(WalletType::id),
            builder
        );
    }

    private static int addWalletType(CommandContext<CommandSourceStack> ctx) {
        String id = StringArgumentType.getString(ctx, "id");
        String displayName = StringArgumentType.getString(ctx, "display_name");
        
        EconomyAPI api = EconomyAPIImpl.getInstance(ctx.getSource().getServer());
        
        if (api.getWalletType(id).isPresent()) {
            ServerTranslationHelper.sendFailure(ctx.getSource(), "economy.admin.wallet_exists", id);
            return 0;
        }
        
        api.registerWalletType(id, Component.literal(displayName));
        ServerTranslationHelper.sendSuccess(ctx.getSource(), "economy.admin.wallet_created", id, displayName);
        return 1;
    }

    private static int removeWalletType(CommandContext<CommandSourceStack> ctx) {
        String id = StringArgumentType.getString(ctx, "id");
        
        EconomyAPI api = EconomyAPIImpl.getInstance(ctx.getSource().getServer());
        
        if (api.getWalletType(id).isEmpty()) {
            ServerTranslationHelper.sendFailure(ctx.getSource(), "economy.admin.wallet_not_found", id);
            return 0;
        }
        
        api.unregisterWalletType(id);
        ServerTranslationHelper.sendSuccess(ctx.getSource(), "economy.admin.wallet_deleted", id);
        return 1;
    }

    private static int setBalance(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        ServerPlayer player = EntityArgument.getPlayer(ctx, "player");
        String walletId = StringArgumentType.getString(ctx, "id");
        double amount = getDouble(ctx, "amount");
        
        EconomyAPI api = EconomyAPIImpl.getInstance(ctx.getSource().getServer());
        
        if (api.getWalletType(walletId).isEmpty()) {
            ServerTranslationHelper.sendFailure(ctx.getSource(), "economy.admin.wallet_not_found", walletId);
            return 0;
        }
        
        try {
            api.setBalance(player.getUUID(), walletId, amount);
            String playerName = player.getName().getString();
            ServerTranslationHelper.sendSuccess(ctx.getSource(), "economy.admin.balance_set", playerName, walletId, amount);
            return 1;
        } catch (WalletTypeNotFoundException e) {
            ServerTranslationHelper.sendFailure(ctx.getSource(), "economy.admin.wallet_not_found", walletId);
            return 0;
        }
    }

    private static int addBalance(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        ServerPlayer player = EntityArgument.getPlayer(ctx, "player");
        String walletId = StringArgumentType.getString(ctx, "id");
        double amount = getDouble(ctx, "amount");
        
        EconomyAPI api = EconomyAPIImpl.getInstance(ctx.getSource().getServer());
        
        if (api.getWalletType(walletId).isEmpty()) {
            ServerTranslationHelper.sendFailure(ctx.getSource(), "economy.admin.wallet_not_found", walletId);
            return 0;
        }
        
        try {
            api.deposit(player.getUUID(), walletId, amount);
            double newBalance = api.getBalance(player.getUUID(), walletId);
            String playerName = player.getName().getString();
            ServerTranslationHelper.sendSuccess(ctx.getSource(), "economy.admin.balance_added", playerName, walletId, amount, newBalance);
            return 1;
        } catch (WalletTypeNotFoundException e) {
            ServerTranslationHelper.sendFailure(ctx.getSource(), "economy.admin.wallet_not_found", walletId);
            return 0;
        }
    }

    private static int removeBalance(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        ServerPlayer player = EntityArgument.getPlayer(ctx, "player");
        String walletId = StringArgumentType.getString(ctx, "id");
        double amount = getDouble(ctx, "amount");
        
        EconomyAPI api = EconomyAPIImpl.getInstance(ctx.getSource().getServer());
        
        if (api.getWalletType(walletId).isEmpty()) {
            ServerTranslationHelper.sendFailure(ctx.getSource(), "economy.admin.wallet_not_found", walletId);
            return 0;
        }
        
        try {
            double currentBalance = api.getBalance(player.getUUID(), walletId);
            if (currentBalance < amount) {
                ServerTranslationHelper.sendFailure(ctx.getSource(), "economy.admin.insufficient_balance", currentBalance, amount);
                return 0;
            }
            
            api.withdraw(player.getUUID(), walletId, amount);
            double newBalance = api.getBalance(player.getUUID(), walletId);
            String playerName = player.getName().getString();
            ServerTranslationHelper.sendSuccess(ctx.getSource(), "economy.admin.balance_removed", playerName, walletId, amount, newBalance);
            return 1;
        } catch (WalletTypeNotFoundException e) {
            ServerTranslationHelper.sendFailure(ctx.getSource(), "economy.admin.wallet_not_found", walletId);
            return 0;
        }
    }

    private static int viewAllBalances(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        ServerPlayer player = EntityArgument.getPlayer(ctx, "player");
        
        EconomyAPI api = EconomyAPIImpl.getInstance(ctx.getSource().getServer());
        Collection<WalletType> types = api.getAllWalletTypes();
        
        if (types.isEmpty()) {
            String playerName = player.getName().getString();
            ServerTranslationHelper.sendSuccess(ctx.getSource(), "economy.admin.balance_header", playerName);
            ServerTranslationHelper.sendSuccess(ctx.getSource(), "economy.admin.no_wallets");
            return 1;
        }
        
        String playerName = player.getName().getString();
        ServerTranslationHelper.sendSuccess(ctx.getSource(), "economy.admin.balance_header", playerName);
        for (WalletType type : types) {
            double balance = api.getBalance(player.getUUID(), type.id());
            ServerTranslationHelper.sendSuccess(ctx.getSource(), "economy.admin.balance_line", type.displayName(), type.id(), balance);
        }
        return 1;
    }

    private static int viewBalance(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        ServerPlayer player = EntityArgument.getPlayer(ctx, "player");
        String walletId = StringArgumentType.getString(ctx, "id");
        
        EconomyAPI api = EconomyAPIImpl.getInstance(ctx.getSource().getServer());
        
        if (api.getWalletType(walletId).isEmpty()) {
            ServerTranslationHelper.sendFailure(ctx.getSource(), "economy.admin.wallet_not_found", walletId);
            return 0;
        }
        
        WalletType type = api.getWalletType(walletId).get();
        double balance = api.getBalance(player.getUUID(), walletId);
        String playerName = player.getName().getString();
        
        ServerTranslationHelper.sendSuccess(ctx.getSource(), "economy.admin.balance_header", playerName);
        ServerTranslationHelper.sendSuccess(ctx.getSource(), "economy.admin.balance_line", type.displayName(), walletId, balance);
        return 1;
    }
}
