package me.tuanzi.economy;

import me.tuanzi.economy.api.EconomyAPIImpl;
import me.tuanzi.economy.commands.BalanceCommand;
import me.tuanzi.economy.commands.EconAdminCommand;
import me.tuanzi.economy.commands.PayCommand;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class EconomyModule implements ModInitializer {
    public static final String MOD_ID = "economy-module";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    @Override
    public void onInitialize() {
        LOGGER.info("Initializing Economy Module...");

        registerCommands();
        registerServerLifecycleEvents();

        LOGGER.info("Economy Module initialized successfully!");
    }

    private void registerCommands() {
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> {
            EconAdminCommand.register(dispatcher, registryAccess);
            BalanceCommand.register(dispatcher, registryAccess);
            PayCommand.register(dispatcher, registryAccess);
        });
    }

    private void registerServerLifecycleEvents() {
        ServerLifecycleEvents.SERVER_STARTING.register(server -> {
            LOGGER.info("Economy Module: Server starting...");
        });

        ServerLifecycleEvents.SERVER_STOPPING.register(server -> {
            LOGGER.info("Economy Module: Server stopping, saving data...");
            EconomyAPIImpl.resetInstance();
        });
    }
}
