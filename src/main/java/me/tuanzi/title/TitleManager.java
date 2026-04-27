package me.tuanzi.title;

import me.tuanzi.economy.utils.ServerTranslationHelper;
import me.tuanzi.statistics.StatisticsModule;
import me.tuanzi.statistics.data.PlayerStatistics;
import net.minecraft.ChatFormatting;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.scores.PlayerTeam;
import net.minecraft.world.scores.Scoreboard;

import java.util.UUID;
import java.util.Map;

public class TitleManager {
    private static TitleManager instance;
    private final MinecraftServer server;
    private int tickCounter = 0;

    private TitleManager(MinecraftServer server) {
        this.server = server;
    }

    public static void init(MinecraftServer server) {
        instance = new TitleManager(server);
    }

    public static TitleManager getInstance() {
        return instance;
    }

    public TitleData getTitleData() {
        return TitleStateSaver.getServerState(server).getData();
    }

    public void tick() {
        if (++tickCounter >= 1200) { // Every 1 minute
            tickCounter = 0;
            checkExpirations();
        }
    }

    public void checkExpirations() {
        long now = System.currentTimeMillis();
        boolean changed = false;

        // Check each player
        for (PlayerTitleData playerData : getTitleData().getPlayerTitles().values()) {
            UUID uuid = playerData.getPlayerId();
            Map<String, Long> expiries = playerData.getTitleExpiries();
            
            // Use iterator to avoid ConcurrentModificationException if we remove items
            var it = expiries.entrySet().iterator();
            while (it.hasNext()) {
                var entry = it.next();
                String titleId = entry.getKey();
                long expiry = entry.getValue();

                boolean playerExpired = expiry != -1 && now > expiry;
                
                // Check global expiry too
                Title title = getTitleData().getTitles().get(titleId);
                boolean globalExpired = title != null && title.isExpired();

                if (playerExpired || globalExpired) {
                    it.remove();
                    changed = true;
                    
                    if (titleId.equals(playerData.getEquippedTitle())) {
                        playerData.setEquippedTitle(null);
                    }

                    ServerPlayer player = server.getPlayerList().getPlayer(uuid);
                    if (player != null) {
                        String titleName = title != null ? title.displayName().getString() : titleId;
                        ServerTranslationHelper.sendMessage(player, "title.notification.expired", titleName);
                        updatePlayerScoreboard(player);
                    }
                }
            }
        }

        if (changed) {
            TitleStateSaver.getServerState(server).setDirty();
        }
    }

    public void equipTitle(ServerPlayer player, String titleId) {
        PlayerTitleData playerData = getTitleData().getOrCreatePlayerTitleData(player.getUUID());
        
        if (titleId != null) {
            if (!playerData.hasTitle(titleId)) {
                return;
            }
            // Also check global
            Title title = getTitleData().getTitles().get(titleId);
            if (title == null || title.isExpired()) {
                return;
            }
        }

        playerData.setEquippedTitle(titleId);
        updatePlayerScoreboard(player);
        
        // Stats
        PlayerStatistics stats = StatisticsModule.getInstance().getDataManager().getPlayerStatistics(player.getName().getString());
        stats.addTitleSwitch();
    }

    public void updatePlayerScoreboard(ServerPlayer player) {
        Scoreboard scoreboard = server.getScoreboard();
        String playerName = player.getScoreboardName();
        String teamName = "t_title_" + player.getName().getString();
        
        PlayerTitleData playerData = getTitleData().getOrCreatePlayerTitleData(player.getUUID());
        String equippedId = playerData.getEquippedTitle();
        
        if (equippedId == null) {
            PlayerTeam team = scoreboard.getPlayerTeam(teamName);
            if (team != null) {
                scoreboard.removePlayerFromTeam(playerName, team);
            }
            return;
        }

        Title title = getTitleData().getTitles().get(equippedId);
        if (title == null || title.isExpired() || !playerData.hasTitle(equippedId)) {
            playerData.setEquippedTitle(null);
            PlayerTeam team = scoreboard.getPlayerTeam(teamName);
            if (team != null) {
                scoreboard.removePlayerFromTeam(playerName, team);
            }
            return;
        }

        PlayerTeam team = scoreboard.getPlayerTeam(teamName);
        if (team == null) {
            team = scoreboard.addPlayerTeam(teamName);
        }

        // Prepend title to name
        Component prefix = title.displayName().copy().append(Component.literal(" "));
        team.setPlayerPrefix(prefix);
        team.setColor(ChatFormatting.RESET); // Keep name color reset
        
        scoreboard.addPlayerToTeam(playerName, team);
    }

