package dev.anvilcraft.resource.ageratum.client.feat.markdown;

import dev.anvilcraft.resource.ageratum.Ageratum;
import dev.anvilcraft.resource.ageratum.client.feat.markdown.component.MDNoticeBoxComponent;
import net.minecraft.resources.ResourceLocation;

/**
 * 内置扩展组件注册。
 *
 * <p>提供 info、tip、warning、danger 四种提示框类型。</p>
 */
public final class BuiltinExtensionComponents {
    private BuiltinExtensionComponents() {
    }

    /**
     * 注册所有内置扩展组件。
     */
    public static void registerAll() {
        MarkdownParser.registerExtensionComponent(
            Ageratum.location("info"),
            context -> new MDNoticeBoxComponent(MDNoticeBoxComponent.NoticeType.INFO, context.renderedContent())
        );

        MarkdownParser.registerExtensionComponent(
            Ageratum.location("tip"),
            context -> new MDNoticeBoxComponent(MDNoticeBoxComponent.NoticeType.TIP, context.renderedContent())
        );

        MarkdownParser.registerExtensionComponent(
            Ageratum.location("warning"),
            context -> new MDNoticeBoxComponent(MDNoticeBoxComponent.NoticeType.WARNING, context.renderedContent())
        );

        MarkdownParser.registerExtensionComponent(
            Ageratum.location("danger"),
            context -> new MDNoticeBoxComponent(MDNoticeBoxComponent.NoticeType.DANGER, context.renderedContent())
        );
    }
}

