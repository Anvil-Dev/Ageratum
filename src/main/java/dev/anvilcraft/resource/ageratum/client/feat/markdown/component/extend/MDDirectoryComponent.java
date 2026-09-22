package dev.anvilcraft.resource.ageratum.client.feat.markdown.component.extend;

import dev.anvilcraft.resource.ageratum.client.feat.markdown.MDExtensionContext;
import dev.anvilcraft.resource.ageratum.client.feat.markdown.component.MDComponent;
import net.minecraft.network.chat.FormattedText;
import net.minecraft.resources.ResourceLocation;

/**
 * 目录扩展组件占位符。
 *
 * <p>对应 Markdown 扩展标签 {@code <directory>} 或 {@code <directory/>}。
 * 实际目录内容需要在整篇文档解析完成后，由 {@code MarkdownParser} 根据全部标题生成。</p>
 */
public final class MDDirectoryComponent extends MDComponent {
    /**
     * Directory component registry ID.
     */
    public static final ResourceLocation ID = ResourceLocation.fromNamespaceAndPath("ageratum", "directory");

    /**
     * 创建目录扩展组件占位符。
     */
    public static MDComponent parse(MDExtensionContext context) {
        return new MDDirectoryComponent();
    }

    private MDDirectoryComponent() {
        super(FormattedText.EMPTY);
    }
}
