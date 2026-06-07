package me.tuanzi.auth.mixin;

import carpet.commands.PlayerCommand;
import com.mojang.brigadier.context.CommandContext;
import me.tuanzi.auth.utils.CarpetHelper;
import net.minecraft.commands.CommandSourceStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = PlayerCommand.class, remap = false)
public class PlayerCommandMixin {

    @Inject(
            method = "spawn",
            at = @At("HEAD")
    )
    private static void onSpawnHead(CommandContext<CommandSourceStack> context, CallbackInfoReturnable<Integer> cir) {
        CarpetHelper.CURRENT_SOURCE.set(context.getSource());
    }

    @Inject(
            method = "spawn",
            at = @At("RETURN")
    )
    private static void onSpawnReturn(CommandContext<CommandSourceStack> context, CallbackInfoReturnable<Integer> cir) {
        CarpetHelper.CURRENT_SOURCE.remove();
    }
}
