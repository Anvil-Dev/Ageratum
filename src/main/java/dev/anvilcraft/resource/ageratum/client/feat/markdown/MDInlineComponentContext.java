package dev.anvilcraft.resource.ageratum.client.feat.markdown;

import net.minecraft.network.chat.Style;
import net.minecraft.resources.Identifier;

import java.util.Map;

/**
 * Markdown 行内组件上下文。
 *
 * @param id        行内组件 ID（如 {@code ageratum:translate}）。
 * @param rawParams 原始参数字符串。
 * @param params    解析后的参数键值对。
 * @param baseStyle 组件输出应继承的基础样式。
 */
public record MDInlineComponentContext(Identifier id, String rawParams, Map<String, String> params, Style baseStyle) {
}
