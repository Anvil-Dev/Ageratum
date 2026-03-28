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
    List<List<Component>> tooltipLines,
    List<Optional<TooltipComponent>> visualTooltipComponent
) {
    public void addTooltip(ItemStack stack) {
        this.tooltipLines.add(Screen.getTooltipFromItem(Minecraft.getInstance(), stack));
        this.visualTooltipComponent.add(stack.getTooltipImage());
    }

    public void renderTooltip(Component text) {
        this.tooltipLines.add(List.of(text));
        this.visualTooltipComponent.add(Optional.empty());
    }
}
