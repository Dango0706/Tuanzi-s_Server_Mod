package me.tuanzi.auth.login.data;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.loader.api.FabricLoader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class PremiumPlayerManager {
    private static final Logger LOGGER = LoggerFactory.getLogger("PremiumPlayerManager");
    private static final String DATA_DIR = FabricLoader.getInstance().getConfigDir().resolve("auth").toString();
    private static final String PREMIUM_PLAYERS_FILE = DATA_DIR + File.separator + "premium_players.json";

    private static PremiumPlayerManager instance;
    private final Set<String> loggedPlayers;
    private final Gson gson;

    private PremiumPlayerManager() {
        this.loggedPlayers = ConcurrentHashMap.newKeySet();
        this.gson = new GsonBuilder().setPrettyPrinting().create();
        new File(DATA_DIR).mkdirs();
    }

    public static synchronized PremiumPlayerManager getInstance() {
        if (instance == null) {
            instance = new PremiumPlayerManager();
        }
        return instance;
    }

    public void load() {
        File file = new File(PREMIUM_PLAYERS_FILE);
        if (file.exists()) {
            String content = me.tuanzi.utils.SafeFileIO.readStringSafely(file.toPath(), LOGGER);
            if (content != null && !content.isEmpty()) {
                try {
                    String[] players = gson.fromJson(content, String[].class);
                    if (players != null) {
                        loggedPlayers.clear();
                        for (String player : players) {
                            if (player != null && !player.isEmpty()) {
                                loggedPlayers.add(player.toLowerCase());
                            }
                        }
                        LOGGER.info("成功加载 {} 个已登录正版玩家记录", loggedPlayers.size());
                    }
                } catch (Exception e) {
                    LOGGER.error("解析已登录正版玩家记录失败: {}", e.getMessage());
                }
            }
        }
    }

    public void save() {
        try {
            String[] players = loggedPlayers.toArray(new String[0]);
            String content = gson.toJson(players);
            me.tuanzi.utils.SafeFileIO.writeStringSafely(new File(PREMIUM_PLAYERS_FILE).toPath(), content, LOGGER);
            me.tuanzi.TuanzisServerMod.debug("成功保存 {} 个已登录正版玩家记录", loggedPlayers.size());
        } catch (IOException e) {
            LOGGER.error("保存已登录正版玩家记录失败: {}", e.getMessage());
        }
    }

    public void markAsLogged(String playerName) {
        if (playerName == null || playerName.isEmpty()) {
            return;
        }
        if (loggedPlayers.add(playerName.toLowerCase())) {
            save();
            LOGGER.info("已记录正版玩家真人首次登录: {}", playerName);
        }
    }

    public boolean hasLogged(String playerName) {
        if (playerName == null || playerName.isEmpty()) {
            return false;
        }
        return loggedPlayers.contains(playerName.toLowerCase());
    }
}
