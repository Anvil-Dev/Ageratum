package dev.anvilcraft.resource.ageratum.client.feat.markdown.component;

import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.serialization.JsonOps;
import lombok.Getter;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.FormattedText;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.network.chat.Style;
import net.minecraft.util.FormattedCharSequence;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BiFunction;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.annotation.Nullable;

/**
 * Markdown 渲染组件基类。
 *
 * <p>该类封装了通用文本渲染能力，以及内联 Markdown 语法（粗体、斜体、删除线、链接、
 * 自动链接、代码跨度）与自定义样式标签（如 {@code <color=#xxxxxx>}）的解析逻辑。</p>
 */
@Getter
public abstract class MDComponent {
    private static final Pattern IMAGE_PATTERN = Pattern.compile("!\\[([^]]*)]\\(([^)]+)\\)");
    private static final Pattern LINK_PATTERN = Pattern.compile("\\[([^]]+)]\\(([^)]+)\\)");
    private static final Pattern STRIKE_PATTERN = Pattern.compile("~~([^~\\n]+)~~");
    private static final Pattern BOLD_ASTERISK_PATTERN = Pattern.compile("\\*\\*([^*\\n]+)\\*\\*");
    private static final Pattern BOLD_UNDERSCORE_PATTERN = Pattern.compile("(?<![A-Za-z0-9_])__([^_\\n]+)__(?![A-Za-z0-9_])");
    private static final Pattern ITALIC_ASTERISK_PATTERN = Pattern.compile("(?<!\\*)\\*([^*\\n]+)\\*(?!\\*)");
    private static final Pattern ITALIC_UNDERSCORE_PATTERN = Pattern.compile("(?<![A-Za-z0-9_])_([^_\\n]+)_(?![A-Za-z0-9_])");
    private static final Pattern AUTOLINK_URL_PATTERN = Pattern.compile("<(https?://[^>\\s]+)>");
    private static final Pattern AUTOLINK_EMAIL_PATTERN = Pattern.compile("<([a-zA-Z0-9._%+\\-]+@[a-zA-Z0-9.\\-]+\\.[a-zA-Z]{2,})>");
    private static final Pattern HOVER_TAG_PATTERN = Pattern.compile("<hover\\b([^>]*)>", Pattern.CASE_INSENSITIVE);
    private static final Pattern CLICK_TAG_PATTERN = Pattern.compile("<click\\b([^>]*)>", Pattern.CASE_INSENSITIVE);
    private static final Pattern TAG_ATTRIBUTE_PATTERN = Pattern.compile("([a-zA-Z_:][-a-zA-Z0-9_:.]*)\\s*=\\s*\"([^\"]*)\"");
    private static final String COMMONMARK_ESCAPABLE_PUNCTUATION = "!\"#$%&'()*+,-./:;<=>?@[\\]^_`{|}~";
    private static final String ESCAPE_TOKEN_PREFIX = "%%MDESC";
    private static final String ESCAPE_TOKEN_SUFFIX = "%%";
    private static final int CODE_SPAN_COLOR = 0x7a4f2f;
    private static final int LINK_COLOR = 0x66ccff;
    /**
     * -- GETTER --
     * 获取组件的 FormattedText。
     *
     * @return 该组件的格式化文本
     */
    @SuppressWarnings("JavadocDeclaration")
    protected final FormattedText text;
    private static final List<InlineStyleParserHolder> INLINE_STYLE_PARSER_HOLDERS = new ArrayList<>();
    private static int nextInlineStyleParserOrder = 0;

    static {
        registerBaseStyleParser();
    }

    /**
     * 使用原始文本创建组件，文本会按默认规则进行 Markdown 内联解析。
     */
    public MDComponent(String text) {
        this(MDComponent.textFormat(text));
    }

    /**
     * 使用已格式化文本创建组件。
     */
    public MDComponent(FormattedText text) {
        this.text = text;
    }

