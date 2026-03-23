# API Reference

This document lists all public-facing APIs in Ageratum, including class summaries, method signatures, and descriptions.

---

## Package Structure

```
dev.anvilcraft.resource.ageratum
├── Ageratum                                    # Mod main class (entry point)
│
├── network/
│   ├── AgeratumNetwork                         # Network registration & dispatch
│   └── OpenGuidePayload                        # Open-guide network packet
│
└── client/
    ├── AgeratumClient                          # Client initialization
    ├── AgeratumClientConfig                    # Client configuration
    │
    ├── registries/
    │   ├── AgeratumRegistries                  # Registry definitions
    │   ├── BuiltinExtensionComponents          # Built-in extension components
    │   ├── BuiltinInlineStyleParsers           # Built-in inline parsers
    │   └── BuiltinRecipeComponentFactories     # Built-in recipe factories
    │
    ├── gui/
    │   └── GuideScreen                         # Document reading GUI
    │
    └── feat/markdown/
        ├── MarkdownParser                      # Markdown parser
        ├── GuideDocumentLoader                 # Document path & IO utilities
        ├── GuideDocumentCache                  # Pre-parsed document cache
        ├── MDDocument                          # Document model
        ├── MDExtensionContext                  # Extension execution context
        ├── MDExtensionComponentFactory         # Extension factory interface
        │
        └── component/
            ├── MDComponent                     # Component base class
            ├── MDTextComponent                 # Text paragraphs
            ├── MDHeaderComponent               # Headings
            ├── MDCodeBlockComponent            # Code blocks
            ├── MDListComponent                 # Lists
            ├── MDQuoteComponent                # Blockquotes
            ├── MDTableComponent                # Tables
            ├── MDImageComponent                # Images
            ├── MDHorizontalRuleComponent       # Horizontal rules
            ├── MDNoticeBoxComponent            # Notice boxes
            ├── MDInlineStyleParser             # Inline parser interface
            └── recipe/
                ├── MDRecipeComponent           # Recipe component base class
                └── MDCraftingTableRecipeComponent # Crafting table recipe component
```

---

## `Ageratum`

The mod main class. Provides global entry-point methods.

```java
package dev.anvilcraft.resource.ageratum;

public class Ageratum {
    /** Mod ID = "ageratum" */
    public static final String MOD_ID = "ageratum";

    /**
     * Creates a ResourceLocation with the "ageratum" namespace.
     * Equivalent to ResourceLocation.fromNamespaceAndPath("ageratum", path)
     */
    public static ResourceLocation location(String path);

    /**
     * Notifies the given player's client to open a guide document.
     * Sends a network packet from the server to the client.
     *
     * @param player   Target player (server-side ServerPlayer)
     * @param location Document resource location, e.g.
     *                 ResourceLocation.fromNamespaceAndPath("mymod", "ageratum/en_us/index.md")
     */
    public static void openGuide(ServerPlayer player, ResourceLocation location);
}
```

---

## `AgeratumRegistries`

Defines all NeoForge custom registries. The central hub for all extension points.

```java
package dev.anvilcraft.resource.ageratum.client.registries;

public final class AgeratumRegistries {

    // ── Extension Component Factory Registry ───────────────────────────────────

    public static final ResourceKey<Registry<MDExtensionComponentFactory>>
        EXTENSION_COMPONENT_FACTORY_REGISTRY_KEY;

    public static final DeferredRegister<MDExtensionComponentFactory>
        EXTENSION_COMPONENT_FACTORIES;                // Ageratum-internal

    public static final Registry<MDExtensionComponentFactory>
        EXTENSION_COMPONENT_FACTORY_REGISTRY;         // Runtime access

    // ── Inline Style Parser Registry ──────────────────────────────────────────

    public static final ResourceKey<Registry<MDInlineStyleParser>>
        INLINE_STYLE_PARSER_REGISTRY_KEY;

    public static final DeferredRegister<MDInlineStyleParser>
        INLINE_STYLE_PARSERS;

    public static final Registry<MDInlineStyleParser>
        INLINE_STYLE_PARSER_REGISTRY;

    // ── Recipe Component Factory Registry ─────────────────────────────────────

    public static final ResourceKey<Registry<MDRecipeComponent.RecipeComponentFactory<?>>>
        RECIPE_COMPONENT_FACTORY_REGISTRY_KEY;

    public static final DeferredRegister<MDRecipeComponent.RecipeComponentFactory<?>>
        RECIPE_COMPONENT_FACTORIES;

    public static final Registry<MDRecipeComponent.RecipeComponentFactory<?>>
        RECIPE_COMPONENT_FACTORY_REGISTRY;

    /**
     * Binds all custom registries to the mod event bus.
     * Called internally by Ageratum — external mods do NOT need to call this.
     */
    public static void register(IEventBus modEventBus);
}
```

