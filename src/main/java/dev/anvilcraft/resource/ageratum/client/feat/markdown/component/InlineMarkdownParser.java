package dev.anvilcraft.resource.ageratum.client.feat.markdown.component;

import dev.anvilcraft.resource.ageratum.client.constants.AgeratumConstants;
import dev.anvilcraft.resource.ageratum.client.feat.markdown.ExtensionParamParser;
import dev.anvilcraft.resource.ageratum.client.feat.markdown.MDInlineComponentContext;
import dev.anvilcraft.resource.ageratum.client.feat.markdown.MDInlineComponentFactory;
import dev.anvilcraft.resource.ageratum.client.feat.markdown.component.MDComponent.InlineStyleMatch;
import dev.anvilcraft.resource.ageratum.client.gui.GuideTheme;
import dev.anvilcraft.resource.ageratum.client.registries.AgeratumRegistries;
import net.minecraft.core.Registry;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.FormattedText;
import net.minecraft.network.chat.Style;
import net.minecraft.resources.ResourceLocation;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.annotation.Nullable;

/** Parses inline Markdown and dispatches custom text styles and inline components. */
public final class InlineMarkdownParser {
    private static final Pattern IMAGE_PATTERN = Pattern.compile("!\\[([^]]*)]\\(([^)]+)\\)");
    private static final Pattern LINK_PATTERN = Pattern.compile("\\[([^]]+)]\\(([^)]+)\\)");
    private static final Pattern STRIKE_PATTERN = Pattern.compile("~~([^~\\n]+)~~");
    private static final Pattern BOLD_ASTERISK_PATTERN = Pattern.compile("\\*\\*([^*\\n]+)\\*\\*");
    private static final Pattern BOLD_UNDERSCORE_PATTERN = Pattern.compile("(?<![A-Za-z0-9_])__([^_\\n]+)__(?![A-Za-z0-9_])");
    private static final Pattern ITALIC_ASTERISK_PATTERN = Pattern.compile("(?<!\\*)\\*([^*\\n]+)\\*(?!\\*)");
    private static final Pattern ITALIC_UNDERSCORE_PATTERN = Pattern.compile("(?<![A-Za-z0-9_])_([^_\\n]+)_(?![A-Za-z0-9_])");
    private static final Pattern AUTOLINK_URL_PATTERN = Pattern.compile("<(https?://[^>\\s]+)>");
    private static final Pattern AUTOLINK_EMAIL_PATTERN = Pattern.compile("<([a-zA-Z0-9._%+\\-]+@[a-zA-Z0-9.\\-]+\\.[a-zA-Z]{2,})>");
    private static final Pattern INLINE_COMPONENT_TAG_PATTERN = Pattern.compile(
        "<\\s*((?:[a-z0-9_.-]+:)?[a-z0-9_./-]+)(?:\\s+([^>]*?))?\\s*/>",
        Pattern.CASE_INSENSITIVE
    );
    private static final String COMMONMARK_ESCAPABLE_PUNCTUATION = "!\"#$%&'()*+,-./:;<=>?@[\\]^_`{|}~";
    private static final String ESCAPE_TOKEN_PREFIX = "%%MDESC";
    private static final String ESCAPE_TOKEN_SUFFIX = "%%";

    private static final Pattern REFERENCE = Pattern.compile(
        "\\[((?:\\\\.|[^\\[\\]\\\\\\n])++)\\](?:\\[((?:\\\\.|[^\\[\\]\\\\\\n])*+)\\])?"
    );
    private static final Pattern TAG = Pattern.compile("</?[a-zA-Z](?:\"[^\"\\n]*\"|'[^'\\n]*'|[^<>\"'\\n])*+>");

    private InlineMarkdownParser() {}

    /** Resolves reference links in prose while preserving literal spans and tag attributes. */
    public static String expandReferences(String text, Map<String, String> definitions) {
        if (definitions.isEmpty() || text.indexOf('[') < 0) return text;

        StringBuilder expanded = new StringBuilder(text.length());
        int start = 0;
        CodeSpan span;
        while ((span = nextCodeSpan(text, start)) != null) {
            appendReferences(expanded, text.substring(start, span.start()), definitions);
            expanded.append(text, span.start(), span.end());
            start = span.end();
        }
        appendReferences(expanded, text.substring(start), definitions);
        return expanded.toString();
    }

