# Inline Style Parsers

This document explains how to register custom inline style parsers, allowing Ageratum's Markdown renderer to recognize and apply your mod's custom inline tags.

> Note: self-closing tags like `<namespace:id .../>` belong to the "inline component" system and should be registered via `MDInlineComponentFactory`.
> `MDInlineStyleParser` in this document is mainly for paired style tags such as `<tag>...</tag>`.

---

## Overview

**Inline Style Parsers** let you extend Ageratum's inline Markdown syntax. By registering an `MDInlineStyleParser`, you can map specific tag pairs (e.g. `<mytag>...</mytag>`) to Minecraft `Style` objects, enabling custom colors, effects, or interactive behaviors.

---

## Core Interface

### `MDInlineStyleParser`

```java
public interface MDInlineStyleParser {
    /** Priority: lower value = higher precedence at the same position */
    int priority();

    /**
     * Attempt to parse an inline style tag starting at pos in text.
     * Returns null if no match is found.
     */
    @Nullable
    MDComponent.InlineStyleMatch parse(String text, int pos);

    /** Create a simple open/close tag parser */
    static MDInlineStyleParser create(
        int priority,
        Pattern openTagPattern,
        String closeTag,
        BiFunction<Style, Matcher, Style> styleFactory
    ) { ... }
}
```

### `MDComponent.InlineStyleMatch`

An `InlineStyleMatch` describes a successful tag match, containing:

- Start and end positions of the tag
- The `Style` to apply to the inner text
- The inner text content

---

## Registration Steps

### 1. Define the Parsers

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

    // ── Example 1: <rainbow>text</rainbow> - Purple text ──────────────────────
    public static final DeferredHolder<MDInlineStyleParser, MDInlineStyleParser> RAINBOW =
        INLINE_STYLE_PARSERS.register(
            "rainbow",
            () -> MDInlineStyleParser.create(
                100,                             // priority (lower = higher precedence)
                Pattern.compile("<rainbow>"),   // open tag pattern
                "</rainbow>",                   // close tag string
                (parentStyle, matcher) ->        // returns modified Style
                    parentStyle.withColor(0xFF55FF).withBold(false)
            )
        );

    // ── Example 2: <highlight=#RRGGBB>text</highlight> - Custom color text ────
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
                    // Note: Minecraft Style has no background color;
                    // this demonstrates colored + underlined text
                    return parentStyle.withColor(color).withUnderlined(true);
                }
            )
        );

    // ── Example 3: <spoiler>text</spoiler> - Obfuscated mask ──────────────────
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

### 2. Bind to the Event Bus in Client Init

```java
@Mod(value = "mymod", dist = Dist.CLIENT)
public class MyModClient {
    public MyModClient(IEventBus modEventBus, ModContainer modContainer) {
        MyModInlineStyleParsers.INLINE_STYLE_PARSERS.register(modEventBus);
    }
}
```

---

## Using in Documents

```markdown
Normal text <rainbow>This is colorful text</rainbow> normal text

<highlight=#FFAA00>This is highlighted text</highlight>

Spoiler: <spoiler>The answer is 42</spoiler>
```

---

## Priority Rules

When multiple parsers match at the same position, the one with the **lower priority value** is tried first.  
If priorities are equal, registration order determines precedence.

Built-in parsers all use priority `0`. Custom parsers are recommended to use the `50`–`200` range to avoid conflicting with built-ins.

---

## Advanced: Fully Custom Parser

If the `MDInlineStyleParser.create()` factory method isn't sufficient (e.g. you need to parse nested content or multiple text segments), implement the full interface:

```java
public class MyAdvancedParser implements MDInlineStyleParser {
    private static final String OPEN  = "<mymod>";
    private static final String CLOSE = "</mymod>";

    @Override
    public int priority() { return 150; }

    @Override
    @Nullable
    public MDComponent.InlineStyleMatch parse(String text, int pos) {
        int start = text.indexOf(OPEN, pos);
        if (start < 0) return null;

        int contentStart = start + OPEN.length();
        int end = text.indexOf(CLOSE, contentStart);
        if (end < 0) return null;

        // Build match using the static factory helper
        Pattern openTag = Pattern.compile(Pattern.quote(OPEN));
        Matcher m = openTag.matcher(text);
        if (!m.find(start)) return null;
        return MDComponent.InlineStyleMatch.of(
            openTag, m, CLOSE,
            (parentStyle, matcher) -> parentStyle.withColor(0x00FFFF)
        );
    }
}
```

> **Note**: See `MDComponent.java` source for the full `InlineStyleMatch.of()` signature.

---

## Interaction with Built-in Parsers

Ageratum's built-in inline parsers (color, obfuscated, hover, click) are all registered with priority `0`.  
To avoid tag name conflicts:

1. Prefix your tag names with your mod ID (e.g. `<mymod_rainbow>`)
2. Or choose names that don't conflict with built-ins: `color`, `o`, `hover`, `click`

---

## See Also

- [Extension Components](04-extension-components.md)
- [Markdown Syntax Reference](03-markdown-syntax.md)
- [API Reference](07-api-reference.md)

