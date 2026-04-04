package dev.anvilcraft.resource.ageratum.client.util;

import java.util.ArrayList;
import java.util.List;

/**
 * 在逻辑基目录下解析用户提供的相对路径工具。
 */
public class RelativePathResolver {
    private RelativePathResolver() {
    }

    /**
     * 将目标路径相对基目录进行规范化。
     *
     * <p>支持 {@code .} 与 {@code ..} 片段；若尝试越过根目录，则会被钳制在根目录，
     * 不会生成父级前缀。</p>
     */
    public static String resolveWithinBase(String baseDir, String target) {
        String source = target;
        while (source.startsWith("/")) {
            source = source.substring(1);
        }
        String combined = baseDir.isEmpty() ? source : baseDir + "/" + source;
        List<String> parts = new ArrayList<>();
        for (String segment : combined.split("/")) {
            if (segment.isEmpty() || ".".equals(segment)) {
                continue;
            }
            if ("..".equals(segment)) {
                if (!parts.isEmpty()) {
                    parts.removeLast();
                }
                continue;
            }
            parts.add(segment);
        }
        return String.join("/", parts);
    }
}

