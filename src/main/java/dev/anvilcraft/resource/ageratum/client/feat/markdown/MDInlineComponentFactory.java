package dev.anvilcraft.resource.ageratum.client.feat.markdown;

import net.minecraft.network.chat.FormattedText;

/**
 * Markdown 行内组件工厂接口。
 *
 * <p>实现该接口并注册到行内组件工厂注册表后，即可解析并渲染
 * {@code <namespace:id .../>} 形式的行内标签。</p>
 */
@FunctionalInterface
public interface MDInlineComponentFactory {
    /**
     * 根据上下文创建行内组件文本。
     */
    FormattedText create(MDInlineComponentContext context);
}

