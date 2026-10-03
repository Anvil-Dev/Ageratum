package dev.anvilcraft.resource.ageratum.test.mixin;
import com.mojang.blaze3d.platform.InputConstants;
import dev.anvilcraft.resource.ageratum.test.StructureTest;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
@Mixin(InputConstants.class)
abstract class ControlKeyMixin {
    @Inject(method = "isKeyDown", at = @At("HEAD"), cancellable = true)
    private static void control(CallbackInfoReturnable<Boolean> callback) {
        if (StructureTest.control) callback.setReturnValue(true);
    }
}
