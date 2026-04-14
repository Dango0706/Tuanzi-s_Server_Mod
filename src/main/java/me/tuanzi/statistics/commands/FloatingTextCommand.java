package me.tuanzi.statistics.commands;

import me.tuanzi.statistics.floatingtext.FloatingTextData;
import me.tuanzi.statistics.floatingtext.FloatingTextManager;
import me.tuanzi.statistics.floatingtext.LeaderboardFormatter;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.arguments.coordinates.Vec3Argument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.permissions.Permission;
import net.minecraft.server.permissions.PermissionLevel;
import net.minecraft.world.phys.Vec3;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;

public class FloatingTextCommand {
    
    private static final Set<String> VALID_COLORS = Set.of(
        "black", "dark_blue", "dark_green", "dark_aqua", "dark_red", 
        "dark_purple", "gold", "gray", "dark_gray", "blue", 
        "green", "aqua", "red", "light_purple", "yellow", "white"
    );
    
    public static void register(CommandDispatcher<CommandSourceStack> dispatcher, CommandBuildContext registryAccess) {
        dispatcher.register(Commands.literal("floatingtext")
            .requires(source -> source.permissions().hasPermission(new Permission.HasCommandLevel(PermissionLevel.GAMEMASTERS)))
            .then(Commands.literal("create")
                .then(Commands.argument("id", StringArgumentType.word())
                    .then(Commands.argument("pos", Vec3Argument.vec3())
                        .executes(FloatingTextCommand::createFloatingText)
                    )
                )
            )
            .then(Commands.literal("setcontent")
                .then(Commands.argument("id", StringArgumentType.word())
                    .suggests(FloatingTextCommand::suggestFloatingTextIds)
                    .then(Commands.argument("statType", StringArgumentType.string())
                        .suggests(FloatingTextCommand::suggestStatTypes)
                        .then(Commands.argument("displayName", StringArgumentType.greedyString())
                            .executes(FloatingTextCommand::setContent)
                        )
                    )
                )
            )
            .then(Commands.literal("updateinterval")
                .then(Commands.argument("id", StringArgumentType.word())
                    .suggests(FloatingTextCommand::suggestFloatingTextIds)
                    .then(Commands.argument("ticks", IntegerArgumentType.integer(1, 1200))
                        .executes(FloatingTextCommand::setUpdateInterval)
                    )
                )
            )
            .then(Commands.literal("move")
                .then(Commands.argument("id", StringArgumentType.word())
                    .suggests(FloatingTextCommand::suggestFloatingTextIds)
                    .then(Commands.argument("pos", Vec3Argument.vec3())
                        .executes(FloatingTextCommand::moveFloatingText)
                    )
                )
            )
            .then(Commands.literal("color")
                .then(Commands.argument("id", StringArgumentType.word())
                    .suggests(FloatingTextCommand::suggestFloatingTextIds)
                    .then(Commands.argument("color", StringArgumentType.string())
                        .suggests(FloatingTextCommand::suggestColors)
                        .executes(FloatingTextCommand::setColor)
                    )
                )
            )
            .then(Commands.literal("delete")
                .then(Commands.argument("id", StringArgumentType.word())
                    .suggests(FloatingTextCommand::suggestFloatingTextIds)
                    .executes(FloatingTextCommand::deleteFloatingText)
                )
            )
            .then(Commands.literal("list")
                .executes(FloatingTextCommand::listFloatingTexts)
            )
            .then(Commands.literal("info")
                .then(Commands.argument("id", StringArgumentType.word())
                    .suggests(FloatingTextCommand::suggestFloatingTextIds)
                    .executes(FloatingTextCommand::showInfo)
                )
            )
        );
    }
    
    private static int createFloatingText(CommandContext<CommandSourceStack> context) {
        String id = StringArgumentType.getString(context, "id");
        CommandSourceStack source = context.getSource();
        ServerLevel level = source.getLevel();
        Vec3 pos = Vec3Argument.getVec3(context, "pos");
        
        FloatingTextManager manager = FloatingTextManager.getInstance();
        if (manager.createFloatingText(id, level, pos.x, pos.y, pos.z)) {
            me.tuanzi.statistics.util.StatsTranslationHelper.sendSuccess(source, "floatingtext.created", id, (int)pos.x, (int)pos.y, (int)pos.z);
            return 1;
        } else {
            me.tuanzi.statistics.util.StatsTranslationHelper.sendFailure(source, "floatingtext.already_exists", id);
            return 0;
        }
    }
    
