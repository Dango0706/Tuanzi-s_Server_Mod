package me.tuanzi.auth.login.security;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Base64;

public class PasswordService {
    private static final Logger LOGGER = LoggerFactory.getLogger("PasswordService");
    private static final int SALT_LENGTH = 16;
    private static final int MIN_PASSWORD_LENGTH = 6;
    private static final int MAX_PASSWORD_LENGTH = 32;
    private static final String HASH_ALGORITHM = "SHA-256";
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    private PasswordService() {
    }

    public static String hashPassword(String password) {
        if (password == null || password.isEmpty()) {
            throw new IllegalArgumentException("密码不能为空");
        }

        try {
            byte[] salt = generateSalt();
            byte[] hash = hashWithSalt(password, salt);
            return encodeHash(salt, hash);
        } catch (NoSuchAlgorithmException e) {
            LOGGER.error("密码哈希失败: {}", e.getMessage());
            throw new RuntimeException("密码哈希失败", e);
        }
    }

    public static boolean verifyPassword(String plainPassword, String hashedPassword) {
        if (plainPassword == null || hashedPassword == null || hashedPassword.isEmpty()) {
            return false;
        }

        try {
            String[] parts = hashedPassword.split("\\$");
            if (parts.length != 2) {
                LOGGER.warn("无效的哈希密码格式");
                return false;
            }

            byte[] salt = Base64.getDecoder().decode(parts[0]);
            byte[] expectedHash = Base64.getDecoder().decode(parts[1]);
            byte[] actualHash = hashWithSalt(plainPassword, salt);

            return MessageDigest.isEqual(expectedHash, actualHash);
        } catch (Exception e) {
            LOGGER.error("密码验证失败: {}", e.getMessage());
            return false;
        }
    }

    public static PasswordValidationResult validatePasswordStrength(String password) {
        if (password == null) {
            return new PasswordValidationResult(false, "密码不能为空");
        }

        if (password.isEmpty()) {
            return new PasswordValidationResult(false, "密码不能为空");
        }

        if (password.isBlank()) {
            return new PasswordValidationResult(false, "密码不能全是空格");
        }

        if (password.length() < MIN_PASSWORD_LENGTH) {
            return new PasswordValidationResult(false, "密码长度不能少于 " + MIN_PASSWORD_LENGTH + " 个字符");
        }

        if (password.length() > MAX_PASSWORD_LENGTH) {
            return new PasswordValidationResult(false, "密码长度不能超过 " + MAX_PASSWORD_LENGTH + " 个字符");
        }

        return new PasswordValidationResult(true, "密码符合要求");
    }

    private static byte[] generateSalt() {
        byte[] salt = new byte[SALT_LENGTH];
        SECURE_RANDOM.nextBytes(salt);
        return salt;
    }

    private static byte[] hashWithSalt(String password, byte[] salt) throws NoSuchAlgorithmException {
        MessageDigest digest = MessageDigest.getInstance(HASH_ALGORITHM);
        digest.update(salt);
        digest.update(password.getBytes(StandardCharsets.UTF_8));
        return digest.digest();
    }

    private static String encodeHash(byte[] salt, byte[] hash) {
        String encodedSalt = Base64.getEncoder().encodeToString(salt);
        String encodedHash = Base64.getEncoder().encodeToString(hash);
        return encodedSalt + "$" + encodedHash;
    }

    public static class PasswordValidationResult {
        private final boolean valid;
        private final String message;

        public PasswordValidationResult(boolean valid, String message) {
            this.valid = valid;
            this.message = message;
        }

        public boolean isValid() {
            return valid;
        }

        public String getMessage() {
            return message;
        }
    }
}
