package dev.anvilcraft.resource.ageratum.mixin;

import dev.anvilcraft.resource.ageratum.client.gui.StructureGuiSession;
import net.minecraft.client.gui.screens.recipebook.RecipeBookComponent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** 预览只展示容器本身，避免沿用玩家的配方书开关并更新其配方已读状态。 */
@Mixin(RecipeBookComponent.class)
public abstract class StructureGuiRecipeBookMixin {
    @Inject(method = "isVisibleAccordingToBookData", at = @At("HEAD"), cancellable = true)
    private void ageratum$hidePreviewRecipeBook(CallbackInfoReturnable<Boolean> cir) {
        if (StructureGuiSession.isRendering()) cir.setReturnValue(false);
    }
}
