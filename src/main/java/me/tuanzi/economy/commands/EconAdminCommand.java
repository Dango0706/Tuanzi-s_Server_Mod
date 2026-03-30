package me.tuanzi.economy.commands;

import me.tuanzi.economy.api.EconomyAPI;
import me.tuanzi.economy.api.EconomyAPIImpl;
import me.tuanzi.economy.currency.WalletType;
import me.tuanzi.economy.exception.WalletTypeNotFoundException;
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
            ctx.getSource().sendFailure(Component.literal("§c钱包类型 '" + id + "' 已存在"));
            return 0;
        }
        
        api.registerWalletType(id, Component.literal(displayName));
        ctx.getSource().sendSuccess(() -> Component.literal("§a成功创建钱包类型 '" + id + "' (显示名称: " + displayName + ")"), true);
        return 1;
    }

    private static int removeWalletType(CommandContext<CommandSourceStack> ctx) {
        String id = StringArgumentType.getString(ctx, "id");
        
        EconomyAPI api = EconomyAPIImpl.getInstance(ctx.getSource().getServer());
        
        if (api.getWalletType(id).isEmpty()) {
            ctx.getSource().sendFailure(Component.literal("§c钱包类型 '" + id + "' 不存在"));
            return 0;
        }
        
        api.unregisterWalletType(id);
        ctx.getSource().sendSuccess(() -> Component.literal("§a成功删除钱包类型 '" + id + "'"), true);
        return 1;
    }

    private static int setBalance(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        ServerPlayer player = EntityArgument.getPlayer(ctx, "player");
        String walletId = StringArgumentType.getString(ctx, "id");
        double amount = getDouble(ctx, "amount");
        
        EconomyAPI api = EconomyAPIImpl.getInstance(ctx.getSource().getServer());
        
        if (api.getWalletType(walletId).isEmpty()) {
            ctx.getSource().sendFailure(Component.literal("§c钱包类型 '" + walletId + "' 不存在"));
            return 0;
        }
        
        try {
            api.setBalance(player.getUUID(), walletId, amount);
            ctx.getSource().sendSuccess(() -> Component.literal("§a已将 " + player.getName().getString() + " 的 " + walletId + " 余额设置为 " + String.format("%.2f", amount)), true);
            return 1;
        } catch (WalletTypeNotFoundException e) {
            ctx.getSource().sendFailure(Component.literal("§c钱包类型 '" + walletId + "' 不存在"));
            return 0;
        }
    }

    private static int addBalance(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        ServerPlayer player = EntityArgument.getPlayer(ctx, "player");
        String walletId = StringArgumentType.getString(ctx, "id");
        double amount = getDouble(ctx, "amount");
        
        EconomyAPI api = EconomyAPIImpl.getInstance(ctx.getSource().getServer());
        
        if (api.getWalletType(walletId).isEmpty()) {
            ctx.getSource().sendFailure(Component.literal("§c钱包类型 '" + walletId + "' 不存在"));
            return 0;
        }
        
        try {
            api.deposit(player.getUUID(), walletId, amount);
            double newBalance = api.getBalance(player.getUUID(), walletId);
            ctx.getSource().sendSuccess(() -> Component.literal("§a已为 " + player.getName().getString() + " 的 " + walletId + " 增加 " + String.format("%.2f", amount) + " (当前余额: " + String.format("%.2f", newBalance) + ")"), true);
            return 1;
        } catch (WalletTypeNotFoundException e) {
            ctx.getSource().sendFailure(Component.literal("§c钱包类型 '" + walletId + "' 不存在"));
            return 0;
        }
    }

    private static int removeBalance(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        ServerPlayer player = EntityArgument.getPlayer(ctx, "player");
        String walletId = StringArgumentType.getString(ctx, "id");
        double amount = getDouble(ctx, "amount");
        
        EconomyAPI api = EconomyAPIImpl.getInstance(ctx.getSource().getServer());
        
        if (api.getWalletType(walletId).isEmpty()) {
            ctx.getSource().sendFailure(Component.literal("§c钱包类型 '" + walletId + "' 不存在"));
            return 0;
        }
        
        try {
            double currentBalance = api.getBalance(player.getUUID(), walletId);
            if (currentBalance < amount) {
                ctx.getSource().sendFailure(Component.literal("§c余额不足! 当前余额: " + String.format("%.2f", currentBalance) + ", 尝试扣除: " + String.format("%.2f", amount)));
                return 0;
            }
            
            api.withdraw(player.getUUID(), walletId, amount);
            double newBalance = api.getBalance(player.getUUID(), walletId);
            ctx.getSource().sendSuccess(() -> Component.literal("§a已从 " + player.getName().getString() + " 的 " + walletId + " 扣除 " + String.format("%.2f", amount) + " (当前余额: " + String.format("%.2f", newBalance) + ")"), true);
            return 1;
        } catch (WalletTypeNotFoundException e) {
            ctx.getSource().sendFailure(Component.literal("§c钱包类型 '" + walletId + "' 不存在"));
            return 0;
        }
    }

    private static int viewAllBalances(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        ServerPlayer player = EntityArgument.getPlayer(ctx, "player");
        
        EconomyAPI api = EconomyAPIImpl.getInstance(ctx.getSource().getServer());
        Collection<WalletType> types = api.getAllWalletTypes();
        
        if (types.isEmpty()) {
            ctx.getSource().sendSuccess(() -> Component.literal("§e" + player.getName().getString() + " 的钱包余额:"), false);
            ctx.getSource().sendSuccess(() -> Component.literal("§7暂无注册的钱包类型"), false);
            return 1;
        }
        
        ctx.getSource().sendSuccess(() -> Component.literal("§e========== §6" + player.getName().getString() + " 的钱包余额 §e=========="), false);
        for (WalletType type : types) {
            double balance = api.getBalance(player.getUUID(), type.id());
            String displayName = type.displayName().getString();
            ctx.getSource().sendSuccess(() -> Component.literal("§b" + displayName + " (" + type.id() + "): §f" + String.format("%.2f", balance)), false);
        }
        return 1;
    }

    private static int viewBalance(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        ServerPlayer player = EntityArgument.getPlayer(ctx, "player");
        String walletId = StringArgumentType.getString(ctx, "id");
        
        EconomyAPI api = EconomyAPIImpl.getInstance(ctx.getSource().getServer());
        
        if (api.getWalletType(walletId).isEmpty()) {
            ctx.getSource().sendFailure(Component.literal("§c钱包类型 '" + walletId + "' 不存在"));
            return 0;
        }
        
        WalletType type = api.getWalletType(walletId).get();
        double balance = api.getBalance(player.getUUID(), walletId);
        String displayName = type.displayName().getString();
        
        ctx.getSource().sendSuccess(() -> Component.literal("§e========== §6" + player.getName().getString() + " 的钱包余额 §e=========="), false);
        ctx.getSource().sendSuccess(() -> Component.literal("§b" + displayName + " (" + walletId + "): §f" + String.format("%.2f", balance)), false);
        return 1;
    }
}
