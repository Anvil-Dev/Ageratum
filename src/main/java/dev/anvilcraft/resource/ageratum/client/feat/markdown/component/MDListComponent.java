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
 * Markdown 列表组件。
 *
 * <p>统一渲染无序列表、有序列表与任务列表，并支持按缩进层级绘制视觉引导带。</p>
 */
public class MDListComponent extends MDBlockComponent<MDListComponent.ListItem> {
    private static final String[] BULLETS = {
        "●",
        "○",
        "■",
        "□",
        "◆",
        "◇",
        "▸",
        "▹"
    };
    private static final String TASK_UNCHECKED = "☐";
    private static final String TASK_CHECKED = "☑";
    private static final int INDENT_WIDTH = 10;
    private static final int MARKER_WIDTH = 12;
    private static final int TASK_UNCHECKED_COLOR = 0x666666;
    private static final int TASK_CHECKED_COLOR = 0x2E7D32;

    /**
     * 使用解析后的列表项创建组件。
     */
    public MDListComponent(List<ListItem> items) {
        this(prepare(items));
    }

    private MDListComponent(PreparedData preparedData) {
        super(preparedData.componentText(), preparedData.cachedItems());
    }

    /**
     * 创建无序列表项。
     */
    public static ListItem unordered(int level, String text) {
        return new ListItem(ListKind.UNORDERED, Math.max(0, level), 0, false, text);
    }

    /**
     * 创建有序列表项。
     */
    public static ListItem ordered(int level, int index, String text) {
        return new ListItem(ListKind.ORDERED, Math.max(0, level), Math.max(1, index), false, text);
    }

    /**
     * 创建任务列表项。
     */
    public static ListItem task(int level, boolean checked, String text) {
        return new ListItem(ListKind.TASK, Math.max(0, level), 0, checked, text);
    }

    /**
     * 渲染整组列表项。
     */
    @Override
    public void render(GuiGraphics guiGraphics, Minecraft minecraft, int maxX, int maxY) {
        int y = 0;
        for (CachedItem<ListItem> cachedItem : this.cachedItems) {
            ListItem item = cachedItem.item();
            int textX = getTextX(item);
            int lineMaxX = getLineMaxX(maxX, textX);
            List<FormattedCharSequence> split = minecraft.font.split(cachedItem.text(), lineMaxX);
            int lineHeight = split.size() * minecraft.font.lineHeight;
            if (lineHeight <= 0 || maxY < lineHeight) {
                return;
            }

            int lineBottom = y + lineHeight;
            for (int level = 0; level <= cachedItem.level(); level++) {
                int bandStartX = level * INDENT_WIDTH;
                guiGraphics.fill(
                    bandStartX,
                    y,
                    bandStartX + INDENT_WIDTH - 1,
                    lineBottom,
                    LEVEL_LINE_COLORS[level % LEVEL_LINE_COLORS.length] | 0x55000000
                );
            }

            guiGraphics.drawString(
                minecraft.font,
                marker(item),
                cachedItem.level() * INDENT_WIDTH,
                y,
                markerColor(item),
                false
            );

            AtomicInteger atomicY = new AtomicInteger(y);
            AtomicInteger atomicMaxY = new AtomicInteger(maxY);
            super.drawContent(guiGraphics, minecraft, split, textX, atomicY, lineHeight, atomicMaxY);
            y = atomicY.get();
            maxY = atomicMaxY.get();
        }
    }

    @Override
    @Nullable
    public Style getStyleAtPosition(Minecraft minecraft, double mouseX, double mouseY, int maxX) {
        if (mouseX < 0 || mouseY < 0) {
            return null;
        }

        double currentY = 0;
        for (CachedItem<ListItem> cachedItem : this.cachedItems) {
            ListItem item = cachedItem.item();
            int itemHeight = getItemHeight(minecraft, cachedItem, maxX);
            if (mouseY >= currentY && mouseY < currentY + itemHeight) {
                int textX = getTextX(item);
                if (mouseX < textX) {
                    return null;
                }
                return this.getStyleAtFormattedTextPosition(
                    minecraft,
                    cachedItem.text(),
                    mouseX - textX,
                    mouseY - currentY,
                    getLineMaxX(maxX, textX)
                );
            }
            currentY += itemHeight;
        }

        return null;
    }

    private static PreparedData prepare(List<ListItem> sourceItems) {
        List<ListItem> items = List.copyOf(sourceItems);
        List<CachedItem<ListItem>> cachedItems = new ArrayList<>(items.size());
        List<FormattedText> componentParts = new ArrayList<>(Math.max(1, items.size() * 2));

        for (int i = 0; i < items.size(); i++) {
            ListItem item = items.get(i);
            Style style = Style.EMPTY;
            if (item.kind == ListKind.TASK && item.checked()) {
                style = style.withStrikethrough(true);
            }
            FormattedText formattedText = MDComponent.textFormat(item.text(), style);
            cachedItems.add(new CachedItem<>(item.level, item, formattedText));
            componentParts.add(formattedText);
            if (i < items.size() - 1) {
                componentParts.add(FormattedText.of("\n"));
            }
        }

        FormattedText componentText = componentParts.isEmpty() ? FormattedText.EMPTY : FormattedText.composite(componentParts);
        return new PreparedData(List.copyOf(cachedItems), componentText);
    }

    private static String marker(ListItem item) {
        return switch (item.kind()) {
            case UNORDERED -> BULLETS[item.level() % BULLETS.length];
            case ORDERED -> item.index() + ".";
            case TASK -> item.checked() ? TASK_CHECKED : TASK_UNCHECKED;
        };
    }

    private static int getTextX(ListItem item) {
        return item.level() * INDENT_WIDTH + MARKER_WIDTH;
    }

    private static int getLineMaxX(int maxX, int textX) {
        return Math.max(1, maxX - textX);
    }

    protected int getItemHeight(Minecraft minecraft, CachedItem<ListItem> cachedItem, int maxX) {
        int textX = getTextX(cachedItem.item());
        return minecraft.font.wordWrapHeight(cachedItem.text(), getLineMaxX(maxX, textX));
    }

    private static int markerColor(ListItem item) {
        if (item.kind() == ListKind.TASK) {
            return item.checked() ? TASK_CHECKED_COLOR : TASK_UNCHECKED_COLOR;
        }
        return 0x000000;
    }

    private record PreparedData(List<CachedItem<ListItem>> cachedItems, FormattedText componentText) {
    }

    /**
     * 列表类型。
     */
    public enum ListKind {
        UNORDERED,
        ORDERED,
        TASK
    }

    /**
     * 单个列表项的数据结构。
     */
    public record ListItem(ListKind kind, int level, int index, boolean checked, String text) {
    }
}

