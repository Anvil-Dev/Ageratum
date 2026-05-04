package dev.anvilcraft.resource.ageratum.client.feat.markdown;

import dev.anvilcraft.resource.ageratum.client.feat.markdown.component.MDComponent;
import dev.anvilcraft.resource.ageratum.client.feat.markdown.component.MDHeaderComponent;
import net.minecraft.resources.Identifier;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import javax.annotation.Nullable;

/**
 * Markdown 文档模型，包含 front matter 与渲染组件。
 */
public record MDDocument(@Nullable Identifier sourceLocation, Map<String, Object> frontMatter, List<MDComponent> components) {

    public MDDocument {
        frontMatter = freezeMap(frontMatter);
        components = List.copyOf(components);
    }

    private static Map<String, Object> freezeMap(Map<String, Object> source) {
        LinkedHashMap<String, Object> copy = new LinkedHashMap<>();
        for (Map.Entry<String, Object> entry : source.entrySet()) {
            copy.put(entry.getKey(), freezeValue(entry.getValue()));
        }
        return Map.copyOf(copy);
    }

    private static Object freezeValue(@Nullable Object value) {
        if (value instanceof Map<?, ?> mapValue) {
            LinkedHashMap<String, Object> nested = new LinkedHashMap<>();
            for (Map.Entry<?, ?> nestedEntry : mapValue.entrySet()) {
                Object nestedKey = nestedEntry.getKey();
                if (nestedKey != null) {
                    nested.put(String.valueOf(nestedKey), freezeValue(nestedEntry.getValue()));
                }
            }
            return freezeMap(nested);
        }
        if (value instanceof List<?> listValue) {
            List<Object> frozen = new ArrayList<>(listValue.size());
            for (Object element : listValue) {
                frozen.add(freezeValue(element));
            }
            return List.copyOf(frozen);
        }
        return value;
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

    /**
     * 从 Front Matter 中读取绑定物品列表。
     *
     * <p>支持以下字段（按优先级）：</p>
     * <ul>
     *   <li>{@code guide.items}</li>
     *   <li>{@code guide.item}</li>
     *   <li>{@code guide.item_id}（兼容旧字段）</li>
     *   <li>{@code items}</li>
     *   <li>{@code item}</li>
     *   <li>{@code item_id}（兼容旧字段）</li>
     * </ul>
     */
    public List<GuideItemBinding> getGuideItemBindings() {
        List<GuideItemBinding> bindings = new ArrayList<>();
        for (String value : this.resolveGuideItemValues()) {
            GuideItemBinding.parse(value).ifPresent(bindings::add);
        }
        return List.copyOf(bindings);
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

    private List<String> resolveGuideItemValues() {
        Object guide = this.frontMatter.get("guide");
        if (guide instanceof Map<?, ?> guideMap) {
            List<String> nestedItems = stringValues(guideMap.get("items"));
            if (!nestedItems.isEmpty()) {
                return nestedItems;
            }
            String nestedItem = stringValue(guideMap.get("item"));
            if (nestedItem != null) {
                return List.of(nestedItem);
            }
            String nestedItemId = stringValue(guideMap.get("item_id"));
            if (nestedItemId != null) {
                return List.of(nestedItemId);
            }
        }

        List<String> items = stringValues(this.frontMatter.get("items"));
        if (!items.isEmpty()) {
            return items;
        }

        String item = stringValue(this.frontMatter.get("item"));
        if (item != null) {
            return List.of(item);
        }

        String itemId = stringValue(this.frontMatter.get("item_id"));
        return itemId == null ? List.of() : List.of(itemId);
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

    private static List<String> stringValues(@Nullable Object value) {
        String singleValue = stringValue(value);
        if (singleValue != null) {
            return List.of(singleValue);
        }
        if (!(value instanceof List<?> listValue)) {
            return List.of();
        }

        List<String> values = new ArrayList<>(listValue.size());
        for (Object element : listValue) {
            String stringElement = stringValue(element);
            if (stringElement != null) {
                values.add(stringElement);
            }
        }
        return values.isEmpty() ? List.of() : List.copyOf(values);
    }
}

