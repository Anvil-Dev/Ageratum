package dev.anvilcraft.resource.ageratum.client.feat.markdown.component;

import dev.anvilcraft.resource.ageratum.client.AgeratumClient;
import dev.anvilcraft.resource.ageratum.client.feat.markdown.MDRenderContext;
import lombok.extern.slf4j.Slf4j;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.FormattedText;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.util.FormattedCharSequence;
import org.apache.commons.lang3.StringEscapeUtils;
import org.codelibs.jhighlight.renderer.Renderer;
import org.codelibs.jhighlight.renderer.XhtmlRendererFactory;
import org.joml.Matrix3x2fStack;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.nodes.Node;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 代码块组件。
 *
 * <p>负责渲染带背景、边框、行号栏的多行代码文本，
 * 并在给定宽度下自动换行。</p>
 */
@Slf4j
public class MDCodeBlockComponent extends MDComponent {
    private static final int PADDING = 4;
    private static final int GUTTER_PADDING = 4;
    private static final int GUTTER_COLOR = 0x22444444;
    private static final int GUTTER_LINE_COLOR = 0x55333333;
    private static final int LINE_NUMBER_COLOR = 0x99555555;
    private static final int CODE_KEYWORD_COLOR = 0xC792EA;
    private static final int CODE_TYPE_COLOR = 0xFFCB6B;
    private static final int CODE_LITERAL_COLOR = 0xF78C6C;
    private static final int CODE_COMMENT_COLOR = 0x939393;
    private static final int CODE_OPERATOR_COLOR = 0x007C1F;
    private static final int CODE_SEPARATOR_COLOR = 0x0021FF;
    private static final int CODE_TEXT_COLOR = 0x444444;
    private static final int BORDER_COLOR = 0x88333333;
    private static final int BACKGROUND_COLOR = 0x22AAAAAA;
    private static final Style CODE_TEXT_STYLE = Style.EMPTY.withColor(CODE_TEXT_COLOR);
    private final List<CodeLineInfo> codeLines;
    private static final String HIGHLIGHT_REGEX = "\\{(\\d+(-\\d+)?,?)+}";
    private static final Map<String, Integer> CODE_SPAN_COLOR = new HashMap<>();

    static {
        CODE_SPAN_COLOR.put("java_keyword", CODE_KEYWORD_COLOR);
        CODE_SPAN_COLOR.put("java_javadoc_tag", CODE_KEYWORD_COLOR);
        CODE_SPAN_COLOR.put("java_plain", CODE_TEXT_COLOR);
        CODE_SPAN_COLOR.put("java_type", CODE_TYPE_COLOR);
        CODE_SPAN_COLOR.put("java_literal", CODE_LITERAL_COLOR);
        CODE_SPAN_COLOR.put("java_javadoc_comment", CODE_COMMENT_COLOR);
        CODE_SPAN_COLOR.put("java_operator", CODE_OPERATOR_COLOR);
        CODE_SPAN_COLOR.put("java_separator", CODE_SEPARATOR_COLOR);
        CODE_SPAN_COLOR.put("java_comment", CODE_COMMENT_COLOR);
        CODE_SPAN_COLOR.put("cpp_plain", CODE_TEXT_COLOR);
        CODE_SPAN_COLOR.put("cpp_doxygen_comment", CODE_COMMENT_COLOR);
        CODE_SPAN_COLOR.put("cpp_comment", CODE_COMMENT_COLOR);
        CODE_SPAN_COLOR.put("cpp_operator", CODE_OPERATOR_COLOR);
        CODE_SPAN_COLOR.put("cpp_doxygen_tag", CODE_KEYWORD_COLOR);
        CODE_SPAN_COLOR.put("cpp_literal", CODE_LITERAL_COLOR);
        CODE_SPAN_COLOR.put("cpp_preproc", 0x800080);
        CODE_SPAN_COLOR.put("cpp_keyword", CODE_KEYWORD_COLOR);
        CODE_SPAN_COLOR.put("cpp_separator", CODE_SEPARATOR_COLOR);
        CODE_SPAN_COLOR.put("cpp_type", CODE_TYPE_COLOR);
        CODE_SPAN_COLOR.put("xml_comment", CODE_COMMENT_COLOR);
        CODE_SPAN_COLOR.put("xml_tag_name", 0x0037ff);
        CODE_SPAN_COLOR.put("xml_processing_instruction", CODE_TEXT_COLOR);
        CODE_SPAN_COLOR.put("xml_rife_name", 0x0000c4);
        CODE_SPAN_COLOR.put("xml_plain", CODE_TEXT_COLOR);
        CODE_SPAN_COLOR.put("xml_tag_symbols", 0x003bff);
        CODE_SPAN_COLOR.put("xml_attribute_value", CODE_LITERAL_COLOR);
        CODE_SPAN_COLOR.put("xml_rife_tag", CODE_TEXT_COLOR);
        CODE_SPAN_COLOR.put("xml_char_data", CODE_TEXT_COLOR);
        CODE_SPAN_COLOR.put("xml_attribute_name", CODE_TEXT_COLOR);
    }

