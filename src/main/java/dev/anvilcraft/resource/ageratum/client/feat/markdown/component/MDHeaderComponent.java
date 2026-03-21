package dev.anvilcraft.resource.ageratum.client.feat.markdown.component;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.annotation.Nullable;

public class MDHeaderComponent extends MDComponent {
    private static final Pattern HEADER_PATTERN = Pattern.compile("^\\s{0,3}(#{1,6})\\s+(.+?)\\s*#*\\s*$");
    protected final int level;

    public MDHeaderComponent(int level, String text) {
        super(text);
        this.level = level;
    }

    public static @Nullable MDHeaderComponent parse(String text) {
        Matcher matcher = HEADER_PATTERN.matcher(text);
        if (!matcher.matches()) return null;
        int level = matcher.group(1).length();
        String headerText = matcher.group(2).trim();
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
    public void render(GuiGraphics guiGraphics, Minecraft minecraft, int maxX, int maxY) {
        PoseStack pose = guiGraphics.pose();
        pose.pushPose();
        pose.scale(this.getScale(), this.getScale(), 1);
        super.render(guiGraphics, minecraft, this.unscale(maxX), this.unscale(maxY));
        pose.translate(0, 0, 0);
        if (this.level == 1) {
            guiGraphics.hLine(5, this.unscale(maxX) - 10, 1, 0x88000000);
        }
        pose.popPose();
    }

    @Override
    public int getHeight(Minecraft minecraft, int maxX, int maxY) {
        int height = this.scale(super.getHeight(minecraft, this.unscale(maxX), this.unscale(maxY)));
        if (this.level == 1) {
            height += 2;
        }
        return height;
    }
}
