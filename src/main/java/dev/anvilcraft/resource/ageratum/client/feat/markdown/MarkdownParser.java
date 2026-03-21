package dev.anvilcraft.resource.ageratum.client.feat.markdown;

import dev.anvilcraft.resource.ageratum.client.feat.markdown.component.MDComponent;
import dev.anvilcraft.resource.ageratum.client.feat.markdown.component.MDCodeBlockComponent;
import dev.anvilcraft.resource.ageratum.client.feat.markdown.component.MDHeaderComponent;
import dev.anvilcraft.resource.ageratum.client.feat.markdown.component.MDHorizontalRuleComponent;
import dev.anvilcraft.resource.ageratum.client.feat.markdown.component.MDImageComponent;
import dev.anvilcraft.resource.ageratum.client.feat.markdown.component.MDListComponent;
import dev.anvilcraft.resource.ageratum.client.feat.markdown.component.MDQuoteComponent;
import dev.anvilcraft.resource.ageratum.client.feat.markdown.component.MDTextComponent;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.Set;
import java.util.TreeSet;
import java.util.function.Function;
import javax.annotation.Nullable;

public class MarkdownParser {
    private static final Pattern ORDERED_LIST_PATTERN = Pattern.compile("^(\\s*)(\\d+)\\.\\s+(.+)$");
    private static final Pattern TASK_LIST_PATTERN = Pattern.compile("^(\\s*)[-+*]\\s+\\[([ xX])]\\s+(.+)$");
    private static final Pattern UNORDERED_LIST_PATTERN = Pattern.compile("^(\\s*)[-+*]\\s+(.+)$");
    private static final Pattern BLOCKQUOTE_PATTERN = Pattern.compile("^\\s*((?:>\\s*)+)(.*)$");
    private static final Pattern HORIZONTAL_RULE_PATTERN = Pattern.compile("^\\s*([-*_])(?:\\s*\\1){2,}\\s*$");
    private final Set<MDComponentParserHolder> mdComponentParserHolders = new TreeSet<>();

    public MarkdownParser() {
        this.registerBaseComponentParser();
    }

    public void registerComponentParser(int priority, MDComponentParser parser) {
        this.mdComponentParserHolders.add(new MDComponentParserHolder(priority, parser));
    }

    private void registerBaseComponentParser() {
        this.registerComponentParser(-10, MDImageComponent::parse);
        this.registerComponentParser(0, MDHeaderComponent::parse);
    }

