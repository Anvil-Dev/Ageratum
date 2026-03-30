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
        this.tooltips.add(new Tooltip(Screen.getTooltipFromItem(Minecraft.getInstance(), stack), stack.getTooltipImage()));
    }

    public void addTooltip(Component text) {
        this.tooltips.add(new Tooltip(List.of(text), Optional.empty()));
    }

    public record Tooltip(List<Component> tooltipLines, Optional<TooltipComponent> visualTooltipComponent) {
    }

    public void enableScissor(int minX, int minY, int maxX, int maxY) {
        minX = Math.round((minX / this.scale()) + this.offsetX() + this.leftPos());
        maxX = Math.round((maxX / this.scale()) + this.offsetX() + this.leftPos());
        minY = Math.round((minY / this.scale()) + this.offsetY() + this.topPos());
        maxY = Math.round((maxY / this.scale()) + this.offsetY() + this.topPos());
        this.graphics().enableScissor(minX, minY, maxX, maxY);
    }

    public void disableScissor() {
        this.graphics().disableScissor();
    }

    public void onEnd(BiConsumer<GuideScreen, MDRenderContext> consumer) {
        this.onEnd().add(consumer);
    }

    public void renderTooltip() {
        for (MDRenderContext.Tooltip tooltip : this.tooltips()) {
            this.graphics()
                .renderTooltip(
                    this.minecraft().font,
                    tooltip.tooltipLines(),
                    tooltip.visualTooltipComponent(),
                    Math.round(this.mouseX()),
                    Math.round(this.mouseY())
                );
        }
    }

    public void onEnd(GuideScreen screen) {
        for (BiConsumer<GuideScreen, MDRenderContext> consumer : this.onEnd()) {
            consumer.accept(screen, this);
        }
        this.onEnd().clear();
    }
}
