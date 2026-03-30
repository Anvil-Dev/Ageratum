package dev.anvilcraft.resource.ageratum.client.feat.markdown;

import dev.anvilcraft.resource.ageratum.client.feat.markdown.component.MDComponent;
import net.minecraft.resources.ResourceLocation;

import java.util.List;
import java.util.Map;

/**
 * Markdown 扩展语法执行上下文。
 *
 * <p>包含扩展组件所需的全部信息：组件 ID、参数、渲染后的内容列表和原始文本。</p>
 *
 * @param sourceLocation  当前正在解析的文档位置。
 * @param id              扩展组件 ID（如 {@code ageratum:info}）。
 * @param rawParams       参数的原始字符串形式。
 * @param params          解析后的参数键值对（仅 {@code <...>} 标签语法提供）。
 * @param renderedContent 块内容解析后的 Markdown 组件列表。
 * @param rawContent      块内容的原始文本。
 */
public record MDExtensionContext(
    ResourceLocation sourceLocation,
    ResourceLocation id,
    String rawParams,
    Map<String, String> params,
    List<MDComponent> renderedContent,
    String rawContent
) {
}

