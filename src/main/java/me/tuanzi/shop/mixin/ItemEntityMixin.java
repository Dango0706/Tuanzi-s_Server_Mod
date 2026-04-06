package me.tuanzi.shop.mixin;

import net.minecraft.world.entity.item.ItemEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ItemEntity.class)
public abstract class ItemEntityMixin {
    @Inject(method = "tick", at = @At("HEAD"), cancellable = true)
    private void onTick(CallbackInfo ci) {
        ItemEntity self = (ItemEntity) (Object) this;
        // 使用 shop_display 标签识别商店展示物品
        if (self.entityTags().contains("shop_display")) {
            if (self.getItem().isEmpty()) {
                self.discard();
                return;
            }
            
            // 强制保持静止
            self.setDeltaMovement(0, 0, 0);
            self.noPhysics = true;
            self.setNoGravity(true);
            
            // 取消后续的所有 tick 逻辑（包括父类的），防止被水流冲走或应用重力/浮力
            ci.cancel();
        }
    }
}
