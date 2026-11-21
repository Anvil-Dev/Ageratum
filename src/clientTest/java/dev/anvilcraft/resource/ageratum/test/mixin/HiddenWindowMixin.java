package dev.anvilcraft.resource.ageratum.test.mixin;

import net.neoforged.neoforge.client.loading.earlydisplay.EarlyWindowHandoff;
import org.lwjgl.sdl.SDLVideo;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(EarlyWindowHandoff.class)
abstract class HiddenWindowMixin {
    @Redirect(method = "completeWindowHandoff", at = @At(value = "INVOKE", target = "Lorg/lwjgl/sdl/SDLVideo;SDL_ShowWindow(J)Z"))
    private static boolean showReviewWindow(long window) {
        return Boolean.getBoolean("ageratum.structureGuiReview") ? SDLVideo.SDL_ShowWindow(window) : SDLVideo.SDL_HideWindow(window);
    }
}