**How external mods plug in:**

```java
DeferredRegister<MDExtensionComponentFactory> myRegister =
    DeferredRegister.create(
        AgeratumRegistries.EXTENSION_COMPONENT_FACTORY_REGISTRY_KEY,
        "mymod"
    );
myRegister.register(modEventBus);
```

---

## `MDExtensionComponentFactory`

Extension component factory interface. Register instances into `EXTENSION_COMPONENT_FACTORY_REGISTRY_KEY`.

```java
@FunctionalInterface
public interface MDExtensionComponentFactory {
    MDComponent create(MDExtensionContext context);
}
```

---

## `MDExtensionContext`

Context object received by an extension component factory.

```java
public record MDExtensionContext(
    ResourceLocation id,                   // e.g. mymod:section
    String rawParams,                      // Raw params string (colon syntax)
    Map<String, String> params,            // Parsed key-value pairs (tag syntax)
    List<MDComponent> renderedContent,     // Parsed child components
    String rawContent                      // Raw block content text
) {}
```

---

## `MDInlineStyleParser`

Inline style parser interface. Register instances into `INLINE_STYLE_PARSER_REGISTRY_KEY`.

```java
public interface MDInlineStyleParser {
    /** Priority: lower value = higher precedence at the same position */
    int priority();

    /**
     * Attempts to match an inline tag starting at pos.
     * Returns null on no match.
     */
    @Nullable
    MDComponent.InlineStyleMatch parse(String text, int pos);

    /**
     * Creates a simple open/close tag parser.
     *
     * @param priority       Parser priority
     * @param openTagPattern Open tag regex pattern
     * @param closeTag       Close tag literal string
     * @param styleFactory   Function returning a new Style from parent Style + Matcher
     */
    static MDInlineStyleParser create(
        int priority,
        Pattern openTagPattern,
        String closeTag,
        BiFunction<Style, Matcher, Style> styleFactory
    );
}
```

---

## `MDRecipeComponent.RecipeComponentFactory<T>`

Recipe component factory interface. Register instances into `RECIPE_COMPONENT_FACTORY_REGISTRY_KEY`.

```java
public interface RecipeComponentFactory<T extends Recipe<?>> {
    RecipeType<T> type();
    MDRecipeComponent create(T recipe);

    static <T extends Recipe<?>> RecipeComponentFactory<T> create(
        RecipeType<T> type,
        Function<T, MDRecipeComponent> factory
    );
}
```

---

## `GuideDocumentLoader`

Pure static utility for document path resolution and content reading.

```java
public final class GuideDocumentLoader {

    /** Default language code = "en_us" */
    public static final String DEFAULT_LANGUAGE_CODE = "en_us";

    /** Converts namespace + file argument to ResourceLocation using the default language */
    public static ResourceLocation toDocumentLocation(String namespace, String fileArgument);

    /** Converts namespace + language code + file argument to ResourceLocation */
    public static ResourceLocation toDocumentLocation(
        String namespace, String languageCode, @Nullable String fileArgument
    );

    /**
     * Resolves the first existing document location in order:
     * current language → en_us fallback.
     */
    public static Optional<ResourceLocation> resolveExistingLocation(
        ResourceManager resourceManager,
        String namespace,
        String languageCode,
        @Nullable String fileArgument
    );

    /** Checks whether a document exists in the current resource pack */
    public static boolean exists(ResourceManager resourceManager, ResourceLocation location);

    /** Reads a document's full text content as UTF-8 */
    public static String read(ResourceManager resourceManager, ResourceLocation location);

    /** Lists all namespaces that contain ageratum documents (sorted) */
    public static List<String> listNamespaces(ResourceManager resourceManager, String languageCode);

    /**
     * Lists all available document relative paths under the given namespace
     * (without .md extension, sorted).
     */
    public static List<String> listFiles(ResourceManager resourceManager,
                                         String namespace, String languageCode);
}
```

---

## `GuideDocumentCache`

Pre-parsed document cache. Populated automatically on resource pack load/reload.

