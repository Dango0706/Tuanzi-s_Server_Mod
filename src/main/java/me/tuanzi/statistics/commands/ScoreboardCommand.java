package me.tuanzi.statistics.commands;

import me.tuanzi.statistics.scoreboard.ScoreboardManager;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.network.chat.Component;
import net.minecraft.server.permissions.Permission;
import net.minecraft.server.permissions.PermissionLevel;

public class ScoreboardCommand {
    private static final String[] STAT_TYPES = {
            "playTime", "playTimeMinutes", "playTimeHours",
            "distanceTraveled", "blocksPlaced", "blocksBroken",
            "kills", "deaths", "damageDealt", "damageTaken"
    };

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher, CommandBuildContext registryAccess) {
        dispatcher.register(Commands.literal("statsboard")
                .requires(source -> source.permissions().hasPermission(new Permission.HasCommandLevel(PermissionLevel.GAMEMASTERS)))
                .then(Commands.literal("create")
                        .then(Commands.argument("statType", StringArgumentType.string())
                                .suggests((context, builder) ->
                                        SharedSuggestionProvider.suggest(STAT_TYPES, builder))
                                .executes(context -> {
                                    String statType = StringArgumentType.getString(context, "statType");
                                    createScoreboard(context.getSource(), statType);
                                    return 1;
                                })
                        )
                )
                .then(Commands.literal("startrotation")
                        .executes(context -> {
                            startRotation(context.getSource());
                            return 1;
                        })
                )
                .then(Commands.literal("stoprotation")
                        .executes(context -> {
                            stopRotation(context.getSource());
                            return 1;
                        })
                )
                .then(Commands.literal("interval")
                        .then(Commands.argument("seconds", IntegerArgumentType.integer(5, 300))
                                .executes(context -> {
                                    int seconds = IntegerArgumentType.getInteger(context, "seconds");
                                    setRotationInterval(context.getSource(), seconds);
                                    return 1;
                                })
                        )
                )
                .then(Commands.literal("update")
                        .then(Commands.argument("ticks", IntegerArgumentType.integer(1, 1200))
                                .executes(context -> {
                                    int ticks = IntegerArgumentType.getInteger(context, "ticks");
                                    setUpdateInterval(context.getSource(), ticks);
                                    return 1;
                                })
                        )
                )
                .then(Commands.literal("remove")
                        .executes(context -> {
                            removeScoreboard(context.getSource());
                            return 1;
                        })
                )
        );
    }

    private static void createScoreboard(CommandSourceStack source, String statType) {
        ScoreboardManager scoreboardManager = ScoreboardManager.getInstance(source.getServer());
        scoreboardManager.createScoreboard(statType);
        me.tuanzi.statistics.util.StatsTranslationHelper.sendSuccess(source, "stats.board.created", getStatTypeDisplayName(source, statType));
    }

    private static void startRotation(CommandSourceStack source) {
        ScoreboardManager scoreboardManager = ScoreboardManager.getInstance(source.getServer());
        scoreboardManager.startRotation();
        me.tuanzi.statistics.util.StatsTranslationHelper.sendSuccess(source, "stats.board.rotation_started");
    }

    private static void stopRotation(CommandSourceStack source) {
        ScoreboardManager scoreboardManager = ScoreboardManager.getInstance(source.getServer());
        scoreboardManager.stopRotation();
        me.tuanzi.statistics.util.StatsTranslationHelper.sendSuccess(source, "stats.board.rotation_stopped");
    }

    private static void setRotationInterval(CommandSourceStack source, int seconds) {
        ScoreboardManager scoreboardManager = ScoreboardManager.getInstance(source.getServer());
        scoreboardManager.setRotationInterval(seconds * 1000L);
        me.tuanzi.statistics.util.StatsTranslationHelper.sendSuccess(source, "stats.board.rotation_interval_set", seconds);
    }

    private static void setUpdateInterval(CommandSourceStack source, int ticks) {
        ScoreboardManager scoreboardManager = ScoreboardManager.getInstance(source.getServer());
        scoreboardManager.setUpdateInterval(ticks);
        double seconds = ticks / 20.0;
        me.tuanzi.statistics.util.StatsTranslationHelper.sendSuccess(source, "stats.board.update_interval_set", ticks, seconds);
    }

    private static void removeScoreboard(CommandSourceStack source) {
        ScoreboardManager scoreboardManager = ScoreboardManager.getInstance(source.getServer());
        scoreboardManager.removeScoreboard();
        me.tuanzi.statistics.util.StatsTranslationHelper.sendSuccess(source, "stats.board.removed");
    }

    private static String getStatTypeDisplayName(CommandSourceStack source, String statType) {
        String key = "stats.type." + statType;
        if (statType.equals("playTime")) key = "stats.type.playTime_seconds";
        else if (statType.equals("playTimeMinutes")) key = "stats.type.playTime_minutes";
        else if (statType.equals("playTimeHours")) key = "stats.type.playTime_hours";
        
        return me.tuanzi.statistics.util.StatsTranslationHelper.translate(key, me.tuanzi.statistics.util.StatsTranslationHelper.getLanguage(source));
    }
}
