# Ageratum 文档系统

欢迎使用 Ageratum！这是一个为 Minecraft 模组设计的强大文档框架。

## 主要功能

- 📖 完整的 Markdown 支持
- 🎯 智能的两级导航系统
- 🔗 灵活的文档间跳转
- 🌐 多语言国际化支持
- ⚡ 高性能缓存机制

## 快速导航

### 📚 指南与教程

- **[快速开始](guide)** - 开始使用 Ageratum
  - [Markdown 语法](guide/introduction)
  - [高级特性](guide/advanced)
  - [API 参考](guide/api)

- **[教程系列](tutorial)** - 循序渐进的学习路径
  - [初级教程](tutorial/basics)
  - [中级教程](tutorial/intermediate)
  - [高级教程](tutorial/advanced)

### 💡 示例与参考

- **[代码示例](examples)** - 实战示例和最佳实践
  - [简单示例](examples/simple)
  - [实战示例](examples/practical)
  - [最佳实践](examples/best_practices)
  - [高级示例](examples/advanced_examples)

### ❓ 常见问题

- **[FAQ](faq)** - 常见问题解答
- **[更新日志](changelog)** - 版本历史和更新说明
- **[快速指南](guide)** - 快速参考

点击事件允许文本响应点击操作：
- <click type="OPEN_URL" data="https://example.com">点击打开链接</click>
- <click type="COPY_TO_CLIPBOARD" data="一些要复制的文本">点击复制文本</click>
- <click type="RUN_COMMAND" data="/ageratum ageratum">点击运行命令</click>

您可以在同一文本中组合多种样式：
- <hover type="SHOW_TEXT" data="组合事件！"><click type="OPEN_URL" data="https://example.com">在我上面悬停或点击我！</click></hover>

---

::: info
这是一个信息框。
:::

::: tip
这是一个提示。
:::

::: warning
这是一个警告。
:::

::: danger
这是一个危险的警告。
:::

---

## 已实现：Setext 标题

Setext 一级标题
=================

Setext 二级标题
-----------------

---

## 已实现：引用块（多层级）

> 一级引用，包含 **粗体** 和 `代码`。
>> 二级引用，包含 ~~删除线~~ 和 [链接](https://example.com/quote)。
>>> 三级引用，包含转义 \*星号\* 和 `字面代码`。
> 从三级回到一级。

---

## 已实现：列表

### 无序列表（多层级，几何符号）

- 0 级项目，包含 *斜体*
    - 1 级项目，包含 ~~删除线~~
        - 2 级项目，包含 **粗体**
            - 3 级项目，包含 `代码`
                - 4 级项目

- 0 级项目，包含 *斜体*
  - 1 级项目，包含 ~~删除线~~
    - 2 级项目，包含 **粗体**
      - 3 级项目，包含 `代码`
        - 4 级项目

### 有序列表（多层级）

1. 0 级有序项，第 1 条
2. 0 级有序项，第 2 条
1. 1 级有序项，第 1 条
    1. 2 级有序项，第 1 条

### 任务列表

- [x] 已完成任务，包含 **粗体** 和 `代码`
- [ ] 未完成任务，包含 *斜体*
    - [x] 嵌套任务（已完成）
        - [ ] 双层嵌套任务（未完成）

---

## 已实现：围栏代码块 - 反引号

```java
// 代码块内部全部按字面渲染
**不是粗体**
_不是斜体_ ~~
不是删除线~~
    [不是链接](https://example.com)
    >
不是
    引用
-[x]
不是
    任务列表
\* \
_ 被转义
但在代码块内都是字面量
```

## 已实现：围栏代码块 - 波浪线

~~~
现在也支持波浪线围栏代码块。
**仍然是字面量** _仍然是字面量_
~~~

## 已实现：缩进代码块（4 个空格）

	int x = 42;
	System.out.println("缩进代码块");
	// 连续缩进行会合并为同一个代码块

---

## 已实现：表格

| 左对齐    |  居中对齐  |    右对齐 |
|:-------|:------:|-------:|
| 单元格 A1 | 单元格 B1 | 单元格 C1 |
| **粗体** |  `代码`  |   *斜体* |
| 单元格 A3 | 单元格 B3 | 单元格 C3 |

---

## 已实现：引用链接定义

[引用链接][example-ref]

[collapsed-ref][]

[shortcut]

[example-ref]: https://example.com

[collapsed-ref]: https://example.com/collapsed

[shortcut]: https://example.com/shortcut

---

## 已实现：图片（独占一行，namespace:path）

![](ageratum:gui/guide/guide.png)

---

## 尚未实现

### 硬换行

第一行  
第二行（行尾两个空格触发硬换行 - 当前仍按软换行处理）

### 引用块内嵌套块级元素

> - 引用中的列表（暂不支持块级嵌套）
    > ```引用中的代码```（暂不支持块级嵌套）

### 脚注、定义列表、数学公式、Front Matter

不属于 CommonMark 核心的扩展语法 - 当前不计划支持。

