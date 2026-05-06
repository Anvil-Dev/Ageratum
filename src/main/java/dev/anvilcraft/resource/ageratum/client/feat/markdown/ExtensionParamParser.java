package dev.anvilcraft.resource.ageratum.client.feat.markdown;

import dev.anvilcraft.resource.ageratum.client.constants.AgeratumConstants;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.regex.Matcher;
import javax.annotation.Nullable;

/**
 * 扩展语法参数解析工具。
 *
 * <p>负责解析 {@code key=value} 格式的参数字符串为键值对映射。
 * 支持无引号、双引号和单引号三种参数值格式。</p>
 */
public final class ExtensionParamParser {
    private ExtensionParamParser() {
    }

    /**
     * 解析参数字符串为键值对映射。
     *
     * <p>支持的格式：
     * <ul>
     *   <li>{@code key=value}</li>
     *   <li>{@code key="value with spaces"}</li>
     *   <li>{@code key='value with spaces'}</li>
     * </ul>
     * </p>
     *
     * @param rawParams 原始参数字符串（可为 {@code null} 或空）
     * @return 解析后的参数映射（不可修改）
     */
    public static Map<String, String> parse(@Nullable String rawParams) {
        if (rawParams == null || rawParams.isBlank()) {
            return Map.of();
        }

        Map<String, String> result = new LinkedHashMap<>();
        Matcher matcher = AgeratumConstants.Patterns.PARAM_PAIR_PATTERN.matcher(rawParams);

        while (matcher.find()) {
            String key = matcher.group(1);
            String value = matcher.group(2);

            // 移除可选的引号
            if ((value.startsWith("\"") && value.endsWith("\"")) ||
                (value.startsWith("'") && value.endsWith("'"))) {
                value = value.substring(1, value.length() - 1);
            }

            result.put(key, value);
        }

        return Map.copyOf(result);
    }
}

