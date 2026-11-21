package dev.anvilcraft.resource.ageratum.mixin;

import dev.anvilcraft.resource.ageratum.client.gui.GuideTheme;
import net.minecraft.client.Minecraft;
import net.minecraft.util.FormattedCharSequence;
import org.joml.Matrix4f;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.Slice;

@Mixin(targets = "net.minecraft.client.gui.Font$StringRenderOutput")
public abstract class GuideTextUnderlineMixin implements GuideTheme.GuideTextSink {
    @Shadow @Final private Matrix4f pose;
    @Unique
    private float ageratum$underlineInset;

    @Override
    public boolean ageratum$acceptGuideText(FormattedCharSequence line) {
        float previous = this.ageratum$underlineInset;
        float pixelScale = Math.abs(this.pose.m11()) * (float) Minecraft.getInstance().getWindow().getGuiScale();
        // Leave a gap above the underline while retaining at least one physical pixel at small zooms.
        this.ageratum$underlineInset = pixelScale > 1 ? Math.min(0.5f, 1 - 1 / pixelScale) : 0;
        try {
            return line.accept(this);
        } finally {
            this.ageratum$underlineInset = previous;
        }
    }

    @ModifyArg(
        method = "accept",
        slice = @Slice(from = @At(value = "INVOKE", target = "Lnet/minecraft/network/chat/Style;isUnderlined()Z")),
        at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/font/glyphs/BakedGlyph$Effect;<init>(FFFFFFFFF)V"),
        index = 3
    )
    private float ageratum$lowerUnderlineTop(float top) {
        return top + this.ageratum$underlineInset;
    }
}
