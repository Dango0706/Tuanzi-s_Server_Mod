package me.tuanzi.auth.login.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import me.tuanzi.auth.login.LoginConfig;
import me.tuanzi.auth.login.LoginStateManager;
import me.tuanzi.auth.login.attempt.LoginAttemptManager;
import me.tuanzi.auth.login.data.AccountManager;
import me.tuanzi.auth.login.security.PasswordService;
import me.tuanzi.auth.login.session.SessionManager;
import me.tuanzi.auth.login.timeout.LoginTimeoutManager;
import me.tuanzi.auth.logging.AuthLogger;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

public class RegisterCommand {

    private static LoginConfig loginConfig;
    private static AccountManager accountManager;
    private static SessionManager sessionManager;
    private static LoginAttemptManager loginAttemptManager;
    private static LoginTimeoutManager loginTimeoutManager;

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher, CommandBuildContext registryAccess) {
        dispatcher.register(Commands.literal("register")
                .then(Commands.argument("password", StringArgumentType.string())
                        .then(Commands.argument("confirmPassword", StringArgumentType.string())
                                .executes(context -> {
                                    return executeRegister(context);
                                })
                        )
                )
        );
    }

    public static void initialize(LoginConfig config, AccountManager accounts, 
                                   SessionManager sessions, LoginAttemptManager attempts,
                                   LoginTimeoutManager timeout) {
        loginConfig = config;
        accountManager = accounts;
        sessionManager = sessions;
        loginAttemptManager = attempts;
        loginTimeoutManager = timeout;
    }

    private static int executeRegister(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();
        
        ServerPlayer player = source.getPlayer();
        if (player == null) {
            source.sendSuccess(() -> Component.literal("§c此命令只能由玩家使用"), false);
            return 0;
        }

        String playerName = player.getName().getString();
        String password = StringArgumentType.getString(context, "password");
        String confirmPassword = StringArgumentType.getString(context, "confirmPassword");

        if (accountManager == null) {
            source.sendSuccess(() -> Component.literal("§c登录系统尚未初始化，请联系管理员"), false);
            return 0;
        }

        if (accountManager.isRegistered(playerName)) {
            source.sendSuccess(() -> Component.literal("§c您已经注册过了，请使用 /login 命令登录"), false);
            return 0;
        }

        PasswordService.PasswordValidationResult validationResult = PasswordService.validatePasswordStrength(password);
        if (!validationResult.isValid()) {
            source.sendSuccess(() -> Component.literal("§c" + validationResult.getMessage()), false);
            return 0;
        }

        if (!password.equals(confirmPassword)) {
            source.sendSuccess(() -> Component.literal("§c两次输入的密码不一致，请重新输入"), false);
            return 0;
        }

        boolean success = accountManager.registerPlayer(playerName, password);
        if (success) {
            String ipAddress = player.getIpAddress();
            accountManager.updateLoginInfo(playerName, ipAddress);

            if (sessionManager != null) {
                sessionManager.createSession(playerName, ipAddress);
            }

            if (loginTimeoutManager != null) {
                loginTimeoutManager.cancelLoginTimer(playerName);
            }

            if (loginAttemptManager != null) {
                loginAttemptManager.resetAttempts(playerName);
            }

            LoginStateManager.getInstance().setLoggedIn(player.getUUID());

            AuthLogger.getInstance().logRegisterEvent(playerName, true, "IP: " + ipAddress);

            source.sendSuccess(() -> Component.literal("§a注册成功！您已自动登录"), false);
            return 1;
        } else {
            AuthLogger.getInstance().logRegisterEvent(playerName, false, "注册失败");
            source.sendSuccess(() -> Component.literal("§c注册失败，请稍后重试"), false);
            return 0;
        }
    }
}
