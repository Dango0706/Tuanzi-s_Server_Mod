package me.tuanzi.cdk;

import me.tuanzi.cdk.commands.CDKAdminCommand;
import me.tuanzi.cdk.commands.CDKCommand;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CDKModule implements ModInitializer {
    public static final String MOD_ID = "cdk-module";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);
    private static CDKModule instance;

    @Override
    public void onInitialize() {
        instance = this;
        ServerLifecycleEvents.SERVER_STARTING.register(CDKManager::init);
        
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> {
            CDKCommand.register(dispatcher, registryAccess);
            CDKAdminCommand.register(dispatcher, registryAccess);
        });
        
        LOGGER.info("CDK Module initialized.");
    }

    public static CDKModule getInstance() {
        return instance;
    }
}
