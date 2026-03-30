package me.tuanzi.economy.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.DoubleArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import me.tuanzi.economy.api.EconomyAPI;
import me.tuanzi.economy.api.EconomyAPIImpl;
import me.tuanzi.economy.currency.WalletType;
import me.tuanzi.economy.exception.InsufficientBalanceException;
import me.tuanzi.economy.exception.WalletTypeNotFoundException;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

import java.util.Collection;

public class PayCommand {
    public static void register(CommandDispatcher<CommandSourceStack> dispatcher, CommandBuildContext registryAccess) {
        dispatcher.register(Commands.literal("pay")
                .then(Commands.argument("player", EntityArgument.player())
                        .then(Commands.argument("amount", DoubleArgumentType.doubleArg(0.01))
                                .then(Commands.argument("id", StringArgumentType.word())
                                        .suggests((context, builder) -> {
                                            EconomyAPI api = EconomyAPIImpl.getInstance(context.getSource().getServer());
                                            Collection<WalletType> walletTypes = api.getAllWalletTypes();
                                            return SharedSuggestionProvider.suggest(
                                                    walletTypes.stream().map(WalletType::id),
                                                    builder
                                            );
                                        })
                                        .executes(PayCommand::executePay)))));
    }

    private static int executePay(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        CommandSourceStack source = context.getSource();
        ServerPlayer sender = source.getPlayerOrException();
        ServerPlayer target = EntityArgument.getPlayer(context, "player");
        double amount = DoubleArgumentType.getDouble(context, "amount");
        String walletId = StringArgumentType.getString(context, "id");

        if (sender.getUUID().equals(target.getUUID())) {
            source.sendFailure(Component.literal("§c不能向自己转账"));
            return 0;
        }

        EconomyAPI api = EconomyAPIImpl.getInstance(source.getServer());

        try {
            WalletType walletType = api.getWalletType(walletId).orElse(null);
            String displayName = walletType != null ? walletType.displayName().getString() : walletId;

            api.transfer(sender.getUUID(), target.getUUID(), walletId, amount);

            source.sendSuccess(() -> Component.literal("§a成功向 " + target.getName().getString() + " 转账 §e" + String.format("%.2f", amount) + " " + displayName), false);

            target.sendSystemMessage(Component.literal("§a收到来自 " + sender.getName().getString() + " 的转账: §e" + String.format("%.2f", amount) + " " + displayName));

            return 1;
        } catch (WalletTypeNotFoundException e) {
            source.sendFailure(Component.literal("§c钱包类型 '" + walletId + "' 不存在"));
            return 0;
        } catch (InsufficientBalanceException e) {
            source.sendFailure(Component.literal("§c余额不足! 当前余额: §e" + String.format("%.2f", e.getCurrentBalance()) + "§c, 需要: §e" + String.format("%.2f", e.getRequiredAmount())));
            return 0;
        }
    }
}
