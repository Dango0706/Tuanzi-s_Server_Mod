package com.example.statistics.commands;

import com.example.statistics.floatingtext.FloatingTextData;
import com.example.statistics.floatingtext.FloatingTextManager;
import com.example.statistics.floatingtext.LeaderboardFormatter;
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
import net.minecraft.commands.arguments.coordinates.Coordinates;
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
        double x = pos.x;
        double y = pos.y;
        double z = pos.z;
        
        FloatingTextManager manager = FloatingTextManager.getInstance();
        
        if (manager.createFloatingText(id, level, x, y, z)) {
            source.sendSuccess(() -> Component.literal("§a成功创建悬浮文字 '" + id + "' 在位置 (" + 
                String.format("%.1f", x) + ", " + String.format("%.1f", y) + ", " + String.format("%.1f", z) + ")"), true);
            return 1;
        } else {
            source.sendSuccess(() -> Component.literal("§c悬浮文字 '" + id + "' 已存在！"), false);
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
            source.sendSuccess(() -> Component.literal("§c不支持的统计类型: " + statType), false);
            return 0;
        }
        
        if (manager.setContent(id, statType, displayName)) {
            source.sendSuccess(() -> Component.literal("§a成功设置悬浮文字 '" + id + "' 的内容为: " + 
                displayName + " (" + statType + ")"), true);
            return 1;
        } else {
            source.sendSuccess(() -> Component.literal("§c悬浮文字 '" + id + "' 不存在！"), false);
            return 0;
        }
    }
    
    private static int setUpdateInterval(CommandContext<CommandSourceStack> context) {
        String id = StringArgumentType.getString(context, "id");
        int ticks = IntegerArgumentType.getInteger(context, "ticks");
        
        CommandSourceStack source = context.getSource();
        FloatingTextManager manager = FloatingTextManager.getInstance();
        
        if (manager.setUpdateInterval(id, ticks)) {
            double seconds = ticks / 20.0;
            source.sendSuccess(() -> Component.literal("§a成功设置悬浮文字 '" + id + "' 的更新间隔为 " + 
                ticks + " tick (" + String.format("%.1f", seconds) + "秒)"), true);
            return 1;
        } else {
            source.sendSuccess(() -> Component.literal("§c悬浮文字 '" + id + "' 不存在！"), false);
            return 0;
        }
    }
    
    private static int moveFloatingText(CommandContext<CommandSourceStack> context) {
        String id = StringArgumentType.getString(context, "id");
        
        CommandSourceStack source = context.getSource();
        FloatingTextManager manager = FloatingTextManager.getInstance();
        FloatingTextData data = manager.getFloatingText(id);
        
        if (data == null) {
            source.sendSuccess(() -> Component.literal("§c悬浮文字 '" + id + "' 不存在！"), false);
            return 0;
        }
        
        Vec3 pos = Vec3Argument.getVec3(context, "pos");
        double x = pos.x;
        double y = pos.y;
        double z = pos.z;
        
        if (manager.moveFloatingText(id, source.getLevel(), x, y, z, false)) {
            source.sendSuccess(() -> Component.literal("§a成功移动悬浮文字 '" + id + "' 到位置 (" + 
                String.format("%.1f", x) + ", " + String.format("%.1f", y) + ", " + String.format("%.1f", z) + ")"), true);
            return 1;
        } else {
            source.sendSuccess(() -> Component.literal("§c移动悬浮文字失败！"), false);
            return 0;
        }
    }
    
    private static int setColor(CommandContext<CommandSourceStack> context) {
        String id = StringArgumentType.getString(context, "id");
        String color = StringArgumentType.getString(context, "color");
        
        CommandSourceStack source = context.getSource();
        FloatingTextManager manager = FloatingTextManager.getInstance();
        
        if (!VALID_COLORS.contains(color.toLowerCase())) {
            source.sendSuccess(() -> Component.literal("§c无效的颜色: " + color + "。可用颜色: " + 
                String.join(", ", VALID_COLORS)), false);
            return 0;
        }
        
        if (manager.setColor(id, color.toLowerCase())) {
            source.sendSuccess(() -> Component.literal("§a成功设置悬浮文字 '" + id + "' 的颜色为: " + color), true);
            return 1;
        } else {
            source.sendSuccess(() -> Component.literal("§c悬浮文字 '" + id + "' 不存在！"), false);
            return 0;
        }
    }
    
    private static int deleteFloatingText(CommandContext<CommandSourceStack> context) {
        String id = StringArgumentType.getString(context, "id");
        
        CommandSourceStack source = context.getSource();
        FloatingTextManager manager = FloatingTextManager.getInstance();
        
        if (manager.deleteFloatingText(id)) {
            source.sendSuccess(() -> Component.literal("§a成功删除悬浮文字 '" + id + "'"), true);
            return 1;
        } else {
            source.sendSuccess(() -> Component.literal("§c悬浮文字 '" + id + "' 不存在！"), false);
            return 0;
        }
    }
    
    private static int listFloatingTexts(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();
        FloatingTextManager manager = FloatingTextManager.getInstance();
        Map<String, FloatingTextData> texts = manager.getAllFloatingTexts();
        
        if (texts.isEmpty()) {
            source.sendSuccess(() -> Component.literal("§e当前没有悬浮文字"), false);
            return 0;
        }
        
        source.sendSuccess(() -> Component.literal("§e========== 悬浮文字列表 §e=========="), false);
        for (Map.Entry<String, FloatingTextData> entry : texts.entrySet()) {
            String id = entry.getKey();
            FloatingTextData data = entry.getValue();
            String statInfo = data.getStatType() != null ? 
                LeaderboardFormatter.getStatDisplayName(data.getStatType()) : "未设置";
            source.sendSuccess(() -> Component.literal("§f- §b" + id + " §7| " + statInfo + 
                " §7| 位置: (" + String.format("%.1f", data.getX()) + ", " + 
                String.format("%.1f", data.getY()) + ", " + String.format("%.1f", data.getZ()) + ")"), false);
        }
        source.sendSuccess(() -> Component.literal("§e总计: " + texts.size() + " 个悬浮文字"), false);
        return texts.size();
    }
    
    private static int showInfo(CommandContext<CommandSourceStack> context) {
        String id = StringArgumentType.getString(context, "id");
        CommandSourceStack source = context.getSource();
        FloatingTextManager manager = FloatingTextManager.getInstance();
        FloatingTextData data = manager.getFloatingText(id);
        
        if (data == null) {
            source.sendSuccess(() -> Component.literal("§c悬浮文字 '" + id + "' 不存在！"), false);
            return 0;
        }
        
        source.sendSuccess(() -> Component.literal("§e========== 悬浮文字信息: " + id + " §e=========="), false);
        source.sendSuccess(() -> Component.literal("§bID: §f" + data.getId()), false);
        source.sendSuccess(() -> Component.literal("§b世界: §f" + data.getWorldName()), false);
        source.sendSuccess(() -> Component.literal("§b位置: §f(" + 
            String.format("%.1f", data.getX()) + ", " + 
            String.format("%.1f", data.getY()) + ", " + 
            String.format("%.1f", data.getZ()) + ")"), false);
        
        String statType = data.getStatType() != null ? 
            LeaderboardFormatter.getStatDisplayName(data.getStatType()) + " (" + data.getStatType() + ")" : "未设置";
        source.sendSuccess(() -> Component.literal("§b统计类型: §f" + statType), false);
        source.sendSuccess(() -> Component.literal("§b显示名称: §f" + (data.getDisplayName() != null ? data.getDisplayName() : "未设置")), false);
        source.sendSuccess(() -> Component.literal("§b颜色: §f" + data.getColor()), false);
        source.sendSuccess(() -> Component.literal("§b更新间隔: §f" + data.getUpdateInterval() + " tick (" + 
            String.format("%.1f", data.getUpdateInterval() / 20.0) + "秒)"), false);
        
        if (data.getCreatedTime() > 0) {
            String createdTime = Instant.ofEpochMilli(data.getCreatedTime())
                .atZone(ZoneId.systemDefault())
                .format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
            source.sendSuccess(() -> Component.literal("§b创建时间: §f" + createdTime), false);
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
