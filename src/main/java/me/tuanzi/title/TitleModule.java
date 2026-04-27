package me.tuanzi.title;

import me.tuanzi.title.commands.TitleAdminCommand;
import me.tuanzi.title.commands.TitleCommand;
import me.tuanzi.title.listeners.TitleListener;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TitleModule implements ModInitializer {
    public static final String MOD_ID = "title-module";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);
    private static TitleModule instance;

    @Override
    public void onInitialize() {
        instance = this;
        ServerLifecycleEvents.SERVER_STARTING.register(TitleManager::init);
        TitleListener.register();
        
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> {
            TitleCommand.register(dispatcher, registryAccess);
            TitleAdminCommand.register(dispatcher, registryAccess);
        });

        ServerTickEvents.END_SERVER_TICK.register(server -> {
            if (TitleManager.getInstance() != null) {
                TitleManager.getInstance().tick();
            }
        });
        
        LOGGER.info("Title Module initialized.");
    }

    public static TitleModule getInstance() {
        return instance;
    }
}
