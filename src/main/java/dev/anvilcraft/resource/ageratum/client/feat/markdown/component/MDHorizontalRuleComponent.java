package dev.anvilcraft.resource.ageratum.client.feat.markdown.component;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.FormattedText;

public class MDHorizontalRuleComponent extends MDComponent {
    public MDHorizontalRuleComponent() {
        super(FormattedText.EMPTY);
    }

    @Override
    public void render(GuiGraphics guiGraphics, Minecraft minecraft, int maxX, int maxY) {
        int y = minecraft.font.lineHeight / 2;
        guiGraphics.hLine(0, Math.max(0, maxX - 1), y, 0x88000000);
    }

    @Override
    public int getHeight(Minecraft minecraft, int maxX, int maxY) {
        return minecraft.font.lineHeight;
    }
}

