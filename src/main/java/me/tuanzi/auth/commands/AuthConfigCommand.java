package me.tuanzi.auth.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.context.CommandContext;
import me.tuanzi.auth.AuthModule;
import me.tuanzi.auth.login.LoginConfig;
import me.tuanzi.auth.utils.TranslationHelper;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
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
            TranslationHelper.sendFailure(source, "auth.command.module_not_initialized");
            return 0;
        }
        
        LoginConfig loginConfig = authModule.getLoginConfig();
        if (loginConfig == null) {
            TranslationHelper.sendFailure(source, "auth.command.config_not_initialized");
            return 0;
        }
        
        loginConfig.loadConfig();
        TranslationHelper.sendSuccess(source, "auth.config.reloaded");
        return 1;
    }

    private static int showConfig(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();
        AuthModule authModule = AuthModule.getInstance();
        
        if (authModule == null) {
            TranslationHelper.sendFailure(source, "auth.command.module_not_initialized");
            return 0;
        }
        
        LoginConfig config = authModule.getLoginConfig();
        if (config == null) {
            TranslationHelper.sendFailure(source, "auth.command.config_not_initialized");
            return 0;
        }
        
        TranslationHelper.sendSuccess(source, "auth.config.show_header");
        TranslationHelper.sendSuccess(source, "auth.config.login_timeout", config.getLoginTimeoutSeconds());
        TranslationHelper.sendSuccess(source, "auth.config.session_persistence", config.getIpSessionPersistenceSeconds());
        TranslationHelper.sendSuccess(source, "auth.config.max_login_attempts", config.getMaxLoginAttempts());
        TranslationHelper.sendSuccess(source, "auth.config.lockout_duration", config.getLockoutDurationSeconds());
        TranslationHelper.sendSuccess(source, "auth.config.min_password_length", config.getMinPasswordLength());
        TranslationHelper.sendSuccess(source, "auth.config.max_password_length", config.getMaxPasswordLength());
        TranslationHelper.sendSuccess(source, "auth.config.show_footer");
        
        return 1;
    }

    private static int setLoginTimeout(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();
        int seconds = IntegerArgumentType.getInteger(context, "seconds");
        
        AuthModule authModule = AuthModule.getInstance();
        if (authModule == null) {
            TranslationHelper.sendFailure(source, "auth.command.module_not_initialized");
            return 0;
        }
        
        LoginConfig config = authModule.getLoginConfig();
        if (config == null) {
            TranslationHelper.sendFailure(source, "auth.command.config_not_initialized");
            return 0;
        }
        
        config.setLoginTimeoutSeconds(seconds);
        config.saveConfig();
        
        TranslationHelper.sendSuccess(source, "auth.config.set_login_timeout", seconds);
        return 1;
    }

    private static int setSessionPersistence(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();
        int seconds = IntegerArgumentType.getInteger(context, "seconds");
        
        AuthModule authModule = AuthModule.getInstance();
        if (authModule == null) {
            TranslationHelper.sendFailure(source, "auth.command.module_not_initialized");
            return 0;
        }
        
        LoginConfig config = authModule.getLoginConfig();
        if (config == null) {
            TranslationHelper.sendFailure(source, "auth.command.config_not_initialized");
            return 0;
        }
        
        config.setIpSessionPersistenceSeconds(seconds);
        config.saveConfig();
        
        TranslationHelper.sendSuccess(source, "auth.config.set_session_persistence", seconds);
        return 1;
    }

    private static int setMaxLoginAttempts(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();
        int attempts = IntegerArgumentType.getInteger(context, "attempts");
        
        AuthModule authModule = AuthModule.getInstance();
        if (authModule == null) {
            TranslationHelper.sendFailure(source, "auth.command.module_not_initialized");
            return 0;
        }
        
        LoginConfig config = authModule.getLoginConfig();
        if (config == null) {
            TranslationHelper.sendFailure(source, "auth.command.config_not_initialized");
            return 0;
        }
        
        config.setMaxLoginAttempts(attempts);
        config.saveConfig();
        
        TranslationHelper.sendSuccess(source, "auth.config.set_max_login_attempts", attempts);
        return 1;
    }

    private static int setLockoutDuration(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();
        int seconds = IntegerArgumentType.getInteger(context, "seconds");
        
        AuthModule authModule = AuthModule.getInstance();
        if (authModule == null) {
            TranslationHelper.sendFailure(source, "auth.command.module_not_initialized");
            return 0;
        }
        
        LoginConfig config = authModule.getLoginConfig();
        if (config == null) {
            TranslationHelper.sendFailure(source, "auth.command.config_not_initialized");
            return 0;
        }
        
        config.setLockoutDurationSeconds(seconds);
        config.saveConfig();
        
        TranslationHelper.sendSuccess(source, "auth.config.set_lockout_duration", seconds);
        return 1;
    }

    private static int setMinPasswordLength(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();
        int length = IntegerArgumentType.getInteger(context, "length");
        
        AuthModule authModule = AuthModule.getInstance();
        if (authModule == null) {
            TranslationHelper.sendFailure(source, "auth.command.module_not_initialized");
            return 0;
        }
        
        LoginConfig config = authModule.getLoginConfig();
        if (config == null) {
            TranslationHelper.sendFailure(source, "auth.command.config_not_initialized");
            return 0;
        }
        
        config.setMinPasswordLength(length);
        config.saveConfig();
        
        TranslationHelper.sendSuccess(source, "auth.config.set_min_password_length", length);
        return 1;
    }

    private static int setMaxPasswordLength(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();
        int length = IntegerArgumentType.getInteger(context, "length");
        
        AuthModule authModule = AuthModule.getInstance();
        if (authModule == null) {
            TranslationHelper.sendFailure(source, "auth.command.module_not_initialized");
            return 0;
        }
        
        LoginConfig config = authModule.getLoginConfig();
        if (config == null) {
            TranslationHelper.sendFailure(source, "auth.command.config_not_initialized");
            return 0;
        }
        
        config.setMaxPasswordLength(length);
        config.saveConfig();
        
        TranslationHelper.sendSuccess(source, "auth.config.set_max_password_length", length);
        return 1;
    }
}
