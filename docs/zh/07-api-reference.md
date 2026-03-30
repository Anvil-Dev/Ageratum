# API 参考

本文档列出 Ageratum 所有面向开发者的公共 API，包括类、方法签名和说明。

---

## 包结构

```
dev.anvilcraft.resource.ageratum
├── Ageratum                                    # 模组主类（入口点）
│
├── network/
│   ├── AgeratumNetwork                         # 网络注册与发送
│   └── OpenGuidePayload                        # 打开文档网络包
│
└── client/
    ├── AgeratumClient                          # 客户端初始化
    ├── AgeratumClientConfig                    # 客户端配置
    │
    ├── registries/
    │   ├── AgeratumRegistries                  # 注册表定义
    │   ├── BuiltinExtensionComponents          # 内置扩展组件
    │   ├── BuiltinInlineStyleParsers           # 内置行内解析器
    │   └── BuiltinRecipeComponentFactories     # 内置配方工厂
    │
    ├── gui/
    │   └── GuideScreen                         # 文档阅读界面
    │
    └── feat/markdown/
        ├── MarkdownParser                      # Markdown 解析器
        ├── GuideDocumentLoader                 # 文档路径与读取工具
        ├── GuideDocumentCache                  # 预解析缓存
        ├── MDDocument                          # 文档模型
        ├── MDExtensionContext                  # 扩展执行上下文
        ├── MDExtensionComponentFactory         # 扩展工厂接口
        │
        └── component/
            ├── MDComponent                     # 组件基类
            ├── MDTextComponent                 # 文本段落
            ├── MDHeaderComponent               # 标题
            ├── MDCodeBlockComponent            # 代码块
            ├── MDListComponent                 # 列表
            ├── MDQuoteComponent                # 引用块
            ├── MDTableComponent                # 表格
            ├── MDImageComponent                # 图片
            ├── MDHorizontalRuleComponent       # 水平线
            ├── MDNoticeBoxComponent            # 提示框
            ├── MDInlineStyleParser             # 行内解析器接口
            └── recipe/
                ├── MDRecipeComponent           # 配方组件基类
                └── MDCraftingTableRecipeComponent # 工作台配方组件
```

---

## `Ageratum`

模组主类，提供全局入口方法。

```java
package dev.anvilcraft.resource.ageratum;

public class Ageratum {
    /** 模组 ID = "ageratum" */
    public static final String MOD_ID = "ageratum";

    /**
     * 创建以 ageratum 为命名空间的 ResourceLocation。
     * 等同于 ResourceLocation.fromNamespaceAndPath("ageratum", path)
     */
    public static ResourceLocation location(String path);

    /**
     * 从服务端通知指定玩家打开文档。
     * 通过网络包发送给客户端，客户端收到后打开 GuideScreen。
     *
     * @param player   目标玩家（服务端 ServerPlayer）
     * @param location 文档资源位置，如 ResourceLocation.fromNamespaceAndPath("mymod", "ageratum/en_us/index.md")
     */
    public static void openGuide(ServerPlayer player, ResourceLocation location);
}
```

---

## `AgeratumRegistries`

自定义注册表定义，是所有扩展点的核心。

```java
package dev.anvilcraft.resource.ageratum.client.registries;

public final class AgeratumRegistries {

    // ── 扩展组件工厂注册表 ──────────────────────────────────────────────────

    /** 注册表键 */
    public static final ResourceKey<Registry<MDExtensionComponentFactory>>
        EXTENSION_COMPONENT_FACTORY_REGISTRY_KEY;

    /** 延迟注册器（Ageratum 内部使用） */
    public static final DeferredRegister<MDExtensionComponentFactory>
        EXTENSION_COMPONENT_FACTORIES;

    /** 注册表实例（运行时访问） */
    public static final Registry<MDExtensionComponentFactory>
        EXTENSION_COMPONENT_FACTORY_REGISTRY;

    // ── 行内组件工厂注册表 ────────────────────────────────────────────────

    public static final ResourceKey<Registry<MDInlineComponentFactory>>
        INLINE_COMPONENT_FACTORY_REGISTRY_KEY;

    public static final DeferredRegister<MDInlineComponentFactory>
        INLINE_COMPONENT_FACTORIES;

    public static final Registry<MDInlineComponentFactory>
        INLINE_COMPONENT_FACTORY_REGISTRY;

    // ── 行内样式解析器注册表 ────────────────────────────────────────────────

    public static final ResourceKey<Registry<MDInlineStyleParser>>
        INLINE_STYLE_PARSER_REGISTRY_KEY;

    public static final DeferredRegister<MDInlineStyleParser>
        INLINE_STYLE_PARSERS;

    public static final Registry<MDInlineStyleParser>
        INLINE_STYLE_PARSER_REGISTRY;

    // ── 配方组件工厂注册表 ──────────────────────────────────────────────────

    public static final ResourceKey<Registry<MDRecipeComponent.RecipeComponentFactory<?>>>
        RECIPE_COMPONENT_FACTORY_REGISTRY_KEY;

    public static final DeferredRegister<MDRecipeComponent.RecipeComponentFactory<?>>
        RECIPE_COMPONENT_FACTORIES;

    public static final Registry<MDRecipeComponent.RecipeComponentFactory<?>>
        RECIPE_COMPONENT_FACTORY_REGISTRY;

    // ── 批量注册 ────────────────────────────────────────────────────────────

    /**
     * 将所有自定义注册表绑定到模组事件总线。
     * 仅供 Ageratum 内部调用，外部模组不需要调用此方法。
     */
    public static void register(IEventBus modEventBus);
}
```

