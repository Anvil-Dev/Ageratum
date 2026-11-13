package dev.anvilcraft.resource.ageratum.test.mixin;

import com.mojang.blaze3d.pipeline.RenderPipeline;
import dev.anvilcraft.resource.ageratum.test.ScrollEntityClientTest;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.resources.Identifier;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(GuiGraphicsExtractor.class)
public abstract class ExportTextureProbeMixin {
    @Inject(method = "innerBlit(Lcom/mojang/blaze3d/pipeline/RenderPipeline;Lnet/minecraft/resources/Identifier;IIIIFFFFI)V", at = @At("HEAD"))
    private void checkExportTexture(RenderPipeline pipeline, Identifier texture, int x0, int x1, int y0, int y1,
                                    float u0, float u1, float v0, float v1, int color, CallbackInfo ci) {
        ScrollEntityClientTest.checkExportTexture(texture);
    }
}
