package me.tuanzi.title.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import me.tuanzi.title.PlayerTitleData;
import me.tuanzi.title.Title;
import me.tuanzi.title.TitleManager;
import me.tuanzi.title.utils.TitleTranslationHelper;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.server.level.ServerPlayer;

import java.util.Set;
import java.util.concurrent.CompletableFuture;

public class TitleCommand {
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
        return SharedSuggestionProvider.suggest(data.getUnlockedTitles(), builder);
    }

    private static int listTitles(CommandContext<CommandSourceStack> ctx) {
        ServerPlayer player = ctx.getSource().getPlayer();
        if (player == null) return 0;

        PlayerTitleData data = TitleManager.getInstance().getTitleData().getOrCreatePlayerTitleData(player.getUUID());
        Set<String> unlocked = data.getUnlockedTitles();

        TitleTranslationHelper.sendSuccess(ctx.getSource(), "--- 你拥有的称号 ---");
        if (unlocked.isEmpty()) {
            ctx.getSource().sendSuccess(() -> Component.literal("§7你目前没有任何称号。"), false);
            return 1;
        }

        for (String id : unlocked) {
            Title title = TitleManager.getInstance().getTitleData().getTitles().get(id);
            if (title == null) continue;

            boolean equipped = id.equals(data.getEquippedTitle());
            Component titleComp = title.displayName().copy()
                    .append(equipped ? " §a[已佩戴]" : " §e[点击佩戴]")
                    .withStyle(style -> style
                            .withHoverEvent(new HoverEvent.ShowText(Component.literal("点击装备称号: " + id)))
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
            TitleTranslationHelper.sendSuccess(ctx.getSource(), "你已佩戴称号: " + title.displayName().getString());
        } else {
            TitleTranslationHelper.sendFailure(ctx.getSource(), "称号不存在或未解锁。");
        }
        
        return 1;
    }

    private static int clearTitle(CommandContext<CommandSourceStack> ctx) {
        ServerPlayer player = ctx.getSource().getPlayer();
        if (player == null) return 0;

        TitleManager.getInstance().equipTitle(player, null);
        TitleTranslationHelper.sendSuccess(ctx.getSource(), "你已卸下称号。");
        return 1;
    }
}