    private static int setContent(CommandContext<CommandSourceStack> context) {
        String id = StringArgumentType.getString(context, "id");
        String statType = StringArgumentType.getString(context, "statType");
        String displayName = StringArgumentType.getString(context, "displayName");
        
        CommandSourceStack source = context.getSource();
        FloatingTextManager manager = FloatingTextManager.getInstance();
        
        if (!LeaderboardFormatter.getSupportedStatTypes().contains(statType)) {
            me.tuanzi.statistics.util.StatsTranslationHelper.sendFailure(source, "floatingtext.unsupported_stat", statType);
            return 0;
        }
        
        if (manager.setContent(id, statType, displayName)) {
            me.tuanzi.statistics.util.StatsTranslationHelper.sendSuccess(source, "floatingtext.content_set", id, displayName + " (" + statType + ")");
            return 1;
        } else {
            me.tuanzi.statistics.util.StatsTranslationHelper.sendFailure(source, "floatingtext.not_found", id);
            return 0;
        }
    }
    
    private static int setUpdateInterval(CommandContext<CommandSourceStack> context) {
        String id = StringArgumentType.getString(context, "id");
        int ticks = IntegerArgumentType.getInteger(context, "ticks");
        
        CommandSourceStack source = context.getSource();
        FloatingTextManager manager = FloatingTextManager.getInstance();
        
        if (manager.setUpdateInterval(id, ticks)) {
            me.tuanzi.statistics.util.StatsTranslationHelper.sendSuccess(source, "floatingtext.interval_set", id, ticks);
            return 1;
        } else {
            me.tuanzi.statistics.util.StatsTranslationHelper.sendFailure(source, "floatingtext.not_found", id);
            return 0;
        }
    }
    
    private static int moveFloatingText(CommandContext<CommandSourceStack> context) {
        String id = StringArgumentType.getString(context, "id");
        CommandSourceStack source = context.getSource();
        FloatingTextManager manager = FloatingTextManager.getInstance();
        
        if (manager.getFloatingText(id) == null) {
            me.tuanzi.statistics.util.StatsTranslationHelper.sendFailure(source, "floatingtext.not_found", id);
            return 0;
        }
        
        Vec3 pos = Vec3Argument.getVec3(context, "pos");
        if (manager.moveFloatingText(id, source.getLevel(), pos.x, pos.y, pos.z, false)) {
            me.tuanzi.statistics.util.StatsTranslationHelper.sendSuccess(source, "floatingtext.moved", id, (int)pos.x, (int)pos.y, (int)pos.z);
            return 1;
        } else {
            me.tuanzi.statistics.util.StatsTranslationHelper.sendFailure(source, "floatingtext.move_failed");
            return 0;
        }
    }
    
    private static int setColor(CommandContext<CommandSourceStack> context) {
        String id = StringArgumentType.getString(context, "id");
        String color = StringArgumentType.getString(context, "color");
        CommandSourceStack source = context.getSource();
        FloatingTextManager manager = FloatingTextManager.getInstance();
        
        if (!VALID_COLORS.contains(color.toLowerCase())) {
            me.tuanzi.statistics.util.StatsTranslationHelper.sendFailure(source, "floatingtext.invalid_color", color, String.join(", ", VALID_COLORS));
            return 0;
        }
        
        if (manager.setColor(id, color.toLowerCase())) {
            me.tuanzi.statistics.util.StatsTranslationHelper.sendSuccess(source, "floatingtext.color_set", id, color);
            return 1;
        } else {
            me.tuanzi.statistics.util.StatsTranslationHelper.sendFailure(source, "floatingtext.not_found", id);
            return 0;
        }
    }
    
    private static int deleteFloatingText(CommandContext<CommandSourceStack> context) {
        String id = StringArgumentType.getString(context, "id");
        CommandSourceStack source = context.getSource();
        FloatingTextManager manager = FloatingTextManager.getInstance();
        
        if (manager.deleteFloatingText(id)) {
            me.tuanzi.statistics.util.StatsTranslationHelper.sendSuccess(source, "floatingtext.deleted", id);
            return 1;
        } else {
            me.tuanzi.statistics.util.StatsTranslationHelper.sendFailure(source, "floatingtext.not_found", id);
            return 0;
        }
    }
    
