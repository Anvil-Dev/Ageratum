package dev.anvilcraft.resource.ageratum.mixin;

import dev.anvilcraft.resource.ageratum.client.gui.StructureGuiSession;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

/** 将只读浮窗的过渡透明度应用于原生界面的背景、文字及控件。 */
@Mixin(GuiGraphicsExtractor.class)
public abstract class StructureGuiGraphicsMixin {
    @ModifyVariable(method = "innerBlit(Lcom/mojang/blaze3d/pipeline/RenderPipeline;Lcom/mojang/blaze3d/textures/GpuTextureView;Lcom/mojang/blaze3d/textures/GpuSampler;IIIIFFFFI)V",
        at = @At("HEAD"), argsOnly = true, ordinal = 4)
    private int ageratum$textureOpacity(int color) { return StructureGuiSession.tint(color); }

    @ModifyVariable(method = "innerFill", at = @At("HEAD"), argsOnly = true, ordinal = 4)
    private int ageratum$fillOpacity(int color) { return StructureGuiSession.tint(color); }

    @ModifyVariable(method = "text(Lnet/minecraft/client/gui/Font;Lnet/minecraft/util/FormattedCharSequence;IIIZ)V",
        at = @At("HEAD"), argsOnly = true, ordinal = 2)
    private int ageratum$textOpacity(int color) { return StructureGuiSession.tint(color); }
}