```java
public final class GuideDocumentCache {

    /**
     * Returns the resource reload listener.
     * Ageratum registers this automatically — external mods rarely need this.
     */
    public static PreparableReloadListener reloadListener();

    /** Gets the pre-parsed document by ResourceLocation */
    public static Optional<MDDocument> getParsedDocument(ResourceLocation location);

    /** Gets a mutable copy of the pre-parsed component list */
    public static Optional<List<MDComponent>> getParsedComponents(ResourceLocation location);

    /** Gets the navigation tree for a given namespace and language */
    public static Optional<NavigationTree> getNavigationTree(String namespace, String languageCode);

    // ── Nested record types ────────────────────────────────────────────────────

    public record NavigationTree(
        List<NavigationDocument> rootDocuments,
        List<NavigationDirectory> rootDirectories
    ) {}

    public record NavigationDirectory(
        String namespace,
        String name,
        @Nullable NavigationDocument indexDocument,
        List<NavigationDocument> documents,
        List<NavigationDirectory> children
    ) {}

    public record NavigationDocument(
        String fileArgument,
        String title,
        ResourceLocation location
    ) {}
}
```

---

## `MDDocument`

Markdown document model produced by `MarkdownParser`.

```java
public record MDDocument(
    @Nullable ResourceLocation sourceLocation,
    Map<String, Object> frontMatter,
    List<MDComponent> components
) {
    /** Resolves the document title (frontMatter → first h1 → file name) */
    public String getTitle(String fileName);

    /** Resolves the document title (falls back to "Untitled" with no fileName) */
    public String getTitle();

    /** Returns the source file name only (no directory path) */
    public Optional<String> getSourceFileName();
}
```

---

## `MDComponent` (Base Class)

Base class for all renderable components.

```java
public abstract class MDComponent {
    protected final FormattedText text;

    /** Renders the component in the given bounds */
    public void render(GuiGraphics guiGraphics, Minecraft minecraft,
                       int maxX, int maxY, float mouseX, float mouseY);

    /** Calculates the component's rendered height for the given width */
    public int getHeight(Minecraft minecraft, int maxX, int maxY);

    /** Returns the text style at a coordinate (for click/hover) */
    @Nullable
    public Style getStyleAtPosition(Minecraft minecraft, double mouseX,
                                    double mouseY, int maxX);

    /** Parses raw Markdown text (inline syntax) into FormattedText */
    public static FormattedText textFormat(String text);
}
```

---

## `MarkdownParser`

Block-level Markdown parser.

```java
public class MarkdownParser {
    public MarkdownParser();

    /**
     * Registers a line-level component parser.
     *
     * @param priority Parser priority (lower = higher precedence)
     * @param parser   Parsing function: receives line text, returns component or null
     */
    public void registerComponentParser(int priority, MDComponentParser parser);

    /**
     * Parses raw Markdown text into a document model.
     *
     * @param location Source resource location (may be null)
     * @param markdown Raw Markdown text
     */
    public MDDocument parseDocument(@Nullable ResourceLocation location, String markdown);
}
```

---

## `AgeratumClientConfig`

Client-side configuration. Managed via `config/ageratum-client.toml`.

```java
public class AgeratumClientConfig {
    /** Whether sidebar tab jumps are recorded in breadcrumbs. Default: false */
    public boolean breadCrumbsHasLabel = false;

    /** Whether to show line numbers in code blocks. Default: true */
    public boolean showCodeBlockLineNumbers = true;

    /** Whether to allow line wrapping in code block content. Default: true */
    public boolean allowCodeBlockLineContentLineBreaks = true;
}
```

Access via `AgeratumClient.CONFIG` (client-only).

---

## Network Protocol

### `OpenGuidePayload`

Direction: **Server → Client**  
Purpose: Instructs the client to open a guide document.

```java
public record OpenGuidePayload(ResourceLocation location) implements CustomPacketPayload {
    public static final Type<OpenGuidePayload> TYPE;         // ID: ageratum:open_guide
    public static final StreamCodec<...> STREAM_CODEC;
}
```

Network version: `"1"`

---

## Constants & Enums

### Built-in Notice Box Types

```java
public enum NoticeType {
    INFO,     // Blue
    TIP,      // Green
    WARNING,  // Orange
    DANGER    // Red
}
```

### Key Constants

| Constant | Value | Location |
|----------|-------|----------|
| `MOD_ID` | `"ageratum"` | `Ageratum` |
| `DEFAULT_LANGUAGE_CODE` | `"en_us"` | `GuideDocumentLoader` |
| `NETWORK_VERSION` | `"1"` | `AgeratumNetwork` |

