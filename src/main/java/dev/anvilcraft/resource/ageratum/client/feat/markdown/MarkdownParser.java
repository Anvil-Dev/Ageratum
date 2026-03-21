package dev.anvilcraft.resource.ageratum.client.feat.markdown;

import dev.anvilcraft.resource.ageratum.client.feat.markdown.component.MDComponent;
import dev.anvilcraft.resource.ageratum.client.feat.markdown.component.MDHeaderComponent;
import dev.anvilcraft.resource.ageratum.client.feat.markdown.component.MDTextComponent;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.function.Function;
import javax.annotation.Nullable;

public class MarkdownParser {
    private final Set<MDComponentParserHolder> mdComponentParserHolders = new TreeSet<>();

    public MarkdownParser() {
        this.registerBaseComponentParser();
    }

    public void registerComponentParser(int priority, MDComponentParser parser) {
        this.mdComponentParserHolders.add(new MDComponentParserHolder(priority, parser));
    }

    private void registerBaseComponentParser() {
        this.registerComponentParser(0, MDHeaderComponent::parse);
    }

    public List<MDComponent> parse(String markdown) {
        String[] split = markdown.split("\n");
        List<MDComponent> components = new ArrayList<>();
        StringBuilder waitStrBuilder = new StringBuilder();
        for (String s : split) {
            MDComponent component = this.parseComponent(s);
            if (component == null) {
                waitStrBuilder.append(s).append("\n");
            } else {
                if (!waitStrBuilder.isEmpty()) {
                    waitStrBuilder.deleteCharAt(waitStrBuilder.length() - 1);
                    components.add(new MDTextComponent(waitStrBuilder.toString()));
                    waitStrBuilder = new StringBuilder();
                }
                components.add(component);
            }
        }
        if (!waitStrBuilder.isEmpty()) {
            waitStrBuilder.deleteCharAt(waitStrBuilder.length() - 1);
            components.add(new MDTextComponent(waitStrBuilder.toString()));
        }
        return components;
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
