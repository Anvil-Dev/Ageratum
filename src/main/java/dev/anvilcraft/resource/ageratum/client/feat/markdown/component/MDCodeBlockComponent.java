package dev.anvilcraft.resource.ageratum.client.feat.markdown.component;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.FormattedText;
import net.minecraft.network.chat.Style;
import net.minecraft.util.FormattedCharSequence;

import java.util.List;

/**
 * 代码块组件。
 *
 * <p>负责渲染带背景、边框、行号栏的多行代码文本，
 * 并在给定宽度下自动换行。</p>
 */
public class MDCodeBlockComponent extends MDComponent {
    private static final int PADDING = 4;
    private static final int GUTTER_PADDING = 4;
    private static final int GUTTER_COLOR = 0x22444444;
    private static final int GUTTER_LINE_COLOR = 0x55333333;
    private static final int LINE_NUMBER_COLOR = 0x99555555;
    private static final int CODE_TEXT_COLOR = 0x00444444;
    private static final int BORDER_COLOR = 0x88333333;
    private static final int BACKGROUND_COLOR = 0x22AAAAAA;
    private final String codeText;

    /**
     * 创建代码块组件。
     *
     * @param text 代码文本（允许包含换行）
     */
    public MDCodeBlockComponent(String text) {
        super(FormattedText.of(text, Style.EMPTY.withColor(CODE_TEXT_COLOR)));
        this.codeText = text;
    }

    /**
     * 渲染代码块主体与行号栏。
     */
    @Override
    public void render(GuiGraphics guiGraphics, Minecraft minecraft, int maxX, int maxY) {
        int blockHeight = this.getHeight(minecraft, maxX, maxY);
        guiGraphics.fill(0, 0, maxX, blockHeight, BACKGROUND_COLOR);
        guiGraphics.renderOutline(0, 0, maxX, blockHeight, BORDER_COLOR);

        String[] lines = this.codeText.split("\\n", -1);
        int gutterWidth = this.getGutterWidth(minecraft, lines.length);
        int contentWidth = Math.max(1, maxX - PADDING * 2 - gutterWidth - GUTTER_PADDING);

        guiGraphics.fill(PADDING, PADDING, PADDING + gutterWidth, Math.max(PADDING + 1, blockHeight - PADDING), GUTTER_COLOR);
        guiGraphics.vLine(PADDING + gutterWidth, PADDING, Math.max(PADDING, blockHeight - PADDING - 1), GUTTER_LINE_COLOR);

        PoseStack pose = guiGraphics.pose();
        pose.pushPose();

        int y = 0;
        int lineNumber = 1;
        for (String line : lines) {
            FormattedText lineText = FormattedText.of(line, Style.EMPTY.withColor(CODE_TEXT_COLOR));
            List<FormattedCharSequence> split = minecraft.font.split(lineText, contentWidth);

            String lineStr = String.valueOf(lineNumber);
            int lineNumX = PADDING + gutterWidth - minecraft.font.width(lineStr) - 1;
            int lineNumY = PADDING + y;
            guiGraphics.drawString(minecraft.font, lineStr, lineNumX, lineNumY, LINE_NUMBER_COLOR, false);

            if (split.isEmpty()) {
                y += minecraft.font.lineHeight;
            } else {
                for (FormattedCharSequence sequence : split) {
                    guiGraphics.drawString(minecraft.font, sequence, PADDING + gutterWidth + GUTTER_PADDING, PADDING + y, 0x000000, false);
                    y += minecraft.font.lineHeight;
                }
            }
            lineNumber++;
        }

        pose.popPose();
    }

    /**
     * 计算代码块在当前宽度下的总高度（含内边距）。
     */
    @Override
    public int getHeight(Minecraft minecraft, int maxX, int maxY) {
        String[] lines = this.codeText.split("\\n", -1);
        int gutterWidth = this.getGutterWidth(minecraft, lines.length);
        int contentWidth = Math.max(1, maxX - PADDING * 2 - gutterWidth - GUTTER_PADDING);
        int lineCount = 0;
        for (String line : lines) {
            int wrapped = minecraft.font.split(FormattedText.of(line, Style.EMPTY.withColor(CODE_TEXT_COLOR)), contentWidth).size();
            lineCount += Math.max(1, wrapped);
        }
        return lineCount * minecraft.font.lineHeight + PADDING * 2;
    }

    /**
     * 根据总行数计算行号栏宽度。
     */
    private int getGutterWidth(Minecraft minecraft, int lineCount) {
        int digits = String.valueOf(Math.max(1, lineCount)).length();
        return minecraft.font.width("0".repeat(Math.max(1, digits))) + 3;
    }
}

