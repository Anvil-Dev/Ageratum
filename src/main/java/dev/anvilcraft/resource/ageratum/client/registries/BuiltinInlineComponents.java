package dev.anvilcraft.resource.ageratum.client.registries;

import dev.anvilcraft.resource.ageratum.client.feat.markdown.MDInlineComponentFactory;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.neoforged.neoforge.registries.DeferredHolder;

/**
 * 内置行内组件注册。
 */
public final class BuiltinInlineComponents {
    /**
     * 翻译组件：{@code <translate key="..." fallback="..."/>}。
     */
    public static final DeferredHolder<MDInlineComponentFactory, MDInlineComponentFactory> TRANSLATE =
        AgeratumRegistries.INLINE_COMPONENT_FACTORIES.register(
            "translate",
            () -> context -> {
                String key = context.params().get("key");
                if (key == null || key.isBlank()) {
                    return Component.empty().withStyle(context.baseStyle());
                }

                String fallback = context.params().get("fallback");
                MutableComponent translated = fallback == null
                    ? Component.translatable(key)
                    : Component.translatableWithFallback(key, fallback);
                return translated.withStyle(context.baseStyle());
            }
        );

    private BuiltinInlineComponents() {
    }

    /**
     * 触发类加载，确保静态注册项初始化。
     */
    public static void init() {
    }
}
