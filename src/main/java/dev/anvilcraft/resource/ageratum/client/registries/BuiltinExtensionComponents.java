package dev.anvilcraft.resource.ageratum.client.registries;

import dev.anvilcraft.resource.ageratum.client.feat.markdown.MDExtensionComponentFactory;
import dev.anvilcraft.resource.ageratum.client.feat.markdown.component.MDNoticeBoxComponent;
import dev.anvilcraft.resource.ageratum.client.feat.markdown.component.recipe.MDRecipeComponent;
import dev.anvilcraft.resource.ageratum.client.feat.markdown.component.structure.MDNBTStructureComponent;
import net.neoforged.neoforge.registries.DeferredHolder;

/**
 * 内置扩展组件注册。
 *
 * <p>提供 info、tip、warning、danger 四种提示框类型。</p>
 */
public final class BuiltinExtensionComponents {
    /**
     * info 提示框组件工厂注册项。
     */
    public static final DeferredHolder<MDExtensionComponentFactory, MDExtensionComponentFactory> INFO =
        AgeratumRegistries.EXTENSION_COMPONENT_FACTORIES.register(
            "info",
            () -> context -> new MDNoticeBoxComponent(MDNoticeBoxComponent.NoticeType.INFO, context.renderedContent())
        );

    /**
     * tip 提示框组件工厂注册项。
     */
    public static final DeferredHolder<MDExtensionComponentFactory, MDExtensionComponentFactory> TIP =
        AgeratumRegistries.EXTENSION_COMPONENT_FACTORIES.register(
            "tip",
            () -> context -> new MDNoticeBoxComponent(MDNoticeBoxComponent.NoticeType.TIP, context.renderedContent())
        );

    /**
     * warning 提示框组件工厂注册项。
     */
    public static final DeferredHolder<MDExtensionComponentFactory, MDExtensionComponentFactory> WARNING =
        AgeratumRegistries.EXTENSION_COMPONENT_FACTORIES.register(
            "warning",
            () -> context -> new MDNoticeBoxComponent(MDNoticeBoxComponent.NoticeType.WARNING, context.renderedContent())
        );

    /**
     * danger 提示框组件工厂注册项。
     */
    public static final DeferredHolder<MDExtensionComponentFactory, MDExtensionComponentFactory> DANGER =
        AgeratumRegistries.EXTENSION_COMPONENT_FACTORIES.register(
            "danger",
            () -> context -> new MDNoticeBoxComponent(MDNoticeBoxComponent.NoticeType.DANGER, context.renderedContent())
        );


    /**
     * 配方扩展组件注册项。
     *
     * <p>对应 Markdown 扩展标签：{@code <recipe id="namespace:path"/>}。</p>
     */
    public static final DeferredHolder<MDExtensionComponentFactory, MDExtensionComponentFactory> RECIPE =
        AgeratumRegistries.EXTENSION_COMPONENT_FACTORIES.register(
            "recipe",
            () -> MDRecipeComponent::parse
        );

    /**
     * 结构 NBT 扩展组件注册项。
     *
     * <p>对应 Markdown 扩展标签：{@code <structure id="namespace:path"/>}。</p>
     */
    public static final DeferredHolder<MDExtensionComponentFactory, MDExtensionComponentFactory> STRUCTURE =
        AgeratumRegistries.EXTENSION_COMPONENT_FACTORIES.register(
            "structure",
            () -> MDNBTStructureComponent::parse
        );


    private BuiltinExtensionComponents() {
    }

    /**
     * 触发类加载，确保静态注册项初始化。
     */
    public static void init() {
    }
}

