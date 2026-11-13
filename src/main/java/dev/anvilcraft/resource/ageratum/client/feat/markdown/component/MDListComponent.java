package dev.anvilcraft.resource.ageratum.client.feat.markdown.component;

import dev.anvilcraft.resource.ageratum.client.gui.GuideTheme;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.FormattedText;
import net.minecraft.network.chat.Style;

import java.util.ArrayList;
import java.util.List;
import javax.annotation.Nullable;

/**
 * Markdown 列表组件。
 *
 * <p>统一渲染无序列表、有序列表与任务列表，按首行对齐标记并为多位编号预留统一宽度。</p>
 */
public class MDListComponent extends MDBlockComponent<MDListComponent.ListItem> {
    private static final int INDENT_WIDTH = 12;
    private static final int MIN_MARKER_WIDTH = 8;
    private static final int MARKER_GAP = 5;
    private final java.util.Map<Integer, Integer> orderedDigits = new java.util.HashMap<>();

    /**
     * 使用解析后的列表项创建组件。
     */
    public MDListComponent(List<ListItem> items) {
        this(prepare(items));
    }

    private MDListComponent(PreparedData preparedData) {
        super(preparedData.componentText(), preparedData.cachedItems());
        for (CachedItem<ListItem> cached : this.cachedItems) {
            if (cached.item().kind() == ListKind.ORDERED) {
                this.orderedDigits.merge(cached.level(), Integer.toString(cached.item().index()).length(), Math::max);
            }
        }
    }

    /**
     * 创建无序列表项。
     */
    public static ListItem unordered(int level, String text) {
        return new ListItem(ListKind.UNORDERED, Math.max(0, level), 0, false, text);
    }

    /**
     * Creates an unordered item with preformatted text.
     */
    public static ListItem unordered(int level, FormattedText text) {
        return new ListItem(ListKind.UNORDERED, Math.max(0, level), 0, false, null, text);
    }

    public static ListItem ordered(int level, int index, String text) {
        return new ListItem(ListKind.ORDERED, Math.max(0, level), Math.max(1, index), false, text);
    }

    /**
     * 创建任务列表项。
     */
    public static ListItem task(int level, boolean checked, String text) {
        return new ListItem(ListKind.TASK, Math.max(0, level), 0, checked, text);
    }

    @Override
    protected int getTextX(CachedItem<ListItem> cachedItem) {
        int x = this.markerWidth(0) + MARKER_GAP;
        for (int level = 1; level <= cachedItem.level(); level++) {
            x += Math.max(INDENT_WIDTH, this.markerWidth(level) + MARKER_GAP);
        }
        return x;
    }

    @Override
    protected void renderDecoration(
        GuiGraphics guiGraphics,
        Minecraft minecraft,
        CachedItem<ListItem> cachedItem,
        int y,
        int lineHeight,
        int maxX
    ) {
        ListItem item = cachedItem.item();
        int right = this.getTextX(cachedItem) - MARKER_GAP;
        int color = GuideTheme.textColor();
        if (item.kind() == ListKind.ORDERED) {
            String text = item.index() + ".";
            guiGraphics.drawString(minecraft.font, text, right - minecraft.font.width(text), y, color, false);
            return;
        }
        int centerX = right - this.markerWidth(cachedItem.level()) / 2;
        int centerY = y + minecraft.font.lineHeight / 2;
        if (item.kind() == ListKind.TASK) {
            guiGraphics.renderOutline(centerX - 3, centerY - 3, 7, 7, color);
            if (item.checked()) {
                guiGraphics.fill(centerX - 2, centerY, centerX, centerY + 1, color);
                guiGraphics.fill(centerX - 1, centerY + 1, centerX + 1, centerY + 2, color);
                guiGraphics.fill(centerX, centerY, centerX + 2, centerY + 1, color);
                guiGraphics.fill(centerX + 1, centerY - 1, centerX + 3, centerY, color);
            }
            return;
        }
        guiGraphics.pose().pushPose();
        guiGraphics.pose().translate(centerX + 0.5f, centerY + 0.5f, 0);
        guiGraphics.pose().scale(0.5f, 0.5f, 1);
        for (int row = -3; row < 3; row++) {
            int halfWidth = 3 - (row < 0 ? -row - 1 : row);
            if (item.level() % 2 == 0 || halfWidth == 1) {
                guiGraphics.fill(-halfWidth, row, halfWidth, row + 1, color);
            } else {
                guiGraphics.fill(-halfWidth, row, -halfWidth + 1, row + 1, color);
                guiGraphics.fill(halfWidth - 1, row, halfWidth, row + 1, color);
            }
        }
        guiGraphics.pose().popPose();
    }

    private int markerWidth(int level) {
        int digits = this.orderedDigits.getOrDefault(level, 0);
        if (digits == 0) return MIN_MARKER_WIDTH;
        int digitWidth = 0;
        for (char digit = '0'; digit <= '9'; digit++) {
            digitWidth = Math.max(digitWidth, Minecraft.getInstance().font.width(String.valueOf(digit)));
        }
        return Math.max(MIN_MARKER_WIDTH, digits * digitWidth + Minecraft.getInstance().font.width("."));
    }

    private static PreparedData prepare(List<ListItem> sourceItems) {
        List<ListItem> items = List.copyOf(sourceItems);
        List<CachedItem<ListItem>> cachedItems = new ArrayList<>(items.size());

        for (ListItem item : items) {
            Style style = Style.EMPTY;
            if (item.kind() == ListKind.TASK && item.checked()) {
                style = style.withStrikethrough(true);
            }
            FormattedText formattedText = item.formattedText() != null
                ? item.formattedText()
                : MDComponent.textFormat(item.text(), style);
            cachedItems.add(new CachedItem<>(item.level(), item, formattedText));
        }

        return new PreparedData(List.copyOf(cachedItems), composeBlockText(cachedItems));
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
    public record ListItem(
        ListKind kind,
        int level,
        int index,
        boolean checked,
        @Nullable String text,
        @Nullable FormattedText formattedText
    ) {
        public ListItem(ListKind kind, int level, int index, boolean checked, String text) {
            this(kind, level, index, checked, text, null);
        }
    }
}

