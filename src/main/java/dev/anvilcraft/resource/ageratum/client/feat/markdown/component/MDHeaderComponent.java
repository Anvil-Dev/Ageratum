package dev.anvilcraft.resource.ageratum.client.feat.markdown.component;

import com.mojang.blaze3d.vertex.PoseStack;
import lombok.Getter;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Style;

import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.annotation.Nullable;

/**
 * Markdown 标题组件。
 *
 * <p>支持 ATX 形式标题（{@code # ~ ######}），并根据标题级别调整缩放比例。</p>
 */
@Getter
public class MDHeaderComponent extends MDComponent {
    private static final Pattern HEADER_PATTERN = Pattern.compile("^\\s{0,3}(#{1,6})\\s+(.+?)\\s*#*\\s*$");
    /**
     * 标题级别（1-6）。
     */
    protected final int level;

    /**
     * 创建标题组件。
     *
     * @param level 标题级别（1-6）
     * @param text  标题文本
     */
    public MDHeaderComponent(int level, String text) {
        super(MDComponent.textFormat(text, MDHeaderComponent.getStyle(level)));
        this.level = level;
    }

    /**
     * 尝试从单行文本解析标题组件。
     */
    public static @Nullable MDHeaderComponent parse(String text) {
        Matcher matcher = HEADER_PATTERN.matcher(text);
        if (!matcher.matches()) return null;
        int level = matcher.group(1).length();
        String headerText = matcher.group(2).trim();
        return new MDHeaderComponent(level, headerText);
    }

    /**
     * 按标题级别计算渲染缩放比例。
     */
    private float getScale() {
        return switch (this.level) {
            case 1 -> 2.0f;
            case 2 -> 1.5f;
            default -> 1.0f;
        };
    }

    private static Style getStyle(int level) {
        Style style = Style.EMPTY;
        if (level % 2 != 0) {
            return style.withBold(true);
        }
        return style;
    }

    private int scale(int value) {
        return (int) Math.ceil(value * this.getScale());
    }

    private int unscale(int value) {
        return (int) Math.floor(value / this.getScale());
    }

    /**
     * 渲染标题文本；一级标题额外绘制一条分隔线。
     */
    @Override
    public void render(GuiGraphics guiGraphics, Minecraft minecraft, int maxX, int maxY, int mouseX, int mouseY) {
        PoseStack pose = guiGraphics.pose();
        pose.pushPose();
        pose.scale(this.getScale(), this.getScale(), 1);
        super.render(guiGraphics, minecraft, this.unscale(maxX), this.unscale(maxY), this.unscale(mouseX), this.unscale(mouseY));
        if (this.level == 1) {
            int y = minecraft.font.lineHeight / 2;
            guiGraphics.hLine(0, Math.max(0, maxX - 1), y, 0x88000000);
        }
        pose.popPose();
    }

    /**
     * 计算标题渲染高度（含一级标题分隔线高度）。
     */
    @Override
    public int getHeight(Minecraft minecraft, int maxX, int maxY) {
        int height = this.scale(super.getHeight(minecraft, this.unscale(maxX), this.unscale(maxY)));
        if (this.level == 1) {
            height += minecraft.font.lineHeight;
        }
        return height;
    }
}
