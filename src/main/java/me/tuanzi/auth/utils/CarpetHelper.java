package me.tuanzi.auth.utils;

import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.server.level.ServerPlayer;
import carpet.patches.EntityPlayerMPFake;

public class CarpetHelper {

    /**
     * 判断当前服务器是否加载了 Carpet 模组
     */
    public static boolean isCarpetLoaded() {
        return FabricLoader.getInstance().isModLoaded("carpet");
    }

    /**
     * 强类型强绑定判断玩家是否为 Carpet 假人
     * 采用 Classloading 隔离模式，在未加载 Carpet 模组的服务器上绝对不激活内部类，避免 NoClassDefFoundError
     */
    public static boolean isFakePlayer(ServerPlayer player) {
        if (!isCarpetLoaded()) {
            return false;
        }
        return CarpetHolder.isFake(player);
    }

    /**
     * 强类型持有内部类，强类型直接判定 instanceof
     */
    private static class CarpetHolder {
        private static boolean isFake(ServerPlayer player) {
            return player instanceof EntityPlayerMPFake;
        }
    }

    public static final ThreadLocal<net.minecraft.commands.CommandSourceStack> CURRENT_SOURCE = new ThreadLocal<>();
}
