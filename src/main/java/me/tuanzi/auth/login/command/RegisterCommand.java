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
import me.tuanzi.auth.utils.TranslationHelper;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
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
            TranslationHelper.sendFailure(source, "auth.command.player_only");
            return 0;
        }

        String playerName = player.getName().getString();
        String password = StringArgumentType.getString(context, "password");
        String confirmPassword = StringArgumentType.getString(context, "confirmPassword");

        if (accountManager == null) {
            TranslationHelper.sendFailure(source, "auth.register.system_not_initialized");
            return 0;
        }

        if (accountManager.isRegistered(playerName)) {
            TranslationHelper.sendFailure(source, "auth.register.already_registered");
            return 0;
        }

        PasswordService.PasswordValidationResult validationResult = PasswordService.validatePasswordStrength(password);
        if (!validationResult.isValid()) {
            source.sendSuccess(() -> net.minecraft.network.chat.Component.literal("§c" + validationResult.getMessage()), false);
            return 0;
        }

        if (!password.equals(confirmPassword)) {
            TranslationHelper.sendFailure(source, "auth.register.password_mismatch");
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

            TranslationHelper.sendSuccess(source, "auth.register.success");
            return 1;
        } else {
            AuthLogger.getInstance().logRegisterEvent(playerName, false, "注册失败");
            TranslationHelper.sendFailure(source, "auth.register.failed");
            return 0;
        }
    }
}
