package dev.anvilcraft.resource.ageratum.client.feat.markdown.component;

import com.mojang.blaze3d.vertex.PoseStack;
import dev.anvilcraft.resource.ageratum.client.AgeratumClient;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.FormattedText;
import net.minecraft.network.chat.Style;
import net.minecraft.util.FormattedCharSequence;

import java.util.ArrayList;
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
    private static final Style CODE_TEXT_STYLE = Style.EMPTY.withColor(CODE_TEXT_COLOR);
    private final List<CodeLineInfo> codeLines;

    /**
     * 创建代码块组件。
     *
     * @param text 代码文本（允许包含换行）
     */
    public MDCodeBlockComponent(String text) {
        super(FormattedText.of(text, CODE_TEXT_STYLE));
        String[] lines = text.split("\\n", -1);
        List<CodeLineInfo> cachedLines = new ArrayList<>(lines.length);
        for (String line : lines) {
            int indentation = 0;
            if (line.startsWith(" ")) {
                String trim = line.trim();
                indentation = trim.isEmpty() ? 0 : line.indexOf(trim.charAt(0));
                line = line.substring(indentation);
            } else if (line.startsWith("\t")) {
                String trim = line.trim();
                int i = trim.isEmpty() ? 0 : line.indexOf(trim.charAt(0));
                indentation = i * 4;
                line = line.substring(i);
            }
            cachedLines.add(new CodeLineInfo(indentation, FormattedText.of(line, CODE_TEXT_STYLE)));
        }
        this.codeLines = List.copyOf(cachedLines);
    }

    /**
     * 渲染代码块主体与行号栏。
     */
    @Override
    public void render(GuiGraphics guiGraphics, Minecraft minecraft, int maxX, int maxY, float mouseX, float mouseY) {
        int blockHeight = this.getHeight(minecraft, maxX, maxY);
        guiGraphics.fill(0, 0, maxX, blockHeight, BACKGROUND_COLOR);
        guiGraphics.renderOutline(0, 0, maxX, blockHeight, BORDER_COLOR);

        int gutterWidth = this.getGutterWidth(minecraft, this.codeLines.size());
        int contentWidth = this.getContentWidth(minecraft, maxX);

        if (AgeratumClient.CONFIG.showCodeBlockLineNumbers) {
            guiGraphics.fill(PADDING, PADDING, PADDING + gutterWidth, Math.max(PADDING + 1, blockHeight - PADDING), GUTTER_COLOR);
            guiGraphics.vLine(PADDING + gutterWidth, PADDING, Math.max(PADDING, blockHeight - PADDING - 1), GUTTER_LINE_COLOR);
        }
        PoseStack pose = guiGraphics.pose();
        pose.pushPose();

        int y = 0;
        int lineNumber = 1;
        for (CodeLineInfo lineInfo : this.codeLines) {
            FormattedText lineText = lineInfo.text();
            int offsetX = minecraft.font.width(" ") * lineInfo.indentation();
            int offsetWidth = contentWidth - offsetX;
            List<FormattedCharSequence> split;
            if (AgeratumClient.CONFIG.allowCodeBlockLineContentLineBreaks) {
                split = minecraft.font.split(lineText, offsetWidth);
            } else {
                split = minecraft.font.split(lineText, Integer.MAX_VALUE);
            }

            if (AgeratumClient.CONFIG.showCodeBlockLineNumbers) {
                String lineStr = String.valueOf(lineNumber);
                int lineNumX = PADDING + gutterWidth - minecraft.font.width(lineStr) - 1;
                int lineNumY = PADDING + y;
                guiGraphics.drawString(minecraft.font, lineStr, lineNumX, lineNumY, LINE_NUMBER_COLOR, false);
            }

            if (split.isEmpty()) {
                y += minecraft.font.lineHeight;
            } else {
                int strX = PADDING + gutterWidth + GUTTER_PADDING + offsetX;
                if (!AgeratumClient.CONFIG.showCodeBlockLineNumbers) {
                    strX = PADDING + offsetX;
                }
                for (FormattedCharSequence sequence : split) {
                    guiGraphics.drawString(
                        minecraft.font,
                        sequence,
                        strX,
                        PADDING + y,
                        0x000000,
                        false
                    );
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
        int contentWidth = this.getContentWidth(minecraft, maxX);
        int lineCount = 0;
        for (CodeLineInfo lineInfo : this.codeLines) {
            int offsetX = minecraft.font.width(" ") * lineInfo.indentation();
            int offsetWidth = AgeratumClient.CONFIG.allowCodeBlockLineContentLineBreaks ? contentWidth - offsetX : Integer.MAX_VALUE;
            int wrapped = minecraft.font.split(lineInfo.text, offsetWidth).size();
            lineCount += Math.max(1, wrapped);
        }
        return lineCount * minecraft.font.lineHeight + PADDING * 2;
    }

    private int getContentWidth(Minecraft minecraft, int maxX) {
        if (!AgeratumClient.CONFIG.allowCodeBlockLineContentLineBreaks) {
            return maxX - PADDING * 2;
        }
        return Math.max(1, maxX - PADDING * 2 - this.getGutterWidth(minecraft, this.codeLines.size()) - GUTTER_PADDING);
    }

    /**
     * 根据总行数计算行号栏宽度。
     */
    private int getGutterWidth(Minecraft minecraft, int lineCount) {
        if (!AgeratumClient.CONFIG.showCodeBlockLineNumbers) {
            return 0;
        }
        int digits = String.valueOf(Math.max(1, lineCount)).length();
        return minecraft.font.width("0".repeat(digits)) + 3;
    }

    private record CodeLineInfo(int indentation, FormattedText text) {

    }
}