    private static int listFloatingTexts(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();
        FloatingTextManager manager = FloatingTextManager.getInstance();
        Map<String, FloatingTextData> texts = manager.getAllFloatingTexts();
        
        if (texts.isEmpty()) {
            me.tuanzi.statistics.util.StatsTranslationHelper.sendSuccess(source, "floatingtext.list_empty");
            return 0;
        }
        
        me.tuanzi.statistics.util.StatsTranslationHelper.sendSuccess(source, "floatingtext.list_header");
        for (Map.Entry<String, FloatingTextData> entry : texts.entrySet()) {
            String id = entry.getKey();
            FloatingTextData data = entry.getValue();
            String statInfo = data.getStatType() != null ? 
                LeaderboardFormatter.getStatDisplayName(data.getStatType()) : "N/A";
            source.sendSuccess(() -> Component.literal("§f- §b" + id + " §7| " + statInfo + 
                " §7| " + String.format("pos: (%.1f, %.1f, %.1f)", data.getX(), data.getY(), data.getZ())), false);
        }
        me.tuanzi.statistics.util.StatsTranslationHelper.sendSuccess(source, "floatingtext.list_total", texts.size());
        return texts.size();
    }
    
    private static int showInfo(CommandContext<CommandSourceStack> context) {
        String id = StringArgumentType.getString(context, "id");
        CommandSourceStack source = context.getSource();
        FloatingTextManager manager = FloatingTextManager.getInstance();
        FloatingTextData data = manager.getFloatingText(id);
        
        if (data == null) {
            me.tuanzi.statistics.util.StatsTranslationHelper.sendFailure(source, "floatingtext.not_found", id);
            return 0;
        }
        
        String lang = me.tuanzi.statistics.util.StatsTranslationHelper.getLanguage(source);
        source.sendSuccess(() -> Component.literal("§e========== [" + me.tuanzi.statistics.util.StatsTranslationHelper.translate("floatingtext.header", lang) + ": " + id + "] =========="), false);
        source.sendSuccess(() -> Component.literal("§bID: §f" + data.getId()), false);
        source.sendSuccess(() -> Component.literal("§b" + me.tuanzi.statistics.util.StatsTranslationHelper.translate("floatingtext.info_world", lang) + ": §f" + data.getWorldName()), false);
        source.sendSuccess(() -> Component.literal("§b" + me.tuanzi.statistics.util.StatsTranslationHelper.translate("floatingtext.info_position", lang) + ": §f(" + 
            String.format("%.1f, %.1f, %.1f", data.getX(), data.getY(), data.getZ()) + ")"), false);
        
        String statTypeDisplayName = data.getStatType() != null ? 
            LeaderboardFormatter.getStatDisplayName(data.getStatType()) : "N/A";
        source.sendSuccess(() -> Component.literal("§b" + me.tuanzi.statistics.util.StatsTranslationHelper.translate("floatingtext.info_stat_type", lang) + ": §f" + statTypeDisplayName), false);
        source.sendSuccess(() -> Component.literal("§b" + me.tuanzi.statistics.util.StatsTranslationHelper.translate("floatingtext.info_display_name", lang) + ": §f" + (data.getDisplayName() != null ? data.getDisplayName() : "N/A")), false);
        source.sendSuccess(() -> Component.literal("§b" + me.tuanzi.statistics.util.StatsTranslationHelper.translate("floatingtext.info_color", lang) + ": §f" + data.getColor()), false);
        source.sendSuccess(() -> Component.literal("§b" + me.tuanzi.statistics.util.StatsTranslationHelper.translate("floatingtext.info_interval", lang) + ": §f" + data.getUpdateInterval() + " tick"), false);
        
        if (data.getCreatedTime() > 0) {
            String createdTime = Instant.ofEpochMilli(data.getCreatedTime())
                .atZone(ZoneId.systemDefault())
                .format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
            source.sendSuccess(() -> Component.literal("§b" + me.tuanzi.statistics.util.StatsTranslationHelper.translate("floatingtext.info_created", lang) + ": §f" + createdTime), false);
        }
        
        return 1;
    }
    
    private static CompletableFuture<Suggestions> suggestFloatingTextIds(CommandContext<CommandSourceStack> context, SuggestionsBuilder builder) {
        FloatingTextManager manager = FloatingTextManager.getInstance();
        return SharedSuggestionProvider.suggest(manager.getAllFloatingTexts().keySet(), builder);
    }
    
    private static CompletableFuture<Suggestions> suggestStatTypes(CommandContext<CommandSourceStack> context, SuggestionsBuilder builder) {
        return SharedSuggestionProvider.suggest(LeaderboardFormatter.getSupportedStatTypes(), builder);
    }
    
    private static CompletableFuture<Suggestions> suggestColors(CommandContext<CommandSourceStack> context, SuggestionsBuilder builder) {
        return SharedSuggestionProvider.suggest(VALID_COLORS, builder);
    }
}
