package dev.anvilcraft.resource.ageratum.client.feat.markdown;

import dev.anvilcraft.resource.ageratum.client.gui.GuideScreen;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.inventory.tooltip.TooltipComponent;
import net.minecraft.world.item.ItemStack;

import java.util.List;
import java.util.Optional;
import java.util.function.BiConsumer;
import javax.annotation.Nullable;

public record MDRenderContext(
    @Nullable MDRenderContext parent,
    Minecraft minecraft,
    GuiGraphics graphics,
    List<Tooltip> tooltips,
    int screenWidth,
    int screenHeight,
    int maxX,
    int maxY,
    float mouseX,
    float mouseY,
    int offsetX,
    int offsetY,
    float scale,
    int leftPos,
    int topPos,
    List<BiConsumer<GuideScreen, MDRenderContext>> onEnd
) {

    public MDRenderContext child() {
        return this.child(this.maxX, this.maxY, this.mouseX, this.mouseY, this.scale);
    }

    public MDRenderContext child(int maxX, int maxY, float mouseX, float mouseY, float scale) {
        return this.child(maxX, maxY, mouseX, mouseY, this.offsetX, this.offsetY, scale);
    }

    public MDRenderContext child(int maxX, int maxY, float mouseX, float mouseY, int offsetX, int offsetY, float scale) {
        return new MDRenderContext(
            this,
            this.minecraft,
            this.graphics,
            this.tooltips,
            this.screenWidth,
            this.screenHeight,
            maxX,
            maxY,
            mouseX,
            mouseY,
            offsetX,
            offsetY,
            scale,
            this.leftPos,
            this.topPos,
            this.onEnd
        );
    }

    public void addTooltip(ItemStack stack) {
        this.tooltips.add(new Tooltip(
            Screen.getTooltipFromItem(Minecraft.getInstance(), stack),
            stack.getTooltipImage(),
            stack
        ));
    }

    public void addTooltip(Component text) {
        this.addTooltip(List.of(text));
    }

    public void addTooltip(List<Component> lines) {
        this.tooltips.add(new Tooltip(lines, Optional.empty(), ItemStack.EMPTY));
    }

    public record Tooltip(
        List<Component> tooltipLines,
        Optional<TooltipComponent> visualTooltipComponent,
        ItemStack stack
    ) {
    }

    public void enableScissor(int minX, int minY, int maxX, int maxY) {
        minX = Math.round((minX + this.offsetX() + this.leftPos()) / this.scale());
        maxX = Math.round((maxX + this.offsetX() + this.leftPos()) / this.scale());
        minY = Math.round((minY + this.offsetY() + this.topPos()) / this.scale());
        maxY = Math.round((maxY + this.offsetY() + this.topPos()) / this.scale());
        this.graphics().enableScissor(minX, minY, maxX, maxY);
    }

    public void disableScissor() {
        this.graphics().disableScissor();
    }

    public void onEnd(BiConsumer<GuideScreen, MDRenderContext> consumer) {
        this.onEnd().add(consumer);
    }

    public void renderTooltip() {
        // 原版提示框按整个屏幕限制宽度和位置；保留手册缩放，但移除书页原点偏移。
        var pose = this.graphics().pose();
        pose.pushPose();
        pose.translate(-this.leftPos(), -this.topPos(), 0);
        try {
            int mouseX = Math.round(this.mouseX() + this.leftPos());
            int mouseY = Math.round(this.mouseY() + this.topPos());
            for (MDRenderContext.Tooltip tooltip : this.tooltips()) {
                ItemStack stack = tooltip.stack();
                if (stack.isEmpty()) {
                    this.graphics()
                        .renderTooltip(
                            this.minecraft().font,
                            tooltip.tooltipLines(),
                            tooltip.visualTooltipComponent(),
                            mouseX,
                            mouseY
                        );
                } else {
                    this.graphics()
                        .renderTooltip(
                            this.minecraft().font,
                            tooltip.tooltipLines(),
                            tooltip.visualTooltipComponent(),
                            stack,
                            mouseX,
                            mouseY
                        );
                }
            }
        } finally {
            pose.popPose();
        }
    }

    public void onEnd(GuideScreen screen) {
        for (BiConsumer<GuideScreen, MDRenderContext> consumer : this.onEnd()) {
            consumer.accept(screen, this);
        }
        this.onEnd().clear();
    }
}
