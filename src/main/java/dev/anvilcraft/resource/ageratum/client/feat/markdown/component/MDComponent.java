package dev.anvilcraft.resource.ageratum.client.feat.markdown.component;

import dev.anvilcraft.lib.v2.font.AnvilLibFont;
import dev.anvilcraft.resource.ageratum.client.constants.AgeratumConstants;
import dev.anvilcraft.resource.ageratum.client.feat.markdown.ExtensionParamParser;
import dev.anvilcraft.resource.ageratum.client.feat.markdown.MDInlineComponentContext;
import dev.anvilcraft.resource.ageratum.client.feat.markdown.MDInlineComponentFactory;
import dev.anvilcraft.resource.ageratum.client.feat.markdown.MDRenderContext;
import dev.anvilcraft.resource.ageratum.client.registries.AgeratumRegistries;
import dev.anvilcraft.resource.ageratum.mixin.accessor.StringSplitterAccessor;
import lombok.Getter;
import net.minecraft.client.Minecraft;
import net.minecraft.client.StringSplitter;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.core.Registry;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.FormattedText;
import net.minecraft.network.chat.Style;
import net.minecraft.resources.Identifier;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.util.FormattedCharSink;
import net.minecraft.world.item.ItemStack;
import org.apache.commons.lang3.function.TriFunction;
import org.apache.commons.lang3.mutable.MutableObject;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
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
    private static final Pattern INLINE_COMPONENT_TAG_PATTERN = Pattern.compile(
        "<\\s*((?:[a-z0-9_.-]+:)?[a-z0-9_./-]+)(?:\\s+([^>]*?))?\\s*/>",
        Pattern.CASE_INSENSITIVE
    );
    private static final String COMMONMARK_ESCAPABLE_PUNCTUATION = "!\"#$%&'()*+,-./:;<=>?@[\\]^_`{|}~";
    private static final String ESCAPE_TOKEN_PREFIX = "%%MDESC";
    private static final String ESCAPE_TOKEN_SUFFIX = "%%";
    private static final int CODE_SPAN_COLOR = 0x7a4f2f;
    /**
     * -- GETTER --
     * 获取组件的 FormattedText。
     *
     * @return 该组件的格式化文本
     */
    @SuppressWarnings("JavadocDeclaration")
    protected final FormattedText text;

    /**
     * 渲染时实际使用的文本，若不为 null 则覆盖 {@link #text}。
     * 用于 post-process 后的文本替换（如断链红色标记）。
     */
    private @Nullable FormattedText effectiveText;

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
     * 设置渲染时覆盖文本（null 表示使用原始 text）。
     */
    public void setEffectiveText(@Nullable FormattedText effectiveText) {
        this.effectiveText = effectiveText;
    }

    /**
     * 获取渲染时实际使用的 FormattingText。
     */
    protected FormattedText getEffectiveText() {
        return this.effectiveText != null ? this.effectiveText : this.text;
    }

    /**
     * 在给定区域内渲染组件内容。
     */
    public void extractRenderState(MDRenderContext context) {
        Minecraft minecraft = context.minecraft();
        int maxX = context.maxX();
        int maxY = context.maxY();
        GuiGraphicsExtractor guiGraphics = context.graphics();
        FormattedText textToRender = this.getEffectiveText();
        List<FormattedCharSequence> split = minecraft.font.split(textToRender, maxX);
        int y = 0;
        for (FormattedCharSequence sequence : split) {
            if (maxY < minecraft.font.lineHeight) return;
            guiGraphics.anvillib$text(AnvilLibFont.getSelectFont(), sequence, 0, y, 0xFF000000, false);
            y += minecraft.font.lineHeight;
            maxY -= minecraft.font.lineHeight;
        }
    }

    /**
     * 计算组件的期望宽度。
     *
     * <p>此方法主要用于行内布局（如 {@code <row>} 组件）场景，
     * 允许组件声明其期望的渲染宽度。</p>
     *
     * <p>默认实现返回 {@code -1}，表示使用父容器分配的宽度。
     * 子类可以重写此方法以声明固定宽度或最小宽度需求。</p>
     *
     * @param minecraft Minecraft 实例
     * @param maxX      可用的最大宽度
     * @param maxY      可用的最大高度
     * @return 期望宽度（像素），{@code -1} 表示使用可用空间
     */
    public int getPreferredWidth(Minecraft minecraft, int maxX, int maxY) {
        return -1;
    }

    /**
     * 计算组件在指定宽度下的渲染高度。
     */
    public int getHeight(Minecraft minecraft, int maxX, int maxY) {
        return minecraft.font.split(this.getEffectiveText(), maxX).size() * minecraft.font.lineHeight;
    }

    /**
     * 获取组件内部指定坐标对应的文本样式。
     *
     * <p>默认实现按普通文本块处理；复杂布局组件可覆盖以匹配其真实渲染坐标。</p>
     */
    @Nullable
    public Style getStyleAtPosition(Minecraft minecraft, double mouseX, double mouseY, int maxX) {
        return this.getStyleAtFormattedTextPosition(minecraft, this.getEffectiveText(), mouseX, mouseY, maxX);
    }

    /**
     * 处理组件内部鼠标滚轮事件。
     *
     * @return 若组件消费事件返回 {@code true}
     */
    public boolean mouseScrolled(Minecraft minecraft, double mouseX, double mouseY, double scrollY, int maxX) {
        return false;
    }

    /**
     * 处理组件内部鼠标按下事件。
     *
     * @return 若组件消费事件返回 {@code true}
     */
    public boolean mouseClicked(Minecraft minecraft, double mouseX, double mouseY, int button, int maxX) {
        return false;
    }

    /**
     * 处理组件内部鼠标拖拽事件。
     *
     * @return 若组件消费事件返回 {@code true}
     */
    public boolean mouseDragged(Minecraft minecraft, double mouseX, double mouseY, int button, double dragX, double dragY, int maxX) {
        return false;
    }

    /**
     * 处理组件内部鼠标释放事件。
     *
     * @return 若组件消费事件返回 {@code true}
     */
    public boolean mouseReleased(Minecraft minecraft, double mouseX, double mouseY, int button, int maxX) {
        return false;
    }

    /**
     * 处理组件内部键盘事件。
     *
     * @return 若组件消费事件返回 {@code true}
     */
    public boolean keyPressed(Minecraft minecraft, double mouseX, double mouseY, int keyCode, int scanCode, int modifiers, int maxX) {
        return false;
    }

    /**
     * 指示组件是否需要阻止父级继续处理当前按键。
     *
     * <p>适用于组件希望独占某些导航按键，但当前按下后内部状态没有发生变化的情况。</p>
     */
    public boolean blocksParentKeyHandling(
        Minecraft minecraft,
        double mouseX,
        double mouseY,
        int keyCode,
        int scanCode,
        int modifiers,
        int maxX
    ) {
        return false;
    }

    /**
     * 根据格式化文本在指定宽度下的换行结果获取命中的文本样式。
     */
    @Nullable
    protected final Style getStyleAtFormattedTextPosition(Minecraft minecraft, FormattedText text, double mouseX, double mouseY, int maxX) {
        if (mouseX < 0 || mouseY < 0 || maxX <= 0) {
            return null;
        }

        List<FormattedCharSequence> lines = minecraft.font.split(text, maxX);
        int lineIndex = (int) Math.floor(mouseY / minecraft.font.lineHeight);
        if (lineIndex < 0 || lineIndex >= lines.size()) {
            return null;
        }

        FormattedCharSequence line = lines.get(lineIndex);
        return MDComponent.componentStyleAtWidth(minecraft.font.getSplitter(), line, (int) Math.floor(mouseX));
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
        Style style = parentStyle.withUnderlined(true).withColor(AgeratumConstants.GuideScreenUI.Colors.LINK_COLOR);
        if (target == null || target.isBlank()) {
            return style;
        }
        return style.withClickEvent(new ClickEvent.OpenUrl(URI.create(target)));
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
     * 解析并应用自定义样式标签文本。
     */
    public static FormattedText parseStyledText(String text, Style parentStyle) {
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

    /**
     * 一次内联样式匹配结果，保存起止标签与样式 / 文本生成逻辑。
     *
     * <p>此类可保存两类工厂之一：
     * <ul>
     *   <li>传统的 {@code BiFunction<Style, Matcher, Style>}：用于在解析时返回一个新的
     *   {@link net.minecraft.network.chat.Style}，随后由解析器对标签内部文本递归解析并应用该样式；</li>
     *   <li>新的 {@code TriFunction<String, Style, Matcher, FormattedText>}：直接基于原始
     *   内部字符串 (innerText)、父样式与匹配器生产最终的 {@link net.minecraft.network.chat.FormattedText}，
     *   这使得解析器可以一次性生成多段文本（例如按字符的渐变色片段）。</li>
     * </ul>
     *
     * <p>当两者都存在时优先使用 textFactory；通常只会有其一被设置（由工厂方法创建）。
     */
    public static final class InlineStyleMatch {
        private final Pattern openTagPattern;
        private final Matcher matcher;
        private final String closeTag;
        private final TriFunction<String, Style, Matcher, FormattedText> textFactory;

        private InlineStyleMatch(
            Pattern openTagPattern,
            Matcher matcher,
            String closeTag,
            TriFunction<String, Style, Matcher, FormattedText> textFactory
        ) {
            this.openTagPattern = openTagPattern;
            this.matcher = matcher;
            this.closeTag = closeTag;
            this.textFactory = textFactory;
        }

        public static InlineStyleMatch of(
            Pattern openTagPattern,
            Matcher matcher,
            String closeTag,
            BiFunction<Style, Matcher, Style> styleFactory
        ) {
            return new InlineStyleMatch(
                openTagPattern,
                matcher,
                closeTag,
                (innerText, style, matcher1) -> parseStyledText(innerText, styleFactory.apply(style, matcher1))
            );
        }

        public static InlineStyleMatch of(
            Pattern openTagPattern,
            Matcher matcher,
            String closeTag,
            TriFunction<String, Style, Matcher, FormattedText> textFactory
        ) {
            return new InlineStyleMatch(openTagPattern, matcher, closeTag, textFactory);
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

        public FormattedText applyTextFactory(String innerText, Style parentStyle) {
            return this.textFactory.apply(innerText, parentStyle, this.matcher);
        }
    }

    private record ParserMatch(Identifier id, int priority, InlineStyleMatch match) {
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
        IMAGE, LINK, STRIKE, BOLD, ITALIC, AUTOLINK
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
            Identifier parserId = registry.getKey(parser);
            if (parserId == null) {
                continue;
            }
            InlineStyleMatch match = parser.parse(text, pos);
            if (match == null) {
                continue;
            }

            if (earliest == null || match.start() < earliest.start() || (
                match.start() == earliest.start() && compareInlineStyleParser(parserId, parser.priority(), earliest) < 0
            )) {
                earliest = new ParserMatch(parserId, parser.priority(), match);
            }
        }
        return earliest;
    }

    private static @Nullable InlineComponentMatch findNextInlineComponent(String text, int pos, Style parentStyle) {
        Matcher matcher = INLINE_COMPONENT_TAG_PATTERN.matcher(text);
        while (matcher.find(pos)) {
            Identifier id = parseInlineComponentId(matcher.group(1));
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

    private static @Nullable Identifier parseInlineComponentId(String idText) {
        String normalized = idText.contains(":") ? idText : "ageratum:" + idText;
        try {
            return Identifier.parse(normalized);
        } catch (RuntimeException exception) {
            return null;
        }
    }

    private static int compareInlineStyleParser(Identifier parserId, int priority, ParserMatch current) {
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

    public boolean isHoverItem(int startX, int startY, float mouseX, float mouseY) {
        return this.isHover(startX, startY, 16, 16, mouseX, mouseY);
    }

    public boolean isHover(int startX, int startY, int width, int height, float mouseX, float mouseY) {
        return mouseX >= startX && mouseX <= startX + width && mouseY >= startY && mouseY <= startY + height;
    }

    protected void extractTooltipRenderState(MDRenderContext context, ItemStack stack, int startX, int startY, float mouseX, float mouseY) {
        if (this.isHoverItem(startX, startY, mouseX, mouseY)) {
            context.addTooltip(stack);
        }
    }

    @Nullable
    public static Style componentStyleAtWidth(StringSplitter stringSplitter, FormattedCharSequence content, int maxWidth) {
        WidthLimitedCharSink stringsplitter$widthlimitedcharsink = new WidthLimitedCharSink(stringSplitter, maxWidth);
        MutableObject<Style> mutableobject = new MutableObject<>();
        content.accept((positionInCurrentSequence, style, codePoint) -> {
            if (!stringsplitter$widthlimitedcharsink.accept(positionInCurrentSequence, style, codePoint)) {
                mutableobject.setValue(style);
                return false;
            } else {
                return true;
            }
        });
        return mutableobject.get();
    }

    private static class WidthLimitedCharSink implements FormattedCharSink {
        StringSplitter stringSplitter;
        private float maxWidth;
        @Getter
        private int position;

        public WidthLimitedCharSink(StringSplitter stringSplitter, float maxWidth) {
            this.stringSplitter = stringSplitter;
            this.maxWidth = maxWidth;
        }

        @Override
        public boolean accept(int positionInCurrentSequence, Style style, int codePoint) {
            this.maxWidth = this.maxWidth - ((StringSplitterAccessor) this.stringSplitter).widthProvider().getWidth(codePoint, style);
            if (this.maxWidth >= 0.0F) {
                this.position = positionInCurrentSequence + Character.charCount(codePoint);
                return true;
            } else {
                return false;
            }
        }
    }
}
