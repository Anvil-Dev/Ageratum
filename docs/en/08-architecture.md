# Architecture

This document describes Ageratum's module layout, core data flows, and extension mechanisms, to help developers understand the codebase for maintenance and contribution.

---

## High-Level Architecture

```
┌─────────────────────────────────────────────────────┐
│                    Game Client                      │
│                                                     │
│  /ageratum command  ──→  AgeratumClient             │
│  Network packet (server) ──→  ClientPayloadHandler  │
│                          │                          │
│                          ▼                          │
│                    GuideScreen (GUI)                │
│                          │                          │
│              ┌───────────┴──────────────┐           │
│              ▼                          ▼           │
│    GuideDocumentCache            GuideDocumentLoader│
│    (pre-parsed cache)            (path utilities)   │
│              │                                      │
│              ▼                                      │
│       MarkdownParser                                │
│    (block-level + inline)                           │
│              │                                      │
│              ▼                                      │
│       MDComponent List                              │
│    ┌──────────────────────────┐                     │
│    │ MDTextComponent          │                     │
│    │ MDHeaderComponent        │                     │
│    │ MDCodeBlockComponent     │                     │
│    │ MDListComponent          │                     │
│    │ MDTableComponent         │                     │
│    │ MDQuoteComponent         │                     │
│    │ MDImageComponent         │                     │
│    │ MDNoticeBoxComponent     │                     │
│    │ MDRecipeComponent        │                     │
│    │ <custom components>      │                     │
│    └──────────────────────────┘                     │
└─────────────────────────────────────────────────────┘

┌──────────────────────────────────────────────────────┐
│              NeoForge Custom Registries              │
│                                                      │
│  ageratum:extension_component_factory                │
│    → MDExtensionComponentFactory (block extensions)  │
│                                                      │
│  ageratum:inline_style_parser                        │
│    → MDInlineStyleParser (inline styles)             │
│                                                      │
│  ageratum:recipe_component_factory                   │
│    → MDRecipeComponent.RecipeComponentFactory        │
└──────────────────────────────────────────────────────┘
```

---

## Module Descriptions

### Main Class (`Ageratum`)

- Declares `MOD_ID`
- Provides `location(path)` utility
- Provides `openGuide(player, location)` server-side entry point

### Client Initialization (`AgeratumClient`)

- Registers NeoForge custom registries (`AgeratumRegistries.register()`)
- Triggers class loading of built-in registrations (prevents JIT lazy-init from missing entries)
- Registers the `/ageratum` client command
- Registers the document pre-load reload listener (`RegisterClientReloadListenersEvent`)

### Parsing Pipeline

```
Raw .md file text
        │
        ▼
  MarkdownParser.parseDocument()
        │
        ├─ Extract Front Matter (--- ... ---)
        │
        ├─ Collect reference link definitions ([ref]: url)
        │
        ├─ Block-level parsing (line-by-line scan)
        │   ├─ Code fence detection
        │   ├─ Extension syntax (::: and <tag>)
        │   ├─ Lists, blockquotes, tables
        │   ├─ Headings, horizontal rules
        │   ├─ Images
        │   └─ Paragraphs (default)
        │
        └─ MDDocument { frontMatter, components }
                │
                ▼
         Component list passed to GuideScreen for rendering
```

### Inline Parsing (`MDComponent.textFormat()`)

```
Raw text
    │
    ├─ Pre-process escape characters (\* → %%MDESC42%%)
    │
    ├─ Expand reference links
    │
    ├─ Extract code spans (`code`) to prevent inner content parsing
    │
    ├─ Autolinks (<url>, <email>)
    │
    ├─ Standard links ([text](url))
    │
    ├─ Bold / Italic / Strikethrough
    │
    ├─ Custom inline styles
    │   (query INLINE_STYLE_PARSER_REGISTRY, sorted by priority)
    │
    └─ Restore escape characters
    │
    ▼
  FormattedText
```

