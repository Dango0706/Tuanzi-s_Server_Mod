package me.tuanzi.shop.mixin;

import me.tuanzi.shop.ShopModule;
import me.tuanzi.shop.events.BlockProtectionHandler;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ServerExplosion;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;

@Mixin(ServerExplosion.class)
public abstract class ServerExplosionMixin {
    @Shadow @Final private ServerLevel level;

    /**
     * 在处理爆炸影响方块之前，过滤掉商店方块（箱子和告示牌）
     */
    @Inject(method = "interactWithBlocks", at = @At("HEAD"))
    private void onInteractWithBlocks(List<BlockPos> targetBlocks, CallbackInfo ci) {
        ShopModule module = ShopModule.getInstance(this.level.getServer());
        if (module == null) return;
        
        BlockProtectionHandler protectionHandler = module.getProtectionHandler();
        if (protectionHandler == null) return;

        // 移除所有属于商店的方块，使其不被爆炸摧毁
        targetBlocks.removeIf(protectionHandler::shouldProtectFromExplosion);
    }
}
