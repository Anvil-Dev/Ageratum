package dev.anvilcraft.resource.ageratum.client.feat.markdown.component;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.FormattedText;
import net.minecraft.util.FormattedCharSequence;

import java.util.List;

/**
 * Markdown 列表组件。
 *
 * <p>统一渲染无序列表、有序列表与任务列表，并支持按缩进层级绘制视觉引导带。</p>
 */
public class MDListComponent extends MDComponent {
    private static final String[] BULLETS = {"•", "▪", "◆", "▸"};
    private static final String TASK_UNCHECKED = "☐";
    private static final String TASK_CHECKED = "☑";
    private static final int INDENT_WIDTH = 10;
    private static final int MARKER_WIDTH = 12;
    private static final int[] INDENT_BAND_COLORS = {
        0x223B6EA8,
        0x225E8A66,
        0x228A6AA8,
        0x228A7A66
    };
    private static final int TASK_UNCHECKED_COLOR = 0x666666;
    private static final int TASK_CHECKED_COLOR = 0x2E7D32;
    private final List<ListItem> items;

    /**
     * 使用解析后的列表项创建组件。
     */
    public MDListComponent(List<ListItem> items) {
        super(FormattedText.EMPTY);
        this.items = List.copyOf(items);
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
        for (ListItem item : this.items) {
            int textX = item.level() * INDENT_WIDTH + MARKER_WIDTH;
            int lineMaxX = Math.max(1, maxX - textX);
            FormattedText text = MDComponent.textFormat(item.text());
            List<FormattedCharSequence> split = minecraft.font.split(text, lineMaxX);
            int lineHeight = split.size() * minecraft.font.lineHeight;
            if (lineHeight <= 0 || maxY < lineHeight) {
                return;
            }

            int lineBottom = y + lineHeight;
            for (int level = 0; level <= item.level(); level++) {
                int bandStartX = level * INDENT_WIDTH;
                guiGraphics.fill(
                    bandStartX,
                    y,
                    bandStartX + INDENT_WIDTH - 1,
                    lineBottom,
                    INDENT_BAND_COLORS[level % INDENT_BAND_COLORS.length]
                );
            }

            guiGraphics.drawString(
                minecraft.font,
                marker(item),
                item.level() * INDENT_WIDTH,
                y,
                markerColor(item),
                false
            );

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
     * 计算列表总高度。
     */
    @Override
    public int getHeight(Minecraft minecraft, int maxX, int maxY) {
        int totalHeight = 0;
        for (ListItem item : this.items) {
            int textX = item.level() * INDENT_WIDTH + MARKER_WIDTH;
            int lineMaxX = Math.max(1, maxX - textX);
            totalHeight += minecraft.font.wordWrapHeight(MDComponent.textFormat(item.text()), lineMaxX);
        }
        return totalHeight;
    }

    private static String marker(ListItem item) {
        return switch (item.kind()) {
            case UNORDERED -> BULLETS[item.level() % BULLETS.length];
            case ORDERED -> item.index() + ".";
            case TASK -> item.checked() ? TASK_CHECKED : TASK_UNCHECKED;
        };
    }

    private static int markerColor(ListItem item) {
        if (item.kind() == ListKind.TASK) {
            return item.checked() ? TASK_CHECKED_COLOR : TASK_UNCHECKED_COLOR;
        }
        return 0x000000;
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

