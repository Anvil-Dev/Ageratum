package dev.anvilcraft.resource.ageratum.client.feat.markdown;

import dev.anvilcraft.resource.ageratum.client.feat.markdown.component.MDComponent;
import dev.anvilcraft.resource.ageratum.client.feat.markdown.component.MDCodeBlockComponent;
import dev.anvilcraft.resource.ageratum.client.feat.markdown.component.MDHeaderComponent;
import dev.anvilcraft.resource.ageratum.client.feat.markdown.component.MDHorizontalRuleComponent;
import dev.anvilcraft.resource.ageratum.client.feat.markdown.component.MDImageComponent;
import dev.anvilcraft.resource.ageratum.client.feat.markdown.component.MDListComponent;
import dev.anvilcraft.resource.ageratum.client.feat.markdown.component.MDQuoteComponent;
import dev.anvilcraft.resource.ageratum.client.feat.markdown.component.MDTableComponent;
import dev.anvilcraft.resource.ageratum.client.feat.markdown.component.MDTextComponent;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.annotation.Nullable;

/**
 * Markdown 解析器。
 *
 * <p>负责将原始 Markdown 文本按块级语义拆分为一组 {@link MDComponent}，
 * 并在解析阶段处理引用链接、代码块、列表、表格、引用块等结构。</p>
 *
 * <p>解析器支持通过 {@link #registerComponentParser(int, MDComponentParser)}
 * 注入额外的行级组件解析器，用于扩展自定义语法。</p>
 */
public class MarkdownParser {
    // ── Block patterns ──────────────────────────────────────────────
    private static final Pattern ORDERED_LIST_PATTERN = Pattern.compile("^(\\s*)(\\d+)\\.\\s+(.+)$");
    private static final Pattern TASK_LIST_PATTERN = Pattern.compile("^(\\s*)[-+*]\\s+\\[([ xX])]\\s+(.+)$");
    private static final Pattern UNORDERED_LIST_PATTERN = Pattern.compile("^(\\s*)[-+*]\\s+(.+)$");
    private static final Pattern BLOCKQUOTE_PATTERN = Pattern.compile("^\\s*((?:>\\s*)+)(.*)$");
    private static final Pattern HORIZONTAL_RULE_PATTERN = Pattern.compile("^\\s*([-*_])(?:\\s*\\1){2,}\\s*$");
    private static final Pattern SETEXT_H1_PATTERN = Pattern.compile("^=+\\s*$");
    private static final Pattern SETEXT_H2_PATTERN = Pattern.compile("^-+\\s*$");
    private static final Pattern CODE_FENCE_PATTERN = Pattern.compile("^(`{3,}|~{3,})(.*)$");
    private static final Pattern INDENTED_CODE_PATTERN = Pattern.compile("^(?:    |\\t)(.*)$");
    private static final Pattern TABLE_ROW_PATTERN = Pattern.compile("^\\|.*\\|\\s*$");
    private static final Pattern LINK_REF_DEF_PATTERN = Pattern.compile(
        "^\\s{0,3}\\[([^\\]]+)]:\\s*(\\S+)(?:\\s+(?:\"[^\"]*\"|'[^']*'|\\([^)]*\\)))?\\s*$"
    );
    // Reference link syntax: [text][id] or [text][] (collapsed)
    private static final Pattern LINK_REF_FULL_PATTERN = Pattern.compile("\\[([^\\]]+)]\\[([^\\]]*)]");
    // Shortcut ref [id] – not followed by ( or [
    private static final Pattern LINK_REF_SHORT_PATTERN = Pattern.compile("\\[([^\\]\\[]+)](?![\\[(])");

    private final Set<MDComponentParserHolder> mdComponentParserHolders = new TreeSet<>();

    /**
     * 创建解析器并注册内置组件解析器。
     */
    public MarkdownParser() {
        this.registerBaseComponentParser();
    }

    /**
     * 注册一个行级组件解析器。
     *
     * @param priority 解析优先级；值越小优先级越高
     * @param parser   组件解析函数
     */
    public void registerComponentParser(int priority, MDComponentParser parser) {
        this.mdComponentParserHolders.add(new MDComponentParserHolder(priority, parser));
    }

    private void registerBaseComponentParser() {
        this.registerComponentParser(-10, MDImageComponent::parse);
        this.registerComponentParser(0, MDHeaderComponent::parse);
    }

