package dev.anvilcraft.resource.ageratum.mixin;

import dev.anvilcraft.resource.ageratum.client.gui.StructureGuiItemOpacity;
import dev.anvilcraft.resource.ageratum.client.gui.StructureGuiSession;
import net.minecraft.client.renderer.state.gui.GuiItemRenderState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(GuiItemRenderState.class)
public abstract class StructureGuiItemStateMixin implements StructureGuiItemOpacity {
    @Unique private float ageratum$opacity = 1;

    @Inject(method = "<init>", at = @At("RETURN"))
    private void ageratum$captureOpacity(CallbackInfo ci) {
        this.ageratum$opacity = StructureGuiSession.opacity();
    }

    @Override
    public float ageratum$opacity() {
        return this.ageratum$opacity;
    }
}
