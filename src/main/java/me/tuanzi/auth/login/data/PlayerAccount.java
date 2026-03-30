package me.tuanzi.auth.login.data;

public class PlayerAccount {
    private String playerName;
    private String passwordHash;
    private long registerTime;
    private long lastLoginTime;
    private String lastLoginIp;
    private int failedAttempts;
    private long lockedUntil;

    public PlayerAccount() {
    }

    public PlayerAccount(String playerName, String passwordHash) {
        this.playerName = playerName;
        this.passwordHash = passwordHash;
        this.registerTime = System.currentTimeMillis();
        this.lastLoginTime = 0;
        this.lastLoginIp = "";
        this.failedAttempts = 0;
        this.lockedUntil = 0;
    }

    public String getPlayerName() {
        return playerName;
    }

    public void setPlayerName(String playerName) {
        this.playerName = playerName;
    }

    public String getPasswordHash() {
        return passwordHash;
    }

    public void setPasswordHash(String passwordHash) {
        this.passwordHash = passwordHash;
    }

    public long getRegisterTime() {
        return registerTime;
    }

    public void setRegisterTime(long registerTime) {
        this.registerTime = registerTime;
    }

    public long getLastLoginTime() {
        return lastLoginTime;
    }

    public void setLastLoginTime(long lastLoginTime) {
        this.lastLoginTime = lastLoginTime;
    }

    public String getLastLoginIp() {
        return lastLoginIp;
    }

    public void setLastLoginIp(String lastLoginIp) {
        this.lastLoginIp = lastLoginIp;
    }

    public int getFailedAttempts() {
        return failedAttempts;
    }

    public void setFailedAttempts(int failedAttempts) {
        this.failedAttempts = failedAttempts;
    }

    public long getLockedUntil() {
        return lockedUntil;
    }

    public void setLockedUntil(long lockedUntil) {
        this.lockedUntil = lockedUntil;
    }

    public boolean isLocked() {
        return lockedUntil > System.currentTimeMillis();
    }

    public void incrementFailedAttempts() {
        this.failedAttempts++;
    }

    public void resetFailedAttempts() {
        this.failedAttempts = 0;
        this.lockedUntil = 0;
    }
}
