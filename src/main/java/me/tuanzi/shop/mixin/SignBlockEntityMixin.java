package me.tuanzi.shop.mixin;

import me.tuanzi.shop.ShopModule;
import me.tuanzi.shop.events.SignChangeHandler;
import me.tuanzi.shop.shop.ShopManager;
import net.minecraft.server.network.FilteredText;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.SignBlockEntity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;

@Mixin(SignBlockEntity.class)
public abstract class SignBlockEntityMixin {
    private static final Logger LOGGER = LoggerFactory.getLogger(ShopModule.MOD_ID);

    @Inject(method = "updateSignText", at = @At("RETURN"))
    private void onUpdateSignText(Player player, boolean frontText, List<FilteredText> lines, CallbackInfo ci) {
        SignBlockEntity self = (SignBlockEntity) (Object) this;
        Level level = self.getLevel();
        
        LOGGER.info("[商店调试] Mixin updateSignText 触发 - 玩家: {}, 正面: {}, 世界: {}", 
                player.getName().getString(), frontText, level != null ? !level.isClientSide() : "null");
        
        if (level == null || level.isClientSide()) {
            LOGGER.debug("[商店调试] 跳过客户端或空世界");
            return;
        }

        if (!(player instanceof ServerPlayer serverPlayer)) {
            LOGGER.debug("[商店调试] 跳过非服务端玩家");
            return;
        }

        LOGGER.info("[商店调试] 告示牌位置: {}", self.getBlockPos());
        LOGGER.info("[商店调试] 告示牌内容行数: {}", lines.size());
        for (int i = 0; i < lines.size(); i++) {
            LOGGER.info("[商店调试]   行[{}]: '{}'", i, lines.get(i).raw());
        }

        ShopManager shopManager = ShopManager.getInstance(serverPlayer.level().getServer());
        if (shopManager == null) {
            LOGGER.warn("[商店调试] ShopManager 为 null");
            return;
        }

        LOGGER.info("[商店调试] ShopManager 获取成功，准备调用 SignChangeHandler");
        
        SignChangeHandler handler = new SignChangeHandler(shopManager);
        handler.handleSignChange(serverPlayer, self, lines, frontText);
    }
}