    /**
     * 在给定区域内渲染组件内容。
     */
    public void render(GuiGraphics guiGraphics, Minecraft minecraft, int maxX, int maxY) {
        List<FormattedCharSequence> split = minecraft.font.split(this.text, maxX);
        PoseStack pose = guiGraphics.pose();
        for (FormattedCharSequence sequence : split) {
            if (maxY < minecraft.font.lineHeight) return;
            guiGraphics.drawString(minecraft.font, sequence, 0, 0, 0x000000, false);
            pose.translate(0, minecraft.font.lineHeight, 0);
            maxY -= minecraft.font.lineHeight;
        }
    }

    /**
     * 计算组件在指定宽度下的渲染高度。
     */
    public int getHeight(Minecraft minecraft, int maxX, int maxY) {
        return minecraft.font.wordWrapHeight(this.text, maxX);
    }

    /**
     * 获取组件内部指定坐标对应的文本样式。
     *
     * <p>默认实现按普通文本块处理；复杂布局组件可覆盖以匹配其真实渲染坐标。</p>
     */
    @Nullable
    public Style getStyleAtPosition(Minecraft minecraft, double mouseX, double mouseY, int maxX) {
        return this.getStyleAtFormattedTextPosition(minecraft, this.text, mouseX, mouseY, maxX);
    }

    /**
     * 根据格式化文本在指定宽度下的换行结果获取命中的文本样式。
     */
    @Nullable
    protected final Style getStyleAtFormattedTextPosition(
        Minecraft minecraft, FormattedText text, double mouseX, double mouseY, int maxX
    ) {
        if (mouseX < 0 || mouseY < 0 || maxX <= 0) {
            return null;
        }

        List<FormattedCharSequence> lines = minecraft.font.split(text, maxX);
        int lineIndex = (int) Math.floor(mouseY / minecraft.font.lineHeight);
        if (lineIndex < 0 || lineIndex >= lines.size()) {
            return null;
        }

        FormattedCharSequence line = lines.get(lineIndex);
        return minecraft.font.getSplitter().componentStyleAtWidth(line, (int) Math.floor(mouseX));
    }

    /**
     * 使用默认样式解析一段 Markdown 内联文本。
     */
    public static FormattedText textFormat(String text) {
        return textFormat(text, Style.EMPTY);
    }

    /**
     * 基于指定基础样式解析 Markdown 内联文本。
     */
    public static FormattedText textFormat(String text, Style baseStyle) {
        EscapeContext escapeContext = protectMarkdownEscapes(text);
        return parseMixedTextWithCodeSpan(escapeContext.text(), escapeContext, baseStyle);
    }

    /**
     * 解析混合文本中的代码跨度与普通 Markdown 片段。
     */
    private static FormattedText parseMixedTextWithCodeSpan(String text, EscapeContext escapeContext, Style baseStyle) {
        List<FormattedText> parts = new ArrayList<>();
        int pos = 0;
        while (pos < text.length()) {
            int tickStart = text.indexOf('`', pos);
            if (tickStart < 0) {
                appendMarkdownPart(parts, text.substring(pos), baseStyle, escapeContext);
                break;
            }

            // Count the opening backtick run
            int tickEnd = tickStart + 1;
            while (tickEnd < text.length() && text.charAt(tickEnd) == '`') tickEnd++;
            int tickCount = tickEnd - tickStart;

            // Find matching closing run of the same length
            int closeStart = findMatchingBackticks(text, tickEnd, tickCount);
            if (closeStart < 0) {
                // No match: emit the backticks literally and continue
                if (tickStart > pos) appendMarkdownPart(parts, text.substring(pos, tickStart), baseStyle, escapeContext);
                parts.add(FormattedText.of(text.substring(tickStart, tickEnd), baseStyle));
                pos = tickEnd;
                continue;
            }

            if (tickStart > pos) appendMarkdownPart(parts, text.substring(pos, tickStart), baseStyle, escapeContext);

            // CommonMark: strip a single leading/trailing space when both present and content is not blank
            String codeSpan = restoreEscapedLiterals(text.substring(tickEnd, closeStart), escapeContext);
            if (codeSpan.length() >= 2 && codeSpan.startsWith(" ") && codeSpan.endsWith(" ") && !codeSpan.isBlank()) {
                codeSpan = codeSpan.substring(1, codeSpan.length() - 1);
            }
            parts.add(FormattedText.of(codeSpan, baseStyle.withColor(CODE_SPAN_COLOR)));
            pos = closeStart + tickCount;
        }

        if (parts.isEmpty()) return FormattedText.EMPTY;
        if (parts.size() == 1) return parts.getFirst();
        return FormattedText.composite(parts);
    }

