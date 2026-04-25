package me.tuanzi.cdk.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import me.tuanzi.cdk.CDKManager;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.server.level.ServerPlayer;

public class CDKCommand {
    public static void register(CommandDispatcher<CommandSourceStack> dispatcher, CommandBuildContext registryAccess) {
        dispatcher.register(Commands.literal("cdk")
                .then(Commands.argument("code", StringArgumentType.word())
                        .executes(CDKCommand::redeem)));
    }

    private static int redeem(CommandContext<CommandSourceStack> ctx) {
        ServerPlayer player = ctx.getSource().getPlayer();
        if (player == null) return 0;

        String code = StringArgumentType.getString(ctx, "code");
        CDKManager.getInstance().redeemCDK(player, code);
        return 1;
    }
}
