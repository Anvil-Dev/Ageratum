package dev.anvilcraft.resource.ageratum.test.mixin;

import dev.anvilcraft.resource.ageratum.test.ScrollEntityClientTest;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import org.joml.Quaternionfc;
import org.joml.Vector3fc;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(GuiGraphicsExtractor.class)
public abstract class EntityPreviewProbeMixin {
    @Inject(method = "entity", at = @At("HEAD"))
    private void checkPreview(EntityRenderState state, float scale, Vector3fc translation, Quaternionfc rotation,
                              Quaternionfc camera, int x0, int y0, int x1, int y1, CallbackInfo ci) {
        ScrollEntityClientTest.checkEntityBounds((GuiGraphicsExtractor) (Object) this, x0, y0, x1, y1);
    }
}
