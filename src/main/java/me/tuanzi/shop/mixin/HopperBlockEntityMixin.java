package me.tuanzi.shop.mixin;

import me.tuanzi.shop.shop.ShopManager;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.Container;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.ChestBlock;
import net.minecraft.world.level.block.HopperBlock;
import net.minecraft.world.level.block.entity.Hopper;
import net.minecraft.world.level.block.entity.HopperBlockEntity;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.ChestType;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(HopperBlockEntity.class)
public abstract class HopperBlockEntityMixin {

    /**
     * 防止漏斗吸取带有 shop_display 标签的商店展示物品
     * 或者防止掉落物被加入到作为商店的容器中
     */
    @Inject(method = "addItem(Lnet/minecraft/world/Container;Lnet/minecraft/world/entity/item/ItemEntity;)Z", at = @At("HEAD"), cancellable = true)
    private static void onAddItem(Container container, ItemEntity entity, CallbackInfoReturnable<Boolean> cir) {
        if (entity != null && entity.entityTags().contains("shop_display")) {
            cir.setReturnValue(false);
            return;
        }

        if (container instanceof BlockEntity be) {
            Level level = be.getLevel();
            if (level != null && !level.isClientSide()) {
                if (isShopAt(level, be.getBlockPos())) {
                    cir.setReturnValue(false);
                }
            }
        }
    }

    /**
     * 防止漏斗向外推送物品到商店容器 (修复输入问题)
     */
    @Inject(method = "ejectItems(Lnet/minecraft/world/level/Level;Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/entity/HopperBlockEntity;)Z", at = @At("HEAD"), cancellable = true)
    private static void onEjectItems(Level level, BlockPos pos, HopperBlockEntity be, CallbackInfoReturnable<Boolean> cir) {
        if (level == null || level.isClientSide()) return;

        BlockState state = be.getBlockState();
        if (state.hasProperty(HopperBlock.FACING)) {
            Direction facing = state.getValue(HopperBlock.FACING);
            BlockPos targetPos = pos.relative(facing);
            if (isShopAt(level, targetPos)) {
                cir.setReturnValue(false);
            }
        }
    }

    /**
     * 防止漏斗从商店容器吸取物品 (修复提取问题)
     */
    @Inject(method = "suckInItems(Lnet/minecraft/world/level/Level;Lnet/minecraft/world/level/block/entity/Hopper;)Z", at = @At("HEAD"), cancellable = true)
    private static void onSuckInItems(Level level, Hopper hopper, CallbackInfoReturnable<Boolean> cir) {
        if (level == null || level.isClientSide()) return;

        BlockPos pos = BlockPos.containing(hopper.getLevelX(), hopper.getLevelY() + 1.0, hopper.getLevelZ());
        if (isShopAt(level, pos)) {
            cir.setReturnValue(false);
        }
    }

    /**
     * 辅助方法：判断指定位置是否属于某个商店（包括大箱子的判断）
     */
    private static boolean isShopAt(Level level, BlockPos pos) {
        ShopManager shopManager = ShopManager.getInstance(level.getServer());
        if (shopManager == null) return false;

        // 1. 直接检查当前位置
        if (shopManager.getShopByPos(pos).isPresent()) {
            return true;
        }

        // 2. 处理大箱子的情况
        BlockState state = level.getBlockState(pos);
        if (state.getBlock() instanceof ChestBlock) {
            ChestType type = state.getValue(ChestBlock.TYPE);
            if (type != ChestType.SINGLE) {
                Direction connected = ChestBlock.getConnectedDirection(state);
                if (connected != null) {
                    BlockPos neighborPos = pos.relative(connected);
                    return shopManager.getShopByPos(neighborPos).isPresent();
                }
            }
        }
        
        return false;
    }
}
