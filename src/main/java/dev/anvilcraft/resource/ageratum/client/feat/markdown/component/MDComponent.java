package dev.anvilcraft.resource.ageratum.client.feat.markdown.component;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.FormattedText;
import net.minecraft.util.FormattedCharSequence;

import java.util.List;

public abstract class MDComponent {
    protected final FormattedText text;

    public MDComponent(String text) {
        this.text = FormattedText.of(text);
    }

    public MDComponent(FormattedText text) {
        this.text = text;
    }

    public void render(GuiGraphics guiGraphics, Minecraft minecraft, int maxX, int maxY) {
        List<FormattedCharSequence> split = minecraft.font.split(this.text, maxX);
        PoseStack pose = guiGraphics.pose();
        for (FormattedCharSequence sequence : split) {
            if (maxY < minecraft.font.lineHeight) return;
            guiGraphics.drawString(minecraft.font, sequence, 0, 0, 0xFFFFFF);
            pose.translate(0, minecraft.font.lineHeight, 0);
            maxY -= minecraft.font.lineHeight;
        }
    }

    public int getHeight(Minecraft minecraft, int maxX, int maxY) {
        return minecraft.font.wordWrapHeight(this.text, maxX);
    }
}
