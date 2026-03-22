package dev.anvilcraft.resource.ageratum.client.feat.markdown.component;

import net.minecraft.network.chat.Style;

import java.util.function.BiFunction;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.annotation.Nullable;

/**
 * Markdown 行内样式解析器注册项。
 *
 * <p>该接口设计为可直接注册到 NeoForge 自定义注册表中，供 {@link MDComponent}
 * 在解析自定义标签时按优先级查询。</p>
 */
public interface MDInlineStyleParser {
    /**
     * 解析优先级；数值越小越先参与同位置竞争。
     */
    int priority();

    /**
     * 从指定起始位置尝试解析一段行内样式标签。
     *
     * @param text Markdown 原始文本
     * @param pos  搜索起始位置
     * @return 匹配结果；若未命中则返回 {@code null}
     */
    @Nullable
    MDComponent.InlineStyleMatch parse(String text, int pos);

    /**
     * 创建一个基于开始标签/结束标签模式的简单解析器。
     */
    static MDInlineStyleParser create(
        int priority,
        Pattern openTagPattern,
        String closeTag,
        BiFunction<Style, Matcher, Style> styleFactory
    ) {
        return new MDInlineStyleParser() {
            @Override
            public int priority() {
                return priority;
            }

            @Override
            public @Nullable MDComponent.InlineStyleMatch parse(String text, int pos) {
                Matcher matcher = openTagPattern.matcher(text);
                if (!matcher.find(pos)) {
                    return null;
                }
                return MDComponent.InlineStyleMatch.of(openTagPattern, matcher, closeTag, styleFactory);
            }
        };
    }
}

