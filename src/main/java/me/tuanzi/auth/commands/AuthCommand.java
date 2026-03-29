package me.tuanzi.auth.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import me.tuanzi.auth.AuthModule;
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
                .then(Commands.literal("reload")
                        .executes(context -> {
                            return reloadConfig(context.getSource());
                        })
                )
        );
    }
    
    private static int addWhitelist(CommandSourceStack source, String playerName) {
        WhitelistManager whitelistManager = AuthModule.getInstance().getWhitelistManager();
        
        if (playerName == null || playerName.trim().isEmpty()) {
            source.sendSuccess(() -> Component.literal("§c玩家名不能为空"), false);
            return 0;
        }
        
        final String trimmedName = playerName.trim();
        
        if (whitelistManager.isInWhitelist(trimmedName)) {
            source.sendSuccess(() -> Component.literal("§c玩家 §f" + trimmedName + " §c已在白名单中"), false);
            return 0;
        }
        
        boolean success = whitelistManager.addToWhitelist(trimmedName);
        if (success) {
            UUID uuid = me.tuanzi.auth.whitelist.OfflineUUIDGenerator.generateOfflineUUID(trimmedName);
            source.sendSuccess(() -> Component.literal("§a已将玩家 §f" + trimmedName + " §a添加到白名单"), false);
            source.sendSuccess(() -> Component.literal("§7盗版 UUID: §f" + uuid.toString()), false);
            return 1;
        } else {
            source.sendSuccess(() -> Component.literal("§c添加玩家到白名单失败"), false);
            return 0;
        }
    }
    
    private static int removeWhitelist(CommandSourceStack source, String playerName) {
        WhitelistManager whitelistManager = AuthModule.getInstance().getWhitelistManager();
        
        if (playerName == null || playerName.trim().isEmpty()) {
            source.sendSuccess(() -> Component.literal("§c玩家名不能为空"), false);
            return 0;
        }
        
        final String trimmedName = playerName.trim();
        
        if (!whitelistManager.isInWhitelist(trimmedName)) {
            source.sendSuccess(() -> Component.literal("§c玩家 §f" + trimmedName + " §c不在白名单中"), false);
            return 0;
        }
        
        boolean success = whitelistManager.removeFromWhitelist(trimmedName);
        if (success) {
            source.sendSuccess(() -> Component.literal("§a已将玩家 §f" + trimmedName + " §a从白名单中移除"), false);
            return 1;
        } else {
            source.sendSuccess(() -> Component.literal("§c从白名单移除玩家失败"), false);
            return 0;
        }
    }
    
    private static int listWhitelist(CommandSourceStack source) {
        WhitelistManager whitelistManager = AuthModule.getInstance().getWhitelistManager();
        Map<UUID, String> whitelistMap = whitelistManager.getWhitelistMap();
        
        if (whitelistMap.isEmpty()) {
            source.sendSuccess(() -> Component.literal("§e白名单为空"), false);
            return 1;
        }
        
        source.sendSuccess(() -> Component.literal("§e========== §6白名单列表 §e=========="), false);
        source.sendSuccess(() -> Component.literal("§b共 §f" + whitelistMap.size() + " §b位玩家"), false);
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
}
