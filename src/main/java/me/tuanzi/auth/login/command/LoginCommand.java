package me.tuanzi.auth.login.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import me.tuanzi.auth.login.LoginConfig;
import me.tuanzi.auth.login.LoginStateManager;
import me.tuanzi.auth.login.attempt.LoginAttemptManager;
import me.tuanzi.auth.login.data.AccountManager;
import me.tuanzi.auth.login.session.SessionManager;
import me.tuanzi.auth.login.timeout.LoginTimeoutManager;
import me.tuanzi.auth.logging.AuthLogger;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

public class LoginCommand {

    private static LoginConfig loginConfig;
    private static AccountManager accountManager;
    private static SessionManager sessionManager;
    private static LoginAttemptManager loginAttemptManager;
    private static LoginTimeoutManager loginTimeoutManager;

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher, CommandBuildContext registryAccess) {
        dispatcher.register(Commands.literal("login")
                .then(Commands.argument("password", StringArgumentType.string())
                        .executes(context -> {
                            return executeLogin(context);
                        })
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

    private static int executeLogin(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();

        ServerPlayer player = source.getPlayer();
        if (player == null) {
            source.sendSuccess(() -> Component.literal("§c此命令只能由玩家使用"), false);
            return 0;
        }

        String playerName = player.getName().getString();
        String password = StringArgumentType.getString(context, "password");

        if (accountManager == null) {
            source.sendSuccess(() -> Component.literal("§c登录系统尚未初始化，请联系管理员"), false);
            return 0;
        }

        if (!accountManager.isRegistered(playerName)) {
            source.sendSuccess(() -> Component.literal("§c您还没有注册，请先使用 /register <密码> <确认密码> 注册"), false);
            return 0;
        }

        if (sessionManager != null && sessionManager.hasValidSession(playerName)) {
            source.sendSuccess(() -> Component.literal("§a您已经登录过了"), false);
            return 0;
        }

        if (loginAttemptManager != null && loginAttemptManager.isLocked(playerName)) {
            String lockMessage = loginAttemptManager.getLockMessage(playerName);
            source.sendSuccess(() -> Component.literal(lockMessage != null ? lockMessage : "§c您的账户已被锁定"), false);
            return 0;
        }

        boolean verified = accountManager.verifyPassword(playerName, password);
        if (verified) {
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

            AuthLogger.getInstance().logLoginEvent(playerName, true, ipAddress);

            source.sendSuccess(() -> Component.literal("§a登录成功！"), false);
            return 1;
        } else {
            if (loginAttemptManager != null) {
                loginAttemptManager.recordFailedAttempt(playerName);
                int remaining = loginAttemptManager.getRemainingAttempts(playerName);

                if (remaining > 0) {
                    AuthLogger.getInstance().logLoginFailed(playerName, remaining);
                    source.sendSuccess(() -> Component.literal("§c密码错误！您还有 §f" + remaining + " §c次尝试机会"), false);
                } else {
                    String lockMessage = loginAttemptManager.getLockMessage(playerName);
                    AuthLogger.getInstance().logAccountLocked(playerName, lockMessage);
                    source.sendSuccess(() -> Component.literal(lockMessage != null ? lockMessage : "§c登录失败次数过多，账户已被锁定"), false);
                }
            } else {
                AuthLogger.getInstance().logLoginEvent(playerName, false, player.getIpAddress(), "密码错误");
                source.sendSuccess(() -> Component.literal("§c密码错误！"), false);
            }
            return 0;
        }
    }
}