    public void giveTitle(UUID playerId, String titleId, long durationMillis, boolean fromAdmin) {
        Title title = getTitleData().getTitles().get(titleId);
        if (title == null) return;

        PlayerTitleData playerData = getTitleData().getOrCreatePlayerTitleData(playerId);
        
        long currentExpiry = playerData.getTitleExpiries().getOrDefault(titleId, 0L);
        long newExpiry;
        
        if (durationMillis == -1) {
            newExpiry = -1;
        } else {
            long start = (currentExpiry == -1 || currentExpiry < System.currentTimeMillis()) ? System.currentTimeMillis() : currentExpiry;
            newExpiry = start + durationMillis;
        }

        playerData.unlockTitle(titleId, newExpiry);
        TitleStateSaver.getServerState(server).setDirty();
        
        ServerPlayer player = server.getPlayerList().getPlayer(playerId);
        
        // Resolve name for statistics
        String playerName = null;
        if (player != null) {
            playerName = player.getName().getString();
            ServerTranslationHelper.sendMessage(player, "title.notification.receive", title.displayName().getString());
        } else {
            String msg = ServerTranslationHelper.translate("title.notification.receive", "zh_cn", title.displayName().getString());
            playerData.addNotification(msg);
            // Try to resolve name from Whitelist
            me.tuanzi.auth.whitelist.WhitelistManager whitelistManager = me.tuanzi.auth.AuthModule.getInstance().getWhitelistManager();
            if (whitelistManager != null) {
                playerName = whitelistManager.getPlayerName(playerId);
            }
        }

        if (playerName != null) {
            PlayerStatistics stats = StatisticsModule.getInstance().getDataManager().getPlayerStatistics(playerName);
            stats.addTitleOwned();
            if (fromAdmin) stats.addTitleFromAdmin();
            else stats.addTitleFromItem();
        }
    }

    public void giveTitleToAll(String titleId, long durationMillis) {
        Title title = getTitleData().getTitles().get(titleId);
        if (title == null) return;

        // Iterate all players who have title data
        for (UUID uuid : getTitleData().getPlayerTitles().keySet()) {
            giveTitle(uuid, titleId, durationMillis, true);
        }
        
        // Also check whitelist for players who don't have title data yet
        me.tuanzi.auth.whitelist.WhitelistManager whitelistManager = me.tuanzi.auth.AuthModule.getInstance().getWhitelistManager();
        if (whitelistManager != null) {
            for (UUID uuid : whitelistManager.getWhitelistMap().keySet()) {
                giveTitle(uuid, titleId, durationMillis, true);
            }
        }
    }

    public ItemStack createTitleItem(String titleId, long durationMillis) {
        Title title = getTitleData().getTitles().get(titleId);
        if (title == null) return ItemStack.EMPTY;

        ItemStack stack = new ItemStack(Items.NAME_TAG);
        String name = ServerTranslationHelper.translate("title.item.name", "zh_cn", title.displayName().getString());
        stack.set(DataComponents.CUSTOM_NAME, Component.literal(name));
        
        CompoundTag tag = new CompoundTag();
        tag.putString("tuanzi_title", titleId);
        tag.putLong("tuanzi_duration", durationMillis);
        stack.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
        
        return stack;
    }
    
    public void deleteTitle(String titleId) {
        getTitleData().removeTitle(titleId);
        // Clean up scoreboard teams if necessary
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            updatePlayerScoreboard(player);
        }
        TitleStateSaver.getServerState(server).setDirty();
    }
}
