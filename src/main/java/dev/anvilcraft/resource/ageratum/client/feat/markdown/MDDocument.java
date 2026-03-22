package dev.anvilcraft.resource.ageratum.client.feat.markdown;

import dev.anvilcraft.resource.ageratum.client.feat.markdown.component.MDComponent;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import javax.annotation.Nullable;

/**
 * Markdown 文档模型，包含 front matter 与渲染组件。
 */
public record MDDocument(@Nullable Map<String, Object> frontMatter, @Nullable List<MDComponent> components) {
    public MDDocument {
        frontMatter = freezeMap(frontMatter == null ? Map.of() : frontMatter);
        components = List.copyOf(components == null ? List.of() : components);
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
}

