package dev.anvilcraft.resource.ageratum.client.feat.markdown;

import dev.anvilcraft.resource.ageratum.client.gui.GuideScreen;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.tooltip.ClientTooltipComponent;
import net.minecraft.client.gui.screens.inventory.tooltip.DefaultTooltipPositioner;
import net.minecraft.network.chat.Component;
import net.minecraft.world.inventory.tooltip.TooltipComponent;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.BiConsumer;
import javax.annotation.Nullable;

public record MDRenderContext(
    @Nullable MDRenderContext parent,
    Minecraft minecraft,
    GuiGraphicsExtractor graphics,
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
        this.graphics().enableScissor(minX, minY, maxX, maxY);
    }

    public void disableScissor() {
        this.graphics().disableScissor();
    }

    public void onEnd(BiConsumer<GuideScreen, MDRenderContext> consumer) {
        this.onEnd().add(consumer);
    }

    public void extractTooltipRenderState() {
        for (MDRenderContext.Tooltip tooltip : this.tooltips()) {
            List<ClientTooltipComponent> list = new ArrayList<>();
            tooltip.tooltipLines().forEach(component -> list.add(ClientTooltipComponent.create(component.getVisualOrderText())));
            tooltip.visualTooltipComponent().ifPresent(component -> list.add(ClientTooltipComponent.create(component)));
            this.graphics().tooltip(
                this.minecraft().font,
                list,
                Math.round(this.mouseX()),
                Math.round(this.mouseY()),
                DefaultTooltipPositioner.INSTANCE,
                null
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
