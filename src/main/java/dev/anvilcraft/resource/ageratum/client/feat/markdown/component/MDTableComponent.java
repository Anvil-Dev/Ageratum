package dev.anvilcraft.resource.ageratum.client.feat.markdown.component;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.FormattedText;
import net.minecraft.network.chat.Style;
import net.minecraft.util.FormattedCharSequence;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Markdown 表格组件。
 *
 * <p>支持解析管道表格语法、列对齐声明行，并渲染表头高亮与隔行背景。</p>
 */
public class MDTableComponent extends MDComponent {
    private static final int PADDING_H = 4;
    private static final int PADDING_V = 2;
    private static final int HEADER_COLOR = 0x33555555;
    private static final int ALT_ROW_COLOR = 0x11AAAAAA;
    private static final int BORDER_COLOR = 0x88444444;
    private static final int HEADER_SEP_COLOR = 0xBB444444;

    /**
     * 表格列对齐方式。
     */
    public enum Alignment {LEFT, CENTER, RIGHT}

    private final List<String[]> rows;
    private final Alignment[] alignments;
    private final int columnCount;
    private final boolean hasHeader;

    /**
     * 创建表格组件。
     */
    private MDTableComponent(List<String[]> rows, Alignment[] alignments, boolean hasHeader) {
        super(FormattedText.EMPTY);
        this.rows = List.copyOf(rows);
        this.alignments = alignments;
        this.columnCount = Math.max(1, alignments.length);
        this.hasHeader = hasHeader;
    }

    /**
     * 从连续表格行文本解析出表格组件。
     */
    public static MDTableComponent parse(List<String> tableRowStrings) {
        List<String[]> rows = new ArrayList<>();
        Alignment[] alignments = null;
        boolean hasHeader = false;
        int maxCols = 1;

        for (String rowStr : tableRowStrings) {
            String[] cells = splitRow(rowStr);
            maxCols = Math.max(maxCols, cells.length);
            if (alignments == null && isSeparator(cells)) {
                alignments = parseAlignments(cells);
                if (!rows.isEmpty()) {
                    hasHeader = true;
                }
            } else {
                rows.add(cells);
            }
        }

        if (alignments == null) {
            alignments = new Alignment[maxCols];
            Arrays.fill(alignments, Alignment.LEFT);
        }
        if (alignments.length < maxCols) {
            Alignment[] padded = Arrays.copyOf(alignments, maxCols);
            Arrays.fill(padded, alignments.length, maxCols, Alignment.LEFT);
            alignments = padded;
        }

        return new MDTableComponent(rows, alignments, hasHeader);
    }

    /**
     * 将单行表格文本拆分为单元格数组。
     */
    private static String[] splitRow(String row) {
        String s = row.trim();
        if (s.startsWith("|")) s = s.substring(1);
        if (s.endsWith("|")) s = s.substring(0, s.length() - 1);
        String[] cells = s.split("\\|", -1);
        for (int i = 0; i < cells.length; i++) cells[i] = cells[i].trim();
        return cells;
    }

    /**
     * 判断一行是否为对齐分隔行（如 {@code |:---|---:|}）。
     */
    private static boolean isSeparator(String[] cells) {
        if (cells.length == 0) return false;
        for (String cell : cells) {
            if (!cell.matches(":?-+:?")) return false;
        }
        return true;
    }

    /**
     * 根据分隔行内容解析每列对齐方式。
     */
    private static Alignment[] parseAlignments(String[] cells) {
        Alignment[] result = new Alignment[cells.length];
        for (int i = 0; i < cells.length; i++) {
            String c = cells[i];
            boolean l = c.startsWith(":");
            boolean r = c.endsWith(":");
            result[i] = (l && r) ? Alignment.CENTER : r ? Alignment.RIGHT : Alignment.LEFT;
        }
        return result;
    }

    /**
     * 渲染表格边框、背景与单元格文本。
     */
    @Override
    public void render(GuiGraphics guiGraphics, Minecraft minecraft, int maxX, int maxY) {
        if (rows.isEmpty()) return;
        int colWidth = computeColWidth(maxX);
        int totalHeight = getHeight(minecraft, maxX, maxY);
        guiGraphics.renderOutline(0, 0, maxX, totalHeight, BORDER_COLOR);

        int y = 0;
        for (int rowIdx = 0; rowIdx < rows.size(); rowIdx++) {
            String[] row = rows.get(rowIdx);
            int rowH = rowHeight(minecraft, row, colWidth);
            boolean isHeader = hasHeader && rowIdx == 0;

            if (isHeader) {
                guiGraphics.fill(1, y, maxX - 1, y + rowH, HEADER_COLOR);
            } else if ((rowIdx - (hasHeader ? 1 : 0)) % 2 == 1) {
                guiGraphics.fill(1, y, maxX - 1, y + rowH, ALT_ROW_COLOR);
            }

            for (int col = 0; col < columnCount; col++) {
                String cell = col < row.length ? row[col] : "";
                int cellX = PADDING_H + col * (colWidth + PADDING_H * 2);
                Style style = isHeader ? Style.EMPTY.withBold(true) : Style.EMPTY;
                List<FormattedCharSequence> lines = minecraft.font.split(MDComponent.textFormat(cell, style), colWidth);

                PoseStack pose = guiGraphics.pose();
                pose.pushPose();
                pose.translate(cellX, y + PADDING_V, 0);
                for (FormattedCharSequence seq : lines) {
                    int drawX = switch (alignments[col]) {
                        case CENTER -> Math.max(0, (colWidth - minecraft.font.width(seq)) / 2);
                        case RIGHT -> Math.max(0, colWidth - minecraft.font.width(seq));
                        default -> 0;
                    };
                    guiGraphics.drawString(minecraft.font, seq, drawX, 0, 0x000000, false);
                    pose.translate(0, minecraft.font.lineHeight, 0);
                }
                pose.popPose();
            }

            for (int col = 1; col < columnCount; col++) {
                guiGraphics.vLine(col * (colWidth + PADDING_H * 2), y, y + rowH - 1, BORDER_COLOR);
            }

            if (rowIdx < rows.size() - 1) {
                int sepColor = (isHeader) ? HEADER_SEP_COLOR : BORDER_COLOR;
                guiGraphics.hLine(1, maxX - 2, y + rowH - 1, sepColor);
            }

            y += rowH;
        }
    }

    /**
     * 计算表格总高度。
     */
    @Override
    public int getHeight(Minecraft minecraft, int maxX, int maxY) {
        if (rows.isEmpty()) return 0;
        int colWidth = computeColWidth(maxX);
        int total = 0;
        for (String[] row : rows) {
            total += rowHeight(minecraft, row, colWidth);
        }
        return total;
    }

    /**
     * 计算等宽列宽（包含左右内边距预算）。
     */
    private int computeColWidth(int maxX) {
        return Math.max(10, (maxX - PADDING_H * 2 * columnCount) / columnCount);
    }

    /**
     * 计算单行表格的渲染高度。
     */
    private int rowHeight(Minecraft minecraft, String[] row, int colWidth) {
        int maxLines = 1;
        for (int col = 0; col < columnCount; col++) {
            String cell = col < row.length ? row[col] : "";
            maxLines = Math.max(maxLines, Math.max(1, minecraft.font.split(MDComponent.textFormat(cell), colWidth).size()));
        }
        return maxLines * minecraft.font.lineHeight + PADDING_V * 2;
    }
}