    /**
     * 在文本中查找与开头反引号数量一致的闭合反引号区间。
     */
    private static int findMatchingBackticks(String text, int startPos, int tickCount) {
        int pos = startPos;
        while (pos < text.length()) {
            int tick = text.indexOf('`', pos);
            if (tick < 0) return -1;
            int end = tick + 1;
            while (end < text.length() && text.charAt(end) == '`') end++;
            if (end - tick == tickCount) return tick;
            pos = end;
        }
        return -1;
    }

    /**
     * 追加一段普通 Markdown 文本片段到结果列表。
     */
    private static void appendMarkdownPart(List<FormattedText> parts, String text, Style style, EscapeContext escapeContext) {
        if (text.isEmpty()) {
            return;
        }
        parts.add(parseMarkdownInlineText(text, style, escapeContext));
    }

    /**
     * 解析不含代码跨度的 Markdown 内联语法。
     */
    private static FormattedText parseMarkdownInlineText(String text, Style parentStyle, EscapeContext escapeContext) {
        List<FormattedText> parts = new ArrayList<>();
        int pos = 0;

        while (pos < text.length()) {
            MarkdownTokenMatch next = findNextMarkdownToken(text, pos);
            if (next == null) {
                parts.add(parseStyledText(restoreEscapedLiterals(text.substring(pos), escapeContext), parentStyle));
                break;
            }

            if (next.start > pos) {
                parts.add(parseStyledText(restoreEscapedLiterals(text.substring(pos, next.start), escapeContext), parentStyle));
            }

            switch (next.type) {
                case IMAGE ->
                    parts.add(FormattedText.of("[image: " + restoreEscapedLiterals(next.content, escapeContext) + "]", parentStyle));
                case LINK -> {
                    String target = extractMarkdownLinkTarget(text.substring(next.start, next.end));
                    Style linkStyle = createLinkStyle(parentStyle, target);
                    parts.add(parseMarkdownInlineText(next.content, linkStyle, escapeContext));
                }
                case STRIKE -> parts.add(parseMarkdownInlineText(next.content, parentStyle.withStrikethrough(true), escapeContext));
                case BOLD -> parts.add(parseMarkdownInlineText(next.content, parentStyle.withBold(true), escapeContext));
                case ITALIC -> parts.add(parseMarkdownInlineText(next.content, parentStyle.withItalic(true), escapeContext));
                case AUTOLINK ->
                    parts.add(FormattedText.of(next.content, createLinkStyle(parentStyle, normalizeAutolinkTarget(next.content))));
            }

            pos = next.end;
        }

        if (parts.isEmpty()) {
            return FormattedText.EMPTY;
        }
        if (parts.size() == 1) {
            return parts.getFirst();
        }
        return FormattedText.composite(parts);
    }