    public List<MDComponent> parse(String markdown) {
        String[] split = markdown.replace("\r\n", "\n").replace('\r', '\n').split("\n", -1);
        List<MDComponent> components = new ArrayList<>();
        StringBuilder paragraphBuilder = new StringBuilder();
        List<MDQuoteComponent.QuoteLine> quoteLines = new ArrayList<>();
        List<MDListComponent.ListItem> listItems = new ArrayList<>();
        StringBuilder codeBlockBuilder = new StringBuilder();
        boolean inCodeBlock = false;

        for (String s : split) {
            if (inCodeBlock) {
                if (s.trim().startsWith("```")) {
                    inCodeBlock = false;
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

            if (s.trim().startsWith("```")) {
                flushParagraphComponent(components, paragraphBuilder);
                flushQuoteComponent(components, quoteLines);
                flushListComponent(components, listItems);
                inCodeBlock = true;
                continue;
            }

            Matcher quoteMatcher = BLOCKQUOTE_PATTERN.matcher(s);
            if (quoteMatcher.matches()) {
                flushParagraphComponent(components, paragraphBuilder);
                flushListComponent(components, listItems);
                int quoteLevel = countQuoteLevel(quoteMatcher.group(1));
                quoteLines.add(new MDQuoteComponent.QuoteLine(quoteLevel, quoteMatcher.group(2)));
                continue;
            }

            Matcher taskListMatcher = TASK_LIST_PATTERN.matcher(s);
            if (taskListMatcher.matches()) {
                flushParagraphComponent(components, paragraphBuilder);
                flushQuoteComponent(components, quoteLines);
                int level = countIndentLevel(taskListMatcher.group(1));
                boolean checked = taskListMatcher.group(2).equalsIgnoreCase("x");
                listItems.add(MDListComponent.task(level, checked, taskListMatcher.group(3)));
                continue;
            }

            Matcher unorderedListMatcher = UNORDERED_LIST_PATTERN.matcher(s);
            if (unorderedListMatcher.matches()) {
                flushParagraphComponent(components, paragraphBuilder);
                flushQuoteComponent(components, quoteLines);
                int level = countIndentLevel(unorderedListMatcher.group(1));
                listItems.add(MDListComponent.unordered(level, unorderedListMatcher.group(2)));
                continue;
            }

            Matcher orderedListMatcher = ORDERED_LIST_PATTERN.matcher(s);
            if (orderedListMatcher.matches()) {
                flushParagraphComponent(components, paragraphBuilder);
                flushQuoteComponent(components, quoteLines);
                int level = countIndentLevel(orderedListMatcher.group(1));
                int orderNumber = Integer.parseInt(orderedListMatcher.group(2));
                listItems.add(MDListComponent.ordered(level, orderNumber, orderedListMatcher.group(3)));
                continue;
            }

            if (HORIZONTAL_RULE_PATTERN.matcher(s).matches()) {
                flushParagraphComponent(components, paragraphBuilder);
                flushQuoteComponent(components, quoteLines);
                flushListComponent(components, listItems);
                components.add(new MDHorizontalRuleComponent());
                continue;
            }

            MDComponent component = this.parseComponent(s);
            if (component == null) {
                if (s.isBlank()) {
                    flushParagraphComponent(components, paragraphBuilder);
                    flushQuoteComponent(components, quoteLines);
                    flushListComponent(components, listItems);
                } else {
                    paragraphBuilder.append(s).append("\n");
                }
            } else {
                flushParagraphComponent(components, paragraphBuilder);
                flushQuoteComponent(components, quoteLines);
                flushListComponent(components, listItems);
                components.add(component);
            }
        }

        if (inCodeBlock) {
            if (!codeBlockBuilder.isEmpty()) {
                codeBlockBuilder.deleteCharAt(codeBlockBuilder.length() - 1);
            }
            components.add(new MDCodeBlockComponent(codeBlockBuilder.toString()));
        }

        flushParagraphComponent(components, paragraphBuilder);
        flushQuoteComponent(components, quoteLines);
        flushListComponent(components, listItems);

        return components;
    }

    private static void flushParagraphComponent(List<MDComponent> components, StringBuilder builder) {
        if (builder.isEmpty()) {
            return;
        }
        builder.deleteCharAt(builder.length() - 1);
        components.add(new MDTextComponent(builder.toString()));
        builder.setLength(0);
    }

    private static void flushQuoteComponent(List<MDComponent> components, List<MDQuoteComponent.QuoteLine> lines) {
        if (lines.isEmpty()) {
            return;
        }
        components.add(new MDQuoteComponent(lines));
        lines.clear();
    }

    private static void flushListComponent(List<MDComponent> components, List<MDListComponent.ListItem> items) {
        if (items.isEmpty()) {
            return;
        }
        components.add(new MDListComponent(items));
        items.clear();
    }

    private static int countQuoteLevel(String markers) {
        int level = 0;
        for (int i = 0; i < markers.length(); i++) {
            if (markers.charAt(i) == '>') {
                level++;
            }
        }
        return Math.max(1, level);
    }

    private static int countIndentLevel(String indent) {
        int width = 0;
        for (int i = 0; i < indent.length(); i++) {
            char ch = indent.charAt(i);
            if (ch == '\t') {
                width += 2;
            } else if (ch == ' ') {
                width += 1;
            }
        }
        return Math.max(0, width / 2);
    }

    public @Nullable MDComponent parseComponent(String string) {
        for (MDComponentParserHolder parserHolder : this.mdComponentParserHolders) {
            MDComponent component = parserHolder.parser().apply(string);
            if (component != null) {
                return component;
            }
        }
        return null;
    }

    @FunctionalInterface
    public interface MDComponentParser extends Function<String, MDComponent> {
    }

    private record MDComponentParserHolder(int priority, MDComponentParser parser) implements Comparable<MDComponentParserHolder> {
        @Override
        public int compareTo(MDComponentParserHolder holder) {
            if (this.equals(holder)) return 0;
            if (this.priority() >= holder.priority()) return 1;
            return -1;
        }
    }
}
