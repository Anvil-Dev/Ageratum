package dev.anvilcraft.resource.ageratum.client.feat.markdown;

import dev.anvilcraft.resource.ageratum.client.feat.markdown.component.InlineMarkdownParser;
import dev.anvilcraft.resource.ageratum.client.feat.markdown.component.MDComponent;
import dev.anvilcraft.resource.ageratum.client.feat.markdown.component.MDHeaderComponent;
import dev.anvilcraft.resource.ageratum.client.feat.markdown.component.MDImageComponent;
import dev.anvilcraft.resource.ageratum.client.feat.markdown.component.extend.MDDirectoryComponent;
import dev.anvilcraft.resource.ageratum.client.feat.markdown.component.extend.MDLatexComponent;
import net.minecraft.resources.ResourceLocation;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.function.BiFunction;
import javax.annotation.Nullable;

/** Parses guide Markdown and dispatches registered single-line components in priority order. */
public class MarkdownParser {
    private final List<LineRule> rules = new ArrayList<>();

    public MarkdownParser() {
        registerComponentParser(-10, MDImageComponent::parse);
        registerComponentParser(-9, (location, text) -> MDLatexComponent.parseLegacyLine(text));
        registerComponentParser(0, MDHeaderComponent::parse);
    }

    /** Lower priorities run first. Rules with equal priority run in registration order. */
    public void registerComponentParser(int priority, MDComponentParser parser) {
        LineRule rule = new LineRule(priority, parser);
        if (!rules.contains(rule)) {
            rules.add(rule);
            rules.sort(Comparator.comparingInt(LineRule::priority));
        }
    }

    public List<MDComponent> parse(ResourceLocation sourceLocation, String markdown) {
        return parseDocument(sourceLocation, markdown).components();
    }

    public MDDocument parseDocument(ResourceLocation sourceLocation, String markdown) {
        return parseBody(sourceLocation, FrontMatter.read(markdown));
    }

    MDDocument parseBody(ResourceLocation location, FrontMatter source) {
        MarkdownBlockParser parser = new MarkdownBlockParser(location, this, source.body());
        return new MDDocument(location, source.values(), MDDirectoryComponent.expand(parser.parse()));
    }

    /** Attempts each registered single-line parser until one accepts the line. */
    public @Nullable MDComponent parseComponent(ResourceLocation sourceLocation, String line) {
        for (LineRule rule : rules) {
            MDComponent component = rule.parser().apply(sourceLocation, line);
            if (component != null) return component;
        }
        return null;
    }

    @Nullable MDComponent parseLine(ResourceLocation location, String line, Map<String, String> references) {
        String start = line.stripLeading();
        if (start.startsWith("#") || start.startsWith("![")) {
            return parseComponent(location, InlineMarkdownParser.expandReferences(line, references));
        }
        return parseComponent(location, line);
    }

    @FunctionalInterface
    public interface MDComponentParser extends BiFunction<ResourceLocation, String, MDComponent> {
    }

    private record LineRule(int priority, MDComponentParser parser) {
    }
}
