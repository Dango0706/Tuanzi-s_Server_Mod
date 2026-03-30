package me.tuanzi.auth.login.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import me.tuanzi.auth.AuthModule;
import me.tuanzi.auth.login.LoginStateManager;
import me.tuanzi.auth.login.data.AccountManager;
import me.tuanzi.auth.login.security.PasswordService;
import me.tuanzi.auth.logging.AuthLogger;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

public class ChangePasswordCommand {

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher, CommandBuildContext registryAccess) {
        dispatcher.register(Commands.literal("changepassword")
                .then(Commands.argument("oldPassword", StringArgumentType.string())
                        .then(Commands.argument("newPassword", StringArgumentType.string())
                                .executes(context -> {
                                    return executeChangePassword(context);
                                })
                        )
                )
        );
    }

    private static int executeChangePassword(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();
        
        ServerPlayer player = source.getPlayer();
        if (player == null) {
            source.sendSuccess(() -> Component.literal("§c此命令只能由玩家使用"), false);
            return 0;
        }

        String playerName = player.getName().getString();
        String oldPassword = StringArgumentType.getString(context, "oldPassword");
        String newPassword = StringArgumentType.getString(context, "newPassword");

        LoginStateManager loginStateManager = LoginStateManager.getInstance();
        if (!loginStateManager.isLoggedIn(player.getUUID())) {
            source.sendSuccess(() -> Component.literal("§c您尚未登录，请先使用 /login 命令登录"), false);
            return 0;
        }

        AuthModule authModule = AuthModule.getInstance();
        if (authModule == null) {
            source.sendSuccess(() -> Component.literal("§c身份验证模块未初始化，请联系管理员"), false);
            return 0;
        }

        AccountManager accountManager = authModule.getAccountManager();
        if (accountManager == null) {
            source.sendSuccess(() -> Component.literal("§c账户管理系统尚未初始化，请联系管理员"), false);
            return 0;
        }

        if (!accountManager.isRegistered(playerName)) {
            source.sendSuccess(() -> Component.literal("§c您尚未注册，请先使用 /register 命令注册"), false);
            return 0;
        }

        PasswordService.PasswordValidationResult validationOldResult = PasswordService.validatePasswordStrength(oldPassword);
        if (!validationOldResult.isValid()) {
            source.sendSuccess(() -> Component.literal("§c旧密码格式无效"), false);
            return 0;
        }

        PasswordService.PasswordValidationResult validationNewResult = PasswordService.validatePasswordStrength(newPassword);
        if (!validationNewResult.isValid()) {
            source.sendSuccess(() -> Component.literal("§c" + validationNewResult.getMessage()), false);
            return 0;
        }

        if (oldPassword.equals(newPassword)) {
            source.sendSuccess(() -> Component.literal("§c新密码不能与旧密码相同"), false);
            return 0;
        }

        boolean success = accountManager.changePassword(playerName, oldPassword, newPassword);
        if (success) {
            AuthLogger.getInstance().logPasswordChangeEvent(playerName, true);
            AuthModule.LOGGER.info("玩家 {} 成功修改密码", playerName);
            source.sendSuccess(() -> Component.literal("§a密码修改成功！"), false);
            return 1;
        } else {
            AuthLogger.getInstance().logPasswordChangeEvent(playerName, false, "旧密码错误");
            source.sendSuccess(() -> Component.literal("§c密码修改失败，请检查旧密码是否正确"), false);
            return 0;
        }
    }
}