**外部模组接入方式：**

```java
// 使用 DeferredRegister.create() 指向对应的 REGISTRY_KEY
DeferredRegister<MDExtensionComponentFactory> myRegister =
    DeferredRegister.create(
        AgeratumRegistries.EXTENSION_COMPONENT_FACTORY_REGISTRY_KEY,
        "mymod"
    );

// 在 mod 事件总线上注册
myRegister.register(modEventBus);
```

---

## `MDExtensionComponentFactory`

扩展组件工厂接口，注册到 `EXTENSION_COMPONENT_FACTORY_REGISTRY_KEY`。

```java
@FunctionalInterface
public interface MDExtensionComponentFactory {
    MDComponent create(MDExtensionContext context);
}
```

---

## `MDExtensionContext`

扩展组件接收的上下文对象。

```java
public record MDExtensionContext(
    ResourceLocation sourceLocation,      // 当前文档位置
    ResourceLocation id,                  // 组件 ID（如 mymod:section）
    String rawParams,                     // 原始参数字符串（冒号语法）
    Map<String, String> params,           // 解析后的键值对（标签语法）
    List<MDComponent> renderedContent,    // 块内容解析后的子组件
    String rawContent                     // 块内容原始文本
) {}
```

---

## `MDInlineStyleParser`

行内样式解析器接口，注册到 `INLINE_STYLE_PARSER_REGISTRY_KEY`。

```java
public interface MDInlineStyleParser {
    /** 优先级：值越小越优先 */
    int priority();

    /**
     * 从 text 的 pos 位置开始尝试匹配行内标签。
     * 返回 null 表示未命中。
     */
    @Nullable
    MDComponent.InlineStyleMatch parse(String text, int pos);

    /**
     * 快速创建简单开始/结束标签解析器。
     *
     * @param priority       解析优先级
     * @param openTagPattern 开始标签的正则模式
     * @param closeTag       结束标签的字面字符串
     * @param styleFactory   根据父 Style 和 Matcher 返回新 Style 的函数
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

## `MDInlineComponentFactory`

行内组件工厂接口，注册到 `INLINE_COMPONENT_FACTORY_REGISTRY_KEY`。

```java
@FunctionalInterface
public interface MDInlineComponentFactory {
    FormattedText create(MDInlineComponentContext context);
}
```

---

## `MDInlineComponentContext`

行内组件接收的上下文对象。

```java
public record MDInlineComponentContext(
    ResourceLocation id,          // 行内组件 ID（如 mymod:badge）
    String rawParams,             // 原始参数字符串
    Map<String, String> params,   // 解析后的键值对
    Style baseStyle               // 当前继承样式
) {}
```

---

## `MDRecipeComponent.RecipeComponentFactory<T>`

配方组件工厂接口，注册到 `RECIPE_COMPONENT_FACTORY_REGISTRY_KEY`。

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

文档路径解析与内容读取工具类（纯静态工具）。

```java
public final class GuideDocumentLoader {

    /** 默认语言代码 = "en_us" */
    public static final String DEFAULT_LANGUAGE_CODE = "en_us";

    /**
     * 将命名空间和文件参数转为 ResourceLocation（使用默认语言）。
     * 路径约定：namespace:ageratum/en_us/<normalizedFile>.md
     */
    public static ResourceLocation toDocumentLocation(String namespace, String fileArgument);

    /**
     * 将命名空间、语言代码和文件参数转为 ResourceLocation。
     */
    public static ResourceLocation toDocumentLocation(
        String namespace, String languageCode, @Nullable String fileArgument
    );

    /**
     * 按"当前语言 → en_us"顺序查找第一个实际存在的文档位置。
     */
    public static Optional<ResourceLocation> resolveExistingLocation(
        ResourceManager resourceManager,
        String namespace,
        String languageCode,
        @Nullable String fileArgument
    );

    /** 检查指定文档是否存在于资源包中 */
    public static boolean exists(ResourceManager resourceManager, ResourceLocation location);

    /** 读取指定文档的全文内容（UTF-8） */
    public static String read(ResourceManager resourceManager, ResourceLocation location);

    /** 列出所有含有 ageratum 文档的命名空间（有序） */
    public static List<String> listNamespaces(ResourceManager resourceManager, String languageCode);

    /** 列出指定命名空间下所有可用文档的相对路径（不含 .md，有序） */
    public static List<String> listFiles(ResourceManager resourceManager, String namespace, String languageCode);
}
```

---

## `GuideDocumentCache`

文档预解析缓存，资源包加载/重载时自动填充。

```java
public final class GuideDocumentCache {

