package dev.anvilcraft.resource.ageratum.client.util;

import java.util.ArrayList;
import java.util.List;

public class PathUtil {
    /**
     * 将相对路径规范化到给定基目录下，支持 ./ 与 ../，并阻止越过根目录。
     */
    public static String normalizePathAgainstBase(String baseDir, String target) {
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
