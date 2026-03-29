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
        source.sendSuccess(() -> Component.literal("§a已创建统计类型为: §f" + getStatTypeDisplayName(statType) + " §a的计分板"), false);
    }

    private static void startRotation(CommandSourceStack source) {
        ScoreboardManager scoreboardManager = ScoreboardManager.getInstance(source.getServer());
        scoreboardManager.startRotation();
        source.sendSuccess(() -> Component.literal("§a计分板轮换已开始"), false);
    }

    private static void stopRotation(CommandSourceStack source) {
        ScoreboardManager scoreboardManager = ScoreboardManager.getInstance(source.getServer());
        scoreboardManager.stopRotation();
        source.sendSuccess(() -> Component.literal("§a计分板轮换已停止"), false);
    }

    private static void setRotationInterval(CommandSourceStack source, int seconds) {
        ScoreboardManager scoreboardManager = ScoreboardManager.getInstance(source.getServer());
        scoreboardManager.setRotationInterval(seconds * 1000L);
        source.sendSuccess(() -> Component.literal("§a计分板轮换间隔已设置为 §f" + seconds + " §a秒"), false);
    }

    private static void setUpdateInterval(CommandSourceStack source, int ticks) {
        ScoreboardManager scoreboardManager = ScoreboardManager.getInstance(source.getServer());
        scoreboardManager.setUpdateInterval(ticks);
        double seconds = ticks / 20.0;
        source.sendSuccess(() -> Component.literal("§a计分板数据更新间隔已设置为 §f" + ticks + " §a刻 (§f" + String.format("%.1f", seconds) + "§a秒)"), false);
    }

    private static void removeScoreboard(CommandSourceStack source) {
        ScoreboardManager scoreboardManager = ScoreboardManager.getInstance(source.getServer());
        scoreboardManager.removeScoreboard();
        source.sendSuccess(() -> Component.literal("§a计分板已移除"), false);
    }

    private static String getStatTypeDisplayName(String statType) {
        switch (statType) {
            case "playTime":
                return "在线时间 (秒)";
            case "playTimeMinutes":
                return "在线时间 (分钟)";
            case "playTimeHours":
                return "在线时间 (小时)";
            case "distanceTraveled":
                return "移动距离";
            case "blocksPlaced":
                return "放置方块";
            case "blocksBroken":
                return "破坏方块";
            case "kills":
                return "击杀数";
            case "deaths":
                return "死亡数";
            case "damageDealt":
                return "造成伤害";
            case "damageTaken":
                return "受到伤害";
            default:
                return statType;
        }
    }
}
