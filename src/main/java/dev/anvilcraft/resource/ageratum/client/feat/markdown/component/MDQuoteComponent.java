package dev.anvilcraft.resource.ageratum.client.feat.markdown.component;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.FormattedText;
import net.minecraft.network.chat.Style;
import net.minecraft.util.FormattedCharSequence;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import javax.annotation.Nullable;

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

    /**
     * 渲染引用块内容与层级竖线。
     */
    @Override
    public void render(GuiGraphics guiGraphics, Minecraft minecraft, int maxX, int maxY) {
        int y = 0;
        for (CachedItem<Integer> line : this.cachedItems) {
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

            AtomicInteger atomicY = new AtomicInteger(y);
            AtomicInteger atomicMaxY = new AtomicInteger(maxY);
            super.drawContent(guiGraphics, minecraft, split, textX, atomicY, lineHeight, atomicMaxY);
            y = atomicY.get();
            maxY = atomicMaxY.get();
        }
    }

    @Override
    protected int getItemHeight(Minecraft minecraft, CachedItem<Integer> cachedItem, int maxX) {
        int textX = cachedItem.level() * LEVEL_INDENT + TEXT_PADDING;
        int lineMaxX = Math.max(1, maxX - textX);
        return minecraft.font.wordWrapHeight(cachedItem.text(), lineMaxX);
    }

    @Override
    @Nullable
    public Style getStyleAtPosition(Minecraft minecraft, double mouseX, double mouseY, int maxX) {
        if (mouseX < 0 || mouseY < 0) {
            return null;
        }

        double currentY = 0;
        for (CachedItem<Integer> line : this.cachedItems) {
            int textX = line.level() * LEVEL_INDENT + TEXT_PADDING;
            int lineMaxX = Math.max(1, maxX - textX);
            int lineHeight = minecraft.font.wordWrapHeight(line.text(), lineMaxX);
            if (mouseY >= currentY && mouseY < currentY + lineHeight) {
                return this.getStyleAtFormattedTextPosition(
                    minecraft,
                    line.text(),
                    mouseX - textX,
                    mouseY - currentY,
                    lineMaxX
                );
            }
            currentY += lineHeight;
        }

        return null;
    }

    private static PreparedData prepare(List<QuoteLine> sourceLines) {
        List<QuoteLine> quoteLines = List.copyOf(sourceLines);
        List<CachedItem<Integer>> cachedLines = new ArrayList<>(quoteLines.size());
        List<FormattedText> componentParts = new ArrayList<>(Math.max(1, quoteLines.size() * 2));

        for (int i = 0; i < quoteLines.size(); i++) {
            QuoteLine line = quoteLines.get(i);
            FormattedText formattedText = MDComponent.textFormat(line.text(), DEFAULT_TEXT_STYLE);
            cachedLines.add(new CachedItem<>(line.level(), line.level(), formattedText));
            componentParts.add(formattedText);
            if (i < quoteLines.size() - 1) {
                componentParts.add(FormattedText.of("\n"));
            }
        }

        FormattedText componentText = componentParts.isEmpty() ? FormattedText.EMPTY : FormattedText.composite(componentParts);
        return new PreparedData(List.copyOf(cachedLines), componentText);
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

