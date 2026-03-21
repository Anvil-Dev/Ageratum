package dev.anvilcraft.resource.ageratum.client.feat.markdown.component;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;

import javax.annotation.Nullable;

public class MDHeaderComponent extends MDComponent {
    protected final int level;

    public MDHeaderComponent(int level, String text) {
        super(text);
        this.level = level;
    }

    public static @Nullable MDHeaderComponent parse(String text) {
        if (!text.startsWith("#")) return null;
        String[] split = text.split(" ");
        if (split.length < 2) return null;
        String levelStr = split[0];
        if (!levelStr.matches("^#+$")) return null;
        int level = levelStr.length();
        String headerText = text.substring(level).trim();
        return new MDHeaderComponent(level, headerText);
    }

    private float getScale() {
        return Math.max(1.5f - (this.level - 1) * 0.2f, 1.0f);
    }

    private int scale(int value) {
        return (int) Math.ceil(value * this.getScale());
    }

    private int unscale(int value) {
        return (int) Math.floor(value / this.getScale());
    }

    @Override
    public void render(GuiGraphics guiGraphics, Minecraft minecraft, int maxX) {
        PoseStack pose = guiGraphics.pose();
        pose.pushPose();
        pose.scale(this.getScale(), this.getScale(), 1);
        super.render(guiGraphics, minecraft, this.unscale(maxX));
        pose.translate(0, 0, 0);
        if (this.level == 1) {
            guiGraphics.hLine(5, this.unscale(maxX) - 5, 1, 0xFFFFFFFF);
        }
        pose.popPose();
    }

    @Override
    public int getHeight(Minecraft minecraft, int maxX) {
        int height = this.scale(super.getHeight(minecraft, this.unscale(maxX)));
        if (this.level == 1) {
            height += 2;
        }
        return height;
    }
}
