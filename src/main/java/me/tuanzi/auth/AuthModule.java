package me.tuanzi.auth;

import me.tuanzi.auth.commands.AuthCommand;
import me.tuanzi.auth.config.AuthConfig;
import me.tuanzi.auth.whitelist.WhitelistManager;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AuthModule implements ModInitializer {
    public static final String MOD_ID = "auth-module";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);
    
    private static AuthModule instance;
    private AuthConfig authConfig;
    private WhitelistManager whitelistManager;
    
    @Override
    public void onInitialize() {
        instance = this;
        
        LOGGER.info("正在初始化玩家身份验证模块...");
        
        authConfig = new AuthConfig();
        authConfig.loadConfig();
        
        whitelistManager = new WhitelistManager();
        whitelistManager.loadData();
        
        registerCommands();
        registerServerLifecycleEvents();
        
        LOGGER.info("玩家身份验证模块初始化完成!");
    }
    
    private void registerCommands() {
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> {
            AuthCommand.register(dispatcher, registryAccess);
        });
    }
    
    private void registerServerLifecycleEvents() {
        ServerLifecycleEvents.SERVER_STOPPING.register(server -> {
            if (authConfig != null) {
                authConfig.saveConfig();
            }
            if (whitelistManager != null) {
                whitelistManager.saveData();
            }
        });
    }
    
    public static AuthModule getInstance() {
        return instance;
    }
    
    public AuthConfig getAuthConfig() {
        return authConfig;
    }
    
    public WhitelistManager getWhitelistManager() {
        return whitelistManager;
    }
}
