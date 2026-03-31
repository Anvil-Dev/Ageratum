package dev.anvilcraft.resource.ageratum.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.mojang.blaze3d.platform.Window;
import dev.anvilcraft.resource.ageratum.client.gui.GuideScreen;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(GuiGraphics.class)
public class GuiGraphicsMixin {

    @WrapOperation(
        method = "guiWidth",
        at = @At(value = "INVOKE", target = "Lcom/mojang/blaze3d/platform/Window;getGuiScaledWidth()I")
    )
    public int guiWidth(Window instance, Operation<Integer> original) {
        int call = original.call(instance);
        if (Minecraft.getInstance().screen instanceof GuideScreen guideScreen) {
            call = (int) Math.round(call * guideScreen.getScale());
        }
        return call;
    }

    @WrapOperation(
        method = "guiHeight",
        at = @At(value = "INVOKE", target = "Lcom/mojang/blaze3d/platform/Window;getGuiScaledHeight()I")
    )
    public int guiHeight(Window instance, Operation<Integer> original) {
        int call = original.call(instance);
        if (Minecraft.getInstance().screen instanceof GuideScreen guideScreen) {
            call = (int) Math.round(call * guideScreen.getScale());
        }
        return call;
    }
}