    private static void appendReferences(StringBuilder result, String text, Map<String, String> definitions) {
        Matcher reference = REFERENCE.matcher(text);
        Matcher tag = TAG.matcher(text);
        for (int pos = 0; pos < text.length(); ) {
            char c = text.charAt(pos);
            if (c == '\\' && pos + 1 < text.length() && isMarkdownEscapable(text.charAt(pos + 1))) {
                result.append(text, pos, pos + 2);
                pos += 2;
            } else if (c == '<' && tag.region(pos, text.length()).lookingAt()) {
                result.append(tag.group());
                pos = tag.end();
            } else if (c == '[' && reference.region(pos, text.length()).lookingAt()) {
                int end = reference.end();
                if (reference.group(2) == null && end < text.length() && text.charAt(end) == '(') {
                    int close = linkDestinationEnd(text, end);
                    result.append(text, pos, close);
                    pos = close;
                    continue;
                }
                String label = reference.group(1);
                String id = reference.group(2);
                String target = definitions.get((id == null || id.isEmpty() ? label : id).toLowerCase(Locale.ROOT));
                if (target == null) result.append(reference.group());
                else result.append('[').append(label).append("](").append(target).append(')');
                pos = end;
            } else {
                result.append(c);
                pos++;
            }
        }
    }

    private static int linkDestinationEnd(String text, int open) {
        int depth = 1;
        for (int i = open + 1; i < text.length(); i++) {
            char c = text.charAt(i);
            if (c == '\\' && i + 1 < text.length()) i++;
            else if (c == '(') depth++;
            else if (c == ')' && --depth == 0) return i + 1;
        }
        return open + 1;
    }

    static FormattedText format(String text, Style baseStyle) {
        List<FormattedText> parts = new ArrayList<>();
        int start = 0;
        CodeSpan span;
        while ((span = nextCodeSpan(text, start)) != null) {
            appendMarkdownPart(parts, text.substring(start, span.start()), baseStyle);
            String code = text.substring(span.contentStart(), span.contentEnd());
            if (code.length() >= 2 && code.startsWith(" ") && code.endsWith(" ") && !code.isBlank()) {
                code = code.substring(1, code.length() - 1);
            }
            parts.add(GuideTheme.inlineCode(code, baseStyle));
            start = span.end();
        }
        appendMarkdownPart(parts, text.substring(start), baseStyle);
        if (parts.isEmpty()) return FormattedText.EMPTY;
        return parts.size() == 1 ? parts.getFirst() : FormattedText.composite(parts);
    }

    /** Visits literal inline code ranges, including their delimiters, with an exclusive end offset. */
    public static void forEachCodeSpan(String text, BiConsumer<Integer, Integer> visitor) {
        int offset = 0;
        CodeSpan span;
        while ((span = nextCodeSpan(text, offset)) != null) {
            visitor.accept(span.start(), span.end());
            offset = span.end();
        }
    }

    private record CodeSpan(int start, int contentStart, int contentEnd, int end) {}

