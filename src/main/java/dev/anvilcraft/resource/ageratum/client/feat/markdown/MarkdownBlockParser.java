package dev.anvilcraft.resource.ageratum.client.feat.markdown;

import dev.anvilcraft.resource.ageratum.client.constants.AgeratumConstants.Patterns;
import dev.anvilcraft.resource.ageratum.client.feat.markdown.component.InlineMarkdownParser;
import dev.anvilcraft.resource.ageratum.client.feat.markdown.component.MDCodeBlockComponent;
import dev.anvilcraft.resource.ageratum.client.feat.markdown.component.MDComponent;
import dev.anvilcraft.resource.ageratum.client.feat.markdown.component.MDHeaderComponent;
import dev.anvilcraft.resource.ageratum.client.feat.markdown.component.MDHorizontalRuleComponent;
import dev.anvilcraft.resource.ageratum.client.feat.markdown.component.MDListComponent;
import dev.anvilcraft.resource.ageratum.client.feat.markdown.component.MDQuoteComponent;
import dev.anvilcraft.resource.ageratum.client.feat.markdown.component.MDTableComponent;
import dev.anvilcraft.resource.ageratum.client.feat.markdown.component.MDTextComponent;
import dev.anvilcraft.resource.ageratum.client.registries.AgeratumRegistries;
import net.minecraft.resources.ResourceLocation;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import javax.annotation.Nullable;

/** Consumes complete blocks in source order. Nested extension bodies share the source and its link definitions. */
final class MarkdownBlockParser {
    private final ResourceLocation location;
    private final MarkdownParser parser;
    private final MarkdownBlockSource source;

    MarkdownBlockParser(ResourceLocation location, MarkdownParser parser, String body) {
        this.location = location;
        this.parser = parser;
        this.source = new MarkdownBlockSource(body, MarkdownBlockParser::isInlineTag);
    }

    List<MDComponent> parse() {
        return new Cursor(0, source.lines.size()).read();
    }

    private static boolean isInlineTag(ResourceLocation id) {
        return switch (id.getPath()) {
            case "hover", "click", "color", "o", "gradient" -> true;
            default -> AgeratumRegistries.INLINE_COMPONENT_FACTORY_REGISTRY.getOptional(id).isPresent();
        };
    }

    private String inline(String text) {
        return InlineMarkdownParser.expandReferences(text, source.references);
    }

    private final class Cursor {
        private int index;
        private final int end;
        private final List<MDComponent> components = new ArrayList<>();
        private final List<String> paragraph = new ArrayList<>();

        Cursor(int start, int end) {
            this.index = start;
            this.end = end;
        }

        List<MDComponent> read() {
            while (index < end) {
                String line = source.lines.get(index);
                MarkdownBlockSource.Fence fence = source.fence(index);
                MarkdownBlockSource.Extension extension = source.extension(index);
                if (fence != null) {
                    emit(new MDCodeBlockComponent(source.text(index + 1, fence.close()), fence.info()));
                    index = Math.min(end, fence.close() + 1);
                } else if (extension != null) {
                    readExtension(extension);
                } else if (source.isDefinition(index)) {
                    index++;
                } else if (line.isBlank()) {
                    flushParagraph();
                    index++;
                } else if (paragraph.isEmpty() && Patterns.INDENTED_CODE_PATTERN.matcher(line).matches()) {
                    readIndentedCode();
                } else if (Patterns.BLOCKQUOTE_PATTERN.matcher(line).matches()) {
                    readQuote();
                } else if (Patterns.TABLE_ROW_PATTERN.matcher(line).matches()) {
                    readTable();
                } else {
                    MDListComponent.ListItem item = listItem(line, -1);
                    if (item != null) readList(item);
                    else readLine(line);
                }
            }
            flushParagraph();
            return components;
        }

        private void readExtension(MarkdownBlockSource.Extension range) {
            boolean standalone = range.close() == index;
            SelfClosingBlockExtensionState block = range.block();
            List<MDComponent> children = standalone ? List.of() : new Cursor(index + 1, range.close()).read();
            MDExtensionComponentFactory factory = AgeratumRegistries.EXTENSION_COMPONENT_FACTORY_REGISTRY
                .getOptional(block.id()).orElse(null);
            MDComponent component = factory == null ? null : factory.create(new MDExtensionContext(
                location, block.id(), block.rawParams(), block.params(), List.copyOf(children),
                standalone ? "" : source.text(index + 1, range.close())
            ));
            if (component != null) {
                emit(component);
            } else if (standalone) {
                readLine(source.lines.get(index));
                return;
            } else {
                flushParagraph();
                components.addAll(children);
            }
            index = range.close() + 1;
        }

