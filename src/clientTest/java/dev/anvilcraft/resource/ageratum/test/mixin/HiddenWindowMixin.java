package dev.anvilcraft.resource.ageratum.test.mixin;

import net.minecraft.client.Minecraft;
import org.lwjgl.glfw.GLFW;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(Minecraft.class)
abstract class HiddenWindowMixin {
    @Redirect(method = "<init>", at = @At(value = "INVOKE", target = "Lorg/lwjgl/glfw/GLFW;glfwShowWindow(J)V"))
    private void showReviewWindow(long window) {
        if (Boolean.getBoolean("ageratum.structureGuiReview")) GLFW.glfwShowWindow(window);
        else GLFW.glfwHideWindow(window);
    }
}
