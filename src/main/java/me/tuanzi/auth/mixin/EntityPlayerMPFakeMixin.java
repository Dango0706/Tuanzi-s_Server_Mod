package me.tuanzi.auth.mixin;

import carpet.patches.EntityPlayerMPFake;
import me.tuanzi.auth.AuthModule;
import me.tuanzi.auth.login.data.AccountManager;
import me.tuanzi.auth.login.data.PremiumPlayerManager;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = EntityPlayerMPFake.class, remap = false)
public class EntityPlayerMPFakeMixin {

    @Inject(
            method = "createFake",
            at = @At("HEAD"),
            cancellable = true
    )
    private static void onCreateFake(String username, MinecraftServer server, Vec3 pos, double yaw, double pitch, ResourceKey<Level> dimension, GameType gamemode, boolean flying, CallbackInfoReturnable<EntityPlayerMPFake> cir) {
        AuthModule authModule = AuthModule.getInstance();
        if (authModule == null) {
            return;
        }

        AccountManager accountManager = authModule.getAccountManager();
        boolean isRegistered = accountManager != null && accountManager.isRegistered(username);

        boolean isPremium = false;
        boolean hasRealLogged = false;
        try {
            isPremium = me.tuanzi.auth.core.PremiumCache.getInstance().checkAndCachePlayerType(username) == me.tuanzi.auth.core.PlayerType.PREMIUM;
            hasRealLogged = PremiumPlayerManager.getInstance().hasLogged(username);
        } catch (Exception e) {
            AuthModule.LOGGER.error("[AuthModule] 检查假人 {} 的正版状态时发生异常", username, e);
        }

        if (isRegistered || (isPremium && hasRealLogged)) {
            String reason = isRegistered ? "此玩家已注册账号" : "此正版玩家曾以真人身份登录过服务器";
            AuthModule.LOGGER.warn("[AuthModule] 拒绝生成假人 {}: {}", username, reason);
            me.tuanzi.TuanzisServerMod.debug("[身份验证] 拒绝假人 {} 连接: 已注册={}, 正版且有登录记录={}", username, isRegistered, (isPremium && hasRealLogged));

            // 获取执行命令的玩家并发送详细的中英文错误
            net.minecraft.commands.CommandSourceStack source = me.tuanzi.auth.utils.CarpetHelper.CURRENT_SOURCE.get();
            if (source != null) {
                String lang = me.tuanzi.auth.utils.TranslationHelper.getLanguage(source);
                String msg = lang.equals("zh_cn")
                        ? "§c禁止生成已注册账号或已登录正版玩家的假人"
                        : "§cSpawning fake players with registered or logged premium accounts is prohibited";
                source.sendFailure(net.minecraft.network.chat.Component.literal(msg));
            }

            // 返回 null 以阻止假人创建
            cir.setReturnValue(null);
        }
    }
}