    /**
     * 返回资源重载监听器，用于注册到客户端重载事件。
     * Ageratum 内部已自动注册，外部模组通常不需要手动调用。
     */
    public static PreparableReloadListener reloadListener();

    /**
     * 根据 ResourceLocation 获取预解析的文档对象。
     */
    public static Optional<MDDocument> getParsedDocument(ResourceLocation location);

    /**
     * 根据 ResourceLocation 获取预解析的渲染组件列表（独立副本）。
     */
    public static Optional<List<MDComponent>> getParsedComponents(ResourceLocation location);

    /**
     * 获取指定命名空间和语言的导航树。
     */
    public static Optional<NavigationTree> getNavigationTree(String namespace, String languageCode);

    // ── 内部记录类（可用于导航树遍历） ────────────────────────────────────────

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

Markdown 文档模型，由 `MarkdownParser` 解析生成。

```java
public record MDDocument(
    @Nullable ResourceLocation sourceLocation,  // 文档资源位置
    Map<String, Object> frontMatter,            // Front Matter 键值对
    List<MDComponent> components                // 渲染组件列表
) {
    /** 获取文档标题（按 frontMatter → 一级标题 → 文件名 回退） */
    public String getTitle(String fileName);

    /** 获取文档标题（无文件名上下文，最终回退为 "Untitled"） */
    public String getTitle();

    /** 获取源文件名（仅文件名，不含目录） */
    public Optional<String> getSourceFileName();
}
```

---

## `MDComponent`（基类）

所有渲染组件的基类。

```java
public abstract class MDComponent {
    protected final FormattedText text;

    /** 在区域内渲染组件 */
    public void render(GuiGraphics guiGraphics, Minecraft minecraft,
                       int maxX, int maxY, float mouseX, float mouseY);

    /** 计算在指定宽度下的渲染高度 */
    public int getHeight(Minecraft minecraft, int maxX, int maxY);

    /** 获取指定坐标对应的文本样式（用于点击/悬停） */
    @Nullable
    public Style getStyleAtPosition(Minecraft minecraft, double mouseX, double mouseY, int maxX);

    /** 将原始 Markdown 文本解析为 FormattedText（含行内语法） */
    public static FormattedText textFormat(String text);
}
```

---

## `MarkdownParser`

Markdown 块级解析器。

```java
public class MarkdownParser {
    public MarkdownParser();

    /**
     * 注册行级组件解析器（用于扩展解析流程）。
     * @param priority 优先级，值越小越优先
     * @param parser   解析函数：接收行文本，返回组件或 null
     */
    public void registerComponentParser(int priority, MDComponentParser parser);

    /**
     * 将原始 Markdown 文本解析为文档对象。
     * @param location 文档来源位置（可为 null）
     * @param markdown 原始 Markdown 文本
     */
    public MDDocument parseDocument(@Nullable ResourceLocation location, String markdown);
}
```

---

## `GuideScreen`

文档阅读界面，继承自 `net.minecraft.client.gui.screens.Screen`。

```java
public class GuideScreen extends Screen {
    /**
     * 创建并打开文档阅读界面（客户端调用）。
     *
     * @param location 文档资源位置
     * @return 若文档存在则返回 Optional<GuideScreen>，否则 Optional.empty()
     */
    public static Optional<GuideScreen> open(ResourceLocation location);
}
```

通常由 Ageratum 内部（命令/网络包）自动调用，外部模组一般不直接使用。

---

## `AgeratumClientConfig`

客户端配置字段，通过配置文件 `config/ageratum-client.toml` 管理。

```java
public class AgeratumClientConfig {
    /** 面包屑导航是否记录侧边标签的跳转，默认 false */
    public boolean breadCrumbsHasLabel = false;

    /** 代码块是否显示行号，默认 true */
    public boolean showCodeBlockLineNumbers = true;

    /** 代码块内容是否允许自动换行，默认 true */
    public boolean allowCodeBlockLineContentLineBreaks = true;
}
```

---

## 网络协议

### `OpenGuidePayload`

客户端 → 服务端方向：无。  
服务端 → 客户端方向：通知打开文档。

```java
public record OpenGuidePayload(ResourceLocation location) implements CustomPacketPayload {
    public static final Type<OpenGuidePayload> TYPE;        // ID: ageratum:open_guide
    public static final StreamCodec<...> STREAM_CODEC;      // ResourceLocation 编解码
}
```

网络版本号：`"1"`

---

## 枚举与常量

### 内置提示框类型

```java
public enum NoticeType {
    INFO,     // 蓝色
    TIP,      // 绿色
    WARNING,  // 橙色
    DANGER    // 红色
}
```

### 默认常量

| 常量 | 值 | 位置 |
|------|-----|------|
| `MOD_ID` | `"ageratum"` | `Ageratum` |
| `DEFAULT_LANGUAGE_CODE` | `"en_us"` | `GuideDocumentLoader` |
| `NETWORK_VERSION` | `"1"` | `AgeratumNetwork` |

