package me.tuanzi.auth.mixin;

import me.tuanzi.auth.login.LoginStateManager;
import me.tuanzi.auth.utils.TranslationHelper;
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
            TranslationHelper.sendMessage((net.minecraft.server.level.ServerPlayer) self, "auth.restriction.no_attack");
            ci.cancel();
        }
    }
}