    private static @Nullable MarkdownTokenMatch findNextMarkdownToken(String text, int pos) {
        MarkdownTokenMatch earliest = null;
        earliest = chooseEarlier(earliest, findToken(IMAGE_PATTERN, text, pos, MarkdownTokenType.IMAGE));
        earliest = chooseEarlier(earliest, findToken(LINK_PATTERN, text, pos, MarkdownTokenType.LINK));
        earliest = chooseEarlier(earliest, findToken(STRIKE_PATTERN, text, pos, MarkdownTokenType.STRIKE));
        earliest = chooseEarlier(earliest, findToken(BOLD_ASTERISK_PATTERN, text, pos, MarkdownTokenType.BOLD));
        earliest = chooseEarlier(earliest, findToken(BOLD_UNDERSCORE_PATTERN, text, pos, MarkdownTokenType.BOLD));
        earliest = chooseEarlier(earliest, findToken(ITALIC_ASTERISK_PATTERN, text, pos, MarkdownTokenType.ITALIC));
        earliest = chooseEarlier(earliest, findToken(ITALIC_UNDERSCORE_PATTERN, text, pos, MarkdownTokenType.ITALIC));
        earliest = chooseEarlier(earliest, findToken(AUTOLINK_URL_PATTERN, text, pos, MarkdownTokenType.AUTOLINK));
        earliest = chooseEarlier(earliest, findToken(AUTOLINK_EMAIL_PATTERN, text, pos, MarkdownTokenType.AUTOLINK));
        return earliest;
    }

    private static @Nullable MarkdownTokenMatch findToken(Pattern pattern, String text, int pos, MarkdownTokenType type) {
        Matcher matcher = pattern.matcher(text);
        if (!matcher.find(pos)) {
            return null;
        }
        return new MarkdownTokenMatch(type, matcher.start(), matcher.end(), matcher.group(1));
    }

    private static @Nullable MarkdownTokenMatch chooseEarlier(
        @Nullable MarkdownTokenMatch current,
        @Nullable MarkdownTokenMatch candidate
    ) {
        if (candidate == null) {
            return current;
        }
        if (current == null || candidate.start < current.start) {
            return candidate;
        }
        return current;
    }

    /**
     * 为链接文本构造带点击事件的样式。
     */
    private static Style createLinkStyle(Style parentStyle, @Nullable String target) {
        Style style = parentStyle.withUnderlined(true).withColor(LINK_COLOR);
        if (target == null || target.isBlank()) {
            return style;
        }
        return style.withClickEvent(new ClickEvent(ClickEvent.Action.OPEN_URL, target));
    }

    /**
     * 从标准 Markdown 链接文本中提取跳转目标。
     */
    private static @Nullable String extractMarkdownLinkTarget(String rawLinkText) {
        Matcher matcher = LINK_PATTERN.matcher(rawLinkText);
        if (!matcher.matches()) {
            return null;
        }
        return matcher.group(2);
    }

    /**
     * 标准化自动链接目标；邮箱自动链接会补成 mailto: 前缀。
     */
    private static String normalizeAutolinkTarget(String content) {
        if (content.startsWith("http://") || content.startsWith("https://") || content.startsWith("mailto:")) {
            return content;
        }
        if (content.contains("@")) {
            return "mailto:" + content;
        }
        return content;
    }

    /**
     * 保护反斜杠转义字符，避免在后续内联解析阶段被误处理。
     */
    private static EscapeContext protectMarkdownEscapes(String text) {
        StringBuilder builder = new StringBuilder();
        List<EscapedLiteral> escapedLiterals = new ArrayList<>();
        for (int i = 0; i < text.length(); i++) {
            char ch = text.charAt(i);
            if (ch == '\\' && i + 1 < text.length() && isMarkdownEscapable(text.charAt(i + 1))) {
                char escapedChar = text.charAt(i + 1);
                String token = ESCAPE_TOKEN_PREFIX + escapedLiterals.size() + ESCAPE_TOKEN_SUFFIX;
                escapedLiterals.add(new EscapedLiteral(token, String.valueOf(escapedChar)));
                builder.append(token);
                i++;
                continue;
            }
            builder.append(ch);
        }
        return new EscapeContext(builder.toString(), escapedLiterals);
    }

