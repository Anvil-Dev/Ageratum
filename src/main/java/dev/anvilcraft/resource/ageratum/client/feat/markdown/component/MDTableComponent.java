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
    private static final Style HEADER_CELL_STYLE = Style.EMPTY.withBold(true);
    private static final CachedCell EMPTY_CELL = new CachedCell(FormattedText.EMPTY, FormattedText.EMPTY);

    /**
     * 表格列对齐方式。
     */
    public enum Alignment {LEFT, CENTER, RIGHT}

    private final List<List<CachedCell>> rows;
    private final Alignment[] alignments;
    private final int columnCount;
    private final boolean hasHeader;

    /**
     * 创建表格组件。
     */
    private MDTableComponent(List<List<CachedCell>> rows, Alignment[] alignments, boolean hasHeader, FormattedText componentText) {
        super(componentText);
        this.rows = rows;
        this.alignments = alignments;
        this.columnCount = Math.max(1, alignments.length);
        this.hasHeader = hasHeader;
    }

    /**
     * 从连续表格行文本解析出表格组件。
     */
    public static MDTableComponent parse(List<String> tableRowStrings) {
        List<String[]> parsedRows = new ArrayList<>();
        Alignment[] alignments = null;
        boolean hasHeader = false;
        int maxCols = 1;

        for (String rowStr : tableRowStrings) {
            String[] cells = splitRow(rowStr);
            maxCols = Math.max(maxCols, cells.length);
            if (alignments == null && isSeparator(cells)) {
                alignments = parseAlignments(cells);
                if (!parsedRows.isEmpty()) {
                    hasHeader = true;
                }
            } else {
                parsedRows.add(cells);
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

        PreparedData preparedData = prepareRows(parsedRows);
        return new MDTableComponent(preparedData.rows(), alignments, hasHeader, preparedData.componentText());
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
        if (this.rows.isEmpty()) return;
        int colWidth = computeColWidth(maxX);
        int totalHeight = getHeight(minecraft, maxX, maxY);
        guiGraphics.renderOutline(0, 0, maxX, totalHeight, BORDER_COLOR);

        int y = 0;
        for (int rowIdx = 0; rowIdx < this.rows.size(); rowIdx++) {
            List<CachedCell> row = this.rows.get(rowIdx);
            boolean isHeader = this.hasHeader && rowIdx == 0;
            int rowH = rowHeight(minecraft, row, colWidth, isHeader);

            if (isHeader) {
                guiGraphics.fill(1, y, maxX - 1, y + rowH, HEADER_COLOR);
            } else if ((rowIdx - (this.hasHeader ? 1 : 0)) % 2 == 1) {
                guiGraphics.fill(1, y, maxX - 1, y + rowH, ALT_ROW_COLOR);
            }

            for (int col = 0; col < this.columnCount; col++) {
                CachedCell cell = col < row.size() ? row.get(col) : EMPTY_CELL;
                int cellX = PADDING_H + col * (colWidth + PADDING_H * 2);
                List<FormattedCharSequence> lines = splitCellLines(minecraft, cell, colWidth, isHeader);

                PoseStack pose = guiGraphics.pose();
                pose.pushPose();
                pose.translate(cellX, y + PADDING_V, 0);
                for (FormattedCharSequence seq : lines) {
                    int drawX = switch (this.alignments[col]) {
                        case CENTER -> Math.max(0, (colWidth - minecraft.font.width(seq)) / 2);
                        case RIGHT -> Math.max(0, colWidth - minecraft.font.width(seq));
                        default -> 0;
                    };
                    guiGraphics.drawString(minecraft.font, seq, drawX, 0, 0x000000, false);
                    pose.translate(0, minecraft.font.lineHeight, 0);
                }
                pose.popPose();
            }

            for (int col = 1; col < this.columnCount; col++) {
                guiGraphics.vLine(col * (colWidth + PADDING_H * 2), y, y + rowH - 1, BORDER_COLOR);
            }

            if (rowIdx < this.rows.size() - 1) {
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
        if (this.rows.isEmpty()) return 0;
        int colWidth = computeColWidth(maxX);
        int total = 0;
        for (int rowIdx = 0; rowIdx < this.rows.size(); rowIdx++) {
            boolean isHeader = this.hasHeader && rowIdx == 0;
            total += rowHeight(minecraft, this.rows.get(rowIdx), colWidth, isHeader);
        }
        return total;
    }

    /**
     * 计算等宽列宽（包含左右内边距预算）。
     */
    private int computeColWidth(int maxX) {
        return Math.max(10, (maxX - PADDING_H * 2 * this.columnCount) / this.columnCount);
    }

    /**
     * 计算单行表格的渲染高度。
     */
    private int rowHeight(Minecraft minecraft, List<CachedCell> row, int colWidth, boolean isHeader) {
        int maxLines = 1;
        for (int col = 0; col < this.columnCount; col++) {
            CachedCell cell = col < row.size() ? row.get(col) : EMPTY_CELL;
            maxLines = Math.max(maxLines, Math.max(1, splitCellLines(minecraft, cell, colWidth, isHeader).size()));
        }
        return maxLines * minecraft.font.lineHeight + PADDING_V * 2;
    }

    /**
     * 使用与渲染一致的样式规则拆分单元格行，避免测量与绘制不一致。
     */
    private List<FormattedCharSequence> splitCellLines(Minecraft minecraft, CachedCell cell, int colWidth, boolean isHeader) {
        return minecraft.font.split(isHeader ? cell.headerText() : cell.bodyText(), colWidth);
    }

    private static PreparedData prepareRows(List<String[]> parsedRows) {
        List<List<CachedCell>> cachedRows = new ArrayList<>(parsedRows.size());
        List<FormattedText> componentParts = new ArrayList<>();

        for (int rowIndex = 0; rowIndex < parsedRows.size(); rowIndex++) {
            String[] row = parsedRows.get(rowIndex);
            List<CachedCell> cachedRow = new ArrayList<>(row.length);
            for (int colIndex = 0; colIndex < row.length; colIndex++) {
                String cell = row[colIndex];
                FormattedText bodyText = MDComponent.textFormat(cell);
                FormattedText headerText = MDComponent.textFormat(cell, HEADER_CELL_STYLE);
                cachedRow.add(new CachedCell(bodyText, headerText));
                componentParts.add(bodyText);
                if (colIndex < row.length - 1) {
                    componentParts.add(FormattedText.of("\t"));
                }
            }
            cachedRows.add(List.copyOf(cachedRow));
            if (rowIndex < parsedRows.size() - 1) {
                componentParts.add(FormattedText.of("\n"));
            }
        }

        FormattedText componentText = componentParts.isEmpty() ? FormattedText.EMPTY : FormattedText.composite(componentParts);
        return new PreparedData(List.copyOf(cachedRows), componentText);
    }

    private record CachedCell(FormattedText bodyText, FormattedText headerText) {
    }

    private record PreparedData(List<List<CachedCell>> rows, FormattedText componentText) {
    }
}
