package me.tuanzi.warp.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import me.tuanzi.warp.TeleportManager;
import me.tuanzi.warp.WarpEntry;
import me.tuanzi.warp.WarpManager;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

import java.util.concurrent.CompletableFuture;

public class WarpCommand {
    public static void register(CommandDispatcher<CommandSourceStack> dispatcher, CommandBuildContext registryAccess) {
        dispatcher.register(Commands.literal("warp")
                .then(Commands.argument("name", StringArgumentType.word())
                        .suggests(WarpCommand::suggestWarps)
                        .executes(WarpCommand::execute)));
    }

    private static CompletableFuture<Suggestions> suggestWarps(CommandContext<CommandSourceStack> ctx, SuggestionsBuilder builder) {
        return SharedSuggestionProvider.suggest(WarpManager.getInstance().getWarpData().getWarps().keySet(), builder);
    }

    private static int execute(CommandContext<CommandSourceStack> ctx) {
        ServerPlayer player = ctx.getSource().getPlayer();
        if (player == null) return 0;

        String name = StringArgumentType.getString(ctx, "name");
        WarpEntry warp = WarpManager.getInstance().getWarp(name);

        if (warp == null) {
            player.sendSystemMessage(Component.literal("§c地标不存在！"));
            return 0;
        }

        TeleportManager.requestTeleport(player, warp.toLocationRecord());
        return 1;
    }
}
