package me.tuanzi.auth.login.session;

public class LoginSession {
    private String playerName;
    private String ipAddress;
    private long loginTime;
    private long expireTime;
    private boolean isLoggedIn;

    public LoginSession() {
    }

    public LoginSession(String playerName, String ipAddress, long sessionDurationMs) {
        this.playerName = playerName;
        this.ipAddress = ipAddress;
        this.loginTime = System.currentTimeMillis();
        this.expireTime = this.loginTime + sessionDurationMs;
        this.isLoggedIn = true;
    }

    public String getPlayerName() {
        return playerName;
    }

    public void setPlayerName(String playerName) {
        this.playerName = playerName;
    }

    public String getIpAddress() {
        return ipAddress;
    }

    public void setIpAddress(String ipAddress) {
        this.ipAddress = ipAddress;
    }

    public long getLoginTime() {
        return loginTime;
    }

    public void setLoginTime(long loginTime) {
        this.loginTime = loginTime;
    }

    public long getExpireTime() {
        return expireTime;
    }

    public void setExpireTime(long expireTime) {
        this.expireTime = expireTime;
    }

    public boolean isLoggedIn() {
        return isLoggedIn;
    }

    public void setLoggedIn(boolean loggedIn) {
        isLoggedIn = loggedIn;
    }

    public boolean isExpired() {
        return System.currentTimeMillis() > expireTime;
    }

    public boolean isValid() {
        return isLoggedIn && !isExpired();
    }

    public boolean isSameIp(String ipAddress) {
        return this.ipAddress != null && this.ipAddress.equals(ipAddress);
    }

    public void refresh(long sessionDurationMs) {
        this.loginTime = System.currentTimeMillis();
        this.expireTime = this.loginTime + sessionDurationMs;
    }
}