    /**
     * 创建代码块组件。
     *
     * @param text 代码文本（允许包含换行）
     */
    public MDCodeBlockComponent(String text) {
        this(text, "");
    }

    public MDCodeBlockComponent(String text, String extra) {
        super(FormattedText.of(text, CODE_TEXT_STYLE));
        this.codeLines = MDCodeBlockComponent.parseCode(text, extra);
    }

    // (\d+(-\d+)?,?)+
    // 单行：例如 5、3、10
    // 多行：例如 5-8、3-10、10-17
    // 多个单行：例如 4,7,9
    // 多行与单行：例如 4,7-13,16,23-27,40
    private static List<Integer> parseHighlightLines(String highlight) {
        List<Integer> highlightLines = new ArrayList<>();
        for (String string : highlight.split(",")) {
            if (string.matches("\\d+-\\d+")) {
                String[] range = string.split("-");
                int start = Integer.parseInt(range[0]);
                int end = Integer.parseInt(range[1]);
                if (end < start) {
                    log.warn("Invalid highlight line range: {}", string);
                    continue;
                }
                for (int i = start; i <= end; i++) {
                    highlightLines.add(i);
                }
            } else {
                try {
                    highlightLines.add(Integer.parseInt(string));
                } catch (NumberFormatException e) {
                    log.warn("Invalid highlight line: {}", string);
                }
            }
        }
        highlightLines.sort(Integer::compareTo);
        return highlightLines;
    }

