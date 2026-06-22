package dev.anvilcraft.resource.ageratum.client.feat.markdown.component;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.FormattedText;
import net.minecraft.network.chat.Style;

import java.util.ArrayList;
import java.util.List;

/**
 * 引用块组件。
 *
 * <p>按层级渲染左侧竖线，并对引用文本使用较浅默认颜色。</p>
 */
public class MDQuoteComponent extends MDBlockComponent<Integer> {
    private static final int LEVEL_INDENT = 10;
    private static final int TEXT_PADDING = 4;
    private static final int DEFAULT_TEXT_COLOR = 0x888888;
    private static final Style DEFAULT_TEXT_STYLE = Style.EMPTY.withColor(DEFAULT_TEXT_COLOR);

    /**
     * 使用解析后的引用行创建组件。
     */
    public MDQuoteComponent(List<QuoteLine> cachedItems) {
        this(prepare(cachedItems));
    }

    private MDQuoteComponent(PreparedData preparedData) {
        super(preparedData.componentText(), preparedData.lines());
    }

    @Override
    protected int getTextX(CachedItem<Integer> cachedItem) {
        return cachedItem.level() * LEVEL_INDENT + TEXT_PADDING;
    }

    @Override
    protected void renderDecoration(
        GuiGraphicsExtractor GuiGraphicsExtractor,
        Minecraft minecraft,
        CachedItem<Integer> cachedItem,
        int y,
        int lineHeight,
        int maxX
    ) {
        int lineBottom = y + lineHeight - 1;
        for (int level = 0; level < cachedItem.level(); level++) {
            int lineX = level * LEVEL_INDENT + 1;
            GuiGraphicsExtractor.vLine(lineX, y, lineBottom, getLevelLineColor(level));
        }
    }

    private static PreparedData prepare(List<QuoteLine> sourceLines) {
        List<QuoteLine> quoteLines = List.copyOf(sourceLines);
        List<CachedItem<Integer>> cachedLines = new ArrayList<>(quoteLines.size());

        for (QuoteLine line : quoteLines) {
            FormattedText formattedText = MDComponent.textFormat(line.text(), DEFAULT_TEXT_STYLE);
            cachedLines.add(new CachedItem<>(line.level(), line.level(), formattedText));
        }

        return new PreparedData(List.copyOf(cachedLines), composeBlockText(cachedLines));
    }

    private static int getLevelLineColor(int level) {
        return LEVEL_LINE_COLORS[level % LEVEL_LINE_COLORS.length] | 0xFF000000;
    }

    private record PreparedData(List<CachedItem<Integer>> lines, FormattedText componentText) {
    }

    /**
     * 单行引用数据，包含层级与文本。
     */
    public record QuoteLine(int level, String text) {
    }
}

