package dev.anvilcraft.resource.ageratum.mixin;

import com.llamalad7.mixinextras.sugar.Local;
import dev.anvilcraft.resource.ageratum.client.gui.StructureGuiItemOpacity;
import net.minecraft.client.gui.render.GuiRenderer;
import net.minecraft.client.renderer.state.gui.GuiItemRenderState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArgs;
import org.spongepowered.asm.mixin.injection.invoke.arg.Args;

@Mixin(GuiRenderer.class)
public abstract class StructureGuiItemRendererMixin {
    @ModifyArgs(method = "submitBlitFromItemAtlas", at = @At(value = "INVOKE",
        target = "Lnet/minecraft/client/renderer/state/gui/BlitRenderState;<init>(Lcom/mojang/blaze3d/pipeline/RenderPipeline;Lnet/minecraft/client/gui/render/TextureSetup;Lorg/joml/Matrix3x2f;IIIIFFFFILnet/minecraft/client/gui/navigation/ScreenRectangle;Lnet/minecraft/client/gui/navigation/ScreenRectangle;)V"))
    private void ageratum$fadeItem(Args args, @Local(argsOnly = true) GuiItemRenderState item) {
        args.set(11, StructureGuiItemOpacity.premultipliedColor(((StructureGuiItemOpacity) (Object) item).ageratum$opacity()));
    }
}
