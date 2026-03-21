<div align="center">

# 藿香 | [Ageratum](./README.en.md)

<img src=".idea/icon.png" style="width: 128px; height: 128px" alt="Ageratum Logo">

[![License](https://img.shields.io/badge/License-LGPL%20v3-blue.svg)](./LICENSE)
[![Asset License](https://img.shields.io/badge/Asset%20License-ARR-green.svg)](./ASSETS_LICENSE)

</div>

# Ageratum - 模组手册框架

一个为 Minecraft Forge/NeoForge 设计的手册模组，用于为其它模组提供游戏内指引与文档阅读能力。Ageratum 提供丰富的 Markdown 渲染、i18n 国际化，以及可扩展的自定义语法与组件机制。

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

### 预加载与缓存

- 资源包加载时自动扫描并预解析 Markdown 文档为 `MDComponent` 列表
- 首次打开文档时直接使用缓存组件，无解析延迟
- 资源包重载时自动刷新缓存

### 跨端文档打开

```java
// 客户端直接打开
Ageratum.openGuide(ResourceLocation location);

// 服务端通知客户端打开（网络发包）
Ageratum.openGuide(ResourceLocation location);
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
- **可扩展性**：通过 `registerExtensionComponent()` 注册自定义块类型

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

```java
MarkdownParser.registerExtensionComponent(
    Ageratum.location("custom"),
    context -> new MyComponent(context.renderedContent(), context.params())
);
```

#### 添加文档

在资源包中创建：
```
assets/<namespace>/ageratum/<language>/index.md
assets/<namespace>/ageratum/en_us/index.md
assets/<namespace>/ageratum/zh_cn/index.md
```

## 许可证

* 除非另有说明，否则所有代码均遵循我们的 [LICENSE 文件（LGPL-3.0）](./LICENSE) 中的规定。
* 除非另有说明，否则所有非代码资产遵循我们的 [ASSETS_LICENSE 文件（ARR）](./ASSETS_LICENSE)中的规定。

