package me.tuanzi.economy.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import me.tuanzi.economy.api.EconomyAPI;
import me.tuanzi.economy.api.EconomyAPIImpl;
import me.tuanzi.economy.currency.WalletType;
import me.tuanzi.economy.exception.WalletTypeNotFoundException;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

import java.util.Collection;

public class BalanceCommand {
    public static void register(CommandDispatcher<CommandSourceStack> dispatcher, CommandBuildContext registryAccess) {
        dispatcher.register(Commands.literal("balance")
                .executes(BalanceCommand::viewAllBalances)
                .then(Commands.argument("id", StringArgumentType.word())
                        .suggests((context, builder) -> {
                            EconomyAPI api = EconomyAPIImpl.getInstance(context.getSource().getServer());
                            Collection<WalletType> walletTypes = api.getAllWalletTypes();
                            return SharedSuggestionProvider.suggest(
                                    walletTypes.stream().map(WalletType::id),
                                    builder
                            );
                        })
                        .executes(BalanceCommand::viewSpecificBalance)));
    }

    private static int viewAllBalances(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        CommandSourceStack source = context.getSource();
        ServerPlayer player = source.getPlayerOrException();
        EconomyAPI api = EconomyAPIImpl.getInstance(source.getServer());

        Collection<WalletType> walletTypes = api.getAllWalletTypes();

        if (walletTypes.isEmpty()) {
            source.sendSuccess(() -> Component.literal("§e========== §6余额查询 §e=========="), false);
            source.sendSuccess(() -> Component.literal("§c暂无可用钱包类型"), false);
            return 1;
        }

        source.sendSuccess(() -> Component.literal("§e========== §6" + player.getName().getString() + " 的余额 §e=========="), false);

        for (WalletType walletType : walletTypes) {
            double balance = api.getBalance(player.getUUID(), walletType.id());
            String displayName = walletType.displayName().getString();
            source.sendSuccess(() -> Component.literal("§b" + displayName + ": §a" + String.format("%.2f", balance)), false);
        }

        return 1;
    }

    private static int viewSpecificBalance(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        CommandSourceStack source = context.getSource();
        ServerPlayer player = source.getPlayerOrException();
        String walletId = StringArgumentType.getString(context, "id");

        EconomyAPI api = EconomyAPIImpl.getInstance(source.getServer());

        try {
            double balance = api.getBalance(player.getUUID(), walletId);
            WalletType walletType = api.getWalletType(walletId).orElse(null);

            String displayName = walletType != null ? walletType.displayName().getString() : walletId;

            source.sendSuccess(() -> Component.literal("§e========== §6余额查询 §e=========="), false);
            source.sendSuccess(() -> Component.literal("§b" + displayName + ": §a" + String.format("%.2f", balance)), false);
        } catch (WalletTypeNotFoundException e) {
            source.sendFailure(Component.literal("§c钱包类型 '" + walletId + "' 不存在"));
        }

        return 1;
    }
}
