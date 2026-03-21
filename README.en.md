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