    private static boolean isMarkdownEscapable(char ch) {
        return ch < 128 && COMMONMARK_ESCAPABLE_PUNCTUATION.indexOf(ch) >= 0;
    }

    /**
     * 恢复此前保护的转义字面量。
     */
    private static String restoreEscapedLiterals(String text, EscapeContext escapeContext) {
        String restored = text;
        for (EscapedLiteral escapedLiteral : escapeContext.escapedLiterals()) {
            restored = restored.replace(escapedLiteral.token(), escapedLiteral.value());
        }
        return restored;
    }

    /**
     * 注册一个自定义内联样式解析器。
     *
     * @param priority 优先级；数值越小越先匹配
     * @param parser   样式解析器
     */
    public static synchronized void registerStyleParser(int priority, InlineStyleParser parser) {
        INLINE_STYLE_PARSER_HOLDERS.add(new InlineStyleParserHolder(priority, nextInlineStyleParserOrder++, parser));
        INLINE_STYLE_PARSER_HOLDERS.sort(null);
    }

    /**
     * 注册基于起止标签的自定义内联样式解析器。
     */
    public static void registerStyleParser(
        int priority,
        Pattern openTagPattern,
        String closeTag,
        BiFunction<Style, Matcher, Style> styleFactory
    ) {
        registerStyleParser(
            priority, (text, pos) -> {
                Matcher matcher = openTagPattern.matcher(text);
                if (!matcher.find(pos)) {
                    return null;
                }
                return InlineStyleMatch.of(openTagPattern, matcher, closeTag, styleFactory);
            }
        );
    }

    private static void registerBaseStyleParser() {
        registerStyleParser(
            0,
            Pattern.compile("<color=#([0-9a-fA-F]{6})>"),
            "</color>",
            (parentStyle, matcher) -> parentStyle.withColor(Integer.parseInt(matcher.group(1), 16))
        );
        registerStyleParser(0, Pattern.compile("<o>"), "</o>", (parentStyle, matcher) -> parentStyle.withObfuscated(true));

        // 注册 hover 事件支持
        registerStyleParser(
            0,
            HOVER_TAG_PATTERN,
            "</hover>",
            (parentStyle, matcher) -> {
                String rawAttributes = matcher.group(1);
                String hoverType = getTagAttribute(rawAttributes, "type");
                String hoverData = getTagAttribute(rawAttributes, "data");
                if (hoverType == null || hoverData == null) {
                    return parentStyle;
                }
                try {
                    if ("SHOW_TEXT".equalsIgnoreCase(hoverType)) {
                        return parentStyle.withHoverEvent(new HoverEvent(
                            HoverEvent.Action.SHOW_TEXT,
                            Component.literal(hoverData)
                        ));
                    } else if ("SHOW_ITEM".equalsIgnoreCase(hoverType)) {
                        return parentStyle.withHoverEvent(new HoverEvent(
                            HoverEvent.Action.SHOW_ITEM,
                            HoverEvent.ItemStackInfo.CODEC.decode(
                                JsonOps.INSTANCE,
                                new GsonBuilder().create().fromJson(hoverData, JsonElement.class)
                            ).getOrThrow().getFirst()
                        ));
                    } else if ("SHOW_ENTITY".equalsIgnoreCase(hoverType)) {
                        return parentStyle.withHoverEvent(new HoverEvent(
                            HoverEvent.Action.SHOW_ENTITY,
                            HoverEvent.EntityTooltipInfo.CODEC.decode(
                                JsonOps.INSTANCE,
                                new GsonBuilder().create().fromJson(hoverData, JsonElement.class)
                            ).getOrThrow().getFirst()
                        ));
                    }
                } catch (Exception e) {
                    // 如果处理失败，保持原样式
                }
                return parentStyle;
            }
        );

        // 注册 click 事件支持
        registerStyleParser(
            0,
            CLICK_TAG_PATTERN,
            "</click>",
            (parentStyle, matcher) -> {
                String rawAttributes = matcher.group(1);
                String clickType = getTagAttribute(rawAttributes, "type");
                String clickData = getTagAttribute(rawAttributes, "data");
                if (clickType == null || clickData == null) {
                    return parentStyle;
                }
                try {
                    if ("OPEN_URL".equalsIgnoreCase(clickType)) {
                        return parentStyle.withClickEvent(new ClickEvent(
                            ClickEvent.Action.OPEN_URL,
                            clickData
                        ));
                    } else if ("COPY_TO_CLIPBOARD".equalsIgnoreCase(clickType)) {
                        return parentStyle.withClickEvent(new ClickEvent(
                            ClickEvent.Action.COPY_TO_CLIPBOARD,
                            clickData
                        ));
                    } else if ("RUN_COMMAND".equalsIgnoreCase(clickType)) {
                        return parentStyle.withClickEvent(new ClickEvent(
                            ClickEvent.Action.RUN_COMMAND,
                            clickData
                        ));
                    } else if ("OPEN_FILE".equalsIgnoreCase(clickType)) {
                        return parentStyle.withClickEvent(new ClickEvent(
                            ClickEvent.Action.OPEN_FILE,
                            clickData
                        ));
                    }
                } catch (Exception e) {
                    // 如果处理失败，保持原样式
                }
                return parentStyle;
            }
        );
    }

