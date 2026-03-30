package me.tuanzi.auth;

import me.tuanzi.auth.commands.AuthCommand;
import me.tuanzi.auth.commands.AuthConfigCommand;
import me.tuanzi.auth.config.AuthConfig;
import me.tuanzi.auth.login.LoginConfig;
import me.tuanzi.auth.login.attempt.LoginAttemptManager;
import me.tuanzi.auth.login.command.ChangePasswordCommand;
import me.tuanzi.auth.login.command.LoginCommand;
import me.tuanzi.auth.login.command.RegisterCommand;
import me.tuanzi.auth.login.data.AccountManager;
import me.tuanzi.auth.login.listener.PlayerJoinListener;
import me.tuanzi.auth.login.listener.PlayerQuitListener;
import me.tuanzi.auth.login.listener.PlayerRestrictionListener;
import me.tuanzi.auth.login.session.SessionManager;
import me.tuanzi.auth.login.timeout.LoginTimeoutManager;
import me.tuanzi.auth.logging.AuthLogger;
import me.tuanzi.auth.utils.TranslationHelper;
import me.tuanzi.auth.whitelist.WhitelistManager;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.server.MinecraftServer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AuthModule implements ModInitializer {
    public static final String MOD_ID = "auth-module";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);
    
    private static AuthModule instance;
    private AuthConfig authConfig;
    private WhitelistManager whitelistManager;
    private LoginConfig loginConfig;
    private AccountManager accountManager;
    private SessionManager sessionManager;
    private LoginAttemptManager loginAttemptManager;
    private LoginTimeoutManager loginTimeoutManager;
    private MinecraftServer server;
    
    @Override
    public void onInitialize() {
        instance = this;
        
        LOGGER.info("正在初始化玩家身份验证模块...");
        
        TranslationHelper.initialize();
        
        authConfig = new AuthConfig();
        authConfig.loadConfig();
        
        whitelistManager = new WhitelistManager();
        whitelistManager.loadData();
        
        loginConfig = new LoginConfig();
        loginConfig.loadConfig();
        
        accountManager = new AccountManager();
        accountManager.loadAccounts();
        
        sessionManager = SessionManager.getInstance(loginConfig);
        
        loginAttemptManager = new LoginAttemptManager(loginConfig);
        
        registerCommands();
        registerServerLifecycleEvents();
        registerLoginListeners();
        
        LOGGER.info("玩家身份验证模块初始化完成!");
    }
    
    public void initializeLoginTimeoutManager(MinecraftServer minecraftServer) {
        this.server = minecraftServer;
        if (loginTimeoutManager == null) {
            loginTimeoutManager = new LoginTimeoutManager(loginConfig, minecraftServer);
            
            RegisterCommand.initialize(loginConfig, accountManager, sessionManager, loginAttemptManager, loginTimeoutManager);
            LoginCommand.initialize(loginConfig, accountManager, sessionManager, loginAttemptManager, loginTimeoutManager);
        }
    }
    
    private void registerCommands() {
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> {
            AuthCommand.register(dispatcher, registryAccess);
            AuthConfigCommand.register(dispatcher, registryAccess);
            RegisterCommand.register(dispatcher, registryAccess);
            LoginCommand.register(dispatcher, registryAccess);
            ChangePasswordCommand.register(dispatcher, registryAccess);
        });
    }
    
    private void registerServerLifecycleEvents() {
        ServerLifecycleEvents.SERVER_STARTING.register(server -> {
            initializeLoginTimeoutManager(server);
        });
        
        ServerLifecycleEvents.SERVER_STOPPING.register(server -> {
            if (authConfig != null) {
                authConfig.saveConfig();
            }
            if (whitelistManager != null) {
                whitelistManager.saveData();
            }
            if (accountManager != null) {
                accountManager.saveAccounts();
            }
            if (loginConfig != null) {
                loginConfig.saveConfig();
            }
            if (sessionManager != null) {
                sessionManager.shutdown();
            }
            if (loginTimeoutManager != null) {
                loginTimeoutManager.shutdown();
            }
            AuthLogger.getInstance().shutdown();
        });
    }
    
    private void registerLoginListeners() {
        ServerPlayConnectionEvents.JOIN.register(new PlayerJoinListener());
        ServerPlayConnectionEvents.DISCONNECT.register(new PlayerQuitListener());
        PlayerRestrictionListener.register();
        
        LOGGER.info("[AuthModule] 登录事件监听器已注册");
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
    
    public LoginConfig getLoginConfig() {
        return loginConfig;
    }
    
    public AccountManager getAccountManager() {
        return accountManager;
    }
    
    public SessionManager getSessionManager() {
        return sessionManager;
    }
    
    public LoginAttemptManager getLoginAttemptManager() {
        return loginAttemptManager;
    }
    
    public LoginTimeoutManager getLoginTimeoutManager() {
        return loginTimeoutManager;
    }
    
    public MinecraftServer getServer() {
        return server;
    }
}
