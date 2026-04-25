package me.tuanzi.title;

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

public class TitleManager {
    private static TitleManager instance;
    private final MinecraftServer server;

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

    public void equipTitle(ServerPlayer player, String titleId) {
        PlayerTitleData playerData = getTitleData().getOrCreatePlayerTitleData(player.getUUID());
        
        if (titleId != null && !playerData.getUnlockedTitles().contains(titleId)) {
            return;
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
        if (title == null) {
            playerData.setEquippedTitle(null);
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

    public void giveTitle(UUID playerId, String titleId, boolean fromAdmin) {
        Title title = getTitleData().getTitles().get(titleId);
        if (title == null) return;

        PlayerTitleData playerData = getTitleData().getOrCreatePlayerTitleData(playerId);
        if (playerData.getUnlockedTitles().contains(titleId)) return;

        playerData.unlockTitle(titleId);
        
        String msg = "§b你获得了称号: §f" + title.displayName().getString();
        ServerPlayer player = server.getPlayerList().getPlayer(playerId);
        
        // Resolve name for statistics
        String playerName = null;
        if (player != null) {
            playerName = player.getName().getString();
            player.sendSystemMessage(Component.literal(msg));
        } else {
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

    public void giveTitleToAll(String titleId) {
        Title title = getTitleData().getTitles().get(titleId);
        if (title == null) return;

        // Iterate all players who have title data
        for (UUID uuid : getTitleData().getPlayerTitles().keySet()) {
            giveTitle(uuid, titleId, true);
        }
        
        // Also check whitelist for players who don't have title data yet
        me.tuanzi.auth.whitelist.WhitelistManager whitelistManager = me.tuanzi.auth.AuthModule.getInstance().getWhitelistManager();
        if (whitelistManager != null) {
            for (UUID uuid : whitelistManager.getWhitelistMap().keySet()) {
                giveTitle(uuid, titleId, true);
            }
        }
    }

    public ItemStack createTitleItem(String titleId) {
        Title title = getTitleData().getTitles().get(titleId);
        if (title == null) return ItemStack.EMPTY;

        ItemStack stack = new ItemStack(Items.NAME_TAG);
        stack.set(DataComponents.CUSTOM_NAME, Component.literal("§b称号命名牌: §f").append(title.displayName()));
        
        CompoundTag tag = new CompoundTag();
        tag.putString("tuanzi_title", titleId);
        stack.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
        
        return stack;
    }
    
    public void deleteTitle(String titleId) {
        getTitleData().removeTitle(titleId);
        // Clean up scoreboard teams if necessary
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            updatePlayerScoreboard(player);
        }
    }
}