### Cache Mechanism (`GuideDocumentCache`)

```
Resource pack load/reload event
        │
        ▼
  RELOAD_LISTENER.prepare()
        │
        ├─ Scan all ageratum/**/*.md files
        ├─ Parse each into MDDocument
        └─ Build NavigationTree (directory structure)
        │
        ▼
  RELOAD_LISTENER.apply()
        │
        ├─ Atomically replace PARSED_DOCUMENT_CACHE (volatile Map)
        └─ Atomically replace NAVIGATION_TREE_CACHE
```

The cache uses `volatile` variables for thread visibility and `Map.copyOf()` for immutability.

---

## Extension Mechanisms

### Extension Component Registration Flow

```
External mod registers
DeferredRegister<MDExtensionComponentFactory>
        │  .register(modEventBus)
        ▼
NeoForge registration event
        │
        ▼
AgeratumRegistries.EXTENSION_COMPONENT_FACTORY_REGISTRY
        │  lookup by ResourceLocation (namespace:name)
        ▼
MarkdownParser encounters extension syntax
        │
        ▼
MDExtensionComponentFactory.create(context)
        │
        ▼
Returns MDComponent → added to document component list
```

### Inline Style Resolution Flow

```
MDComponent.textFormat(rawText)
        │
        ▼
Query AgeratumRegistries.INLINE_STYLE_PARSER_REGISTRY
(all registered parsers, sorted by priority ascending)
        │
        ▼
Scan text character by character:
  for each parser (ascending priority):
    parser.parse(text, pos) → InlineStyleMatch or null
        │
        ▼
Take the earliest match (smallest startPos);
if tie on position, take the lowest priority value
        │
        ▼
Apply Style to matched range, recursively process inner text
        │
        ▼
Output FormattedText
```

---

## Design Principles

### 1. Separation of Concerns (SoC)

Each class has a single responsibility:

- `MarkdownParser`: text → component list only
- `GuideDocumentLoader`: path normalization and IO only
- `GuideDocumentCache`: preload cache management only
- `GuideScreen`: GUI rendering and user interaction only

### 2. Client-Side Isolation (`@Mod(dist = Dist.CLIENT)`)

All rendering-related code (GUI, Markdown parsing, registries) is isolated to the client side via `@Mod(dist = Dist.CLIENT)`. The server side holds only `Ageratum` (no render code) and `AgeratumNetwork`.

### 3. Registry-Driven Extension

All extension points are implemented via NeoForge Custom Registries, not reflection or interface scanning. Benefits:

- Registration lifecycle aligns with the game (frozen after `FMLCommonSetupEvent`)
- Supports `DeferredHolder` lazy references
- Automatic conflict detection for duplicate keys

### 4. Preload + Cache

All documents are parsed once on resource pack load and cached. This eliminates IO and parsing overhead on every document open. The cache is invalidated and rebuilt automatically on resource pack reload.

### 5. Language Fallback

When looking up documents, Ageratum first tries the client's current language (e.g. `zh_cn`), then falls back to `en_us` if not found. This simplifies mod development: **providing only the `en_us` version is sufficient** for all language clients.

---

## File Size Guidelines

The project follows a "no oversized files" policy:

| File Type | Line Limit | Policy |
|-----------|-----------|--------|
| Single Java class | ~400 lines | Split if exceeded |
| Inner classes | Extracted to separate files |
| Static utility methods | Moved to dedicated utility class |

---

## Testing Strategy

The current version has no unit tests. Functionality is verified through in-game testing (`/ageratum` command + resource packs).

Planned future additions:

- Unit tests for `MarkdownParser` text output (game-environment-independent)
- Path normalization tests for `GuideDocumentLoader`

---

## See Also

- [API Reference](07-api-reference.md)
- [Extension Components](04-extension-components.md)
- [Inline Style Parsers](05-inline-style-parsers.md)

