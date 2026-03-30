package me.tuanzi.auth.mixin;

import me.tuanzi.auth.login.LoginStateManager;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Player.class)
public abstract class PlayerMixin {

    @Inject(
            method = "attack",
            at = @At("HEAD"),
            cancellable = true
    )
    private void onAttack(Entity target, CallbackInfo ci) {
        Player self = (Player) (Object) this;
        
        if (!LoginStateManager.getInstance().isLoggedIn(self.getUUID())) {
            self.sendSystemMessage(Component.literal("§c请先登录后再进行攻击！"));
            ci.cancel();
        }
    }
}
