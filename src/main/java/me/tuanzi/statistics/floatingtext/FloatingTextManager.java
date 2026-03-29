package me.tuanzi.statistics.floatingtext;

import me.tuanzi.statistics.StatisticsModule;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Display;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

import java.util.Collections;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class FloatingTextManager {
    private static FloatingTextManager instance;
    private final Map<String, FloatingTextData> floatingTexts = new ConcurrentHashMap<>();
    private final Map<String, Display.TextDisplay> textDisplays = new ConcurrentHashMap<>();
    private final Map<String, Long> lastUpdateTicks = new ConcurrentHashMap<>();
    private final FloatingTextDataManager dataManager;

    private FloatingTextManager() {
        this.dataManager = new FloatingTextDataManager();
    }

    public static FloatingTextManager getInstance() {
        if (instance == null) {
            instance = new FloatingTextManager();
        }
        return instance;
    }

    public void loadData() {
        Map<String, FloatingTextData> loadedData = dataManager.loadData();
        floatingTexts.clear();
        floatingTexts.putAll(loadedData);
        StatisticsModule.LOGGER.info("Loaded {} floating texts", floatingTexts.size());
    }

    public void saveData() {
        dataManager.saveData(floatingTexts);
        StatisticsModule.LOGGER.debug("Saved {} floating texts", floatingTexts.size());
    }

    public boolean createFloatingText(String id, ServerLevel level, double x, double y, double z) {
        if (floatingTexts.containsKey(id)) {
            return false;
        }

        double adjustedY = adjustYForCollision(level, x, y, z);

        FloatingTextData data = new FloatingTextData();
        data.setId(id);
        data.setWorldName(getWorldKeyString(level));
        data.setX(x);
        data.setY(adjustedY);
        data.setZ(z);
        data.setUpdateInterval(20);
        data.setCreatedTime(System.currentTimeMillis());

        floatingTexts.put(id, data);

        Display.TextDisplay textDisplay = createTextDisplayEntity(level, x, adjustedY, z);
        if (textDisplay != null) {
            textDisplays.put(id, textDisplay);
            lastUpdateTicks.put(id, 0L);
            saveData();
            return true;
        }

        floatingTexts.remove(id);
        return false;
    }

    private String getWorldKeyString(ServerLevel level) {
        ResourceKey<Level> dimensionKey = level.dimension();
        return dimensionKey.identifier().toString();
    }

    private Display.TextDisplay createTextDisplayEntity(ServerLevel level, double x, double y, double z) {
        try {
            Display.TextDisplay textDisplay = new Display.TextDisplay(EntityType.TEXT_DISPLAY, level);
            textDisplay.setPos(x, y, z);
            textDisplay.setText(Component.literal("悬浮文字"));
            textDisplay.setBillboardConstraints(Display.BillboardConstraints.CENTER);
            textDisplay.setViewRange(1.0F);
            textDisplay.setLineWidth(200);

            level.addFreshEntity(textDisplay);
            return textDisplay;
        } catch (Exception e) {
            StatisticsModule.LOGGER.error("Failed to create TextDisplay entity: {}", e.getMessage());
            return null;
        }
    }

    private double adjustYForCollision(ServerLevel level, double x, double y, double z) {
        BlockPos pos = BlockPos.containing(x, y, z);
        BlockState state = level.getBlockState(pos);

        int adjustments = 0;
        while (!state.isAir() && adjustments < 2) {
            y += 1.0;
            pos = BlockPos.containing(x, y, z);
            state = level.getBlockState(pos);
            adjustments++;
        }

        return y;
    }

    public boolean setContent(String id, String statType, String displayName) {
        FloatingTextData data = floatingTexts.get(id);
        if (data == null) {
            return false;
        }

        data.setStatType(statType);
        data.setDisplayName(displayName);
        saveData();

        updateTextDisplay(id);
        return true;
    }

    public boolean setUpdateInterval(String id, int ticks) {
        FloatingTextData data = floatingTexts.get(id);
        if (data == null) {
            return false;
        }

        data.setUpdateInterval(Math.max(1, Math.min(1200, ticks)));
        saveData();
        return true;
    }

    public boolean moveFloatingText(String id, ServerLevel level, double x, double y, double z, boolean relative) {
        FloatingTextData data = floatingTexts.get(id);
        if (data == null) {
            return false;
        }

        double newX = relative ? data.getX() + x : x;
        double newY = relative ? data.getY() + y : y;
        double newZ = relative ? data.getZ() + z : z;

        newY = adjustYForCollision(level, newX, newY, newZ);

        data.setX(newX);
        data.setY(newY);
        data.setZ(newZ);

        Display.TextDisplay textDisplay = textDisplays.get(id);
        if (textDisplay != null) {
            textDisplay.setPos(newX, newY, newZ);
        }

        saveData();
        return true;
    }

    public boolean setColor(String id, String color) {
        FloatingTextData data = floatingTexts.get(id);
        if (data == null) {
            return false;
        }

        data.setColor(color);
        saveData();
        updateTextDisplay(id);
        return true;
    }

    public boolean deleteFloatingText(String id) {
        FloatingTextData data = floatingTexts.remove(id);
        if (data == null) {
            return false;
        }

        Display.TextDisplay textDisplay = textDisplays.remove(id);
        if (textDisplay != null) {
            textDisplay.discard();
        }

        lastUpdateTicks.remove(id);
        saveData();
        return true;
    }

    public FloatingTextData getFloatingText(String id) {
        return floatingTexts.get(id);
    }

    public Map<String, FloatingTextData> getAllFloatingTexts() {
        return Collections.unmodifiableMap(floatingTexts);
    }

    public void updateAllDisplays(long currentTick) {
        for (String id : floatingTexts.keySet()) {
            FloatingTextData data = floatingTexts.get(id);
            if (data == null || data.getStatType() == null || data.getStatType().isEmpty()) {
                continue;
            }

            Long lastUpdate = lastUpdateTicks.getOrDefault(id, 0L);
            if (currentTick - lastUpdate >= data.getUpdateInterval()) {
                updateTextDisplay(id);
                lastUpdateTicks.put(id, currentTick);
            }
        }
    }

    public void updateTextDisplay(String id) {
        FloatingTextData data = floatingTexts.get(id);
        Display.TextDisplay textDisplay = textDisplays.get(id);

        if (data == null || textDisplay == null || data.getStatType() == null || data.getStatType().isEmpty()) {
            return;
        }

        String leaderboardText = LeaderboardFormatter.formatLeaderboard(
                data.getStatType(),
                data.getDisplayName(),
                data.getColor()
        );

        textDisplay.setText(Component.literal(leaderboardText));
    }

    public void rebuildAllEntities(ServerLevel level) {
        String worldKey = getWorldKeyString(level);
        for (Map.Entry<String, FloatingTextData> entry : floatingTexts.entrySet()) {
            String id = entry.getKey();
            FloatingTextData data = entry.getValue();

            if (!data.getWorldName().equals(worldKey)) {
                continue;
            }

            Display.TextDisplay textDisplay = createTextDisplayEntity(level, data.getX(), data.getY(), data.getZ());
            if (textDisplay != null) {
                textDisplays.put(id, textDisplay);
                updateTextDisplay(id);
            }
        }
    }

    public void clearAll() {
        for (Display.TextDisplay textDisplay : textDisplays.values()) {
            if (textDisplay != null) {
                textDisplay.discard();
            }
        }
        textDisplays.clear();
        floatingTexts.clear();
        lastUpdateTicks.clear();
    }
}
