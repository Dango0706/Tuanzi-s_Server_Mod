package me.tuanzi.statistics;

import me.tuanzi.statistics.commands.FloatingTextCommand;
import me.tuanzi.statistics.commands.ScoreboardCommand;
import me.tuanzi.statistics.commands.StatsCommand;
import me.tuanzi.statistics.data.StatisticsDataManager;
import me.tuanzi.statistics.floatingtext.FloatingTextManager;
import me.tuanzi.statistics.listeners.*;
import me.tuanzi.statistics.scoreboard.ScoreboardManager;
import me.tuanzi.statistics.util.StatsTranslationHelper;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.event.player.PlayerBlockBreakEvents;
import net.fabricmc.fabric.api.event.player.UseItemCallback;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class StatisticsModule implements ModInitializer {
    public static final String MOD_ID = "statistics-module";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);
    
    private static StatisticsModule instance;
    private StatisticsDataManager dataManager;
    
    @Override
    public void onInitialize() {
        instance = this;
        
        LOGGER.info("正在初始化统计模块...");
        
        StatsTranslationHelper.initialize();
        
        dataManager = new StatisticsDataManager();
        dataManager.loadData();
        
        registerListeners();
        registerCommands();
        registerServerLifecycleEvents();
        
        LOGGER.info("统计模块初始化完成");
    }
    
    private void registerListeners() {
        ServerPlayConnectionEvents.JOIN.register(new PlayerJoinListener());
        ServerPlayConnectionEvents.DISCONNECT.register(new PlayerLeaveListener());
        
        PlayerBlockBreakEvents.BEFORE.register(new BlockBreakListener());
        
        ServerTickEvents.END_SERVER_TICK.register(new PlayerMoveListener());
        
        ServerLivingEntityEvents.ALLOW_DEATH.register(new EntityDeathListener());
        
        UseItemCallback.EVENT.register(new ItemDurabilityListener());
        
        ServerLivingEntityEvents.AFTER_DAMAGE.register(new DamageListener());
        
        ChatListener.register();
        
        SessionListener.register();
        
        PlayerActivityListener.register();
    }
    
    private void registerCommands() {
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> {
            StatsCommand.register(dispatcher, registryAccess);
            ScoreboardCommand.register(dispatcher, registryAccess);
            FloatingTextCommand.register(dispatcher, registryAccess);
        });
    }
    
    private void registerServerLifecycleEvents() {
        ServerLifecycleEvents.SERVER_STARTING.register(server -> {
            FloatingTextManager.getInstance().loadData();
        });
        
        ServerLifecycleEvents.SERVER_STOPPING.register(server -> {
            if (dataManager != null) {
                dataManager.shutdown();
            }
            ScoreboardManager.reset();
            FloatingTextManager.getInstance().saveData();
            FloatingTextManager.getInstance().clearAll();
        });
        
        ServerTickEvents.END_SERVER_TICK.register(server -> {
            FloatingTextManager.getInstance().updateAllDisplays(server.getTickCount());
        });
        
        ServerLifecycleEvents.SERVER_STARTED.register(server -> {
            for (net.minecraft.server.level.ServerLevel level : server.getAllLevels()) {
                FloatingTextManager.getInstance().rebuildAllEntities(level);
            }
            ScoreboardManager.getInstance(server).loadConfig();
        });
    }
    
    public static StatisticsModule getInstance() {
        return instance;
    }
    
    public StatisticsDataManager getDataManager() {
        return dataManager;
    }
}
