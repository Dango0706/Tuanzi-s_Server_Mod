package me.tuanzi.economy.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import me.tuanzi.backup.BackupManager;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.network.chat.Style;
import net.minecraft.server.permissions.Permission;
import net.minecraft.server.permissions.PermissionLevel;

import java.util.Timer;
import java.util.TimerTask;

public class BackupCommand {
    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("eco")
                .requires(source -> source.permissions().hasPermission(new Permission.HasCommandLevel(PermissionLevel.GAMEMASTERS)))
                .then(Commands.literal("backup")
                        .executes(ctx -> {
                            String timestamp = BackupManager.getInstance().performBackup(ctx.getSource().getServer(), "manual");
                            if (timestamp != null) {
                                ctx.getSource().sendSuccess(() -> Component.literal("§a备份成功！时间戳: §e" + timestamp), true);
                            } else {
                                ctx.getSource().sendFailure(Component.literal("§c备份失败，请检查控制台。"));
                            }
                            return 1;
                        }))
                .then(Commands.literal("restore")
                        .then(Commands.argument("timestamp", StringArgumentType.string())
                                .executes(ctx -> {
                                    String timestamp = StringArgumentType.getString(ctx, "timestamp");
                                    CommandSourceStack source = ctx.getSource();
                                    
                                    source.sendSuccess(() -> Component.literal("§6正在请求恢复至备份: §e" + timestamp), true);
                                    source.sendSuccess(() -> Component.literal("§c§l警告：恢复操作将覆盖当前所有数据！"), true);
                                    
                                    Component confirmBtn = Component.literal("§a§l[点击此处确认恢复]")
                                            .setStyle(Style.EMPTY
                                                .withClickEvent(new ClickEvent.RunCommand("/eco restore " + timestamp + " confirm"))
                                                .withHoverEvent(new HoverEvent.ShowText(Component.literal("§7点击后将立即开始：\n§71. 备份当前数据\n§72. 恢复指定数据\n§73. 关闭服务器"))));
                                    
                                    source.sendSuccess(() -> confirmBtn, false);
                                    return 1;
                                })
                                .then(Commands.literal("confirm")
                                        .executes(ctx -> {
                                            String timestamp = StringArgumentType.getString(ctx, "timestamp");
                                            CommandSourceStack source = ctx.getSource();

                                            // 1. 恢复前先备份当前内容
                                            source.sendSuccess(() -> Component.literal("§6[1/3] 正在对当前内容进行紧急备份..."), true);
                                            String preRestoreBackup = BackupManager.getInstance().performBackup(source.getServer(), "pre_restore");
                                            if (preRestoreBackup == null) {
                                                source.sendFailure(Component.literal("§c[错误] 紧急备份失败，已中止恢复操作以确保安全。"));
                                                return 0;
                                            }
                                            source.sendSuccess(() -> Component.literal("§a紧急备份完成: §e" + preRestoreBackup), true);

                                            // 2. 执行恢复
                                            source.sendSuccess(() -> Component.literal("§6[2/3] 正在恢复目标备份数据..."), true);
                                            if (BackupManager.getInstance().restoreBackup(source.getServer(), timestamp)) {
                                                source.sendSuccess(() -> Component.literal("§a§l[3/3] 数据恢复成功！"), true);
                                                source.sendSuccess(() -> Component.literal("§c服务器将在 10 秒后自动停机完成应用，请手动重启。"), true);
                                                
                                                // 延迟关闭服务器
                                                new Timer().schedule(new TimerTask() {
                                                    @Override
                                                    public void run() {
                                                        source.getServer().halt(false);
                                                    }
                                                }, 10000);
                                            } else {
                                                source.sendFailure(Component.literal("§c[错误] 数据恢复失败！请检查时间戳是否正确。"));
                                            }
                                            return 1;
                                        }))))
                .then(Commands.literal("interval")
                        .then(Commands.argument("hours", IntegerArgumentType.integer(1))
                                .executes(ctx -> {
                                    int hours = IntegerArgumentType.getInteger(ctx, "hours");
                                    BackupManager.getInstance().getConfig().setBackupIntervalHours(hours);
                                    BackupManager.getInstance().getConfig().save();
                                    ctx.getSource().sendSuccess(() -> Component.literal("§a备份间隔已设置为: §e" + hours + " §a小时"), true);
                                    return 1;
                                })))
        );
    }
}
