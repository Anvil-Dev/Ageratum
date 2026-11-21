package dev.anvilcraft.resource.ageratum.mixin;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import dev.anvilcraft.lib.v2.rendering.preview.level.SandboxRenderLevel;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;

import java.util.concurrent.atomic.AtomicInteger;

@Mixin(Level.class)
public abstract class PreviewEntityIdMixin {
    @Unique
    private static final AtomicInteger ageratum$previewEntityIds = new AtomicInteger();

    // Preview entities never receive the spawn packet that assigns a normal client entity its ID.
    @ModifyReturnValue(method = "getNextEntityId", at = @At("RETURN"))
    private int ageratum$assignPreviewEntityId(int id) {
        if (id != 0 || !((Object) this instanceof SandboxRenderLevel)) return id;
        return ageratum$previewEntityIds.updateAndGet(previous -> previous == Integer.MIN_VALUE ? -1 : previous - 1);
    }
}
