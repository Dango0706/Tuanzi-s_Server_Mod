package me.tuanzi.warp;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import me.tuanzi.warp.commands.WarpCommand;
import me.tuanzi.warp.commands.WarpAdminCommand;
import me.tuanzi.warp.commands.WarpBackCommand;

public class WarpModule implements ModInitializer {
    public static final String MOD_ID = "warp-module";

    @Override
    public void onInitialize() {
        ServerLifecycleEvents.SERVER_STARTING.register(server -> {
            WarpManager.init(server);
            TeleportManager.init();
        });

        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> {
            WarpCommand.register(dispatcher, registryAccess);
            WarpAdminCommand.register(dispatcher, registryAccess);
            WarpBackCommand.register(dispatcher, registryAccess);
        });
    }
}
