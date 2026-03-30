package me.tuanzi.auth.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import me.tuanzi.auth.AuthModule;
import me.tuanzi.auth.login.data.AccountManager;
import me.tuanzi.auth.login.security.PasswordService;
import me.tuanzi.auth.logging.AuthLogger;
import me.tuanzi.auth.utils.TranslationHelper;
import me.tuanzi.auth.whitelist.WhitelistManager;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.network.chat.Component;
import net.minecraft.server.permissions.Permission;
import net.minecraft.server.permissions.PermissionLevel;

import java.util.Map;
import java.util.UUID;

public class AuthCommand {
    
    public static void register(CommandDispatcher<CommandSourceStack> dispatcher, CommandBuildContext registryAccess) {
        dispatcher.register(Commands.literal("auth")
                .requires(source -> source.permissions().hasPermission(new Permission.HasCommandLevel(PermissionLevel.GAMEMASTERS)))
                .then(Commands.literal("whitelist")
                        .then(Commands.literal("add")
                                .then(Commands.argument("player", StringArgumentType.string())
                                        .executes(context -> {
                                            String playerName = StringArgumentType.getString(context, "player");
                                            return addWhitelist(context.getSource(), playerName);
                                        })
                                )
                        )
                        .then(Commands.literal("remove")
                                .then(Commands.argument("player", StringArgumentType.string())
                                        .suggests((context, builder) -> {
                                            WhitelistManager whitelistManager = AuthModule.getInstance().getWhitelistManager();
                                            return SharedSuggestionProvider.suggest(
                                                    whitelistManager.getWhitelistMap().values().stream(),
                                                    builder
                                            );
                                        })
                                        .executes(context -> {
                                            String playerName = StringArgumentType.getString(context, "player");
                                            return removeWhitelist(context.getSource(), playerName);
                                        })
                                )
                        )
                        .then(Commands.literal("list")
                                .executes(context -> {
                                    return listWhitelist(context.getSource());
                                })
                        )
                )
                .then(Commands.literal("password")
                        .then(Commands.literal("reset")
                                .then(Commands.argument("player", StringArgumentType.string())
                                        .suggests((context, builder) -> {
                                            AccountManager accountManager = AuthModule.getInstance().getAccountManager();
                                            return SharedSuggestionProvider.suggest(
                                                    context.getSource().getServer().getPlayerList().getPlayers()
                                                            .stream()
                                                            .map(p -> p.getName().getString()),
                                                    builder
                                            );
                                        })
                                        .then(Commands.argument("newPassword", StringArgumentType.string())
                                                .executes(context -> {
                                                    String playerName = StringArgumentType.getString(context, "player");
                                                    String newPassword = StringArgumentType.getString(context, "newPassword");
                                                    return resetPassword(context.getSource(), playerName, newPassword);
                                                })
                                        )
                                )
                        )
                )
                .then(Commands.literal("reload")
                        .executes(context -> {
                            return reloadConfig(context.getSource());
                        })
                )
        );
    }
    
    private static int addWhitelist(CommandSourceStack source, String playerName) {
        WhitelistManager whitelistManager = AuthModule.getInstance().getWhitelistManager();
        String operatorName = source.getTextName();
        
        if (playerName == null || playerName.trim().isEmpty()) {
            TranslationHelper.sendSuccess(source, "auth.password.player_name_empty");
            return 0;
        }
        
        final String trimmedName = playerName.trim();
        
        if (whitelistManager.isInWhitelist(trimmedName)) {
            TranslationHelper.sendSuccess(source, "auth.whitelist.already_in", trimmedName);
            return 0;
        }
        
        boolean success = whitelistManager.addToWhitelist(trimmedName);
        if (success) {
            UUID uuid = me.tuanzi.auth.whitelist.OfflineUUIDGenerator.generateOfflineUUID(trimmedName);
            AuthLogger.getInstance().logAdminOperation(operatorName, "添加白名单", trimmedName, true);
            TranslationHelper.sendSuccess(source, "auth.whitelist.add_success", trimmedName);
            source.sendSuccess(() -> Component.literal("§7盗版 UUID: §f" + uuid.toString()), false);
            return 1;
        } else {
            AuthLogger.getInstance().logAdminOperation(operatorName, "添加白名单", trimmedName, false);
            TranslationHelper.sendSuccess(source, "auth.whitelist.add_failed");
            return 0;
        }
    }
    
