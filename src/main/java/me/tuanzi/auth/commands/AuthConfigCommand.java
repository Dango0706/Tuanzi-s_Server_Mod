package me.tuanzi.auth.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.context.CommandContext;
import me.tuanzi.auth.AuthModule;
import me.tuanzi.auth.login.LoginConfig;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.permissions.Permission;
import net.minecraft.server.permissions.PermissionLevel;

public class AuthConfigCommand {

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher, CommandBuildContext registryAccess) {
        dispatcher.register(Commands.literal("authconfig")
                .requires(source -> source.permissions().hasPermission(new Permission.HasCommandLevel(PermissionLevel.GAMEMASTERS)))
                .then(Commands.literal("reload")
                        .executes(AuthConfigCommand::reloadConfig)
                )
                .then(Commands.literal("show")
                        .executes(AuthConfigCommand::showConfig)
                )
                .then(Commands.literal("set")
                        .then(Commands.literal("loginTimeout")
                                .then(Commands.argument("seconds", IntegerArgumentType.integer(1))
                                        .executes(AuthConfigCommand::setLoginTimeout)
                                )
                        )
                        .then(Commands.literal("sessionPersistence")
                                .then(Commands.argument("seconds", IntegerArgumentType.integer(1))
                                        .executes(AuthConfigCommand::setSessionPersistence)
                                )
                        )
                        .then(Commands.literal("maxLoginAttempts")
                                .then(Commands.argument("attempts", IntegerArgumentType.integer(1))
                                        .executes(AuthConfigCommand::setMaxLoginAttempts)
                                )
                        )
                        .then(Commands.literal("lockoutDuration")
                                .then(Commands.argument("seconds", IntegerArgumentType.integer(30, 3600))
                                        .executes(AuthConfigCommand::setLockoutDuration)
                                )
                        )
                        .then(Commands.literal("minPasswordLength")
                                .then(Commands.argument("length", IntegerArgumentType.integer(4, 32))
                                        .executes(AuthConfigCommand::setMinPasswordLength)
                                )
                        )
                        .then(Commands.literal("maxPasswordLength")
                                .then(Commands.argument("length", IntegerArgumentType.integer(8, 128))
                                        .executes(AuthConfigCommand::setMaxPasswordLength)
                                )
                        )
                )
        );
    }

    private static int reloadConfig(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();
        AuthModule authModule = AuthModule.getInstance();
        
        if (authModule == null) {
            source.sendFailure(Component.literal("§cAuthModule 未初始化"));
            return 0;
        }
        
        LoginConfig loginConfig = authModule.getLoginConfig();
        if (loginConfig == null) {
            source.sendFailure(Component.literal("§cLoginConfig 未初始化"));
            return 0;
        }
        
        loginConfig.loadConfig();
        source.sendSuccess(() -> Component.literal("§a配置文件已重新加载"), false);
        return 1;
    }

    private static int showConfig(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();
        AuthModule authModule = AuthModule.getInstance();
        
        if (authModule == null) {
            source.sendFailure(Component.literal("§cAuthModule 未初始化"));
            return 0;
        }
        
        LoginConfig config = authModule.getLoginConfig();
        if (config == null) {
            source.sendFailure(Component.literal("§cLoginConfig 未初始化"));
            return 0;
        }
        
        source.sendSuccess(() -> Component.literal("§6===== 登录配置 ====="), false);
        source.sendSuccess(() -> Component.literal("§e登录超时: §f" + config.getLoginTimeoutSeconds() + " 秒"), false);
        source.sendSuccess(() -> Component.literal("§e会话持久化: §f" + config.getIpSessionPersistenceSeconds() + " 秒"), false);
        source.sendSuccess(() -> Component.literal("§e最大登录尝试: §f" + config.getMaxLoginAttempts() + " 次"), false);
        source.sendSuccess(() -> Component.literal("§e锁定时长: §f" + config.getLockoutDurationSeconds() + " 秒"), false);
        source.sendSuccess(() -> Component.literal("§e密码最小长度: §f" + config.getMinPasswordLength() + " 字符"), false);
        source.sendSuccess(() -> Component.literal("§e密码最大长度: §f" + config.getMaxPasswordLength() + " 字符"), false);
        source.sendSuccess(() -> Component.literal("§6==================="), false);
        
        return 1;
    }

    private static int setLoginTimeout(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();
        int seconds = IntegerArgumentType.getInteger(context, "seconds");
        
        AuthModule authModule = AuthModule.getInstance();
        if (authModule == null) {
            source.sendFailure(Component.literal("§cAuthModule 未初始化"));
            return 0;
        }
        
        LoginConfig config = authModule.getLoginConfig();
        if (config == null) {
            source.sendFailure(Component.literal("§cLoginConfig 未初始化"));
            return 0;
        }
        
        config.setLoginTimeoutSeconds(seconds);
        config.saveConfig();
        
        source.sendSuccess(() -> Component.literal("§a登录超时已设置为 §f" + seconds + " §a秒"), false);
        return 1;
    }

    private static int setSessionPersistence(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();
        int seconds = IntegerArgumentType.getInteger(context, "seconds");
        
        AuthModule authModule = AuthModule.getInstance();
        if (authModule == null) {
            source.sendFailure(Component.literal("§cAuthModule 未初始化"));
            return 0;
        }
        
        LoginConfig config = authModule.getLoginConfig();
        if (config == null) {
            source.sendFailure(Component.literal("§cLoginConfig 未初始化"));
            return 0;
        }
        
        config.setIpSessionPersistenceSeconds(seconds);
        config.saveConfig();
        
        source.sendSuccess(() -> Component.literal("§a会话持久化已设置为 §f" + seconds + " §a秒"), false);
        return 1;
    }

    private static int setMaxLoginAttempts(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();
        int attempts = IntegerArgumentType.getInteger(context, "attempts");
        
        AuthModule authModule = AuthModule.getInstance();
        if (authModule == null) {
            source.sendFailure(Component.literal("§cAuthModule 未初始化"));
            return 0;
        }
        
        LoginConfig config = authModule.getLoginConfig();
        if (config == null) {
            source.sendFailure(Component.literal("§cLoginConfig 未初始化"));
            return 0;
        }
        
        config.setMaxLoginAttempts(attempts);
        config.saveConfig();
        
        source.sendSuccess(() -> Component.literal("§a最大登录尝试次数已设置为 §f" + attempts + " §a次"), false);
        return 1;
    }

    private static int setLockoutDuration(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();
        int seconds = IntegerArgumentType.getInteger(context, "seconds");
        
        AuthModule authModule = AuthModule.getInstance();
        if (authModule == null) {
            source.sendFailure(Component.literal("§cAuthModule 未初始化"));
            return 0;
        }
        
        LoginConfig config = authModule.getLoginConfig();
        if (config == null) {
            source.sendFailure(Component.literal("§cLoginConfig 未初始化"));
            return 0;
        }
        
        config.setLockoutDurationSeconds(seconds);
        config.saveConfig();
        
        source.sendSuccess(() -> Component.literal("§a锁定时长已设置为 §f" + seconds + " §a秒"), false);
        return 1;
    }

    private static int setMinPasswordLength(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();
        int length = IntegerArgumentType.getInteger(context, "length");
        
        AuthModule authModule = AuthModule.getInstance();
        if (authModule == null) {
            source.sendFailure(Component.literal("§cAuthModule 未初始化"));
            return 0;
        }
        
        LoginConfig config = authModule.getLoginConfig();
        if (config == null) {
            source.sendFailure(Component.literal("§cLoginConfig 未初始化"));
            return 0;
        }
        
        config.setMinPasswordLength(length);
        config.saveConfig();
        
        source.sendSuccess(() -> Component.literal("§a密码最小长度已设置为 §f" + length + " §a字符"), false);
        return 1;
    }

    private static int setMaxPasswordLength(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();
        int length = IntegerArgumentType.getInteger(context, "length");
        
        AuthModule authModule = AuthModule.getInstance();
        if (authModule == null) {
            source.sendFailure(Component.literal("§cAuthModule 未初始化"));
            return 0;
        }
        
        LoginConfig config = authModule.getLoginConfig();
        if (config == null) {
            source.sendFailure(Component.literal("§cLoginConfig 未初始化"));
            return 0;
        }
        
        config.setMaxPasswordLength(length);
        config.saveConfig();
        
        source.sendSuccess(() -> Component.literal("§a密码最大长度已设置为 §f" + length + " §a字符"), false);
        return 1;
    }
}
