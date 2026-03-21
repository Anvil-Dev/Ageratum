package dev.anvilcraft.resource.ageratum.client.feat.markdown;

import dev.anvilcraft.resource.ageratum.client.feat.markdown.component.MDComponent;

/**
 * Markdown 扩展语法组件工厂接口。
 *
 * <p>实现此接口以创建自定义的 Markdown 扩展组件。
 * 工厂会接收完整的扩展上下文，包括参数、渲染内容和原始文本。</p>
 */
@FunctionalInterface
public interface MDExtensionComponentFactory {
    /**
     * 根据扩展上下文创建 Markdown 组件。
     *
     * @param context 扩展语法执行上下文，包含 ID、参数、内容等信息
     * @return 创建的组件实例
     */
    MDComponent create(MDExtensionContext context);
}

