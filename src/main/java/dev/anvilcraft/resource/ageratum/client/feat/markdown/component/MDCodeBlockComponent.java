package dev.anvilcraft.resource.ageratum.client.feat.markdown.component;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.FormattedText;
import net.minecraft.network.chat.Style;

public class MDCodeBlockComponent extends MDComponent {
    private static final int PADDING = 4;
    private static final int BORDER_COLOR = 0x88333333;
    private static final int BACKGROUND_COLOR = 0x22AAAAAA;

    public MDCodeBlockComponent(String text) {
        super(FormattedText.of(text, Style.EMPTY.withColor(0x00444444)));
    }

    @Override
    public void render(GuiGraphics guiGraphics, Minecraft minecraft, int maxX, int maxY) {
        int blockHeight = this.getHeight(minecraft, maxX, maxY);
        guiGraphics.fill(0, 0, maxX, blockHeight, BACKGROUND_COLOR);
        guiGraphics.renderOutline(0, 0, maxX, blockHeight, BORDER_COLOR);

        PoseStack pose = guiGraphics.pose();
        pose.pushPose();
        pose.translate(PADDING, PADDING, 0);
        super.render(guiGraphics, minecraft, maxX - PADDING * 2, maxY - PADDING * 2);
        pose.popPose();
    }

    @Override
    public int getHeight(Minecraft minecraft, int maxX, int maxY) {
        return super.getHeight(minecraft, Math.max(1, maxX - PADDING * 2), maxY) + PADDING * 2;
    }
}

