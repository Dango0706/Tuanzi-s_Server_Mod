package me.tuanzi.title.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import me.tuanzi.economy.utils.ServerTranslationHelper;
import me.tuanzi.title.PlayerTitleData;
import me.tuanzi.title.Title;
import me.tuanzi.title.TitleManager;
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

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.Collection;
import java.util.concurrent.CompletableFuture;

public class TitleAdminCommand {
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd-HH:mm:ss");

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher, CommandBuildContext registryAccess) {
        dispatcher.register(Commands.literal("titleadmin")
                .requires(source -> source.permissions().hasPermission(new Permission.HasCommandLevel(PermissionLevel.GAMEMASTERS)))
                .then(Commands.literal("create")
                        .then(Commands.argument("id", StringArgumentType.word())
                                .then(Commands.argument("displayName", StringArgumentType.greedyString())
                                        .executes(TitleAdminCommand::createTitle))))
                .then(Commands.literal("setexpiry")
                        .then(Commands.argument("id", StringArgumentType.word())
                                .suggests(TitleAdminCommand::suggestAllTitles)
                                .then(Commands.argument("date", StringArgumentType.string())
                                        .executes(TitleAdminCommand::setGlobalExpiry))))
                .then(Commands.literal("list")
                        .executes(TitleAdminCommand::listAllTitles))
                .then(Commands.literal("give")
                        .then(Commands.argument("targets", EntityArgument.players())
                                .then(Commands.argument("id", StringArgumentType.word())
                                        .suggests(TitleAdminCommand::suggestAllTitles)
                                        .executes(ctx -> giveTitle(ctx, -1))
                                        .then(Commands.argument("days", IntegerArgumentType.integer(1))
                                                .executes(ctx -> giveTitle(ctx, IntegerArgumentType.getInteger(ctx, "days")))))))
                .then(Commands.literal("giveall")
                        .then(Commands.argument("id", StringArgumentType.word())
                                .suggests(TitleAdminCommand::suggestAllTitles)
                                .executes(ctx -> giveTitleToAll(ctx, -1))
                                .then(Commands.argument("days", IntegerArgumentType.integer(1))
                                        .executes(ctx -> giveTitleToAll(ctx, IntegerArgumentType.getInteger(ctx, "days"))))))
                .then(Commands.literal("modifyplayer")
                        .then(Commands.argument("targets", EntityArgument.players())
                                .then(Commands.argument("id", StringArgumentType.word())
                                        .suggests(TitleAdminCommand::suggestAllTitles)
                                        .then(Commands.literal("set")
                                                .then(Commands.argument("days", IntegerArgumentType.integer(-1))
                                                        .executes(ctx -> modifyPlayer(ctx, "set"))))
                                        .then(Commands.literal("add")
                                                .then(Commands.argument("days", IntegerArgumentType.integer(1))
                                                        .executes(ctx -> modifyPlayer(ctx, "add"))))
                                        .then(Commands.literal("remove")
                                                .then(Commands.argument("days", IntegerArgumentType.integer(1))
                                                        .executes(ctx -> modifyPlayer(ctx, "remove")))))))
                .then(Commands.literal("delete")
                        .then(Commands.argument("id", StringArgumentType.word())
                                .suggests(TitleAdminCommand::suggestAllTitles)
                                .executes(TitleAdminCommand::deleteTitle)))
                .then(Commands.literal("getitem")
                        .then(Commands.argument("id", StringArgumentType.word())
                                .suggests(TitleAdminCommand::suggestAllTitles)
                                .executes(ctx -> getItem(ctx, -1))
                                .then(Commands.argument("days", IntegerArgumentType.integer(1))
                                        .executes(ctx -> getItem(ctx, IntegerArgumentType.getInteger(ctx, "days")))))));
    }

    private static CompletableFuture<Suggestions> suggestAllTitles(CommandContext<CommandSourceStack> ctx, SuggestionsBuilder builder) {
        return SharedSuggestionProvider.suggest(TitleManager.getInstance().getTitleData().getTitles().keySet(), builder);
    }

    private static int createTitle(CommandContext<CommandSourceStack> ctx) {
        String id = StringArgumentType.getString(ctx, "id");
        String displayNameStr = StringArgumentType.getString(ctx, "displayName").replace("&", "§");
        Component displayName = Component.literal(displayNameStr);

        Title title = new Title(id, displayName, -1);
        TitleManager.getInstance().getTitleData().addTitle(title);
        
        ServerTranslationHelper.sendSuccess(ctx.getSource(), "title.admin.create.success", id, displayNameStr);
        
        // Give the admin the item
        ServerPlayer player = ctx.getSource().getPlayer();
        if (player != null) {
            ItemStack item = TitleManager.getInstance().createTitleItem(id, -1);
            if (!player.getInventory().add(item)) {
                player.drop(item, false);
            }
        }
        
        return 1;
    }

    private static int setGlobalExpiry(CommandContext<CommandSourceStack> ctx) {
        String id = StringArgumentType.getString(ctx, "id");
        String dateStr = StringArgumentType.getString(ctx, "date");
        
        long timestamp;
        if (dateStr.equalsIgnoreCase("permanent") || dateStr.equals("-1")) {
            timestamp = -1;
        } else {
            try {
                LocalDateTime dateTime = LocalDateTime.parse(dateStr, DATE_FORMATTER);
                timestamp = dateTime.toInstant(ZoneOffset.ofHours(8)).toEpochMilli();
            } catch (Exception e) {
                ctx.getSource().sendFailure(Component.literal("§c日期格式错误！请使用: yyyy-MM-dd-HH:mm:ss 或 permanent"));
                return 0;
            }
        }

        Title oldTitle = TitleManager.getInstance().getTitleData().getTitles().get(id);
        if (oldTitle == null) {
            ServerTranslationHelper.sendFailure(ctx.getSource(), "title.command.set.failure");
            return 0;
        }

        Title newTitle = new Title(id, oldTitle.displayName(), timestamp);
        TitleManager.getInstance().getTitleData().addTitle(newTitle);
        
        String feedback = timestamp == -1 ? "永久" : dateStr;
        ctx.getSource().sendSuccess(() -> Component.literal("§a已设置称号 §e" + id + " §a的全局有效期为: §b" + feedback), true);
        return 1;
    }

    private static int listAllTitles(CommandContext<CommandSourceStack> ctx) {
        Collection<Title> titles = TitleManager.getInstance().getTitleData().getTitles().values();
        ServerTranslationHelper.sendSuccess(ctx.getSource(), "title.admin.list.header");
        
        String lang = ServerTranslationHelper.getLanguage(ctx.getSource());
        String btnText = ServerTranslationHelper.translate("title.admin.list.item_btn", lang);
        String hoverText = ServerTranslationHelper.translate("title.admin.list.item_hover", lang);

        for (Title title : titles) {
            String expiryInfo = title.globalExpiry() == -1 ? "" : " §7(到期: " + DATE_FORMATTER.format(LocalDateTime.ofInstant(java.time.Instant.ofEpochMilli(title.globalExpiry()), ZoneOffset.ofHours(8))) + ")";
            Component info = Component.literal("§b" + title.id() + ": ")
                    .append(title.displayName())
                    .append(expiryInfo)
                    .append(btnText)
                    .withStyle(style -> style
                            .withHoverEvent(new HoverEvent.ShowText(Component.literal(hoverText)))
                            .withClickEvent(new ClickEvent.RunCommand("/titleadmin getitem " + title.id())));
            ctx.getSource().sendSuccess(() -> info, false);
        }
        return 1;
    }

    private static int giveTitle(CommandContext<CommandSourceStack> ctx, int days) {
        try {
            Collection<ServerPlayer> targets = EntityArgument.getPlayers(ctx, "targets");
            String id = StringArgumentType.getString(ctx, "id");
            long duration = days == -1 ? -1 : (long) days * 24 * 60 * 60 * 1000;
            
            for (ServerPlayer target : targets) {
                TitleManager.getInstance().giveTitle(target.getUUID(), id, duration, true);
            }
            
            ServerTranslationHelper.sendSuccess(ctx.getSource(), "title.admin.give.success", targets.size(), id);
        } catch (Exception e) {
            ServerTranslationHelper.sendFailure(ctx.getSource(), "title.admin.give.failure", e.getMessage());
        }
        return 1;
    }

    private static int giveTitleToAll(CommandContext<CommandSourceStack> ctx, int days) {
        String id = StringArgumentType.getString(ctx, "id");
        long duration = days == -1 ? -1 : (long) days * 24 * 60 * 60 * 1000;
        TitleManager.getInstance().giveTitleToAll(id, duration);
        ServerTranslationHelper.sendSuccess(ctx.getSource(), "title.admin.give.success", -1, id);
        return 1;
    }

    private static int modifyPlayer(CommandContext<CommandSourceStack> ctx, String mode) {
        try {
            Collection<ServerPlayer> targets = EntityArgument.getPlayers(ctx, "targets");
            String id = StringArgumentType.getString(ctx, "id");
            int days = IntegerArgumentType.getInteger(ctx, "days");
            
            long now = System.currentTimeMillis();
            
            for (ServerPlayer target : targets) {
                PlayerTitleData data = TitleManager.getInstance().getTitleData().getOrCreatePlayerTitleData(target.getUUID());
                long currentExpiry = data.getTitleExpiries().getOrDefault(id, 0L);
                
                long newExpiry;
                if (mode.equals("set")) {
                    newExpiry = days == -1 ? -1 : now + (long) days * 24 * 60 * 60 * 1000;
                } else if (mode.equals("add")) {
                    long start = (currentExpiry == -1 || currentExpiry < now) ? now : currentExpiry;
                    newExpiry = start + (long) days * 24 * 60 * 60 * 1000;
                } else { // remove
                    if (currentExpiry == -1 || currentExpiry < now) {
                        newExpiry = 0; // Already expired or permanent (we don't remove from permanent easily here)
                    } else {
                        newExpiry = currentExpiry - (long) days * 24 * 60 * 60 * 1000;
                        if (newExpiry < now) newExpiry = now; // Set to now if it would expire in the past
                    }
                }
                
                data.unlockTitle(id, newExpiry);
                TitleManager.getInstance().updatePlayerScoreboard(target);
            }
            
            ctx.getSource().sendSuccess(() -> Component.literal("§a已成功修改 §e" + targets.size() + " §a名玩家的称号 §b" + id + " §a时间。"), true);
            TitleManager.getInstance().getTitleData().removeTitle(""); // Trigger dirty indirectly if needed, or better:
            // Actually, TitleStateSaver.getServerState(ctx.getSource().getServer()).setDirty();
        } catch (Exception e) {
            ctx.getSource().sendFailure(Component.literal("§c修改失败: " + e.getMessage()));
        }
        return 1;
    }

    private static int deleteTitle(CommandContext<CommandSourceStack> ctx) {
        String id = StringArgumentType.getString(ctx, "id");
        TitleManager.getInstance().deleteTitle(id);
        ServerTranslationHelper.sendSuccess(ctx.getSource(), "title.admin.delete.success", id);
        return 1;
    }

    private static int getItem(CommandContext<CommandSourceStack> ctx, int days) {
        ServerPlayer player = ctx.getSource().getPlayer();
        if (player == null) return 0;
        
        String id = StringArgumentType.getString(ctx, "id");
        long duration = days == -1 ? -1 : (long) days * 24 * 60 * 60 * 1000;
        ItemStack item = TitleManager.getInstance().createTitleItem(id, duration);
        if (item.isEmpty()) {
            ServerTranslationHelper.sendFailure(ctx.getSource(), "title.command.set.failure");
            return 0;
        }
        
        if (!player.getInventory().add(item)) {
            player.drop(item, false);
        }
        ServerTranslationHelper.sendSuccess(ctx.getSource(), "title.admin.list.item_hover"); // Reusing similar key
        return 1;
    }
}
