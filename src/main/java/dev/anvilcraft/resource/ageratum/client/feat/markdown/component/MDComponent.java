package dev.anvilcraft.resource.ageratum.client.feat.markdown.component;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.FormattedText;
import net.minecraft.network.chat.Style;
import net.minecraft.util.FormattedCharSequence;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BiFunction;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.annotation.Nullable;

public abstract class MDComponent {
    private static final Pattern IMAGE_PATTERN = Pattern.compile("!\\[([^]]*)]\\(([^)]+)\\)");
    private static final Pattern LINK_PATTERN = Pattern.compile("\\[([^]]+)]\\(([^)]+)\\)");
    private static final Pattern STRIKE_PATTERN = Pattern.compile("~~([^~\\n]+)~~");
    private static final Pattern BOLD_ASTERISK_PATTERN = Pattern.compile("\\*\\*([^*\\n]+)\\*\\*");
    private static final Pattern BOLD_UNDERSCORE_PATTERN = Pattern.compile("__([^_\\n]+)__");
    private static final Pattern ITALIC_ASTERISK_PATTERN = Pattern.compile("(?<!\\*)\\*([^*\\n]+)\\*(?!\\*)");
    private static final Pattern ITALIC_UNDERSCORE_PATTERN = Pattern.compile("(?<!_)_([^_\\n]+)_(?!_)");
    private static final String COMMONMARK_ESCAPABLE_PUNCTUATION = "!\"#$%&'()*+,-./:;<=>?@[\\]^_`{|}~";
    private static final String ESCAPE_TOKEN_PREFIX = "%%MDESC";
    private static final String ESCAPE_TOKEN_SUFFIX = "%%";
    private static final int CODE_SPAN_COLOR = 0x7a4f2f;
    private static final int LINK_COLOR = 0x66ccff;
    protected final FormattedText text;
    private static final List<InlineStyleParserHolder> INLINE_STYLE_PARSER_HOLDERS = new ArrayList<>();
    private static int nextInlineStyleParserOrder = 0;

    static {
        registerBaseStyleParser();
    }

    public MDComponent(String text) {
        this(MDComponent.textFormat(text));
    }

    public MDComponent(FormattedText text) {
        this.text = text;
    }

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

    public int getHeight(Minecraft minecraft, int maxX, int maxY) {
        return minecraft.font.wordWrapHeight(this.text, maxX);
    }

    public static FormattedText textFormat(String text) {
        return textFormat(text, Style.EMPTY);
    }

    public static FormattedText textFormat(String text, Style baseStyle) {
        EscapeContext escapeContext = protectMarkdownEscapes(text);
        return parseMixedTextWithCodeSpan(escapeContext.text(), escapeContext, baseStyle);
    }

    private static FormattedText parseMixedTextWithCodeSpan(String text, EscapeContext escapeContext, Style baseStyle) {
        List<FormattedText> parts = new ArrayList<>();
        int pos = 0;
        while (pos < text.length()) {
            int codeStart = text.indexOf('`', pos);
            if (codeStart < 0) {
                appendMarkdownPart(parts, text.substring(pos), baseStyle, escapeContext);
                break;
            }

            int codeEnd = text.indexOf('`', codeStart + 1);
            if (codeEnd < 0) {
                appendMarkdownPart(parts, text.substring(pos), baseStyle, escapeContext);
                break;
            }

            if (codeStart > pos) {
                appendMarkdownPart(parts, text.substring(pos, codeStart), baseStyle, escapeContext);
            }
            String codeSpan = restoreEscapedLiterals(text.substring(codeStart + 1, codeEnd), escapeContext);
            parts.add(FormattedText.of(codeSpan, baseStyle.withColor(CODE_SPAN_COLOR)));
            pos = codeEnd + 1;
        }

        if (parts.isEmpty()) {
            return FormattedText.EMPTY;
        }
        if (parts.size() == 1) {
            return parts.getFirst();
        }
        return FormattedText.composite(parts);
    }

    private static void appendMarkdownPart(List<FormattedText> parts, String text, Style style, EscapeContext escapeContext) {
        if (text.isEmpty()) {
            return;
        }
        parts.add(parseMarkdownInlineText(text, style, escapeContext));
    }

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
                case IMAGE -> parts.add(FormattedText.of("[image: " + restoreEscapedLiterals(next.content, escapeContext) + "]", parentStyle));
                case LINK -> parts.add(parseMarkdownInlineText(next.content, parentStyle.withUnderlined(true).withColor(LINK_COLOR), escapeContext));
                case STRIKE -> parts.add(parseMarkdownInlineText(next.content, parentStyle.withStrikethrough(true), escapeContext));
                case BOLD -> parts.add(parseMarkdownInlineText(next.content, parentStyle.withBold(true), escapeContext));
                case ITALIC -> parts.add(parseMarkdownInlineText(next.content, parentStyle.withItalic(true), escapeContext));
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
        return earliest;
    }

    private static @Nullable MarkdownTokenMatch findToken(Pattern pattern, String text, int pos, MarkdownTokenType type) {
        Matcher matcher = pattern.matcher(text);
        if (!matcher.find(pos)) {
            return null;
        }
        return new MarkdownTokenMatch(type, matcher.start(), matcher.end(), matcher.group(1));
    }

    private static @Nullable MarkdownTokenMatch chooseEarlier(@Nullable MarkdownTokenMatch current, @Nullable MarkdownTokenMatch candidate) {
        if (candidate == null) {
            return current;
        }
        if (current == null || candidate.start < current.start) {
            return candidate;
        }
        return current;
    }

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

    private static String restoreEscapedLiterals(String text, EscapeContext escapeContext) {
        String restored = text;
        for (EscapedLiteral escapedLiteral : escapeContext.escapedLiterals()) {
            restored = restored.replace(escapedLiteral.token(), escapedLiteral.value());
        }
        return restored;
    }

    public static synchronized void registerStyleParser(int priority, InlineStyleParser parser) {
        INLINE_STYLE_PARSER_HOLDERS.add(new InlineStyleParserHolder(priority, nextInlineStyleParserOrder++, parser));
        INLINE_STYLE_PARSER_HOLDERS.sort(null);
    }

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
    }

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

    public @FunctionalInterface interface InlineStyleParser {
        @Nullable
        InlineStyleMatch parse(String text, int pos);
    }

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
        ITALIC
    }

    private static final class MarkdownTokenMatch {
        private final MarkdownTokenType type;
        private final int start;
        private final int end;
        private final String content;

        private MarkdownTokenMatch(MarkdownTokenType type, int start, int end, String content) {
            this.type = type;
            this.start = start;
            this.end = end;
            this.content = content;
        }
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
     * 查找与开始标签匹配的结束标签位置（处理嵌套）
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
