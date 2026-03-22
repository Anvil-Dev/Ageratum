<div align="center">

# Ageratum | [藿香](./README.md)

<img src=".idea/icon.png" style="width: 128px; height: 128px" alt="Ageratum Logo">

[![License](https://img.shields.io/badge/License-LGPL%20v3-blue.svg)](./LICENSE)
[![Asset License](https://img.shields.io/badge/Asset%20License-ARR-green.svg)](./ASSETS_LICENSE)

</div>

# Ageratum - In-Game Handbook Framework

A handbook-focused mod for Minecraft Forge/NeoForge, designed to provide in-game guides for other mods. Ageratum offers rich Markdown rendering, i18n localization, and an extensible custom syntax/component system.

## Features

### Core Markdown Support

✅ **Block Elements**
- ATX headings (`# ~ ######`) and Setext headings (underline style)
- Paragraphs and line breaks
- Ordered, unordered, and task lists (multi-level nesting)
- Blockquotes (multi-level nesting)
- Fenced code blocks (backticks and tildes) and indented code blocks
- Horizontal rules
- Tables with alignment settings
- Images (namespace-local references)

✅ **Inline Elements**
- **bold**, *italic*, ~~strikethrough~~
- Inline code spans (multi-backtick support)
- [Links](https://example.com) and autolinks
- Escape character support
- Custom color tags
- **Hover and Click Events** (`<hover>` and `<click>` tags)

✅ **Advanced Features**
- Reference link definitions and reference link syntax
- Automatic link expansion
- Code block line numbers
- Table column alignment (left/center/right)

### Internationalization (i18n)

- Documents organized by `ageratum/<language_code>/` (e.g., `en_us`, `zh_cn`)
- Default fallback to `en_us` if localized version missing
- Full support for multi-byte characters (Chinese, Japanese, etc.)

### Extension Syntax

Two block-level extension syntaxes for custom components:

#### 1. Colon Syntax
```markdown
::: info
This is an info box.
:::

::: tip
This is a tip.
:::

::: warning
This is a warning.
:::

::: danger
This is a danger warning.
:::
```

#### 2. Tag Syntax
```markdown
<namespace:component key="value" param=123>
Block content supports Markdown syntax.
</namespace:component>

<namespace:component/>
Self-closing form without content.
```

Namespace can be omitted (defaults to `ageratum:`).

### Built-in Extension Components

- `ageratum:info` - Blue info box
- `ageratum:tip` - Green tip box
- `ageratum:warning` - Orange warning box
- `ageratum:danger` - Red danger box

### Hover and Click Events

Support interactive text that allows players to hover for tooltips or click to perform actions.

#### Hover Events (`<hover>`)

Display tooltip text when hovering over text:

```markdown
<hover type="SHOW_TEXT" data="This is the tooltip">Hover over me</hover>
```

**Supported Types:**
- `SHOW_TEXT` - Display plain text tooltip (`data` is the tooltip content)

#### Click Events (`<click>`)

Execute an action when clicking on text:

```markdown
<click type="OPEN_URL" data="https://example.com">Click to open link</click>
<click type="COPY_TO_CLIPBOARD" data="Text to copy">Click to copy</click>
<click type="SUGGEST_COMMAND" data="/say hello">Click to suggest command</click>
```

**Supported Types:**
- `OPEN_URL` - Open a URL (`data` is the complete URL)
- `COPY_TO_CLIPBOARD` - Copy text to clipboard (`data` is the text to copy)
- `SUGGEST_COMMAND` - Suggest a command in chat (`data` is the command text)

#### Combining Styles

You can combine multiple styles in the same text:

```markdown
<hover type="SHOW_TEXT" data="This is a tooltip"><click type="OPEN_URL" data="https://example.com">Click and hover on me!</click></hover>
```

### Preloading & Caching

- Automatically scans and pre-parses Markdown documents to `MDComponent` lists on resource load
- Opens cached components immediately without parsing delay
- Auto-refreshes cache on resource reload

### Cross-side Guide Opening

```java
// Client: open directly
Ageratum.openGuide(ResourceLocation location);

// Server: notify client via network packet
Ageratum.openGuide(ResourceLocation location);
```

## Project Structure

### Directory Layout

```
src/main/java/dev/anvilcraft/resource/ageratum/
├── Ageratum.java                           // Main mod class + command registration
├── GuideDocumentLoader.java                // Document loading utils
├── GuideDocumentCache.java                 // Preload cache & reload listener
│
├── client/
│   ├── AgeratumClient.java                 // Client hooks (reserved)
│   ├── gui/
│   │   └── GuideScreen.java                // Guide reading GUI
│   └── feat/markdown/
│       ├── MarkdownParser.java             // Markdown block-level parser
│       ├── BuiltinExtensionComponents.java // Built-in extension registration
│       ├── BlockExtensionState.java        // Block extension state machine
│       ├── SelfClosingBlockExtensionState.java
│       ├── ExtensionParamParser.java       // Parameter parsing utility
│       ├── MDExtensionContext.java         // Extension execution context
│       ├── MDExtensionComponentFactory.java // Extension factory interface
│       └── component/
│           ├── MDComponent.java            // Base class + inline parsing
│           ├── MDTextComponent.java        // Plain text paragraphs
│           ├── MDHeaderComponent.java      // Headings
│           ├── MDCodeBlockComponent.java   // Code blocks
│           ├── MDListComponent.java        // Lists (inc. task lists)
│           ├── MDQuoteComponent.java       // Blockquotes
│           ├── MDTableComponent.java       // Tables
│           ├── MDImageComponent.java       // Images
│           ├── MDHorizontalRuleComponent.java
│           └── MDNoticeBoxComponent.java   // Notice box container
│
└── network/
    ├── AgeratumNetwork.java                // Network registration & dispatch
    └── OpenGuidePayload.java               // Guide open network packet
```

### Design Principles

- **Separation of Concerns**: Each class handles a single responsibility
- **No Oversized Classes**: Longest files ~400 lines, all inner classes extracted
- **Comprehensive Documentation**: Chinese Javadoc for all public APIs, inline comments for complex logic
- **Extensibility**: Register custom block types via `registerExtensionComponent()`

## Usage Guide

### Players

Open guides with client command:

```
/ageratum <namespace> [file]

Examples:
/ageratum ageratum                  # Opens ageratum:en_us/index.md
/ageratum mymod guide              # Opens mymod:en_us/guide.md
/ageratum mymod zh_cn/tutorial     # Opens mymod:zh_cn/tutorial.md
```

Tab completion supported for namespaces and file names.

### Developers

#### Register Custom Extension

Use registration methods described in NeoForge docs:

1. `DeferredRegister` (recommended)
2. `RegisterEvent` (advanced usage)

```java
public static final DeferredRegister<MDExtensionComponentFactory> EXT_COMPONENT_FACTORIES =
    AgeratumRegistries.createExtensionComponentFactoryRegister("your_modid");

public static final DeferredHolder<MDExtensionComponentFactory, MDExtensionComponentFactory> CUSTOM =
    EXT_COMPONENT_FACTORIES.register("custom", () ->
        context -> new MyComponent(context.renderedContent(), context.params())
    );

// In your mod constructor
EXT_COMPONENT_FACTORIES.register(modEventBus);
```

#### Register Custom Inline Style Parser

Inline style parsers are registered through `INLINE_STYLE_PARSER_REGISTRY_KEY`. `MDComponent` queries this registry and resolves matches by position + parser priority.

```java
package com.example.mymod.client.markdown;

import dev.anvilcraft.resource.ageratum.client.feat.markdown.component.MDInlineStyleParser;
import dev.anvilcraft.resource.ageratum.client.registries.AgeratumRegistries;
import net.minecraft.network.chat.Style;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.regex.Pattern;

public final class MyInlineStyleParsers {
    // Use your own modid here, not ageratum
    public static final DeferredRegister<MDInlineStyleParser> INLINE_STYLE_PARSERS = DeferredRegister.create(
        AgeratumRegistries.INLINE_STYLE_PARSER_REGISTRY_KEY,
        "mymod"
    );

    // Example tag: <rainbow>text</rainbow>
    public static final DeferredHolder<MDInlineStyleParser, MDInlineStyleParser> RAINBOW =
        INLINE_STYLE_PARSERS.register(
            "rainbow",
            () -> MDInlineStyleParser.create(
                100, // smaller value = higher precedence at same position
                Pattern.compile("<rainbow>"),
                "</rainbow>",
                (Style parentStyle, java.util.regex.Matcher matcher) -> parentStyle.withColor(0xFF55FF)
            )
        );

    private MyInlineStyleParsers() {
    }
}
```

Register it in your client init:

```java
public class MyModClient {
    public MyModClient(IEventBus modEventBus) {
        MyInlineStyleParsers.INLINE_STYLE_PARSERS.register(modEventBus);
    }
}
```

Markdown usage:

```markdown
normal text <rainbow>colored text</rainbow> normal text
```

See full guide: `docs/inline-style-parser-example.en.md`.

#### Add Documentation

Create in resource pack:
```
assets/<namespace>/ageratum/<language>/index.md
assets/<namespace>/ageratum/en_us/index.md
assets/<namespace>/ageratum/zh_cn/index.md
```

## License

* Code unless otherwise stated default to our [LICENSE file(LGPL-3.0)](./LICENSE) here
* Non-Code assets (Located here) go by our [ASSET_LICENSE file(ARR)](./ASSETS_LICENSE) here
