package dev.anvilcraft.resource.ageratum.client.feat.markdown;

import dev.anvilcraft.resource.ageratum.client.feat.markdown.component.MDCodeBlockComponent;
import dev.anvilcraft.resource.ageratum.client.feat.markdown.component.MDComponent;
import dev.anvilcraft.resource.ageratum.client.feat.markdown.component.MDHeaderComponent;
import dev.anvilcraft.resource.ageratum.client.feat.markdown.component.MDHorizontalRuleComponent;
import dev.anvilcraft.resource.ageratum.client.feat.markdown.component.MDImageComponent;
import dev.anvilcraft.resource.ageratum.client.feat.markdown.component.MDListComponent;
import dev.anvilcraft.resource.ageratum.client.feat.markdown.component.MDQuoteComponent;
import dev.anvilcraft.resource.ageratum.client.feat.markdown.component.MDTableComponent;
import dev.anvilcraft.resource.ageratum.client.feat.markdown.component.MDTextComponent;
import dev.anvilcraft.resource.ageratum.client.registries.AgeratumRegistries;
import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceLocation;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.TreeSet;
import java.util.function.BiFunction;
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
 * <p>同时支持两种扩展语法：</p>
 * <ul>
 *   <li>{@code ::: namespace:location ... :::} 冒号语法</li>
 *   <li>{@code <namespace:location>...</namespace:location>} 和 {@code <namespace:location/>} 标签语法</li>
 * </ul>
 *
 * <p>扩展组件通过 NeoForge Custom Registry {@code ageratum:extension_component_factory} 注册。</p>
 *
 * <p>解析器也支持通过 {@link #registerComponentParser(int, MDComponentParser)} 注入行级组件解析器。</p>
 */
public class MarkdownParser {
    // ── 块级模式定义 ──────────────────────────────────────────────────

    private static final Pattern ORDERED_LIST_PATTERN = Pattern.compile("^(\\s*)(\\d+)\\.\\s+(.+)$");
    private static final Pattern TASK_LIST_PATTERN = Pattern.compile("^(\\s*)[-+*]\\s+\\[([ xX])]\\s+(.+)$");
    private static final Pattern UNORDERED_LIST_PATTERN = Pattern.compile("^(\\s*)[-+*]\\s+(.+)$");
    private static final Pattern BLOCKQUOTE_PATTERN = Pattern.compile("^\\s*((?:>\\s*)+)(.*)$");
    private static final Pattern HORIZONTAL_RULE_PATTERN = Pattern.compile("^\\s*([-*_])(?:\\s*\\1){2,}\\s*$");
    private static final Pattern SETEXT_H1_PATTERN = Pattern.compile("^=+\\s*$");
    private static final Pattern SETEXT_H2_PATTERN = Pattern.compile("^-+\\s*$");
    private static final Pattern CODE_FENCE_PATTERN = Pattern.compile("^(`{3,}|~{3,})(.*)$");
    private static final Pattern INDENTED_CODE_PATTERN = Pattern.compile("^(?: {4}|\\t)(.*)$");
    private static final Pattern TABLE_ROW_PATTERN = Pattern.compile("^\\|.*\\|\\s*$");
    private static final Pattern LINK_REF_DEF_PATTERN = Pattern.compile(
        "^\\s{0,3}\\[([^]]+)]:\\s*(\\S+)(?:\\s+(?:\"[^\"]*\"|'[^']*'|\\([^)]*\\)))?\\s*$"
    );
    private static final Pattern LINK_REF_FULL_PATTERN = Pattern.compile("\\[([^]]+)]\\[([^]]*)]");
    private static final Pattern LINK_REF_SHORT_PATTERN = Pattern.compile("\\[([^]\\[]+)](?![\\[(])");

    // ── 扩展语法模式定义 ────────────────────────────────────────────────

    private static final Pattern EXTENSION_COLON_OPEN_PATTERN = Pattern.compile(
        "^:::\\s+((?:[a-z0-9_.-]+:)?[a-z0-9_./-]+)(?:\\s+(.*))?$",
        Pattern.CASE_INSENSITIVE
    );
    private static final Pattern EXTENSION_TAG_OPEN_PATTERN = Pattern.compile(
        "^<\\s*((?:[a-z0-9_.-]+:)?[a-z0-9_./-]+)(?:\\s+([^>]*?))?\\s*(/?)>\\s*$",
        Pattern.CASE_INSENSITIVE
    );

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

    /**
     * 将 Markdown 文本解析为组件列表。
     *
     * <p>解析流程：
     * <ol>
     *   <li>规范化换行符</li>
     *   <li>收集和展开引用链接定义</li>
     *   <li>按行扫描，识别块级元素</li>
     *   <li>缓冲区刷新为对应组件</li>
     * </ol>
     * </p>
     *
     * @param markdown 原始 Markdown 文本
     * @return 按渲染顺序排列的组件列表
     */
    public List<MDComponent> parse(ResourceLocation sourceLocation, String markdown) {
        return this.parseDocument(sourceLocation, markdown).components();
    }

    /**
     * 将 Markdown 文本解析为文档模型，并携带文档来源位置。
     */
    public MDDocument parseDocument(ResourceLocation sourceLocation, String markdown) {
        String normalized = markdown.replace("\r\n", "\n").replace('\r', '\n');
        FrontMatterParseResult frontMatterParseResult = extractFrontMatter(normalized);
        List<MDComponent> components = this.parseComponents(sourceLocation, frontMatterParseResult.body());
        return new MDDocument(sourceLocation, frontMatterParseResult.frontMatter(), components);
    }

    private List<MDComponent> parseComponents(ResourceLocation sourceLocation, String markdownBody) {
        String normalized = markdownBody;
        String[] split = normalized.split("\n", -1);

        // 第一遍：收集引用链接定义
        Map<String, String> linkRefs = collectLinkRefs(split);

        // 第二遍：展开引用链接，但保留定义行原样交给后续块级解析跳过
        if (!linkRefs.isEmpty()) {
            normalized = expandLinkRefs(split, linkRefs);
            split = normalized.split("\n", -1);
        }

        List<MDComponent> components = new ArrayList<>();
        StringBuilder paragraphBuilder = new StringBuilder();
        List<MDQuoteComponent.QuoteLine> quoteLines = new ArrayList<>();
        List<MDListComponent.ListItem> listItems = new ArrayList<>();
        List<String> tableRows = new ArrayList<>();
        StringBuilder codeBlockBuilder = new StringBuilder();
        StringBuilder indentedCodeBuilder = new StringBuilder();
        String codeFence = null;
        String codeFenceAll = null;
        boolean inIndentedCode = false;
        BlockExtensionState extensionBlock = null;

        for (String s : split) {
            // ── 扩展块内部 ──────────────────────────────────────────
            if (extensionBlock != null) {
                if (extensionBlock.matchesCloseLine(s)) {
                    flushAll(components, paragraphBuilder, quoteLines, listItems, tableRows, indentedCodeBuilder);
                    String rawContent = extensionBlock.rawContent();
                    List<MDComponent> renderedContent = this.parse(sourceLocation, rawContent);
                    MDComponent extensionComponent = createExtensionComponent(sourceLocation, extensionBlock, renderedContent, rawContent);
                    if (extensionComponent != null) {
                        components.add(extensionComponent);
                    } else {
                        components.addAll(renderedContent);
                    }
                    extensionBlock = null;
                } else {
                    extensionBlock.appendLine(s);
                }
                continue;
            }

            // ── 围栏代码块内部 ──────────────────────────────────────
            if (codeFence != null) {
                Matcher closeMatcher = CODE_FENCE_PATTERN.matcher(s.trim());
                if (closeMatcher.matches()
                    && closeMatcher.group(1).charAt(0) == codeFence.charAt(0)
                    && closeMatcher.group(1).length() >= codeFence.length()) {
                    if (!codeBlockBuilder.isEmpty()) {
                        codeBlockBuilder.deleteCharAt(codeBlockBuilder.length() - 1);
                    }
                    components.add(new MDCodeBlockComponent(codeBlockBuilder.toString(), codeFenceAll.substring(codeFence.length())));
                    codeFence = null;
                    codeFenceAll = null;
                    codeBlockBuilder = new StringBuilder();
                } else {
                    codeBlockBuilder.append(s).append("\n");
                }
                continue;
            }

            // ── 自闭合扩展语法 ──────────────────────────────────────
            MDComponent selfClosingExtension = trySelfClosingExtensionBlock(sourceLocation, s);
            if (selfClosingExtension != null) {
                flushAll(components, paragraphBuilder, quoteLines, listItems, tableRows, indentedCodeBuilder);
                components.add(selfClosingExtension);
                continue;
            }

            // ── 块级扩展语法 ────────────────────────────────────────
            BlockExtensionState openExtension = tryOpenExtensionBlock(s);
            if (openExtension != null) {
                flushAll(components, paragraphBuilder, quoteLines, listItems, tableRows, indentedCodeBuilder);
                extensionBlock = openExtension;
                continue;
            }

            // ── 围栏代码块开启 ──────────────────────────────────────
            Matcher fenceMatcher = CODE_FENCE_PATTERN.matcher(s.trim());
            if (fenceMatcher.matches()) {
                flushAll(components, paragraphBuilder, quoteLines, listItems, tableRows, indentedCodeBuilder);
                inIndentedCode = false;
                codeFence = fenceMatcher.group(1);
                codeFenceAll = s.trim();
                continue;
            }

            // ── 缩进代码块 ──────────────────────────────────────────
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

            // ── 跳过引用链接定义行 ──────────────────────────────────
            if (LINK_REF_DEF_PATTERN.matcher(s).matches()) {
                continue;
            }

            // ── 块引用 ──────────────────────────────────────────────
            Matcher quoteMatcher = BLOCKQUOTE_PATTERN.matcher(s);
            if (quoteMatcher.matches()) {
                flushParagraphComponent(components, paragraphBuilder);
                flushListComponent(components, listItems);
                flushTableComponent(components, tableRows);
                int rawLevel = countQuoteLevel(quoteMatcher.group(1));
                int normalizedLevel = normalizeRelativeLevel(rawLevel, quoteLines, MDQuoteComponent.QuoteLine::level);
                quoteLines.add(new MDQuoteComponent.QuoteLine(normalizedLevel, quoteMatcher.group(2)));
                continue;
            }

            // ── 表格行 ──────────────────────────────────────────────
            if (TABLE_ROW_PATTERN.matcher(s).matches()) {
                flushParagraphComponent(components, paragraphBuilder);
                flushQuoteComponent(components, quoteLines);
                flushListComponent(components, listItems);
                tableRows.add(s);
                continue;
            }

            // ── 任务列表 ────────────────────────────────────────────
            Matcher taskMatcher = TASK_LIST_PATTERN.matcher(s);
            if (taskMatcher.matches()) {
                flushParagraphComponent(components, paragraphBuilder);
                flushQuoteComponent(components, quoteLines);
                flushTableComponent(components, tableRows);
                int rawLevel = countIndentLevel(taskMatcher.group(1));
                int normalizedLevel = normalizeRelativeLevel(rawLevel, listItems, MDListComponent.ListItem::level);
                listItems.add(MDListComponent.task(
                    normalizedLevel,
                    taskMatcher.group(2).equalsIgnoreCase("x"),
                    taskMatcher.group(3)
                ));
                continue;
            }

            // ── 无序列表 ────────────────────────────────────────────
            Matcher unorderedMatcher = UNORDERED_LIST_PATTERN.matcher(s);
            if (unorderedMatcher.matches()) {
                flushParagraphComponent(components, paragraphBuilder);
                flushQuoteComponent(components, quoteLines);
                flushTableComponent(components, tableRows);
                int rawLevel = countIndentLevel(unorderedMatcher.group(1));
                int normalizedLevel = normalizeRelativeLevel(rawLevel, listItems, MDListComponent.ListItem::level);
                listItems.add(MDListComponent.unordered(normalizedLevel, unorderedMatcher.group(2)));
                continue;
            }

            // ── 有序列表 ────────────────────────────────────────────
            Matcher orderedMatcher = ORDERED_LIST_PATTERN.matcher(s);
            if (orderedMatcher.matches()) {
                flushParagraphComponent(components, paragraphBuilder);
                flushQuoteComponent(components, quoteLines);
                flushTableComponent(components, tableRows);
                int rawLevel = countIndentLevel(orderedMatcher.group(1));
                int normalizedLevel = normalizeRelativeLevel(rawLevel, listItems, MDListComponent.ListItem::level);
                listItems.add(MDListComponent.ordered(
                    normalizedLevel,
                    Integer.parseInt(orderedMatcher.group(2)),
                    orderedMatcher.group(3)
                ));
                continue;
            }

            // ── Setext 标题（在水平线检查前） ──────────────────────
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

            // ── 水平线 ──────────────────────────────────────────────
            if (HORIZONTAL_RULE_PATTERN.matcher(s).matches()) {
                flushAll(components, paragraphBuilder, quoteLines, listItems, tableRows, indentedCodeBuilder);
                components.add(new MDHorizontalRuleComponent());
                continue;
            }

            // ── ATX 标题 / 图片 / 注册的行级解析器 ─────────────────
            MDComponent component = this.parseComponent(sourceLocation, s);
            if (component == null) {
                if (s.isBlank()) {
                    flushAll(components, paragraphBuilder, quoteLines, listItems, tableRows, indentedCodeBuilder);
                } else {
                    paragraphBuilder.append(s).append("\n");
                }
            } else {
                flushAll(components, paragraphBuilder, quoteLines, listItems, tableRows, indentedCodeBuilder);
                components.add(component);
            }
        }

        // 刷新未闭合的围栏代码块
        if (codeFence != null) {
            if (!codeBlockBuilder.isEmpty()) codeBlockBuilder.deleteCharAt(codeBlockBuilder.length() - 1);
            components.add(new MDCodeBlockComponent(codeBlockBuilder.toString()));
        }

        // 未闭合扩展块按忽略处理

        flushAll(components, paragraphBuilder, quoteLines, listItems, tableRows, indentedCodeBuilder);
        return components;
    }

    private static FrontMatterParseResult extractFrontMatter(String markdown) {
        String[] lines = markdown.split("\n", -1);
        if (lines.length == 0 || !"---".equals(lines[0].trim())) {
            return new FrontMatterParseResult(Map.of(), markdown);
        }

        int closeIndex = -1;
        for (int i = 1; i < lines.length; i++) {
            if ("---".equals(lines[i].trim())) {
                closeIndex = i;
                break;
            }
        }
        if (closeIndex < 0) {
            return new FrontMatterParseResult(Map.of(), markdown);
        }

        List<String> yamlLines = new ArrayList<>(Math.max(0, closeIndex - 1));
        yamlLines.addAll(Arrays.asList(lines).subList(1, closeIndex));

        StringBuilder bodyBuilder = new StringBuilder();
        for (int i = closeIndex + 1; i < lines.length; i++) {
            if (!bodyBuilder.isEmpty()) {
                bodyBuilder.append('\n');
            }
            bodyBuilder.append(lines[i]);
        }

        Map<String, Object> frontMatter = parseFrontMatterYaml(yamlLines);
        return new FrontMatterParseResult(frontMatter, bodyBuilder.toString());
    }

    private static Map<String, Object> parseFrontMatterYaml(List<String> yamlLines) {
        @SuppressWarnings("MismatchedQueryAndUpdateOfCollection")
        LinkedHashMap<String, Object> root = new LinkedHashMap<>();
        List<MapLevel> levels = new ArrayList<>();
        levels.add(new MapLevel(0, root));

        for (String line : yamlLines) {
            if (line == null || line.isBlank()) {
                continue;
            }
            String trimmed = line.trim();
            if (trimmed.startsWith("#")) {
                continue;
            }

            int colon = trimmed.indexOf(':');
            if (colon <= 0) {
                continue;
            }

            int indent = countYamlIndent(line);
            while (levels.size() > 1 && indent < levels.getLast().indent()) {
                levels.removeLast();
            }

            Map<String, Object> current = levels.getLast().map();
            String key = trimmed.substring(0, colon).trim();
            String valuePart = trimmed.substring(colon + 1).trim();
            if (key.isEmpty()) {
                continue;
            }

            if (valuePart.isEmpty()) {
                LinkedHashMap<String, Object> nested = new LinkedHashMap<>();
                current.put(key, nested);
                levels.add(new MapLevel(indent + 1, nested));
            } else {
                current.put(key, parseYamlScalar(valuePart));
            }
        }

        return Map.copyOf(root);
    }

    private static int countYamlIndent(String line) {
        int width = 0;
        for (int i = 0; i < line.length(); i++) {
            char ch = line.charAt(i);
            if (ch == ' ') {
                width++;
                continue;
            }
            if (ch == '\t') {
                width += 2;
                continue;
            }
            break;
        }
        return width;
    }

    private static @Nullable Object parseYamlScalar(String valuePart) {
        String value = valuePart.trim();
        if ((value.startsWith("\"") && value.endsWith("\"")) || (value.startsWith("'") && value.endsWith("'"))) {
            return value.substring(1, value.length() - 1);
        }
        if ("true".equalsIgnoreCase(value)) {
            return Boolean.TRUE;
        }
        if ("false".equalsIgnoreCase(value)) {
            return Boolean.FALSE;
        }
        if ("null".equalsIgnoreCase(value) || "~".equals(value)) {
            return null;
        }
        try {
            if (value.contains(".") || value.contains("e") || value.contains("E")) {
                return Double.parseDouble(value);
            }
            return Long.parseLong(value);
        } catch (NumberFormatException ignored) {
            return value;
        }
    }

    /**
     * 创建扩展组件。
     */
    private static @Nullable MDComponent createExtensionComponent(
        ResourceLocation sourceLocation,
        BlockExtensionState block,
        List<MDComponent> renderedContent,
        String rawContent
    ) {
        return getMdComponent(sourceLocation, renderedContent, rawContent, block);
    }

    /**
     * 创建自闭合扩展组件。
     */
    @SuppressWarnings("SameParameterValue")
    private static @Nullable MDComponent createExtensionComponent(
        ResourceLocation sourceLocation,
        SelfClosingBlockExtensionState block,
        List<MDComponent> renderedContent,
        String rawContent
    ) {
        return getMdComponent(sourceLocation, renderedContent, rawContent, block);
    }


    private static @Nullable MDComponent getMdComponent(
        ResourceLocation sourceLocation,
        List<MDComponent> renderedContent,
        String rawContent,
        SelfClosingBlockExtensionState block
    ) {
        Registry<MDExtensionComponentFactory> registry = AgeratumRegistries.EXTENSION_COMPONENT_FACTORY_REGISTRY;
        MDExtensionComponentFactory factory = registry.getOptional(block.id()).orElse(null);
        if (factory == null) {
            return null;
        }
        return factory.create(new MDExtensionContext(
            sourceLocation,
            block.id(),
            block.rawParams(),
            block.params(),
            List.copyOf(renderedContent),
            rawContent
        ));
    }

    /**
     * 尝试打开块级扩展。
     */
    private static @Nullable BlockExtensionState tryOpenExtensionBlock(String line) {
        String trimmed = line.trim();

        if (containsInlineClosingTag(trimmed)) {
            return null;
        }

        // 尝试冒号语法
        Matcher colonMatcher = EXTENSION_COLON_OPEN_PATTERN.matcher(trimmed);
        if (colonMatcher.matches()) {
            ResourceLocation id = parseExtensionId(colonMatcher.group(1));
            if (id == null) {
                return null;
            }
            String rawParams = colonMatcher.group(2) == null ? "" : colonMatcher.group(2).trim();
            return BlockExtensionState.colon(id, rawParams);
        }

        // 尝试标签语法
        Matcher tagMatcher = EXTENSION_TAG_OPEN_PATTERN.matcher(trimmed);
        if (tagMatcher.matches()) {
            if (isReservedInlineTag(tagMatcher.group(1))) {
                return null;
            }
            ResourceLocation id = parseExtensionId(tagMatcher.group(1));
            if (id == null) {
                return null;
            }
            // 自闭合标签由 trySelfClosingExtensionBlock 处理
            if ("/".equals(tagMatcher.group(3))) {
                return null;
            }
            String rawParams = tagMatcher.group(2) == null ? "" : tagMatcher.group(2).trim();
            return BlockExtensionState.tag(id, rawParams, ExtensionParamParser.parse(rawParams));
        }

        return null;
    }

    /**
     * 尝试解析自闭合扩展。
     */
    private static @Nullable MDComponent trySelfClosingExtensionBlock(ResourceLocation sourceLocation, String line) {
        String trimmed = line.trim();
        if (containsInlineClosingTag(trimmed)) {
            return null;
        }
        Matcher tagMatcher = EXTENSION_TAG_OPEN_PATTERN.matcher(trimmed);

        if (tagMatcher.matches()) {
            if (isReservedInlineTag(tagMatcher.group(1))) {
                return null;
            }
            ResourceLocation id = parseExtensionId(tagMatcher.group(1));
            if (id == null || !"/".equals(tagMatcher.group(3))) {
                return null;
            }
            String rawParams = tagMatcher.group(2) == null ? "" : tagMatcher.group(2).trim();
            Map<String, String> params = ExtensionParamParser.parse(rawParams);
            return createExtensionComponent(
                sourceLocation,
                new SelfClosingBlockExtensionState(id, rawParams, params),
                List.of(),
                ""
            );
        }

        return null;
    }

    /**
     * 解析扩展组件 ID，支持省略 {@code ageratum:} 前缀。
     */
    private static @Nullable ResourceLocation parseExtensionId(String idText) {
        String normalized = idText.contains(":") ? idText : "ageratum:" + idText;
        try {
            return ResourceLocation.parse(normalized);
        } catch (RuntimeException exception) {
            return null;
        }
    }

    /**
     * 判断该行是否已经包含了内联闭合标签；若包含，则不应按块级扩展处理。
     */
    private static boolean containsInlineClosingTag(String trimmed) {
        return trimmed.contains("</");
    }

    /**
     * 保留给内联样式系统的标签名，避免被块级扩展语法抢占。
     */
    private static boolean isReservedInlineTag(String idText) {
        String simpleId = idText.contains(":") ? idText.substring(idText.indexOf(':') + 1) : idText;
        return "hover".equalsIgnoreCase(simpleId)
               || "click".equalsIgnoreCase(simpleId)
               || "color".equalsIgnoreCase(simpleId)
               || "o".equalsIgnoreCase(simpleId);
    }

    // ── 缓冲区刷新辅助方法 ──────────────────────────────────────────────

    /**
     * 刷新所有临时缓冲区为对应组件。
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

    private static void flushParagraphComponent(List<MDComponent> components, StringBuilder builder) {
        if (builder.isEmpty()) return;
        builder.deleteCharAt(builder.length() - 1);
        components.add(new MDTextComponent(builder.toString()));
        builder.setLength(0);
    }

    private static void flushQuoteComponent(List<MDComponent> components, List<MDQuoteComponent.QuoteLine> lines) {
        if (lines.isEmpty()) return;
        components.add(new MDQuoteComponent(lines));
        lines.clear();
    }

    private static void flushListComponent(List<MDComponent> components, List<MDListComponent.ListItem> items) {
        if (items.isEmpty()) return;
        components.add(new MDListComponent(items));
        items.clear();
    }

    private static void flushTableComponent(List<MDComponent> components, List<String> tableRows) {
        if (tableRows.isEmpty()) return;
        components.add(MDTableComponent.parse(tableRows));
        tableRows.clear();
    }

    private static void flushIndentedCodeComponent(List<MDComponent> components, StringBuilder builder) {
        if (builder.isEmpty()) return;
        String content = builder.toString();
        while (content.endsWith("\n\n")) content = content.substring(0, content.length() - 1);
        if (content.endsWith("\n")) content = content.substring(0, content.length() - 1);
        if (!content.isEmpty()) components.add(new MDCodeBlockComponent(content));
        builder.setLength(0);
    }

    // ── 引用链接辅助方法 ────────────────────────────────────────────────

    /**
     * 收集文档中的引用链接定义。
     */
    private static Map<String, String> collectLinkRefs(String[] lines) {
        Map<String, String> refs = new LinkedHashMap<>();
        for (String line : lines) {
            Matcher m = LINK_REF_DEF_PATTERN.matcher(line);
            if (m.matches()) {
                refs.put(m.group(1).toLowerCase(), m.group(2));
            }
        }
        return refs;
    }

    /**
     * 展开引用链接语法为普通内联链接。
     */
    private static String expandLinkRefs(String[] lines, Map<String, String> refs) {
        List<String> expandedLines = new ArrayList<>(lines.length);
        String codeFence = null;
        for (String line : lines) {
            if (LINK_REF_DEF_PATTERN.matcher(line).matches()) {
                expandedLines.add(line);
                continue;
            }

            Matcher fenceMatcher = CODE_FENCE_PATTERN.matcher(line.trim());
            if (codeFence != null) {
                expandedLines.add(line);
                if (fenceMatcher.matches()
                    && fenceMatcher.group(1).charAt(0) == codeFence.charAt(0)
                    && fenceMatcher.group(1).length() >= codeFence.length()) {
                    codeFence = null;
                }
                continue;
            }

            if (fenceMatcher.matches()) {
                codeFence = fenceMatcher.group(1);
                expandedLines.add(line);
                continue;
            }

            expandedLines.add(expandLinkRefsInLine(line, refs));
        }
        return String.join("\n", expandedLines);
    }

    private static String expandLinkRefsInLine(String markdown, Map<String, String> refs) {
        // 替换 [text][id] → [text](url)
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

        // 替换 [id] 快捷引用 → [id](url)
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


    // ── 其他辅助方法 ────────────────────────────────────────────────────

    /**
     * 统计块引用前缀中的 {@code >} 层级。
     */
    private static int countQuoteLevel(String markers) {
        int level = 0;
        for (int i = 0; i < markers.length(); i++) {
            if (markers.charAt(i) == '>') level++;
        }
        return Math.max(1, level);
    }

    /**
     * 统计列表缩进层级。
     */
    private static int countIndentLevel(String indent) {
        int width = 0;
        for (int i = 0; i < indent.length(); i++) {
            char ch = indent.charAt(i);
            if (ch == '\t') {
                width += 2;
            } else if (ch == ' ') width++;
        }
        return Math.max(0, width / 2);
    }

    /**
     * 规范化层级：允许同级/回退，向下最多增加 1 层。
     */
    private static <T> int normalizeRelativeLevel(int rawLevel, List<T> items, Function<T, Integer> levelExtractor) {
        int normalized = Math.max(0, rawLevel);
        if (items.isEmpty()) {
            return normalized;
        }
        int lastIndex = items.size() - 1;
        int previousLevel = Math.max(0, levelExtractor.apply(items.get(lastIndex)));
        return Math.min(normalized, previousLevel + 1);
    }

    /**
     * 使用已注册解析器尝试将一行文本解析为组件。
     */
    public @Nullable MDComponent parseComponent(ResourceLocation sourceLocation, String string) {
        for (MDComponentParserHolder parserHolder : this.mdComponentParserHolders) {
            MDComponent component = parserHolder.parser().apply(sourceLocation, string);
            if (component != null) return component;
        }
        return null;
    }

    /**
     * 行级组件解析函数接口。
     */
    @FunctionalInterface
    public interface MDComponentParser extends BiFunction<ResourceLocation, String, MDComponent> {
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

    private record MapLevel(int indent, Map<String, Object> map) {
        private MapLevel {
            Objects.requireNonNull(map, "map");
        }
    }

    private record FrontMatterParseResult(Map<String, Object> frontMatter, String body) {
    }
}