    // ── Public entry point ───────────────────────────────────────────
    /**
     * 将 Markdown 文本解析为组件列表。
     *
     * <p>解析流程分为两阶段：先做引用链接预处理，再做逐行块级扫描；
     * 最终会将段落、引用、列表、表格与代码块等临时缓冲区统一刷新为组件。</p>
     *
     * @param markdown 原始 Markdown 文本
     * @return 按渲染顺序排列的组件列表
     */
    public List<MDComponent> parse(String markdown) {
        String normalized = markdown.replace("\r\n", "\n").replace('\r', '\n');
        String[] split = normalized.split("\n", -1);

        // Pre-pass 1: collect link reference definitions
        Map<String, String> linkRefs = collectLinkRefs(split);
        // Pre-pass 2: expand reference links in the full text
        if (!linkRefs.isEmpty()) {
            normalized = expandLinkRefs(normalized, linkRefs);
            split = normalized.split("\n", -1);
        }

        List<MDComponent> components = new ArrayList<>();
        StringBuilder paragraphBuilder = new StringBuilder();
        List<MDQuoteComponent.QuoteLine> quoteLines = new ArrayList<>();
        List<MDListComponent.ListItem> listItems = new ArrayList<>();
        List<String> tableRows = new ArrayList<>();
        StringBuilder codeBlockBuilder = new StringBuilder();
        StringBuilder indentedCodeBuilder = new StringBuilder();
        String codeFence = null;          // null = not in fenced code block
        boolean inIndentedCode = false;

        for (String s : split) {
            // ── Inside fenced code block ────────────────────────────
            if (codeFence != null) {
                Matcher closeMatcher = CODE_FENCE_PATTERN.matcher(s.trim());
                if (closeMatcher.matches()
                    && closeMatcher.group(1).charAt(0) == codeFence.charAt(0)
                    && closeMatcher.group(1).length() >= codeFence.length()) {
                    codeFence = null;
                    if (!codeBlockBuilder.isEmpty()) {
                        codeBlockBuilder.deleteCharAt(codeBlockBuilder.length() - 1);
                    }
                    components.add(new MDCodeBlockComponent(codeBlockBuilder.toString()));
                    codeBlockBuilder = new StringBuilder();
                } else {
                    codeBlockBuilder.append(s).append("\n");
                }
                continue;
            }

            // ── Code fence opening (``` or ~~~) ─────────────────────
            Matcher fenceMatcher = CODE_FENCE_PATTERN.matcher(s.trim());
            if (fenceMatcher.matches()) {
                flushAll(components, paragraphBuilder, quoteLines, listItems, tableRows, indentedCodeBuilder);
                inIndentedCode = false;
                codeFence = fenceMatcher.group(1);
                continue;
            }

            // ── Indented code block (4 spaces or tab) ───────────────
            Matcher indentMatcher = INDENTED_CODE_PATTERN.matcher(s);
            if (indentMatcher.matches() && paragraphBuilder.isEmpty() && listItems.isEmpty() && quoteLines.isEmpty()) {
                flushTableComponent(components, tableRows);
                inIndentedCode = true;
                indentedCodeBuilder.append(indentMatcher.group(1)).append("\n");
                continue;
            }
            if (inIndentedCode && s.isBlank()) {
                indentedCodeBuilder.append("\n");
                continue;
            }
            if (inIndentedCode) {
                inIndentedCode = false;
                flushIndentedCodeComponent(components, indentedCodeBuilder);
            }

            // ── Skip link reference definition lines ────────────────
            if (LINK_REF_DEF_PATTERN.matcher(s).matches()) {
                continue;
            }

            // ── Blockquote ──────────────────────────────────────────
            Matcher quoteMatcher = BLOCKQUOTE_PATTERN.matcher(s);
            if (quoteMatcher.matches()) {
                flushParagraphComponent(components, paragraphBuilder);
                flushListComponent(components, listItems);
                flushTableComponent(components, tableRows);
                quoteLines.add(new MDQuoteComponent.QuoteLine(countQuoteLevel(quoteMatcher.group(1)), quoteMatcher.group(2)));
                continue;
            }

            // ── Table row ───────────────────────────────────────────
            if (TABLE_ROW_PATTERN.matcher(s).matches()) {
                flushParagraphComponent(components, paragraphBuilder);
                flushQuoteComponent(components, quoteLines);
                flushListComponent(components, listItems);
                tableRows.add(s);
                continue;
            }

            // ── Task list ───────────────────────────────────────────
            Matcher taskMatcher = TASK_LIST_PATTERN.matcher(s);
            if (taskMatcher.matches()) {
                flushParagraphComponent(components, paragraphBuilder);
                flushQuoteComponent(components, quoteLines);
                flushTableComponent(components, tableRows);
                listItems.add(MDListComponent.task(countIndentLevel(taskMatcher.group(1)),
                    taskMatcher.group(2).equalsIgnoreCase("x"), taskMatcher.group(3)));
                continue;
            }

            // ── Unordered list ──────────────────────────────────────
            Matcher unorderedMatcher = UNORDERED_LIST_PATTERN.matcher(s);
            if (unorderedMatcher.matches()) {
                flushParagraphComponent(components, paragraphBuilder);
                flushQuoteComponent(components, quoteLines);
                flushTableComponent(components, tableRows);
                listItems.add(MDListComponent.unordered(countIndentLevel(unorderedMatcher.group(1)), unorderedMatcher.group(2)));
                continue;
            }

            // ── Ordered list ────────────────────────────────────────
            Matcher orderedMatcher = ORDERED_LIST_PATTERN.matcher(s);
            if (orderedMatcher.matches()) {
                flushParagraphComponent(components, paragraphBuilder);
                flushQuoteComponent(components, quoteLines);
                flushTableComponent(components, tableRows);
                listItems.add(MDListComponent.ordered(countIndentLevel(orderedMatcher.group(1)),
                    Integer.parseInt(orderedMatcher.group(2)), orderedMatcher.group(3)));
                continue;
            }

            // ── Setext headings (must be before HR check) ───────────
            boolean isSetextH1 = SETEXT_H1_PATTERN.matcher(s).matches();
            boolean isSetextH2 = !isSetextH1 && SETEXT_H2_PATTERN.matcher(s).matches();
            if ((isSetextH1 || isSetextH2) && !paragraphBuilder.isEmpty()) {
                String accumulated = paragraphBuilder.toString();
                if (accumulated.endsWith("\n")) accumulated = accumulated.substring(0, accumulated.length() - 1);
                int lastNl = accumulated.lastIndexOf('\n');
                String headingText = lastNl >= 0 ? accumulated.substring(lastNl + 1) : accumulated;
                String remaining = lastNl >= 0 ? accumulated.substring(0, lastNl) : "";
                paragraphBuilder.setLength(0);
                if (!remaining.isEmpty()) {
                    paragraphBuilder.append(remaining).append("\n");
                    flushParagraphComponent(components, paragraphBuilder);
                }
                flushQuoteComponent(components, quoteLines);
                flushListComponent(components, listItems);
                flushTableComponent(components, tableRows);
                components.add(new MDHeaderComponent(isSetextH1 ? 1 : 2, headingText));
                continue;
            }

            // ── Horizontal rule ─────────────────────────────────────
            if (HORIZONTAL_RULE_PATTERN.matcher(s).matches()) {
                flushAll(components, paragraphBuilder, quoteLines, listItems, tableRows, indentedCodeBuilder);
                components.add(new MDHorizontalRuleComponent());
                continue;
            }

            // ── ATX heading / image / registered parsers ─────────────
            MDComponent component = this.parseComponent(s);
            if (component == null) {
                if (s.isBlank()) {
                    flushAll(components, paragraphBuilder, quoteLines, listItems, tableRows, indentedCodeBuilder);
                    inIndentedCode = false;
                } else {
                    paragraphBuilder.append(s).append("\n");
                }
            } else {
                flushAll(components, paragraphBuilder, quoteLines, listItems, tableRows, indentedCodeBuilder);
                components.add(component);
            }
        }

        // Flush unclosed fenced code block
        if (codeFence != null) {
            if (!codeBlockBuilder.isEmpty()) codeBlockBuilder.deleteCharAt(codeBlockBuilder.length() - 1);
            components.add(new MDCodeBlockComponent(codeBlockBuilder.toString()));
        }

        inIndentedCode = false;
        flushAll(components, paragraphBuilder, quoteLines, listItems, tableRows, indentedCodeBuilder);
        return components;
    }

