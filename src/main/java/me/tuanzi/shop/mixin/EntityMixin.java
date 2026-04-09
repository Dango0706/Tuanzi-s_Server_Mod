package me.tuanzi.shop.mixin;

import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Entity.class)
public abstract class EntityMixin {
    /**
     * 在 Entity 基类中统一拦截保存判定。
     * 如果实体带有 shop_display 标签，则禁止保存到存档。
     */
    @Inject(method = "shouldBeSaved", at = @At("HEAD"), cancellable = true)
    private void onShouldBeSaved(CallbackInfoReturnable<Boolean> cir) {
        Entity self = (Entity) (Object) this;
        if (self.entityTags().contains("shop_display")) {
            cir.setReturnValue(false);
        }
    }
}
