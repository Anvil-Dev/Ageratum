package dev.anvilcraft.resource.ageratum.client.feat.markdown.component;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.FormattedText;
import net.minecraft.network.chat.Style;
import net.minecraft.util.FormattedCharSequence;

import java.util.ArrayList;
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
    private static final Style DEFAULT_TEXT_STYLE = Style.EMPTY.withColor(DEFAULT_TEXT_COLOR);
    private static final int[] LEVEL_LINE_COLORS = {
        0xFF7A7A7A,
        0xFF6A7FA8,
        0xFF8A6AA8,
        0xFF7A8F66
    };
    private final List<CachedQuoteLine> lines;

    /**
     * 使用解析后的引用行创建组件。
     */
    public MDQuoteComponent(List<QuoteLine> lines) {
        this(prepare(lines));
    }

    private MDQuoteComponent(PreparedData preparedData) {
        super(preparedData.componentText());
        this.lines = preparedData.lines();
    }

    /**
     * 渲染引用块内容与层级竖线。
     */
    @Override
    public void render(GuiGraphics guiGraphics, Minecraft minecraft, int maxX, int maxY) {
        int y = 0;
        for (CachedQuoteLine line : this.lines) {
            int textX = line.level() * LEVEL_INDENT + TEXT_PADDING;
            int lineMaxX = Math.max(1, maxX - textX);
            List<FormattedCharSequence> split = minecraft.font.split(line.text(), lineMaxX);
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
        for (CachedQuoteLine line : this.lines) {
            int textX = line.level() * LEVEL_INDENT + TEXT_PADDING;
            int lineMaxX = Math.max(1, maxX - textX);
            totalHeight += minecraft.font.wordWrapHeight(line.text(), lineMaxX);
        }
        return totalHeight;
    }

    private static PreparedData prepare(List<QuoteLine> sourceLines) {
        List<QuoteLine> quoteLines = List.copyOf(sourceLines);
        List<CachedQuoteLine> cachedLines = new ArrayList<>(quoteLines.size());
        List<FormattedText> componentParts = new ArrayList<>(Math.max(1, quoteLines.size() * 2));

        for (int i = 0; i < quoteLines.size(); i++) {
            QuoteLine line = quoteLines.get(i);
            FormattedText formattedText = MDComponent.textFormat(line.text(), DEFAULT_TEXT_STYLE);
            cachedLines.add(new CachedQuoteLine(line.level(), formattedText));
            componentParts.add(formattedText);
            if (i < quoteLines.size() - 1) {
                componentParts.add(FormattedText.of("\n"));
            }
        }

        FormattedText componentText = componentParts.isEmpty() ? FormattedText.EMPTY : FormattedText.composite(componentParts);
        return new PreparedData(List.copyOf(cachedLines), componentText);
    }

    private static int getLevelLineColor(int level) {
        return LEVEL_LINE_COLORS[level % LEVEL_LINE_COLORS.length];
    }

    private record CachedQuoteLine(int level, FormattedText text) {
    }

    private record PreparedData(List<CachedQuoteLine> lines, FormattedText componentText) {
    }

    /**
     * 单行引用数据，包含层级与文本。
     */
    public record QuoteLine(int level, String text) {
    }
}