    // ── Flush helpers ─────────────────────────────────────────────────
    /**
     * 将所有临时缓冲区刷新为对应组件。
     */
    private static void flushAll(
        List<MDComponent> components,
        StringBuilder paragraph,
        List<MDQuoteComponent.QuoteLine> quoteLines,
        List<MDListComponent.ListItem> listItems,
        List<String> tableRows,
        StringBuilder indentedCode
    ) {
        flushParagraphComponent(components, paragraph);
        flushQuoteComponent(components, quoteLines);
        flushListComponent(components, listItems);
        flushTableComponent(components, tableRows);
        flushIndentedCodeComponent(components, indentedCode);
    }

    /**
     * 刷新段落文本缓冲区。
     */
    private static void flushParagraphComponent(List<MDComponent> components, StringBuilder builder) {
        if (builder.isEmpty()) return;
        builder.deleteCharAt(builder.length() - 1);
        components.add(new MDTextComponent(builder.toString()));
        builder.setLength(0);
    }

    /**
     * 刷新引用块缓冲区。
     */
    private static void flushQuoteComponent(List<MDComponent> components, List<MDQuoteComponent.QuoteLine> lines) {
        if (lines.isEmpty()) return;
        components.add(new MDQuoteComponent(lines));
        lines.clear();
    }

