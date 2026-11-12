package dev.anvilcraft.resource.ageratum.mixin;

import dev.anvilcraft.resource.ageratum.client.gui.StructureGuiSession;
import net.minecraft.client.gui.GuiGraphics;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

/** 原生控件恢复绘制颜色时仍保留整个浮窗的透明度。 */
@Mixin(GuiGraphics.class)
public abstract class StructureGuiGraphicsMixin {
    @ModifyVariable(method = "setColor", at = @At("HEAD"), argsOnly = true, ordinal = 3)
    private float ageratum$previewOpacity(float alpha) {
        return alpha * StructureGuiSession.opacity();
    }
}
