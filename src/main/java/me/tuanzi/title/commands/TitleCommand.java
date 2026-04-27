package me.tuanzi.title.commands;

import com.mojang.brigadier.CommandDispatcher;
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
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.server.level.ServerPlayer;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

public class TitleCommand {
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher, CommandBuildContext registryAccess) {
        dispatcher.register(Commands.literal("titles")
                .executes(TitleCommand::listTitles)
                .then(Commands.literal("list")
                        .executes(TitleCommand::listTitles))
                .then(Commands.literal("clear")
                        .executes(TitleCommand::clearTitle))
                .then(Commands.literal("set")
                        .then(Commands.argument("id", StringArgumentType.word())
                                .suggests(TitleCommand::suggestUnlockedTitles)
                                .executes(TitleCommand::setTitle))));
    }

    private static CompletableFuture<Suggestions> suggestUnlockedTitles(CommandContext<CommandSourceStack> ctx, SuggestionsBuilder builder) {
        ServerPlayer player = ctx.getSource().getPlayer();
        if (player == null) return builder.buildFuture();

        PlayerTitleData data = TitleManager.getInstance().getTitleData().getOrCreatePlayerTitleData(player.getUUID());
        return SharedSuggestionProvider.suggest(data.getTitleExpiries().keySet(), builder);
    }

    private static int listTitles(CommandContext<CommandSourceStack> ctx) {
        ServerPlayer player = ctx.getSource().getPlayer();
        if (player == null) return 0;

        PlayerTitleData data = TitleManager.getInstance().getTitleData().getOrCreatePlayerTitleData(player.getUUID());
        Map<String, Long> expiries = data.getTitleExpiries();

        ServerTranslationHelper.sendSuccess(ctx.getSource(), "title.command.list.header");
        if (expiries.isEmpty()) {
            ServerTranslationHelper.sendSuccess(ctx.getSource(), "title.command.list.empty");
            return 1;
        }

        String lang = ServerTranslationHelper.getLanguage(player);
        long now = System.currentTimeMillis();

        for (Map.Entry<String, Long> entry : expiries.entrySet()) {
            String id = entry.getKey();
            long expiry = entry.getValue();

            Title title = TitleManager.getInstance().getTitleData().getTitles().get(id);
            if (title == null || title.isExpired()) continue;
            if (expiry != -1 && now > expiry) continue;

            boolean equipped = id.equals(data.getEquippedTitle());
            String suffix = ServerTranslationHelper.translate(equipped ? "title.command.list.equipped" : "title.command.list.click_equip", lang);
            
            String expiryStr;
            if (expiry == -1) {
                expiryStr = "永久";
            } else {
                expiryStr = DATE_FORMATTER.format(LocalDateTime.ofInstant(java.time.Instant.ofEpochMilli(expiry), ZoneOffset.ofHours(8)));
            }
            
            String hover = ServerTranslationHelper.translate("title.command.list.hover", lang, id) + "\n§7有效期至: §e" + expiryStr;

            Component titleComp = title.displayName().copy()
                    .append(suffix)
                    .withStyle(style -> style
                            .withHoverEvent(new HoverEvent.ShowText(Component.literal(hover)))
                            .withClickEvent(new ClickEvent.RunCommand("/titles set " + id)));
            
            ctx.getSource().sendSuccess(() -> titleComp, false);
        }

        return 1;
    }

    private static int setTitle(CommandContext<CommandSourceStack> ctx) {
        ServerPlayer player = ctx.getSource().getPlayer();
        if (player == null) return 0;

        String id = StringArgumentType.getString(ctx, "id");
        TitleManager.getInstance().equipTitle(player, id);
        
        Title title = TitleManager.getInstance().getTitleData().getTitles().get(id);
        if (title != null) {
            ServerTranslationHelper.sendSuccess(ctx.getSource(), "title.command.set.success", title.displayName().getString());
        } else {
            ServerTranslationHelper.sendFailure(ctx.getSource(), "title.command.set.failure");
        }
        
        return 1;
    }

    private static int clearTitle(CommandContext<CommandSourceStack> ctx) {
        ServerPlayer player = ctx.getSource().getPlayer();
        if (player == null) return 0;

        TitleManager.getInstance().equipTitle(player, null);
        ServerTranslationHelper.sendSuccess(ctx.getSource(), "title.command.clear.success");
        return 1;
    }
}
