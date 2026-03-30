package me.tuanzi.auth.login.data;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import me.tuanzi.auth.login.security.PasswordService;
import net.fabricmc.loader.api.FabricLoader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

public class AccountManager {
    private static final Logger LOGGER = LoggerFactory.getLogger("AccountManager");
    private static final String DATA_DIR = FabricLoader.getInstance().getConfigDir().resolve("auth").toString();
    private static final String ACCOUNTS_FILE = DATA_DIR + File.separator + "accounts.json";

    private final Map<String, PlayerAccount> accountsMap;
    private final Gson gson;

    public AccountManager() {
        this.accountsMap = new ConcurrentHashMap<>();
        this.gson = new GsonBuilder().setPrettyPrinting().create();

        new File(DATA_DIR).mkdirs();
    }

    public void loadAccounts() {
        try {
            File file = new File(ACCOUNTS_FILE);
            if (file.exists()) {
                FileReader reader = new FileReader(file);
                AccountData data = gson.fromJson(reader, AccountData.class);
                reader.close();

                if (data != null && data.getAccounts() != null) {
                    accountsMap.clear();
                    for (PlayerAccount account : data.getAccounts()) {
                        if (account.getPlayerName() != null && !account.getPlayerName().isEmpty()) {
                            accountsMap.put(account.getPlayerName().toLowerCase(), account);
                        }
                    }
                    LOGGER.info("成功加载 {} 个账户数据", accountsMap.size());
                }
            } else {
                LOGGER.info("账户数据文件不存在，将创建新文件");
            }
        } catch (IOException e) {
            LOGGER.error("加载账户数据失败: {}", e.getMessage());
        }
    }

    public void saveAccounts() {
        try {
            AccountData data = new AccountData();
            for (PlayerAccount account : accountsMap.values()) {
                data.getAccounts().add(account);
            }

            FileWriter writer = new FileWriter(ACCOUNTS_FILE);
            gson.toJson(data, writer);
            writer.close();
            LOGGER.debug("成功保存 {} 个账户数据", accountsMap.size());
        } catch (IOException e) {
            LOGGER.error("保存账户数据失败: {}", e.getMessage());
        }
    }

    public boolean registerPlayer(String playerName, String password) {
        if (playerName == null || playerName.isEmpty()) {
            LOGGER.warn("注册失败: 玩家名称不能为空");
            return false;
        }

        if (isRegistered(playerName)) {
            LOGGER.warn("注册失败: 玩家 {} 已存在", playerName);
            return false;
        }

        PasswordService.PasswordValidationResult validationResult = PasswordService.validatePasswordStrength(password);
        if (!validationResult.isValid()) {
            LOGGER.warn("注册失败: {}", validationResult.getMessage());
            return false;
        }

        String passwordHash = PasswordService.hashPassword(password);
        PlayerAccount account = new PlayerAccount(playerName, passwordHash);
        accountsMap.put(playerName.toLowerCase(), account);
        saveAccounts();

        LOGGER.info("玩家 {} 注册成功", playerName);
        return true;
    }

    public boolean isRegistered(String playerName) {
        if (playerName == null || playerName.isEmpty()) {
            return false;
        }
        return accountsMap.containsKey(playerName.toLowerCase());
    }

    public boolean verifyPassword(String playerName, String password) {
        if (playerName == null || password == null) {
            return false;
        }

        PlayerAccount account = accountsMap.get(playerName.toLowerCase());
        if (account == null) {
            return false;
        }

        if (account.isLocked()) {
            LOGGER.warn("玩家 {} 账户已被锁定", playerName);
            return false;
        }

        boolean verified = PasswordService.verifyPassword(password, account.getPasswordHash());
        if (!verified) {
            account.incrementFailedAttempts();
            saveAccounts();
            LOGGER.warn("玩家 {} 密码验证失败，失败次数: {}", playerName, account.getFailedAttempts());
        }

        return verified;
    }

    public boolean changePassword(String playerName, String oldPassword, String newPassword) {
        if (playerName == null || oldPassword == null || newPassword == null) {
            LOGGER.warn("修改密码失败: 参数不能为空");
            return false;
        }

        PlayerAccount account = accountsMap.get(playerName.toLowerCase());
        if (account == null) {
            LOGGER.warn("修改密码失败: 玩家 {} 不存在", playerName);
            return false;
        }

        if (!PasswordService.verifyPassword(oldPassword, account.getPasswordHash())) {
            LOGGER.warn("修改密码失败: 玩家 {} 旧密码验证失败", playerName);
            return false;
        }

        PasswordService.PasswordValidationResult validationResult = PasswordService.validatePasswordStrength(newPassword);
        if (!validationResult.isValid()) {
            LOGGER.warn("修改密码失败: {}", validationResult.getMessage());
            return false;
        }

        String newPasswordHash = PasswordService.hashPassword(newPassword);
        account.setPasswordHash(newPasswordHash);
        saveAccounts();

        LOGGER.info("玩家 {} 密码修改成功", playerName);
        return true;
    }

    public boolean resetPassword(String playerName, String newPassword) {
        if (playerName == null || newPassword == null) {
            LOGGER.warn("重置密码失败: 参数不能为空");
            return false;
        }

        PlayerAccount account = accountsMap.get(playerName.toLowerCase());
        if (account == null) {
            LOGGER.warn("重置密码失败: 玩家 {} 不存在", playerName);
            return false;
        }

        PasswordService.PasswordValidationResult validationResult = PasswordService.validatePasswordStrength(newPassword);
        if (!validationResult.isValid()) {
            LOGGER.warn("重置密码失败: {}", validationResult.getMessage());
            return false;
        }

        String newPasswordHash = PasswordService.hashPassword(newPassword);
        account.setPasswordHash(newPasswordHash);
        account.resetFailedAttempts();
        saveAccounts();

        LOGGER.info("玩家 {} 密码重置成功", playerName);
        return true;
    }

    public Optional<PlayerAccount> getAccount(String playerName) {
        if (playerName == null || playerName.isEmpty()) {
            return Optional.empty();
        }
        return Optional.ofNullable(accountsMap.get(playerName.toLowerCase()));
    }

    public void updateLoginInfo(String playerName, String ip) {
        PlayerAccount account = accountsMap.get(playerName.toLowerCase());
        if (account != null) {
            account.setLastLoginTime(System.currentTimeMillis());
            account.setLastLoginIp(ip != null ? ip : "");
            account.resetFailedAttempts();
            saveAccounts();
        }
    }

    public int getAccountCount() {
        return accountsMap.size();
    }

    public void clearAccounts() {
        accountsMap.clear();
        saveAccounts();
        LOGGER.info("所有账户数据已清除");
    }
}