    private static int removeWhitelist(CommandSourceStack source, String playerName) {
        WhitelistManager whitelistManager = AuthModule.getInstance().getWhitelistManager();
        String operatorName = source.getTextName();
        
        if (playerName == null || playerName.trim().isEmpty()) {
            TranslationHelper.sendSuccess(source, "auth.password.player_name_empty");
            return 0;
        }
        
        final String trimmedName = playerName.trim();
        
        if (!whitelistManager.isInWhitelist(trimmedName)) {
            TranslationHelper.sendSuccess(source, "auth.whitelist.not_in", trimmedName);
            return 0;
        }
        
        boolean success = whitelistManager.removeFromWhitelist(trimmedName);
        if (success) {
            AuthLogger.getInstance().logAdminOperation(operatorName, "移除白名单", trimmedName, true);
            TranslationHelper.sendSuccess(source, "auth.whitelist.remove_success", trimmedName);
            return 1;
        } else {
            AuthLogger.getInstance().logAdminOperation(operatorName, "移除白名单", trimmedName, false);
            TranslationHelper.sendSuccess(source, "auth.whitelist.remove_failed");
            return 0;
        }
    }
    
    private static int listWhitelist(CommandSourceStack source) {
        WhitelistManager whitelistManager = AuthModule.getInstance().getWhitelistManager();
        Map<UUID, String> whitelistMap = whitelistManager.getWhitelistMap();
        
        if (whitelistMap.isEmpty()) {
            TranslationHelper.sendSuccess(source, "auth.whitelist.empty");
            return 1;
        }
        
        TranslationHelper.sendSuccess(source, "auth.whitelist.list_header");
        TranslationHelper.sendSuccess(source, "auth.whitelist.list_count", whitelistMap.size());
        source.sendSuccess(() -> Component.literal("§e-----------------------------------"), false);
        
        for (Map.Entry<UUID, String> entry : whitelistMap.entrySet()) {
            UUID uuid = entry.getKey();
            String name = entry.getValue();
            source.sendSuccess(() -> Component.literal("§f" + name + " §7- §f" + uuid.toString()), false);
        }
        
        return 1;
    }
    
    private static int reloadConfig(CommandSourceStack source) {
        AuthModule authModule = AuthModule.getInstance();
        
        authModule.getAuthConfig().loadConfig();
        authModule.getWhitelistManager().loadData();
        
        source.sendSuccess(() -> Component.literal("§a已重载身份验证配置和白名单"), false);
        return 1;
    }
    
    private static int resetPassword(CommandSourceStack source, String playerName, String newPassword) {
        String operatorName = source.getTextName();
        
        if (playerName == null || playerName.trim().isEmpty()) {
            TranslationHelper.sendSuccess(source, "auth.password.player_name_empty");
            return 0;
        }
        
        if (newPassword == null || newPassword.isEmpty()) {
            TranslationHelper.sendSuccess(source, "auth.password.new_password_empty");
            return 0;
        }
        
        final String trimmedName = playerName.trim();
        
        AuthModule authModule = AuthModule.getInstance();
        AccountManager accountManager = authModule.getAccountManager();
        
        if (accountManager == null) {
            source.sendSuccess(() -> Component.literal("§c账户管理系统尚未初始化"), false);
            return 0;
        }
        
        if (!accountManager.isRegistered(trimmedName)) {
            TranslationHelper.sendSuccess(source, "auth.password.player_not_registered", trimmedName);
            return 0;
        }
        
        PasswordService.PasswordValidationResult validationResult = PasswordService.validatePasswordStrength(newPassword);
        if (!validationResult.isValid()) {
            source.sendSuccess(() -> Component.literal("§c" + validationResult.getMessage()), false);
            return 0;
        }
        
        boolean success = accountManager.resetPassword(trimmedName, newPassword);
        if (success) {
            AuthLogger.getInstance().logAdminOperation(operatorName, "重置密码", trimmedName, true);
            AuthModule.LOGGER.info("管理员 {} 重置了玩家 {} 的密码", operatorName, trimmedName);
            TranslationHelper.sendSuccess(source, "auth.password.reset_success", trimmedName);
            return 1;
        } else {
            AuthLogger.getInstance().logAdminOperation(operatorName, "重置密码", trimmedName, false);
            TranslationHelper.sendSuccess(source, "auth.password.reset_failed");
            return 0;
        }
    }
}