    private static List<CodeLineInfo> parseCode(String text, String extra) {
        Pattern compile = Pattern.compile(HIGHLIGHT_REGEX);
        Matcher matcher = compile.matcher(extra);
        String highlight = "";
        if (matcher.find()) {
            highlight = matcher.group();
        }
        String lang = extra.substring(0, highlight.isEmpty() ? extra.length() : extra.indexOf(highlight)).trim();
        List<Integer> highlightLines = List.of();
        if (!highlight.isEmpty()) {
            highlight = highlight.substring(1, highlight.length() - 1);
            highlightLines = MDCodeBlockComponent.parseHighlightLines(highlight);
        }
        if (XhtmlRendererFactory.getSupportedTypes().contains(lang)) {
            Renderer renderer = XhtmlRendererFactory.getRenderer(lang);
            try {
                String highlighted = renderer.highlight("", text, "UTF-8", false);
                Document doc = Jsoup.parse(highlighted);
                List<CodeLineInfo> cachedLines = new ArrayList<>();
                for (Element code : doc.select("code")) {
                    MutableComponent codeComponent = Component.empty();
                    boolean start = true;
                    int indentation = 0;
                    for (Element child : code.children()) {
                        if (child.tag().getName().equals("br")) {
                            cachedLines.add(new CodeLineInfo(indentation, codeComponent, highlightLines.contains(cachedLines.size() + 1)));
                            codeComponent = Component.empty();
                            indentation = 0;
                            start = true;
                        }
                        for (Node node : child.childNodesCopy()) {
                            String text1 = node.toString().trim();
                            if (start && text1.startsWith("&nbsp;")) {
                                Matcher matcher1 = Pattern.compile("^(&nbsp;)+").matcher(text1);
                                if (matcher.find()) {
                                    String indentationStr = matcher1.group();
                                    indentation = indentationStr.replace("&nbsp;", " ").length();
                                    text1 = text1.substring(indentationStr.length());
                                }
                            }
                            start = false;
                            //noinspection deprecation
                            text1 = StringEscapeUtils.unescapeHtml4(text1);
                            if (text1.trim().isEmpty()) {
                                continue;
                            }
                            codeComponent.append(
                                Component.literal(text1)
                                    .withStyle(Style.EMPTY.withColor(CODE_SPAN_COLOR.getOrDefault(
                                        child.attr("class"),
                                        CODE_TEXT_COLOR
                                    )))
                            );
                        }
                    }
                }
                return cachedLines;
            } catch (Exception e) {
                log.error(e.getMessage(), e);
            }
        }
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
            cachedLines.add(new CodeLineInfo(indentation, FormattedText.of(line, CODE_TEXT_STYLE), false));
        }
        return List.copyOf(cachedLines);
    }

    /**
     * 渲染代码块主体与行号栏。
     */
    @Override
    public void extractRenderState(MDRenderContext context) {
        Minecraft minecraft = context.minecraft();
        int maxX = context.maxX();
        int maxY = context.maxY();
        GuiGraphicsExtractor GuiGraphicsExtractor = context.graphics();
        int blockHeight = this.getHeight(minecraft, maxX, maxY);
        GuiGraphicsExtractor.fill(0, 0, maxX, blockHeight, BACKGROUND_COLOR);
        GuiGraphicsExtractor.renderOutline(0, 0, maxX, blockHeight, BORDER_COLOR);
        int gutterWidth = this.getGutterWidth(minecraft, this.codeLines.size());
        int contentWidth = this.getContentWidth(minecraft, maxX);

        if (AgeratumClient.CONFIG.showCodeBlockLineNumbers) {
            GuiGraphicsExtractor.fill(PADDING, PADDING, PADDING + gutterWidth, Math.max(PADDING + 1, blockHeight - PADDING), GUTTER_COLOR);
            GuiGraphicsExtractor.vLine(PADDING + gutterWidth, PADDING, Math.max(PADDING, blockHeight - PADDING - 1), GUTTER_LINE_COLOR);
        }
        Matrix3x2fStack pose = GuiGraphicsExtractor.pose()();
        pose.pushMatrix();
        context.enableScissor(1, 1, maxX - 1, blockHeight - 1);
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

            if (lineInfo.highlight()) {
                int highlightColor = 0x29657585;
                if (AgeratumClient.CONFIG.showCodeBlockLineNumbers) {
                    GuiGraphicsExtractor.fill(
                        PADDING + gutterWidth,
                        PADDING + y,
                        maxX,
                        PADDING + y + minecraft.font.lineHeight * split.size(),
                        highlightColor
                    );
                } else {
                    GuiGraphicsExtractor.fill(
                        PADDING,
                        PADDING + y,
                        maxX,
                        PADDING + y + minecraft.font.lineHeight * split.size(),
                        highlightColor
                    );
                }
            }

            if (AgeratumClient.CONFIG.showCodeBlockLineNumbers) {
                String lineStr = String.valueOf(lineNumber);
                int lineNumX = PADDING + gutterWidth - minecraft.font.width(lineStr) - 1;
                int lineNumY = PADDING + y;
                GuiGraphicsExtractor.text(minecraft.font, lineStr, lineNumX, lineNumY, LINE_NUMBER_COLOR, false);
            }

            if (split.isEmpty()) {
                y += minecraft.font.lineHeight;
            } else {
                int strX = PADDING + gutterWidth + GUTTER_PADDING + offsetX;
                if (!AgeratumClient.CONFIG.showCodeBlockLineNumbers) {
                    strX = PADDING + offsetX;
                }
                for (FormattedCharSequence sequence : split) {
                    GuiGraphicsExtractor.text(
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
        context.disableScissor();
        pose.popMatrix();
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

    private record CodeLineInfo(int indentation, FormattedText text, boolean highlight) {

    }
}