    private static @Nullable CodeSpan nextCodeSpan(String text, int start) {
        for (int i = start; i < text.length(); ) {
            char c = text.charAt(i++);
            if (c == '\\' && i < text.length() && isMarkdownEscapable(text.charAt(i))) {
                i++;
            } else if (c == '`') {
                int open = i - 1;
                while (i < text.length() && text.charAt(i) == '`') i++;
                int length = i - open;
                int close = findMatchingBackticks(text, i, length);
                if (close >= 0) return new CodeSpan(open, i, close, close + length);
            }
        }
        return null;
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
    private static void appendMarkdownPart(List<FormattedText> parts, String text, Style style) {
        if (text.isEmpty()) {
            return;
        }
        EscapeContext escapes = protectMarkdownEscapes(text);
        parts.add(parseMarkdownInlineText(escapes.text(), style, escapes));
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
                    Style linkStyle = createLinkStyle(parentStyle, target == null ? null : restoreEscapedLiterals(target, escapeContext));
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
        Style style = parentStyle.withUnderlined(true).withColor(AgeratumConstants.GuideScreenUI.Colors.LINK_COLOR);
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
        String prefix = ESCAPE_TOKEN_PREFIX;
        while (text.contains(prefix)) prefix += "%";
        for (int i = 0; i < text.length(); i++) {
            char ch = text.charAt(i);
            if (ch == '\\' && i + 1 < text.length() && isMarkdownEscapable(text.charAt(i + 1))) {
                char escapedChar = text.charAt(i + 1);
                String token = prefix + escapedLiterals.size() + ESCAPE_TOKEN_SUFFIX;
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
     * 解析并应用自定义样式标签文本。
     */
    static FormattedText parseStyledText(String text, Style parentStyle) {
        List<FormattedText> parts = new ArrayList<>();
        int pos = 0;

        while (pos < text.length()) {
            // 查找最近的标签
            ParserMatch nextTag = findNextTag(text, pos);
            InlineComponentMatch nextInlineComponent = findNextInlineComponent(text, pos, parentStyle);

            if (nextInlineComponent != null && (nextTag == null || nextInlineComponent.start() < nextTag.start())) {
                if (nextInlineComponent.start() > pos) {
                    String plainText = text.substring(pos, nextInlineComponent.start());
                    parts.add(FormattedText.of(plainText, parentStyle));
                }
                parts.add(nextInlineComponent.text());
                pos = nextInlineComponent.end();
                continue;
            }

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

                    InlineStyleMatch match = nextTag.match();
                    FormattedText innerText = match.applyTextFactory(innerContent, parentStyle);
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

    private record ParserMatch(ResourceLocation id, int priority, InlineStyleMatch match) {
        private int start() {
            return this.match.start();
        }

        private int end() {
            return this.match.end();
        }
    }

    private record InlineComponentMatch(int start, int end, FormattedText text) {
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

    private static @Nullable Matcher findNextTag(Pattern pattern, String text, int pos) {
        Matcher matcher = pattern.matcher(text);
        if (matcher.find(pos)) {
            return matcher;
        }
        return null;
    }

    private static @Nullable ParserMatch findNextTag(String text, int pos) {
        ParserMatch earliest = null;
        Registry<MDInlineStyleParser> registry = AgeratumRegistries.INLINE_STYLE_PARSER_REGISTRY;
        for (MDInlineStyleParser parser : registry) {
            ResourceLocation parserId = registry.getKey(parser);
            if (parserId == null) {
                continue;
            }
            InlineStyleMatch match = parser.parse(text, pos);
            if (match == null) {
                continue;
            }

            if (earliest == null
                || match.start() < earliest.start()
                || (match.start() == earliest.start() && compareInlineStyleParser(parserId, parser.priority(), earliest) < 0)) {
                earliest = new ParserMatch(parserId, parser.priority(), match);
            }
        }
        return earliest;
    }

    private static @Nullable InlineComponentMatch findNextInlineComponent(String text, int pos, Style parentStyle) {
        Matcher matcher = INLINE_COMPONENT_TAG_PATTERN.matcher(text);
        while (matcher.find(pos)) {
            ResourceLocation id = parseInlineComponentId(matcher.group(1));
            if (id == null) {
                pos = matcher.start() + 1;
                continue;
            }

            Registry<MDInlineComponentFactory> registry = AgeratumRegistries.INLINE_COMPONENT_FACTORY_REGISTRY;
            MDInlineComponentFactory factory = registry.getOptional(id).orElse(null);
            if (factory == null) {
                pos = matcher.start() + 1;
                continue;
            }

            String rawParams = matcher.group(2) == null ? "" : matcher.group(2).trim();
            Map<String, String> params = ExtensionParamParser.parse(rawParams);
            FormattedText componentText = factory.create(new MDInlineComponentContext(id, rawParams, params, parentStyle));
            return new InlineComponentMatch(matcher.start(), matcher.end(), componentText);
        }
        return null;
    }

    private static @Nullable ResourceLocation parseInlineComponentId(String idText) {
        String normalized = idText.contains(":") ? idText : "ageratum:" + idText;
        try {
            return ResourceLocation.parse(normalized);
        } catch (RuntimeException exception) {
            return null;
        }
    }

    private static int compareInlineStyleParser(ResourceLocation parserId, int priority, ParserMatch current) {
        int priorityCompare = Integer.compare(priority, current.priority());
        if (priorityCompare != 0) {
            return priorityCompare;
        }
        return parserId.compareTo(current.id());
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