        private void readIndentedCode() {
            List<String> code = new ArrayList<>();
            while (index < end) {
                String line = source.lines.get(index);
                Matcher match = Patterns.INDENTED_CODE_PATTERN.matcher(line);
                if (match.matches()) code.add(match.group(1));
                else if (line.isBlank()) code.add("");
                else break;
                index++;
            }
            while (!code.isEmpty() && code.getLast().isEmpty()) code.removeLast();
            if (!code.isEmpty()) emit(new MDCodeBlockComponent(String.join("\n", code)));
        }

        private void readQuote() {
            List<MDQuoteComponent.QuoteLine> quotes = new ArrayList<>();
            while (index < end) {
                Matcher match = Patterns.BLOCKQUOTE_PATTERN.matcher(source.lines.get(index));
                if (!match.matches()) break;
                int depth = (int) match.group(1).chars().filter(ch -> ch == '>').count();
                int level = quotes.isEmpty() ? depth : Math.min(depth, quotes.getLast().level() + 1);
                quotes.add(new MDQuoteComponent.QuoteLine(level, inline(match.group(2))));
                index++;
            }
            emit(new MDQuoteComponent(quotes));
        }

        private void readTable() {
            List<String> rows = new ArrayList<>();
            while (index < end && Patterns.TABLE_ROW_PATTERN.matcher(source.lines.get(index)).matches()) {
                rows.add(inline(source.lines.get(index++)));
            }
            emit(MDTableComponent.parse(rows));
        }

        private void readList(MDListComponent.ListItem first) {
            List<MDListComponent.ListItem> items = new ArrayList<>();
            items.add(first);
            index++;
            while (index < end) {
                MDListComponent.ListItem next = listItem(source.lines.get(index), items.getLast().level());
                if (next == null) break;
                items.add(next);
                index++;
            }
            emit(new MDListComponent(items));
        }

        private void readLine(String line) {
            index++;
            int heading = Patterns.SETEXT_H1_PATTERN.matcher(line).matches() ? 1
                : Patterns.SETEXT_H2_PATTERN.matcher(line).matches() ? 2 : 0;
            if (heading > 0 && !paragraph.isEmpty()) {
                String title = paragraph.removeLast();
                emit(new MDHeaderComponent(heading, inline(title)));
            } else if (Patterns.HORIZONTAL_RULE_PATTERN.matcher(line).matches()) {
                emit(new MDHorizontalRuleComponent());
            } else {
                MDComponent component = parser.parseLine(location, line, source.references);
                if (component == null) paragraph.add(line);
                else emit(component);
            }
        }

        private void emit(MDComponent component) {
            flushParagraph();
            components.add(component);
        }

        private void flushParagraph() {
            if (!paragraph.isEmpty()) {
                components.add(new MDTextComponent(inline(String.join("\n", paragraph))));
                paragraph.clear();
            }
        }
    }

    private @Nullable MDListComponent.ListItem listItem(String line, int previousLevel) {
        Matcher task = Patterns.TASK_LIST_PATTERN.matcher(line);
        if (task.matches()) {
            return MDListComponent.task(listLevel(task.group(1), previousLevel), task.group(2).equalsIgnoreCase("x"), inline(task.group(3)));
        }
        Matcher bullet = Patterns.UNORDERED_LIST_PATTERN.matcher(line);
        if (bullet.matches()) {
            return MDListComponent.unordered(listLevel(bullet.group(1), previousLevel), inline(bullet.group(2)));
        }
        Matcher number = Patterns.ORDERED_LIST_PATTERN.matcher(line);
        if (number.matches()) {
            try {
                return MDListComponent.ordered(listLevel(number.group(1), previousLevel),
                    Integer.parseInt(number.group(2)), inline(number.group(3)));
            } catch (NumberFormatException ignored) {
                // A number outside the supported list range is ordinary text.
            }
        }
        return null;
    }

    private static int listLevel(String indent, int previousLevel) {
        int raw = indent.chars().map(ch -> ch == '\t' ? 2 : 1).sum() / 2;
        return previousLevel < 0 ? raw : Math.min(previousLevel + 1, raw);
    }
}
