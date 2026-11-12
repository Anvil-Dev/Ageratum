package dev.anvilcraft.resource.ageratum.mixin;

import dev.anvilcraft.resource.ageratum.client.gui.StructureGuiSession;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** 原生 GUI 只在浮窗内绘制自身背景，不再次模糊或遮罩整个手册。 */
@Mixin(Screen.class)
public abstract class StructureGuiScreenMixin {
    @Inject(method = "extractBackground", at = @At("HEAD"), cancellable = true)
    private void ageratum$previewBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick, CallbackInfo ci) {
        if (StructureGuiSession.isRendering()) ci.cancel();
    }
}