    /**
     * 刷新列表缓冲区。
     */
    private static void flushListComponent(List<MDComponent> components, List<MDListComponent.ListItem> items) {
        if (items.isEmpty()) return;
        components.add(new MDListComponent(items));
        items.clear();
    }

    /**
     * 刷新表格缓冲区。
     */
    private static void flushTableComponent(List<MDComponent> components, List<String> tableRows) {
        if (tableRows.isEmpty()) return;
        components.add(MDTableComponent.parse(tableRows));
        tableRows.clear();
    }

    /**
     * 刷新缩进代码块缓冲区，并处理尾部空行。
     */
    private static void flushIndentedCodeComponent(List<MDComponent> components, StringBuilder builder) {
        if (builder.isEmpty()) return;
        String content = builder.toString();
        while (content.endsWith("\n\n")) content = content.substring(0, content.length() - 1);
        if (content.endsWith("\n")) content = content.substring(0, content.length() - 1);
        if (!content.isEmpty()) components.add(new MDCodeBlockComponent(content));
        builder.setLength(0);
    }

    // ── Link reference helpers ────────────────────────────────────────
    /**
     * 收集文档中的引用链接定义（如 {@code [id]: url}）。
     */
    private static Map<String, String> collectLinkRefs(String[] lines) {
        Map<String, String> refs = new LinkedHashMap<>();
        for (String line : lines) {
            Matcher m = LINK_REF_DEF_PATTERN.matcher(line);
            if (m.matches()) refs.put(m.group(1).toLowerCase(), m.group(2));
        }
        return refs;
    }

    /**
     * 展开引用链接语法为普通内联链接。
     */
    private static String expandLinkRefs(String markdown, Map<String, String> refs) {
        // Replace [text][id] → [text](url)
        Matcher full = LINK_REF_FULL_PATTERN.matcher(markdown);
        StringBuilder sb = new StringBuilder();
        while (full.find()) {
            String text = full.group(1);
            String id = full.group(2).isEmpty() ? text : full.group(2);
            String url = refs.get(id.toLowerCase());
            full.appendReplacement(sb, Matcher.quoteReplacement(url != null ? "[" + text + "](" + url + ")" : full.group(0)));
        }
        full.appendTail(sb);
        markdown = sb.toString();

        // Replace [id] shortcut → [id](url) when not followed by ( or [
        Matcher shortcut = LINK_REF_SHORT_PATTERN.matcher(markdown);
        sb = new StringBuilder();
        while (shortcut.find()) {
            String text = shortcut.group(1);
            String url = refs.get(text.toLowerCase());
            shortcut.appendReplacement(sb, Matcher.quoteReplacement(url != null ? "[" + text + "](" + url + ")" : shortcut.group(0)));
        }
        shortcut.appendTail(sb);
        return sb.toString();
    }

    // ── Misc helpers ──────────────────────────────────────────────────
    /**
     * 统计引用块前缀中的 {@code >} 层级。
     */
    private static int countQuoteLevel(String markers) {
        int level = 0;
        for (int i = 0; i < markers.length(); i++) {
            if (markers.charAt(i) == '>') level++;
        }
        return Math.max(1, level);
    }

    /**
     * 统计列表缩进层级（2 个空格视为 1 级，Tab 按 2 空格处理）。
     */
    private static int countIndentLevel(String indent) {
        int width = 0;
        for (int i = 0; i < indent.length(); i++) {
            char ch = indent.charAt(i);
            if (ch == '\t') width += 2;
            else if (ch == ' ') width++;
        }
        return Math.max(0, width / 2);
    }

    /**
     * 使用已注册解析器尝试将一行文本解析为组件。
     *
     * @param string 单行文本
     * @return 命中时返回组件，否则返回 {@code null}
     */
    public @Nullable MDComponent parseComponent(String string) {
        for (MDComponentParserHolder parserHolder : this.mdComponentParserHolders) {
            MDComponent component = parserHolder.parser().apply(string);
            if (component != null) return component;
        }
        return null;
    }

    /**
     * 行级组件解析函数接口。
     */
    @FunctionalInterface
    public interface MDComponentParser extends Function<String, MDComponent> {
    }

    /**
     * 解析器持有器，按优先级排序。
     */
    private record MDComponentParserHolder(int priority, MDComponentParser parser)
        implements Comparable<MDComponentParserHolder> {
        @Override
        public int compareTo(MDComponentParserHolder holder) {
            if (this.equals(holder)) return 0;
            return this.priority() >= holder.priority() ? 1 : -1;
        }
    }
}
