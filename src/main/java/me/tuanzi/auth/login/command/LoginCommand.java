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
import me.tuanzi.auth.utils.TranslationHelper;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
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
            TranslationHelper.sendFailure(source, "auth.command.player_only");
            return 0;
        }

        String playerName = player.getName().getString();
        String password = StringArgumentType.getString(context, "password");

        if (accountManager == null) {
            TranslationHelper.sendFailure(source, "auth.login.system_not_initialized");
            return 0;
        }

        if (!accountManager.isRegistered(playerName)) {
            TranslationHelper.sendFailure(source, "auth.login.not_registered");
            return 0;
        }

        if (sessionManager != null && sessionManager.hasValidSession(playerName)) {
            TranslationHelper.sendSuccess(source, "auth.login.already_logged_in");
            return 0;
        }

        if (loginAttemptManager != null && loginAttemptManager.isLocked(playerName)) {
            String lockMessage = loginAttemptManager.getLockMessage(playerName);
            source.sendSuccess(() -> net.minecraft.network.chat.Component.literal(lockMessage != null ? lockMessage : TranslationHelper.translatable("auth.login.account_locked").getString()), false);
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

            TranslationHelper.sendSuccess(source, "auth.login.success");
            return 1;
        } else {
            if (loginAttemptManager != null) {
                loginAttemptManager.recordFailedAttempt(playerName);
                int remaining = loginAttemptManager.getRemainingAttempts(playerName);

                if (remaining > 0) {
                    AuthLogger.getInstance().logLoginFailed(playerName, remaining);
                    TranslationHelper.sendSuccess(source, "auth.login.password_wrong", remaining);
                } else {
                    String lockMessage = loginAttemptManager.getLockMessage(playerName);
                    AuthLogger.getInstance().logAccountLocked(playerName, lockMessage);
                    source.sendSuccess(() -> net.minecraft.network.chat.Component.literal(lockMessage != null ? lockMessage : TranslationHelper.translatable("auth.login.locked_too_many_attempts").getString()), false);
                }
            } else {
                AuthLogger.getInstance().logLoginEvent(playerName, false, player.getIpAddress(), "密码错误");
                TranslationHelper.sendSuccess(source, "auth.login.password_wrong_simple");
            }
            return 0;
        }
    }
}
