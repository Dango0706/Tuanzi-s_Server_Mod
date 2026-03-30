package me.tuanzi.auth.login;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.loader.api.FabricLoader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;

public class LoginConfig {
    private static final Logger LOGGER = LoggerFactory.getLogger("LoginConfig");
    private static final String CONFIG_DIR = FabricLoader.getInstance().getConfigDir().resolve("auth").toString();
    private static final String CONFIG_FILE = CONFIG_DIR + File.separator + "login_config.json";
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    private int loginTimeoutSeconds = 60;
    private int ipSessionPersistenceSeconds = 3600;
    private int maxLoginAttempts = 5;
    private int lockoutDurationSeconds = 300;
    private int minPasswordLength = 6;
    private int maxPasswordLength = 32;

    public LoginConfig() {
        new File(CONFIG_DIR).mkdirs();
    }

    public void loadConfig() {
        File configFile = new File(CONFIG_FILE);
        if (configFile.exists()) {
            try (FileReader reader = new FileReader(configFile)) {
                LoginConfig loaded = GSON.fromJson(reader, LoginConfig.class);
                if (loaded != null) {
                    this.loginTimeoutSeconds = loaded.loginTimeoutSeconds;
                    this.ipSessionPersistenceSeconds = loaded.ipSessionPersistenceSeconds;
                    this.maxLoginAttempts = loaded.maxLoginAttempts;
                    this.lockoutDurationSeconds = loaded.lockoutDurationSeconds;
                    this.minPasswordLength = loaded.minPasswordLength;
                    this.maxPasswordLength = loaded.maxPasswordLength;
                }
                LOGGER.info("已加载登录配置文件");
            } catch (IOException e) {
                LOGGER.error("加载登录配置文件失败: {}", e.getMessage());
                saveConfig();
            }
        } else {
            saveConfig();
            LOGGER.info("已创建默认登录配置文件");
        }
    }

    public void saveConfig() {
        try {
            new File(CONFIG_DIR).mkdirs();
            try (FileWriter writer = new FileWriter(CONFIG_FILE)) {
                GSON.toJson(this, writer);
            }
        } catch (IOException e) {
            LOGGER.error("保存登录配置文件失败: {}", e.getMessage());
        }
    }

    public int getLoginTimeoutSeconds() {
        return loginTimeoutSeconds;
    }

    public void setLoginTimeoutSeconds(int loginTimeoutSeconds) {
        this.loginTimeoutSeconds = loginTimeoutSeconds;
    }

    public int getIpSessionPersistenceSeconds() {
        return ipSessionPersistenceSeconds;
    }

    public void setIpSessionPersistenceSeconds(int ipSessionPersistenceSeconds) {
        this.ipSessionPersistenceSeconds = ipSessionPersistenceSeconds;
    }

    public int getMaxLoginAttempts() {
        return maxLoginAttempts;
    }

    public void setMaxLoginAttempts(int maxLoginAttempts) {
        this.maxLoginAttempts = maxLoginAttempts;
    }

    public int getLockoutDurationSeconds() {
        return lockoutDurationSeconds;
    }

    public void setLockoutDurationSeconds(int lockoutDurationSeconds) {
        this.lockoutDurationSeconds = lockoutDurationSeconds;
    }

    public int getMinPasswordLength() {
        return minPasswordLength;
    }

    public void setMinPasswordLength(int minPasswordLength) {
        this.minPasswordLength = minPasswordLength;
    }

    public int getMaxPasswordLength() {
        return maxPasswordLength;
    }

    public void setMaxPasswordLength(int maxPasswordLength) {
        this.maxPasswordLength = maxPasswordLength;
    }
}