    /**
     * 从标签属性文本中提取指定属性值。
     */
    private static @Nullable String getTagAttribute(String rawAttributes, String attributeName) {
        Matcher matcher = TAG_ATTRIBUTE_PATTERN.matcher(rawAttributes);
        while (matcher.find()) {
            if (attributeName.equalsIgnoreCase(matcher.group(1))) {
                return matcher.group(2);
            }
        }
        return null;
    }

    /**
     * 解析并应用自定义样式标签文本。
     */
    public static FormattedText parseStyledText(String text, Style parentStyle) {
        List<FormattedText> parts = new ArrayList<>();
        int pos = 0;

        while (pos < text.length()) {
            // 查找最近的标签
            ParserMatch nextTag = findNextTag(text, pos);

            if (nextTag != null) {
                int tagStart = nextTag.start();

                // 添加标签前的普通文本
                if (tagStart > pos) {
                    String plainText = text.substring(pos, tagStart);
                    parts.add(FormattedText.of(plainText, parentStyle));
                }

                // 找到对应的结束标签（考虑嵌套）
                int contentStart = nextTag.end();
                int closeTagIndex = findMatchingCloseTag(text, contentStart, nextTag.match());

                if (closeTagIndex != -1) {
                    // 递归解析内部内容
                    String innerContent = text.substring(contentStart, closeTagIndex);
                    FormattedText innerText = parseStyledText(innerContent, nextTag.match().applyStyle(parentStyle));
                    parts.add(innerText);

                    pos = closeTagIndex + nextTag.match().closeTag().length();
                } else {
                    // 没有找到匹配的结束标签，将剩余内容作为普通文本
                    parts.add(FormattedText.of(text.substring(pos), parentStyle));
                    pos = text.length();
                }
            } else {
                // 没有更多的标签，添加剩余文本
                parts.add(FormattedText.of(text.substring(pos), parentStyle));
                pos = text.length();
            }
        }

        if (parts.isEmpty()) {
            return FormattedText.EMPTY;
        } else if (parts.size() == 1) {
            return parts.getFirst();
        } else {
            return FormattedText.composite(parts);
        }
    }

    /**
     * 自定义内联样式解析接口。
     */
    public @FunctionalInterface interface InlineStyleParser {
        @Nullable
        InlineStyleMatch parse(String text, int pos);
    }

    /**
     * 一次内联样式匹配结果，保存起止标签与样式生成逻辑。
     */
    public static final class InlineStyleMatch {
        private final Pattern openTagPattern;
        private final Matcher matcher;
        private final String closeTag;
        private final BiFunction<Style, Matcher, Style> styleFactory;

