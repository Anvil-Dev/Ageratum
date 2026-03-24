# 行内样式解析器开发

本文档介绍如何注册自定义行内样式解析器，使 Ageratum 的 Markdown 渲染器能够识别并应用你模组专属的行内标签。

---

## 概念介绍

**行内样式解析器**（Inline Style Parser）允许你扩展 Ageratum 的行内 Markdown 语法。通过注册 `MDInlineStyleParser`，你可以让特定的标签对（如 `<mytag>...</mytag>`）映射到 Minecraft `Style` 对象，从而实现自定义颜色、特效或交互行为。

---

## 核心接口

### `MDInlineStyleParser`

```java
public interface MDInlineStyleParser {
    /** 解析优先级；数值越小，越先参与同位置竞争 */
    int priority();

    /**
     * 从文本 pos 位置开始，尝试解析一段行内样式。
     * 若未命中则返回 null。
     */
    @Nullable
    MDComponent.InlineStyleMatch parse(String text, int pos);

    /** 创建基于开始/结束标签模式的简单解析器 */
    static MDInlineStyleParser create(
        int priority,
        Pattern openTagPattern,
        String closeTag,
        BiFunction<Style, Matcher, Style> styleFactory
    ) { ... }
}
```

### `MDComponent.InlineStyleMatch`

`InlineStyleMatch` 描述一次成功的标签匹配，包含：

- 标签开始和结束位置
- 应用于内部文本的 `Style`
- 内部文本内容

---

## 注册步骤

### 1. 定义解析器类

```java
package com.example.mymod.client.markdown;

import dev.anvilcraft.resource.ageratum.client.feat.markdown.component.MDInlineStyleParser;
import dev.anvilcraft.resource.ageratum.client.registries.AgeratumRegistries;
import net.minecraft.network.chat.Style;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.regex.Pattern;

public final class MyModInlineStyleParsers {

    public static final DeferredRegister<MDInlineStyleParser> INLINE_STYLE_PARSERS =
        DeferredRegister.create(
            AgeratumRegistries.INLINE_STYLE_PARSER_REGISTRY_KEY,
            "mymod"
        );

    // ── 示例 1：<rainbow>文本</rainbow> - 紫色文字 ──────────────────────────
    public static final DeferredHolder<MDInlineStyleParser, MDInlineStyleParser> RAINBOW =
        INLINE_STYLE_PARSERS.register(
            "rainbow",
            () -> MDInlineStyleParser.create(
                100,                                          // 优先级（值越小越优先）
                Pattern.compile("<rainbow>"),                // 开始标签正则
                "</rainbow>",                                // 结束标签字符串
                (parentStyle, matcher) -> parentStyle        // 返回修改后的 Style
                    .withColor(0xFF55FF)
                    .withBold(false)
            )
        );

    // ── 示例 2：<highlight=#RRGGBB>文本</highlight> - 自定义背景色 ─────────
    private static final Pattern HIGHLIGHT_PATTERN =
        Pattern.compile("<highlight=#([0-9a-fA-F]{6})>", Pattern.CASE_INSENSITIVE);

    public static final DeferredHolder<MDInlineStyleParser, MDInlineStyleParser> HIGHLIGHT =
        INLINE_STYLE_PARSERS.register(
            "highlight",
            () -> MDInlineStyleParser.create(
                50,
                HIGHLIGHT_PATTERN,
                "</highlight>",
                (parentStyle, matcher) -> {
                    int color = Integer.parseInt(matcher.group(1), 16);
                    // 注意：Minecraft 的 Style 没有"背景色"，这里演示颜色文字
                    return parentStyle.withColor(color).withUnderlined(true);
                }
            )
        );

    // ── 示例 3：<spoiler>文本</spoiler> - 混淆遮罩（悬停显示） ────────────
    public static final DeferredHolder<MDInlineStyleParser, MDInlineStyleParser> SPOILER =
        INLINE_STYLE_PARSERS.register(
            "spoiler",
            () -> MDInlineStyleParser.create(
                200,
                Pattern.compile("<spoiler>"),
                "</spoiler>",
                (parentStyle, matcher) -> parentStyle.withObfuscated(true)
            )
        );

    private MyModInlineStyleParsers() {}
}
```

### 2. 在客户端初始化中绑定

```java
@Mod(value = "mymod", dist = Dist.CLIENT)
public class MyModClient {
    public MyModClient(IEventBus modEventBus, ModContainer modContainer) {
        MyModInlineStyleParsers.INLINE_STYLE_PARSERS.register(modEventBus);
    }
}
```

---

## 在文档中使用

```markdown
普通文本 <rainbow>这是彩色文字</rainbow> 普通文本

<highlight=#FFAA00>这是高亮文字</highlight>

剧透内容：<spoiler>答案是 42</spoiler>（悬停不可见，因为混淆）
```

---

## 优先级规则

当同一位置有多个解析器命中时，优先级数值**越小**的越先尝试匹配。  
若优先级相同，以注册顺序决定。

内置解析器的优先级均为 `0`，自定义解析器推荐使用 `50`~`200` 的范围，避免与内置冲突。

---

## 进阶：完全自定义解析器

如果 `MDInlineStyleParser.create()` 工厂方法满足不了需求（例如需要解析嵌套内容或多段文本），可以实现完整接口：

```java
public class MyAdvancedParser implements MDInlineStyleParser {
    @Override
    public int priority() { return 150; }

    @Override
    @Nullable
    public MDComponent.InlineStyleMatch parse(String text, int pos) {
        // 查找开始标签
        int start = text.indexOf("<mymod>", pos);
        if (start < 0) return null;

        // 查找结束标签
        int contentStart = start + "<mymod>".length();
        int end = text.indexOf("</mymod>", contentStart);
        if (end < 0) return null;

        // 返回匹配结果（使用工厂方法简化）
        Pattern openTag = Pattern.compile("<mymod>");
        return MDComponent.InlineStyleMatch.of(
            openTag,
            openTag.matcher(text).find(start) ? openTag.matcher(text) : null,
            // ... 构造匹配结果
        );
    }
}
```

> **注意**：`MDComponent.InlineStyleMatch.of()` 的完整签名请参考源码 `MDComponent.java`。

---

## 与内置解析器的交互

Ageratum 的内置行内解析器（颜色、混淆、悬停、点击）均以优先级 `0` 注册。  
为了避免标签名冲突，建议：

1. 使用你的 mod ID 作为标签前缀（如 `<mymod_rainbow>`）
2. 或者使用不与内置冲突的名称（内置：`color`、`o`、`hover`、`click`）

---

## 参见

- [扩展组件开发](04-extension-components.md)
- [Markdown 语法参考](03-markdown-syntax.md)
- [API 参考](07-api-reference.md)

