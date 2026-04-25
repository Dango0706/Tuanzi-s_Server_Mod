package me.tuanzi.cdk;

import me.tuanzi.statistics.StatisticsModule;
import me.tuanzi.statistics.data.PlayerStatistics;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

import java.security.SecureRandom;
import java.util.List;
import java.util.UUID;

public class CDKManager {
    private static CDKManager instance;
    private final MinecraftServer server;
    private final CDKHistoryManager historyManager;
    private final SecureRandom random = new SecureRandom();

    private CDKManager(MinecraftServer server) {
        this.server = server;
        this.historyManager = new CDKHistoryManager();
    }

    public static void init(MinecraftServer server) {
        instance = new CDKManager(server);
    }

    public static CDKManager getInstance() {
        return instance;
    }

    public CDKData getCDKData() {
        return CDKStateSaver.getServerState(server).getData();
    }

    public CDKHistoryManager getHistoryManager() {
        return historyManager;
    }

    public void redeemCDK(ServerPlayer player, String code) {
        CDKEntry entry = getCDKData().getCDK(code);
        if (entry == null) {
            player.sendSystemMessage(Component.literal("§c无效的礼包码。"));
            return;
        }

        if (entry.isExpired()) {
            player.sendSystemMessage(Component.literal("§c该礼包码已过期。"));
            return;
        }

        if (entry.getType() == CDKType.GLOBAL_SINGLE && entry.getCurrentUses() > 0) {
            player.sendSystemMessage(Component.literal("§c该礼包码已被他人领用。"));
            return;
        }

        if (entry.isFull()) {
            player.sendSystemMessage(Component.literal("§c该礼包码的使用次数已达上限。"));
            return;
        }

        if (entry.hasUsed(player.getUUID())) {
            player.sendSystemMessage(Component.literal("§c你已经领取过该礼包了。"));
            return;
        }

        // --- SUCCESS LOGIC ---
        entry.incrementUses();
        entry.markUsed(player.getUUID());
        historyManager.addRecord(player.getUUID(), code, entry.getCommands());
        
        // Stats
        PlayerStatistics stats = StatisticsModule.getInstance().getDataManager().getPlayerStatistics(player.getName().getString());
        stats.addCdkRedeemed();

        // Execution
        executeCDKCommands(player, entry.getCommands());
        
        // Notify
        player.sendSystemMessage(Component.literal(entry.getSuccessMessage()));
        
        CDKStateSaver.getServerState(server).setDirty();
    }

    private void executeCDKCommands(ServerPlayer player, List<String> commands) {
        String playerName = player.getName().getString();
        for (String cmd : commands) {
            String processedCmd = cmd.replace("%player%", playerName);
            // Execute as console (Permission 4) for safety
            server.getCommands().performPrefixedCommand(server.createCommandSourceStack(), processedCmd);
        }
    }

    public String generateRandomCode() {
        String chars = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 12; i++) {
            sb.append(chars.charAt(random.nextInt(chars.length())));
        }
        return sb.toString();
    }

    public String createCDK(String code, CDKType type, int maxUses) {
        if (code.equalsIgnoreCase("random")) {
            code = generateRandomCode();
        }
        CDKEntry entry = new CDKEntry(code, type, maxUses);
        getCDKData().addCDK(entry);
        CDKStateSaver.getServerState(server).setDirty();
        return code;
    }

    public int bulkGenerate(String baseCode, int count) {
        CDKEntry base = getCDKData().getCDK(baseCode);
        if (base == null) return 0;

        int success = 0;
        for (int i = 0; i < count; i++) {
            String newCode = generateRandomCode();
            if (getCDKData().getCDK(newCode) == null) {
                getCDKData().addCDK(base.cloneWithNewCode(newCode));
                success++;
            }
        }
        CDKStateSaver.getServerState(server).setDirty();
        return success;
    }
}
