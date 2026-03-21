package dev.anvilcraft.resource.ageratum.client.feat.markdown.component;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.FormattedText;
import net.minecraft.network.chat.Style;
import net.minecraft.util.FormattedCharSequence;

import java.util.List;

/**
 * 引用块组件。
 *
 * <p>按层级渲染左侧竖线，并对引用文本使用较浅默认颜色。</p>
 */
public class MDQuoteComponent extends MDComponent {
    private static final int LEVEL_INDENT = 10;
    private static final int TEXT_PADDING = 4;
    private static final int DEFAULT_TEXT_COLOR = 0x888888;
    private static final int[] LEVEL_LINE_COLORS = {
        0xFF7A7A7A,
        0xFF6A7FA8,
        0xFF8A6AA8,
        0xFF7A8F66
    };
    private final List<QuoteLine> lines;

    /**
     * 使用解析后的引用行创建组件。
     */
    public MDQuoteComponent(List<QuoteLine> lines) {
        super(FormattedText.EMPTY);
        this.lines = List.copyOf(lines);
    }

    /**
     * 渲染引用块内容与层级竖线。
     */
    @Override
    public void render(GuiGraphics guiGraphics, Minecraft minecraft, int maxX, int maxY) {
        int y = 0;
        for (QuoteLine line : this.lines) {
            int textX = line.level() * LEVEL_INDENT + TEXT_PADDING;
            int lineMaxX = Math.max(1, maxX - textX);
            FormattedText formatted = MDComponent.textFormat(line.text(), Style.EMPTY.withColor(DEFAULT_TEXT_COLOR));
            List<FormattedCharSequence> split = minecraft.font.split(formatted, lineMaxX);
            int lineHeight = split.size() * minecraft.font.lineHeight;
            if (lineHeight <= 0 || maxY < lineHeight) {
                return;
            }

            int lineBottom = y + lineHeight - 1;
            for (int level = 0; level < line.level(); level++) {
                int lineX = level * LEVEL_INDENT + 1;
                guiGraphics.vLine(lineX, y, lineBottom, getLevelLineColor(level));
            }

            PoseStack pose = guiGraphics.pose();
            pose.pushPose();
            pose.translate(textX, y, 0);
            for (FormattedCharSequence sequence : split) {
                guiGraphics.drawString(minecraft.font, sequence, 0, 0, 0x000000, false);
                pose.translate(0, minecraft.font.lineHeight, 0);
            }
            pose.popPose();

            y += lineHeight;
            maxY -= lineHeight;
        }
    }

    /**
     * 计算引用块总高度。
     */
    @Override
    public int getHeight(Minecraft minecraft, int maxX, int maxY) {
        int totalHeight = 0;
        for (QuoteLine line : this.lines) {
            int textX = line.level() * LEVEL_INDENT + TEXT_PADDING;
            int lineMaxX = Math.max(1, maxX - textX);
            FormattedText formatted = MDComponent.textFormat(line.text(), Style.EMPTY.withColor(DEFAULT_TEXT_COLOR));
            totalHeight += minecraft.font.wordWrapHeight(formatted, lineMaxX);
        }
        return totalHeight;
    }

    private static int getLevelLineColor(int level) {
        return LEVEL_LINE_COLORS[level % LEVEL_LINE_COLORS.length];
    }

    /**
     * 单行引用数据，包含层级与文本。
     */
    public record QuoteLine(int level, String text) {
    }
}

