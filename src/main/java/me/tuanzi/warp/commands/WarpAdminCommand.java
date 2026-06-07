package me.tuanzi.warp.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import me.tuanzi.warp.WarpEntry;
import me.tuanzi.warp.WarpManager;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.arguments.coordinates.Vec3Argument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.permissions.Permission;
import net.minecraft.server.permissions.PermissionLevel;
import net.minecraft.world.phys.Vec3;

import java.util.concurrent.CompletableFuture;

public class WarpAdminCommand {
    public static void register(CommandDispatcher<CommandSourceStack> dispatcher, CommandBuildContext registryAccess) {
        dispatcher.register(Commands.literal("warpadmin")
                .requires(source -> source.permissions().hasPermission(new Permission.HasCommandLevel(PermissionLevel.GAMEMASTERS)))
                .then(Commands.literal("create")
                        .then(Commands.argument("name", StringArgumentType.word())
                                .then(Commands.argument("pos", Vec3Argument.vec3())
                                        .executes(WarpAdminCommand::createWarp))))
                .then(Commands.literal("delete")
                        .then(Commands.argument("name", StringArgumentType.word())
                                .suggests(WarpAdminCommand::suggestWarps)
                                .executes(WarpAdminCommand::deleteWarp)))
                .then(Commands.literal("list")
                        .executes(WarpAdminCommand::listWarps)));
    }

    private static CompletableFuture<Suggestions> suggestWarps(CommandContext<CommandSourceStack> ctx, SuggestionsBuilder builder) {
        return SharedSuggestionProvider.suggest(WarpManager.getInstance().getWarpData().getWarps().keySet(), builder);
    }

    private static int createWarp(CommandContext<CommandSourceStack> ctx) {
        CommandSourceStack source = ctx.getSource();
        ServerPlayer player = source.getPlayer();
        if (player == null) return 0;

        String name = StringArgumentType.getString(ctx, "name");
        Vec3 pos = Vec3Argument.getVec3(ctx, "pos");

        boolean success = WarpManager.getInstance().createWarp(
                name,
                player.level().dimension(),
                pos.x, pos.y, pos.z,
                player.getYRot(), player.getXRot()
        );

        if (success) {
            source.sendSuccess(() -> Component.literal("§a成功创建地标: " + name), true);
        } else {
            source.sendFailure(Component.literal("§c地标已存在: " + name));
        }
        return 1;
    }

    private static int deleteWarp(CommandContext<CommandSourceStack> ctx) {
        String name = StringArgumentType.getString(ctx, "name");
        boolean success = WarpManager.getInstance().deleteWarp(name);

        if (success) {
            ctx.getSource().sendSuccess(() -> Component.literal("§a成功删除地标: " + name), true);
        } else {
            ctx.getSource().sendFailure(Component.literal("§c地标不存在: " + name));
        }
        return 1;
    }

    private static int listWarps(CommandContext<CommandSourceStack> ctx) {
        CommandSourceStack source = ctx.getSource();
        source.sendSuccess(() -> Component.literal("§e--- 地标列表 ---"), false);

        for (WarpEntry warp : WarpManager.getInstance().getAllWarps()) {
            source.sendSuccess(() -> Component.literal("§b" + warp.getName() + " §7- " + warp.getDimension().identifier().toString() + " (" + String.format("%.1f, %.1f, %.1f", warp.getX(), warp.getY(), warp.getZ()) + ")"), false);
        }
        return 1;
    }
}
