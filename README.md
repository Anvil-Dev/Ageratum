<div align="center">

# 藿香 | [Ageratum](./README.en.md)

<img src=".idea/icon.png" style="width: 128px; height: 128px" alt="Ageratum Logo">

![](https://cdn.jsdelivr.net/npm/@intergrav/devins-badges@3/assets/cozy/built-with/java21_vector.svg)
![](https://cdn.jsdelivr.net/npm/@intergrav/devins-badges@3/assets/cozy/supported/neoforge_vector.svg)

![](https://cdn.jsdelivr.net/npm/@intergrav/devins-badges@3/assets/cozy/unsupported/fabric_vector.svg)
![](https://cdn.jsdelivr.net/npm/@intergrav/devins-badges@3/assets/cozy/unsupported/forge_vector.svg)

[![](https://cdn.jsdelivr.net/npm/@intergrav/devins-badges@3/assets/cozy/available/modrinth_vector.svg)][Modrinth]
[![](https://cdn.jsdelivr.net/npm/@intergrav/devins-badges@3/assets/cozy/available/curseforge_vector.svg)][CurseForge]

[![](https://cdn.jsdelivr.net/npm/@intergrav/devins-badges@3/assets/cozy/documentation/readthedocs_vector.svg)][Documentation]
[![](https://cdn.jsdelivr.net/npm/@intergrav/devins-badges@3/assets/cozy/donate/patreon-plural_vector.svg)][Patreon]
[![](https://cdn.jsdelivr.net/npm/@intergrav/devins-badges@3/assets/cozy/social/discord-plural_vector.svg)][Discord]

[![License](https://img.shields.io/badge/License-LGPL%20v3-blue.svg)][License]
[![Asset License](https://img.shields.io/badge/Asset%20License-ARR-green.svg)][Asset License]

[Patreon]: https://www.patreon.com/gugle2308
[License]: https://spdx.org/licenses/LGPL-3.0-only.html
[Asset License]: https://gist.github.com/Gu-ZT/38961ed5c97500cf61b04ab048fa38ad
[Modrinth]: https://modrinth.com/mod/ageratum
[CurseForge]: https://www.curseforge.com/minecraft/mc-mods/ageratum
[Documentation]: https://ageratum.anvilcraft.dev/
[Discord]: https://discord.gg/4ZsG8m9XcM

</div>

# Ageratum - 模组手册框架

一个为 Minecraft NeoForge 设计的手册模组，用于为其它模组提供游戏内指引与文档阅读能力。Ageratum 提供丰富的 Markdown 渲染、i18n
国际化，以及可扩展的自定义语法与组件机制。

## [文档](https://www.anvilcraft.dev/posts/docs/ageratum/)

## 功能特性

### 核心 Markdown 支持

✅ **块级元素**

- ATX 标题（`# ~ ######`）与 Setext 标题（下划线式）
- 段落与换行处理
- 有序列表、无序列表、任务列表（支持多层嵌套）
- 块引用（支持多层嵌套）
- 围栏代码块（反引号与波浪线）与缩进代码块
- 水平分隔线
- 表格（含对齐设置）
- 图片（命名空间本地化引用）

✅ **内联元素**

- **粗体**、*斜体*、~~删除线~~
- 行内代码跨度（支持多反引号）
- [超链接](https://example.com) 与自动链接
- 转义字符支持
- 自定义颜色标签
- **悬停与点击事件**（`<hover>` 与 `<click>` 标签）

✅ **高级特性**

- 引用链接定义与引用链接语法
- 链接自动展开
- 代码块行号与语法着色
- 表格列对齐（左/中/右）

### 国际化（i18n）

- 文档资源按 `ageratum/<language_code>/` 组织（如 `en_us`、`zh_cn`）
- 默认从 `en_us` 读取，缺失文档自动回退到英文版本
- 完全支持多字节文字（中文、日文等）

### 扩展语法

两种块级扩展语法允许自定义组件：

#### 1. 冒号语法

```markdown
::: info
这是一个提示框。
:::

::: tip
这是一个建议。
:::

::: warning
这是一个警告。
:::

::: danger
这是一个危险警告。
:::
```

#### 2. 标签语法

```markdown
<namespace:component key="value" param=123>
块内容支持 Markdown 语法。
</namespace:component>

<namespace:component/>
自闭合形式，不含内容。
```

命名空间可省略，默认使用 `ageratum:`。

### 内置扩展组件

- `ageratum:info` - 蓝色提示框
- `ageratum:tip` - 绿色建议框
- `ageratum:warning` - 橙色警告框
- `ageratum:danger` - 红色危险框

### 悬停与点击事件

支持交互式文本，允许玩家悬停查看提示或点击执行操作。

#### 悬停事件（`<hover>`）

在文本上悬停时显示提示信息：

```markdown
<hover type="SHOW_TEXT" data="这是提示文本">在我上面悬停</hover>
```

**支持的类型：**

- `SHOW_TEXT`：显示纯文本提示信息（`data` 为提示文本内容）

#### 点击事件（`<click>`）

点击文本时执行相应操作：

```markdown
<click type="OPEN_URL" data="https://example.com">点击打开链接</click>
<click type="COPY_TO_CLIPBOARD" data="复制的内容">点击复制</click>
<click type="SUGGEST_COMMAND" data="/say hello">点击建议命令</click>
```

**支持的类型：**

- `OPEN_URL`：打开网址（`data` 为完整 URL）
- `COPY_TO_CLIPBOARD`：复制文本到剪贴板（`data` 为要复制的内容）
- `SUGGEST_COMMAND`：在聊天框中建议命令（`data` 为命令文本）

#### 组合使用

可在同一文本中组合多种样式：

```markdown
<hover type="SHOW_TEXT" data="这是一条提示"><click type="OPEN_URL" data="https://example.com">点击查看官网</click></hover>
```

### 预加载与缓存

- 资源包加载时自动扫描并预解析 Markdown 文档为 `MDComponent` 列表
- 首次打开文档时直接使用缓存组件，无解析延迟
- 资源包重载时自动刷新缓存

### 跨端文档打开

```java
// 客户端直接打开
Ageratum.openGuide(ResourceLocation location);

// 服务端通知客户端打开（网络发包）
Ageratum.

openGuide(ResourceLocation location);
```

## 项目结构

### 目录层次

```
src/main/java/dev/anvilcraft/resource/ageratum/
├── Ageratum.java                           // 模组主类 + 命令注册
├── GuideDocumentLoader.java                // 文档加载工具（路径解析、资源枚举）
├── GuideDocumentCache.java                 // 预加载缓存与资源重载监听
│
├── client/
│   ├── AgeratumClient.java                 // 客户端钩子（预留）
│   ├── gui/
│   │   └── GuideScreen.java                // 文档读取界面
│   └── feat/markdown/
│       ├── MarkdownParser.java             // Markdown 块级解析器
│       ├── BuiltinExtensionComponents.java // 内置扩展组件注册
│       ├── BlockExtensionState.java        // 块级扩展状态机
│       ├── SelfClosingBlockExtensionState.java
│       ├── ExtensionParamParser.java       // 参数解析工具
│       ├── MDExtensionContext.java         // 扩展执行上下文
│       ├── MDExtensionComponentFactory.java // 扩展工厂接口
│       └── component/
│           ├── MDComponent.java            // 基类 + 内联语法解析
│           ├── MDTextComponent.java        // 纯文本段落
│           ├── MDHeaderComponent.java      // 标题
│           ├── MDCodeBlockComponent.java   // 代码块
│           ├── MDListComponent.java        // 列表（含任务列表）
│           ├── MDQuoteComponent.java       // 块引用
│           ├── MDTableComponent.java       // 表格
│           ├── MDImageComponent.java       // 图片
│           ├── MDHorizontalRuleComponent.java
│           └── MDNoticeBoxComponent.java   // 提示框容器
│
└── network/
    ├── AgeratumNetwork.java                // 网络注册与分发
    └── OpenGuidePayload.java               // 文档打开网络包
```

### 设计原则

- **分离关注点**：每个类仅负责单一职责
- **无过长类**：最长的文件 ~400 行，内部类均提取为独立文件
- **足量文档**：所有公共 API 均有中文 Javadoc，复杂逻辑有行内注释
- **可扩展性**：通过 NeoForge 注册机制（`DeferredRegister` / `RegisterEvent`）注册自定义块类型

## 使用指南

### 玩家

在客户端执行命令打开指南：

```
/ageratum <namespace> [file]

例：
/ageratum ageratum                  # 打开 ageratum:en_us/index.md
/ageratum mymod guide              # 打开 mymod:en_us/guide.md
/ageratum mymod zh_cn/tutorial     # 打开 mymod:zh_cn/tutorial.md
```

### 开发者

#### 注册自定义扩展组件

推荐遵循 NeoForge 文档中的注册方式：

1. `DeferredRegister`（推荐）
2. `RegisterEvent`（高级场景）

```java
public static final DeferredRegister<MDExtensionComponentFactory> EXT_COMPONENT_FACTORIES =
    AgeratumRegistries.createExtensionComponentFactoryRegister("your_modid");

public static final DeferredHolder<MDExtensionComponentFactory, MDExtensionComponentFactory> CUSTOM =
    EXT_COMPONENT_FACTORIES.register(
        "custom", () ->
            context -> new MyComponent(context.renderedContent(), context.params())
    );

// 在你的模组构造函数中
EXT_COMPONENT_FACTORIES.

register(modEventBus);
```

#### 注册自定义行内样式解析器

行内样式解析器通过 `INLINE_STYLE_PARSER_REGISTRY_KEY` 注册，解析器会在 `MDComponent` 里按优先级参与标签匹配。

```java
package com.example.mymod.client.markdown;

import dev.anvilcraft.resource.ageratum.client.feat.markdown.component.MDInlineStyleParser;
import dev.anvilcraft.resource.ageratum.client.registries.AgeratumRegistries;
import net.minecraft.network.chat.Style;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.regex.Pattern;

public final class MyInlineStyleParsers {
    // 使用你自己的 modid，而不是 ageratum
    public static final DeferredRegister<MDInlineStyleParser> INLINE_STYLE_PARSERS = DeferredRegister.create(
        AgeratumRegistries.INLINE_STYLE_PARSER_REGISTRY_KEY,
        "mymod"
    );

    // 示例标签：<rainbow>文本</rainbow>
    public static final DeferredHolder<MDInlineStyleParser, MDInlineStyleParser> RAINBOW =
        INLINE_STYLE_PARSERS.register(
            "rainbow",
            () -> MDInlineStyleParser.create(
                100, // priority：数值越小越先参与同位置竞争
                Pattern.compile("<rainbow>"),
                "</rainbow>",
                (Style parentStyle, java.util.regex.Matcher matcher) -> parentStyle.withColor(0xFF55FF)
            )
        );

    private MyInlineStyleParsers() {
    }
}
```

在你的客户端初始化中注册：

```java
public class MyModClient {
    public MyModClient(IEventBus modEventBus) {
        MyInlineStyleParsers.INLINE_STYLE_PARSERS.register(modEventBus);
    }
}
```

Markdown 使用示例：

```markdown
普通文本 <rainbow>彩色文本</rainbow> 普通文本
```

完整示例文档见：`docs/inline-style-parser-example.zh.md`。

#### 添加文档

在资源包中创建：

```
assets/<namespace>/ageratum/<language>/index.md
assets/<namespace>/ageratum/en_us/index.md
assets/<namespace>/ageratum/zh_cn/index.md
```

### 配方组件（`<recipe/>`）

使用 `recipe` 扩展可以在文档中直接渲染配方：

```markdown
<recipe id="minecraft:acacia_boat"/>
```

- `id`：必填，目标配方的 `ResourceLocation`
- 当前内置支持：`RecipeType.CRAFTING`（工作台配方）
- 渲染行为：输入网格使用每个 `Ingredient` 的第一个候选物品进行静态展示
- 回退行为：若客户端世界未就绪、配方不存在或无匹配工厂，则该组件不占可见高度

开发者可通过 `AgeratumRegistries.RECIPE_COMPONENT_FACTORIES` 注册更多配方类型：

```java
public static final DeferredHolder<MDRecipeComponent.RecipeComponentFactory<?>, MDRecipeComponent.RecipeComponentFactory<?>> SMELTING =
    AgeratumRegistries.RECIPE_COMPONENT_FACTORIES.register(
        "smelting",
        () -> MDRecipeComponent.RecipeComponentFactory.create(RecipeType.SMELTING, MDSmeltingRecipeComponent::new)
    );
```

### 结构 NBT 组件（`<structure/>`）

使用结构扩展可以在文档中直接渲染 `.nbt` 结构文件的摘要、俯视方块预览与 NBT 树：

```markdown
<structure id="minecraft:village/plains/houses/plains_small_house_1"/>

<structure id="./test.nbt"/>
```

- `id` / `path`：必填，目标结构文件的 `ResourceLocation`
- `maxDepth`：可选，最大展开深度，默认 `2`
- `maxEntries`：可选，每层最多显示的键/列表项数量，默认 `12`
- 支持相对路径，按当前文档目录解析；可直接读取资源包中与 Markdown 同目录的 `.nbt` 文件，预览模式下则会从 `run/ageratum_review/` 读取对应文件
- 组件会优先显示结构尺寸、调色板、方块数、实体数等摘要，其后渲染俯视方块预览，并在下方展示受限深度的 NBT 树
- 将鼠标悬停到预览区域的方块上时，可查看方块 ID、结构坐标、palette 索引及是否带块实体数据

## 许可证

* 除非另有说明，否则所有代码均遵循我们的 [LICENSE 文件（LGPL-3.0）](./LICENSE) 中的规定。
* 除非另有说明，否则所有非代码资产遵循我们的 [ASSETS_LICENSE 文件（ARR）](./ASSETS_LICENSE)中的规定。

