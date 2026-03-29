package dev.anvilcraft.resource.ageratum.client.feat.markdown;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.inventory.tooltip.TooltipComponent;
import net.minecraft.world.item.ItemStack;

import java.util.List;
import java.util.Optional;

public record MDRenderContext(
    GuiGraphics graphics,
    List<Tooltip> tooltips
) {
    public void addTooltip(ItemStack stack) {
        this.tooltips.add(new Tooltip(Screen.getTooltipFromItem(Minecraft.getInstance(), stack), stack.getTooltipImage()));
    }

    public void addTooltip(Component text) {
        this.tooltips.add(new Tooltip(List.of(text), Optional.empty()));
    }

    public record Tooltip(List<Component> tooltipLines, Optional<TooltipComponent> visualTooltipComponent) {
    }
}
