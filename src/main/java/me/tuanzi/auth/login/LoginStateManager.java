package me.tuanzi.auth.login;

import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class LoginStateManager {
    
    private static LoginStateManager instance;
    
    private final Set<UUID> loggedInPlayers = ConcurrentHashMap.newKeySet();
    private final Map<UUID, Long> loginTimes = new ConcurrentHashMap<>();
    
    private LoginStateManager() {
    }
    
    public static synchronized LoginStateManager getInstance() {
        if (instance == null) {
            instance = new LoginStateManager();
        }
        return instance;
    }
    
    public boolean isLoggedIn(UUID playerId) {
        return loggedInPlayers.contains(playerId);
    }
    
    public boolean isLoggedIn(String playerName) {
        return loggedInPlayers.stream()
                .anyMatch(uuid -> getPlayerNameByUUID(uuid).equalsIgnoreCase(playerName));
    }
    
    public void setLoggedIn(UUID playerId) {
        loggedInPlayers.add(playerId);
        loginTimes.put(playerId, System.currentTimeMillis());
    }
    
    public void setLoggedOut(UUID playerId) {
        loggedInPlayers.remove(playerId);
        loginTimes.remove(playerId);
    }
    
    public long getLoginTime(UUID playerId) {
        return loginTimes.getOrDefault(playerId, 0L);
    }
    
    public void clearAll() {
        loggedInPlayers.clear();
        loginTimes.clear();
    }
    
    public int getLoggedInCount() {
        return loggedInPlayers.size();
    }
    
    private String getPlayerNameByUUID(UUID uuid) {
        return "";
    }
}
