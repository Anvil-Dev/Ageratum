package dev.anvilcraft.resource.ageratum.client.feat.markdown;

import dev.anvilcraft.resource.ageratum.client.feat.markdown.component.MDComponent;
import net.minecraft.resources.ResourceLocation;

import java.util.List;
import java.util.Map;

/**
 * Markdown 扩展语法执行上下文。
 *
 * <p>包含扩展组件所需的全部信息：组件 ID、参数、渲染后的内容列表和原始文本。</p>
 */
public record MDExtensionContext(
    /** 扩展组件 ID（如 {@code ageratum:info}）。 */
    ResourceLocation id,

    /** 参数的原始字符串形式。 */
    String rawParams,

    /** 解析后的参数键值对（仅 {@code <...>} 标签语法提供）。 */
    Map<String, String> params,

    /** 块内容解析后的 Markdown 组件列表。 */
    List<MDComponent> renderedContent,

    /** 块内容的原始文本。 */
    String rawContent
) {
}

