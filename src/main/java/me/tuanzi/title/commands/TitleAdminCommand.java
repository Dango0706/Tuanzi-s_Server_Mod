package me.tuanzi.title.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import me.tuanzi.title.Title;
import me.tuanzi.title.TitleManager;
import me.tuanzi.title.utils.TitleTranslationHelper;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.permissions.Permission;
import net.minecraft.server.permissions.PermissionLevel;
import net.minecraft.world.item.ItemStack;

import java.util.Collection;
import java.util.concurrent.CompletableFuture;

public class TitleAdminCommand {
    public static void register(CommandDispatcher<CommandSourceStack> dispatcher, CommandBuildContext registryAccess) {
        dispatcher.register(Commands.literal("titleadmin")
                .requires(source -> source.permissions().hasPermission(new Permission.HasCommandLevel(PermissionLevel.GAMEMASTERS)))
                .then(Commands.literal("create")
                        .then(Commands.argument("id", StringArgumentType.word())
                                .then(Commands.argument("displayName", StringArgumentType.greedyString())
                                        .executes(TitleAdminCommand::createTitle))))
                .then(Commands.literal("list")
                        .executes(TitleAdminCommand::listAllTitles))
                .then(Commands.literal("give")
                        .then(Commands.argument("targets", EntityArgument.players())
                                .then(Commands.argument("id", StringArgumentType.word())
                                        .suggests(TitleAdminCommand::suggestAllTitles)
                                        .executes(TitleAdminCommand::giveTitle))))
                .then(Commands.literal("giveall")
                        .then(Commands.argument("id", StringArgumentType.word())
                                .suggests(TitleAdminCommand::suggestAllTitles)
                                .executes(TitleAdminCommand::giveTitleToAll)))
                .then(Commands.literal("delete")
                        .then(Commands.argument("id", StringArgumentType.word())
                                .suggests(TitleAdminCommand::suggestAllTitles)
                                .executes(TitleAdminCommand::deleteTitle)))
                .then(Commands.literal("getitem")
                        .then(Commands.argument("id", StringArgumentType.word())
                                .suggests(TitleAdminCommand::suggestAllTitles)
                                .executes(TitleAdminCommand::getItem))));
    }

    private static CompletableFuture<Suggestions> suggestAllTitles(CommandContext<CommandSourceStack> ctx, SuggestionsBuilder builder) {
        return SharedSuggestionProvider.suggest(TitleManager.getInstance().getTitleData().getTitles().keySet(), builder);
    }

    private static int createTitle(CommandContext<CommandSourceStack> ctx) {
        String id = StringArgumentType.getString(ctx, "id");
        String displayNameStr = StringArgumentType.getString(ctx, "displayName").replace("&", "§");
        Component displayName = Component.literal(displayNameStr);

        Title title = new Title(id, displayName);
        TitleManager.getInstance().getTitleData().addTitle(title);
        
        TitleTranslationHelper.sendSuccess(ctx.getSource(), "成功创建称号: " + id + " (" + displayNameStr + ")");
        
        // Give the admin the item
        ServerPlayer player = ctx.getSource().getPlayer();
        if (player != null) {
            ItemStack item = TitleManager.getInstance().createTitleItem(id);
            if (!player.getInventory().add(item)) {
                player.drop(item, false);
            }
        }
        
        return 1;
    }

    private static int listAllTitles(CommandContext<CommandSourceStack> ctx) {
        Collection<Title> titles = TitleManager.getInstance().getTitleData().getTitles().values();
        TitleTranslationHelper.sendSuccess(ctx.getSource(), "--- 服务器所有称号 ---");
        
        for (Title title : titles) {
            Component info = Component.literal("§b" + title.id() + ": ")
                    .append(title.displayName())
                    .append(" §e[获取命名牌]")
                    .withStyle(style -> style
                            .withHoverEvent(new HoverEvent.ShowText(Component.literal("点击获取该称号的命名牌")))
                            .withClickEvent(new ClickEvent.RunCommand("/titleadmin getitem " + title.id())));
            ctx.getSource().sendSuccess(() -> info, false);
        }
        return 1;
    }

    private static int giveTitle(CommandContext<CommandSourceStack> ctx) {
        try {
            Collection<ServerPlayer> targets = EntityArgument.getPlayers(ctx, "targets");
            String id = StringArgumentType.getString(ctx, "id");
            
            for (ServerPlayer target : targets) {
                TitleManager.getInstance().giveTitle(target.getUUID(), id, true);
            }
            
            TitleTranslationHelper.sendSuccess(ctx.getSource(), "已向 " + targets.size() + " 名玩家发放称号: " + id);
        } catch (Exception e) {
            TitleTranslationHelper.sendFailure(ctx.getSource(), "发放失败: " + e.getMessage());
        }
        return 1;
    }

    private static int giveTitleToAll(CommandContext<CommandSourceStack> ctx) {
        String id = StringArgumentType.getString(ctx, "id");
        TitleManager.getInstance().giveTitleToAll(id);
        TitleTranslationHelper.sendSuccess(ctx.getSource(), "已向全服（含离线）发放称号: " + id);
        return 1;
    }

    private static int deleteTitle(CommandContext<CommandSourceStack> ctx) {
        String id = StringArgumentType.getString(ctx, "id");
        TitleManager.getInstance().deleteTitle(id);
        TitleTranslationHelper.sendSuccess(ctx.getSource(), "已成功删除称号: " + id);
        return 1;
    }

    private static int getItem(CommandContext<CommandSourceStack> ctx) {
        ServerPlayer player = ctx.getSource().getPlayer();
        if (player == null) return 0;
        
        String id = StringArgumentType.getString(ctx, "id");
        ItemStack item = TitleManager.getInstance().createTitleItem(id);
        if (item.isEmpty()) {
            TitleTranslationHelper.sendFailure(ctx.getSource(), "称号不存在。");
            return 0;
        }
        
        if (!player.getInventory().add(item)) {
            player.drop(item, false);
        }
        TitleTranslationHelper.sendSuccess(ctx.getSource(), "已获得称号命名牌: " + id);
        return 1;
    }
}
