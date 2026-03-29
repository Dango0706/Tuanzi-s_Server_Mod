package me.tuanzi.auth.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.loader.api.FabricLoader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;

public class AuthConfig {
    private static final Logger LOGGER = LoggerFactory.getLogger("AuthConfig");
    private static final String CONFIG_DIR = FabricLoader.getInstance().getConfigDir().resolve("auth").toString();
    private static final String CONFIG_FILE = CONFIG_DIR + File.separator + "auth_config.json";
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    
    private String kickMessage = "§c您不在服务器白名单中，请联系管理员申请访问权限。";
    private boolean enableAuthLog = true;
    private int logRetentionDays = 30;
    private int cacheExpiryMinutes = 30;
    private boolean enablePremiumCache = true;
    
    public AuthConfig() {
        new File(CONFIG_DIR).mkdirs();
    }
    
    public void loadConfig() {
        File configFile = new File(CONFIG_FILE);
        if (configFile.exists()) {
            try (FileReader reader = new FileReader(configFile)) {
                AuthConfig loaded = GSON.fromJson(reader, AuthConfig.class);
                if (loaded != null) {
                    this.kickMessage = loaded.kickMessage;
                    this.enableAuthLog = loaded.enableAuthLog;
                    this.logRetentionDays = loaded.logRetentionDays;
                    this.cacheExpiryMinutes = loaded.cacheExpiryMinutes;
                    this.enablePremiumCache = loaded.enablePremiumCache;
                }
                LOGGER.info("已加载身份验证配置文件");
            } catch (IOException e) {
                LOGGER.error("加载身份验证配置文件失败: {}", e.getMessage());
                saveConfig();
            }
        } else {
            saveConfig();
            LOGGER.info("已创建默认身份验证配置文件");
        }
    }
    
    public void saveConfig() {
        try {
            new File(CONFIG_DIR).mkdirs();
            try (FileWriter writer = new FileWriter(CONFIG_FILE)) {
                GSON.toJson(this, writer);
            }
        } catch (IOException e) {
            LOGGER.error("保存身份验证配置文件失败: {}", e.getMessage());
        }
    }
    
    public String getKickMessage() {
        return kickMessage;
    }
    
    public void setKickMessage(String kickMessage) {
        this.kickMessage = kickMessage;
    }
    
    public boolean isEnableAuthLog() {
        return enableAuthLog;
    }
    
    public void setEnableAuthLog(boolean enableAuthLog) {
        this.enableAuthLog = enableAuthLog;
    }
    
    public int getLogRetentionDays() {
        return logRetentionDays;
    }
    
    public void setLogRetentionDays(int logRetentionDays) {
        this.logRetentionDays = logRetentionDays;
    }
    
    public int getCacheExpiryMinutes() {
        return cacheExpiryMinutes;
    }
    
    public void setCacheExpiryMinutes(int cacheExpiryMinutes) {
        this.cacheExpiryMinutes = cacheExpiryMinutes;
    }
    
    public boolean isEnablePremiumCache() {
        return enablePremiumCache;
    }
    
    public void setEnablePremiumCache(boolean enablePremiumCache) {
        this.enablePremiumCache = enablePremiumCache;
    }
}
