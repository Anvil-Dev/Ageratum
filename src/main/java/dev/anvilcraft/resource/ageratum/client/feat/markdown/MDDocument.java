package dev.anvilcraft.resource.ageratum.client.feat.markdown;

import dev.anvilcraft.resource.ageratum.client.feat.markdown.component.MDComponent;
import dev.anvilcraft.resource.ageratum.client.feat.markdown.component.MDHeaderComponent;
import net.minecraft.resources.ResourceLocation;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import javax.annotation.Nullable;

/**
 * Markdown 文档模型，包含 front matter 与渲染组件。
 */
public record MDDocument(Map<String, Object> frontMatter, List<MDComponent> components, @Nullable ResourceLocation sourceLocation) {
    public MDDocument(Map<String, Object> frontMatter, List<MDComponent> components) {
        this(frontMatter, components, null);
    }

    public MDDocument {
        frontMatter = freezeMap(frontMatter);
        components = List.copyOf(components);
    }

    private static Map<String, Object> freezeMap(Map<String, Object> source) {
        LinkedHashMap<String, Object> copy = new LinkedHashMap<>();
        for (Map.Entry<String, Object> entry : source.entrySet()) {
            Object value = entry.getValue();
            if (value instanceof Map<?, ?> mapValue) {
                LinkedHashMap<String, Object> nested = new LinkedHashMap<>();
                for (Map.Entry<?, ?> nestedEntry : mapValue.entrySet()) {
                    Object nestedKey = nestedEntry.getKey();
                    if (nestedKey != null) {
                        nested.put(String.valueOf(nestedKey), nestedEntry.getValue());
                    }
                }
                copy.put(entry.getKey(), freezeMap(nested));
            } else {
                copy.put(entry.getKey(), value);
            }
        }
        return Map.copyOf(copy);
    }

    /**
     * 获取文档标题，按 front matter -> 顶部一级标题 -> 文件名 回退。
     */
    public String getTitle(String fileName) {
        String fallbackFileName = fileName.isBlank() ? this.getSourceFileName().orElse("") : fileName;
        return this.resolveFrontMatterTitle()
            .or(this::resolveTopHeadingTitle)
            .orElseGet(() -> titleFromFileName(fallbackFileName));
    }

    /**
     * 获取文档标题；无文件名上下文时，最后回退为 "Untitled"。
     */
    public String getTitle() {
        return this.getTitle("");
    }

    /**
     * 获取文档来源文件名（仅文件名，不含目录）。
     */
    public Optional<String> getSourceFileName() {
        if (this.sourceLocation == null) {
            return Optional.empty();
        }
        String path = this.sourceLocation.getPath().replace('\\', '/');
        int slashIndex = path.lastIndexOf('/');
        String fileName = slashIndex >= 0 ? path.substring(slashIndex + 1) : path;
        if (fileName.isBlank()) {
            return Optional.empty();
        }
        return Optional.of(fileName);
    }

    private Optional<String> resolveFrontMatterTitle() {
        Object navigation = this.frontMatter.get("navigation");
        if (navigation instanceof Map<?, ?> navigationMap) {
            String navigationTitle = stringValue(navigationMap.get("title"));
            if (navigationTitle != null) {
                return Optional.of(navigationTitle);
            }
        }

        String title = stringValue(this.frontMatter.get("title"));
        return title == null ? Optional.empty() : Optional.of(title);
    }

    private Optional<String> resolveTopHeadingTitle() {
        for (MDComponent component : this.components) {
            if (component instanceof MDHeaderComponent header && header.getLevel() == 1) {
                String heading = component.getText().getString().trim();
                if (!heading.isEmpty()) {
                    return Optional.of(heading);
                }
            }
        }
        return Optional.empty();
    }

    private static String titleFromFileName(String fileName) {
        String normalized = fileName.trim().replace('\\', '/');
        int slashIndex = normalized.lastIndexOf('/');
        if (slashIndex >= 0) {
            normalized = normalized.substring(slashIndex + 1);
        }
        if (normalized.toLowerCase(Locale.ROOT).endsWith(".md")) {
            normalized = normalized.substring(0, normalized.length() - 3);
        }
        return normalized.isBlank() ? "Untitled" : normalized;
    }

    @Nullable
    private static String stringValue(@Nullable Object value) {
        if (value instanceof String string) {
            String trimmed = string.trim();
            return trimmed.isEmpty() ? null : trimmed;
        }
        return null;
    }
}

