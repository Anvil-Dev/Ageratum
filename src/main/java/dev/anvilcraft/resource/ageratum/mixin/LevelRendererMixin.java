package dev.anvilcraft.resource.ageratum.mixin;

import dev.anvilcraft.resource.ageratum.client.feat.structure.StructureProjectionManager;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.state.level.LevelRenderState;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LevelRenderer.class)
public abstract class LevelRendererMixin {
    @Shadow @Final private LevelRenderState levelRenderState;

    // Both transparency modes have closed their passes here. The world depth and view matrix are still active.
    @Inject(method = "executeOutline", at = @At("HEAD"))
    private void ageratum$renderStructureProjection(CallbackInfo ci) {
        StructureProjectionManager.renderProjection(this.levelRenderState.cameraRenderState);
    }
}