        private InlineStyleMatch(
            Pattern openTagPattern,
            Matcher matcher,
            String closeTag,
            BiFunction<Style, Matcher, Style> styleFactory
        ) {
            this.openTagPattern = openTagPattern;
            this.matcher = matcher;
            this.closeTag = closeTag;
            this.styleFactory = styleFactory;
        }

        public static InlineStyleMatch of(
            Pattern openTagPattern,
            Matcher matcher,
            String closeTag,
            BiFunction<Style, Matcher, Style> styleFactory
        ) {
            return new InlineStyleMatch(openTagPattern, matcher, closeTag, styleFactory);
        }

        public int start() {
            return this.matcher.start();
        }

        public int end() {
            return this.matcher.end();
        }

        public Pattern openTagPattern() {
            return this.openTagPattern;
        }

        public String closeTag() {
            return this.closeTag;
        }

        public Style applyStyle(Style parentStyle) {
            return this.styleFactory.apply(parentStyle, this.matcher);
        }
    }

    private record ParserMatch(InlineStyleParserHolder holder, InlineStyleMatch match) {
        private int start() {
            return this.match.start();
        }

        private int end() {
            return this.match.end();
        }
    }

    private enum MarkdownTokenType {
        IMAGE,
        LINK,
        STRIKE,
        BOLD,
        ITALIC,
        AUTOLINK
    }

    private record MarkdownTokenMatch(MarkdownTokenType type, int start, int end, String content) {
    }

    private record EscapedLiteral(String token, String value) {
    }

    private record EscapeContext(String text, List<EscapedLiteral> escapedLiterals) {
    }

    private record InlineStyleParserHolder(int priority, int order, InlineStyleParser parser)
        implements Comparable<InlineStyleParserHolder> {
        @Override
        public int compareTo(InlineStyleParserHolder holder) {
            int priorityCompare = Integer.compare(this.priority(), holder.priority());
            if (priorityCompare != 0) {
                return priorityCompare;
            }
            return Integer.compare(this.order(), holder.order());
        }
    }

    private static @Nullable Matcher findNextTag(Pattern pattern, String text, int pos) {
        Matcher matcher = pattern.matcher(text);
        if (matcher.find(pos)) {
            return matcher;
        }
        return null;
    }

    private static @Nullable ParserMatch findNextTag(String text, int pos) {
        ParserMatch earliest = null;
        for (InlineStyleParserHolder parserHolder : INLINE_STYLE_PARSER_HOLDERS) {
            InlineStyleMatch match = parserHolder.parser().parse(text, pos);
            if (match == null) {
                continue;
            }

            if (earliest == null
                || match.start() < earliest.start()
                || (match.start() == earliest.start() && parserHolder.compareTo(earliest.holder()) < 0)) {
                earliest = new ParserMatch(parserHolder, match);
            }
        }
        return earliest;
    }

    /**
     * 查找与开始标签匹配的结束标签位置（支持同标签嵌套）。
     */
    private static int findMatchingCloseTag(String text, int startPos, InlineStyleMatch match) {
        int depth = 1;
        int pos = startPos;

        while (pos < text.length() && depth > 0) {
            Matcher nextOpenMatcher = findNextTag(match.openTagPattern(), text, pos);
            int nextOpen = nextOpenMatcher == null ? -1 : nextOpenMatcher.start();
            int nextClose = text.indexOf(match.closeTag(), pos);

            if (nextClose == -1) {
                // 没有找到结束标签
                return -1;
            }

            if (nextOpen != -1 && nextOpen < nextClose) {
                // 遇到嵌套的开始标签
                depth++;
                pos = nextOpenMatcher.end();
            } else {
                // 遇到结束标签
                depth--;
                if (depth == 0) {
                    return nextClose;
                }
                pos = nextClose + match.closeTag().length();
            }
        }

        return -1;
    }
}
