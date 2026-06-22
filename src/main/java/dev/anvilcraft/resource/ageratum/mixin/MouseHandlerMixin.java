package dev.anvilcraft.resource.ageratum.mixin;

import dev.anvilcraft.resource.ageratum.client.feat.structure.StructureProjectionManager;
import net.minecraft.client.Minecraft;
import net.minecraft.client.MouseHandler;
import net.neoforged.neoforge.client.event.InputEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(MouseHandler.class)
public abstract class MouseHandlerMixin {
    @Shadow public abstract boolean isLeftPressed();

    @Shadow public abstract boolean isMiddlePressed();

    @Shadow public abstract boolean isRightPressed();

    @Shadow public abstract double xpos();

    @Shadow public abstract double ypos();

    @Inject(method = "onScroll", at = @At("HEAD"), cancellable = true)
    private void ageratum$handleStructureProjectionScroll(long windowPointer, double xOffset, double yOffset, CallbackInfo ci) {
        Minecraft minecraft = Minecraft.getInstance();
        if (windowPointer != minecraft.getWindow().handle()) {
            return;
        }
        //noinspection UnstableApiUsage
        if (StructureProjectionManager.handleMouseScroll(new InputEvent.MouseScrollingEvent(
            xOffset,
            yOffset,
            this.isLeftPressed(),
            this.isMiddlePressed(),
            this.isRightPressed(),
            this.xpos(),
            this.ypos()
        ))) {
            ci.cancel();
        }
    }
}

