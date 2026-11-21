package dev.anvilcraft.resource.ageratum.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.mojang.blaze3d.platform.Window;
import dev.anvilcraft.resource.ageratum.client.gui.GuideScreen;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.navigation.ScreenRectangle;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(GuiGraphicsExtractor.class)
public class GuiGraphicsExtractorMixin {

    // Nested scissors are intersected in window GUI coordinates after applying the guide's zoom.
    @WrapOperation(
        method = "<init>",
        at = @At(value = "NEW", target = "(IIII)Lnet/minecraft/client/gui/navigation/ScreenRectangle;")
    )
    private ScreenRectangle ageratum$windowScissorBounds(int x, int y, int width, int height, Operation<ScreenRectangle> original) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.gui.screen() instanceof GuideScreen) {
            Window window = minecraft.getWindow();
            return original.call(x, y, window.getGuiScaledWidth(), window.getGuiScaledHeight());
        }
        return original.call(x, y, width, height);
    }

    @WrapOperation(
        method = "guiWidth",
        at = @At(value = "INVOKE", target = "Lcom/mojang/blaze3d/platform/Window;getGuiScaledWidth()I")
    )
    public int guiWidth(Window instance, Operation<Integer> original) {
        int call = original.call(instance);
        if (Minecraft.getInstance().gui.screen() instanceof GuideScreen guideScreen) {
            call = guideScreen.width;
        }
        return call;
    }

    @WrapOperation(
        method = "guiHeight",
        at = @At(value = "INVOKE", target = "Lcom/mojang/blaze3d/platform/Window;getGuiScaledHeight()I")
    )
    public int guiHeight(Window instance, Operation<Integer> original) {
        int call = original.call(instance);
        if (Minecraft.getInstance().gui.screen() instanceof GuideScreen guideScreen) {
            call = guideScreen.height;
        }
        return call;
    }
}
