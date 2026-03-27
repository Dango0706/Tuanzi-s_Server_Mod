package com.example.statistics;

import com.example.statistics.commands.ScoreboardCommand;
import com.example.statistics.commands.StatsCommand;
import com.example.statistics.data.StatisticsDataManager;
import com.example.statistics.listeners.*;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.event.player.PlayerBlockBreakEvents;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.fabricmc.fabric.api.event.player.UseItemCallback;
import net.fabricmc.fabric.api.message.v1.ServerMessageEvents;
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
        
        LOGGER.info("Initializing Statistics Module...");
        
        dataManager = new StatisticsDataManager();
        dataManager.loadData();
        
        registerListeners();
        registerCommands();
        registerServerLifecycleEvents();
        
        LOGGER.info("Statistics Module initialized successfully!");
    }
    
    private void registerListeners() {
        ServerPlayConnectionEvents.JOIN.register(new PlayerJoinListener());
        ServerPlayConnectionEvents.DISCONNECT.register(new PlayerLeaveListener());
        
        PlayerBlockBreakEvents.BEFORE.register(new BlockBreakListener());
        UseBlockCallback.EVENT.register(new BlockPlaceListener());
        
        ServerTickEvents.END_SERVER_TICK.register(new PlayerMoveListener());
        
        ServerLivingEntityEvents.ALLOW_DEATH.register(new EntityDeathListener());
        
        UseItemCallback.EVENT.register(new ItemDurabilityListener());
        
        ServerLivingEntityEvents.AFTER_DAMAGE.register(new DamageListener());
        
        ChatListener.register();
    }
    
    private void registerCommands() {
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> {
            StatsCommand.register(dispatcher, registryAccess);
            ScoreboardCommand.register(dispatcher, registryAccess);
        });
    }
    
    private void registerServerLifecycleEvents() {
        ServerLifecycleEvents.SERVER_STOPPING.register(server -> {
            if (dataManager != null) {
                dataManager.shutdown();
            }
        });
    }
    
    public static StatisticsModule getInstance() {
        return instance;
    }
    
    public StatisticsDataManager getDataManager() {
        return dataManager;
    }
}
