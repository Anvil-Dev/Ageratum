# Markdown 语法参考

Ageratum 实现了 CommonMark 的核心子集，并扩展了若干游戏内专有语法。

---

## 块级元素

### 标题

支持 ATX 风格（`#` 号）和 Setext 风格（下划线）：

```markdown
# 一级标题

## 二级标题

### 三级标题

#### 四级标题

##### 五级标题

###### 六级标题

Setext 一级标题
===============

Setext 二级标题
---------------
```

### 段落与换行

连续非空行构成同一段落。两行之间有空行则开始新段落：

```markdown
这是第一段。

这是第二段。
同一段落中的第二行（软换行，渲染时合并为一行）。
```

> **注意**：行尾两个空格触发的"硬换行"当前按软换行处理。

### 引用块

支持多层级嵌套，`>` 符号可累叠：

```markdown
> 一级引用
>> 二级引用
>>> 三级引用
> 回到一级引用
```

### 列表

#### 无序列表

支持 `-`、`+`、`*` 作为列表标记，支持多层级嵌套（以 4 个空格或 1 个 Tab 缩进）：

```markdown
- 项目 A
    - 子项目 A1
        - 孙项目 A1a
- 项目 B
```

每级用不同的几何符号（●、○、■）渲染。

#### 有序列表

```markdown
1. 第一项
2. 第二项
    1. 子项
3. 第三项
```

#### 任务列表

```markdown
- [x] 已完成
- [ ] 未完成
    - [x] 嵌套已完成
```

### 代码块

#### 围栏代码块（反引号）

````markdown
```java
public class Hello {
    public static void main(String[] args) {
        System.out.println("Hello, Ageratum!");
    }
}
```
````

支持语言标记（当前仅影响显示，不做语法高亮）。

#### 围栏代码块（波浪线）

```markdown
~~~
波浪线围栏代码块，与反引号等效。
~~~
```

#### 缩进代码块

行首缩进 4 个空格或 1 个 Tab：

```markdown
    int x = 42;
    System.out.println(x);
```

> 代码块内容**完全按字面渲染**，不解析任何 Markdown 语法。

### 水平分隔线

使用三个或更多 `-`、`*` 或 `_`（允许中间有空格）：

```markdown
---

***
___
```

### 表格

支持列对齐（`:` 号控制）：

```markdown
| 左对齐   |  居中对齐  |    右对齐 |
|:---------|:----------:|----------:|
| 内容 A   |   内容 B   |    内容 C |
| **粗体** |   `代码`   |    *斜体* |
```

表格单元格内支持行内 Markdown（粗体、斜体、代码跨度等）。

### 图片

图片**必须独占一行**，使用命名空间路径：

```markdown
![图片描述](namespace:textures/路径/图片.png)
```

---

## 行内元素

### 粗体

```markdown
**粗体**  或  __粗体__
```

### 斜体

```markdown
*斜体*  或  _斜体_
```

> 下划线斜体（`_斜体_`）前后不能紧接字母或数字。

### 删除线

```markdown
~~删除线~~
```

### 行内代码

```markdown
`代码`  或  ``含有`反引号`的代码``
```

### 链接

#### 内联链接

```markdown
[链接文本](https://example.com)
```

#### 引用链接

```markdown
[链接文本][引用ID]
[collapsed][]
[shortcut]

[引用ID]: https://example.com

[collapsed]: https://example.com/c

[shortcut]: https://example.com/s
```

#### 自动链接

```markdown
<https://example.com>
<user@example.com>
```

### 转义字符

CommonMark 可转义标点符号（`!`、`"`、`#`、`$`、`%`、`&`、`'`、`(`、`)`、`*`、`+`、`,`、`-`、`.`、`/`、`:`、`;`、`<`、`=`、`>`、`?`、`@`、`[`、`\`、`]`、`^`、`_`、
`` ` ``、`{`、`|`、`}`、`~`）：

```markdown
\*不是斜体\*
\[不是链接\]
```

---

## 扩展语法

### 冒号语法

使用 `:::` 定义提示框（内置 4 种类型）：

```markdown
::: info
这是一条信息提示。
:::

::: tip
这是一条建议。
:::

::: warning
这是一条警告。
:::

::: danger
这是一条危险警告。
:::
```

冒号语法内的内容支持完整 Markdown 语法（段落、列表、代码块等）。

### 标签语法

使用 XML 风格标签定义自定义块组件：

```markdown
<namespace:component key="value" num=42>
这里是块内容，支持 Markdown 语法。
</namespace:component>

<namespace:component key="value"/>
```

- `namespace` 可以省略，默认使用 `ageratum`
- 参数使用 `key="value"` 或 `key=value` 格式
- 自闭合标签（`/>`）不含块内容

### 结构 NBT 组件

使用 `structure`（或兼容别名 `nbt_structure`）扩展，可以在文档中渲染 `data/<namespace>/structure/*.nbt`
结构文件的摘要与 NBT 树状视图：

```markdown
<structure id="minecraft:village/plains/houses/plains_small_house_1"/>

<structure id="./test.nbt"/>

<nbt_structure id="example:ruins/arch" maxDepth="3" maxEntries="8"/>
```

- `id` / `path`：必填，目标结构文件的 `ResourceLocation`
- `maxDepth`：可选，最大展开深度，默认 `2`
- `maxEntries`：可选，每层最多显示的键/列表项数量，默认 `12`
- 支持相对路径：如 `./test.nbt`、`../shared/demo.nbt`，相对于当前文档所在目录解析
- 预览模式下，相对路径会从 `run/ageratum_review/` 下的对应位置读取 `.nbt` 文件
- 渲染内容：结构尺寸、调色板/方块/实体统计，以及受限深度的 NBT 树
- 回退行为：当结构文件不存在或读取失败时，组件会在文档中显示错误信息

---

## 内置扩展组件

| 组件 ID                    | 触发方式                         | 外观              |
|--------------------------|------------------------------|-----------------|
| `ageratum:info`          | `::: info` 或 `<info/>`       | 🔵 蓝色信息框        |
| `ageratum:tip`           | `::: tip` 或 `<tip/>`         | 🟢 绿色建议框        |
| `ageratum:warning`       | `::: warning` 或 `<warning/>` | 🟠 橙色警告框        |
| `ageratum:danger`        | `::: danger` 或 `<danger/>`   | 🔴 红色危险框        |
| `ageratum:recipe`        | `<recipe id="..."/>`         | 配方渲染            |
| `ageratum:structure`     | `<structure id="..."/>`      | 结构 NBT 摘要 + 树视图 |
| `ageratum:nbt_structure` | `<nbt_structure id="..."/>`  | 结构 NBT 摘要 + 树视图 |

### 配方组件

```markdown
<recipe id="minecraft:crafting_table"/>
```

参数：

- `id`：**必填**，目标配方的 ResourceLocation

---

## 内置行内标签

### 颜色标签

```markdown
<color=#FF5500>橙色文字</color>
```

`#` 后接 6 位十六进制颜色码（RGB）。

### 混淆标签

```markdown
<o>混淆文字</o>
```

渲染为随机字符的"乱码"效果。

### 悬停事件

```markdown
<hover type="SHOW_TEXT" data="这是提示内容">悬停我</hover>
<hover type="SHOW_ITEM" data="{"id":"minecraft:diamond","count":1}">悬停查看物品</hover>
<hover type="SHOW_ENTITY" data="{"type":"minecraft:zombie","id":"...","name":"僵尸"}">悬停查看实体</hover>
```

支持类型：

| `type`        | `data` 格式              | 说明     |
|---------------|------------------------|--------|
| `SHOW_TEXT`   | 纯文本字符串                 | 显示文本提示 |
| `SHOW_ITEM`   | ItemStackInfo JSON     | 显示物品提示 |
| `SHOW_ENTITY` | EntityTooltipInfo JSON | 显示实体提示 |

### 点击事件

```markdown
<click type="OPEN_URL" data="https://example.com">点击打开</click>
<click type="COPY_TO_CLIPBOARD" data="要复制的文本">点击复制</click>
<click type="SUGGEST_COMMAND" data="/say hello">点击填入命令</click>
<click type="RUN_COMMAND" data="/ageratum ageratum">点击运行命令</click>
```

支持类型：

| `type`              | `data` 格式 | 说明     |
|---------------------|-----------|--------|
| `OPEN_URL`          | 完整 URL    | 打开网页   |
| `COPY_TO_CLIPBOARD` | 任意文本      | 复制到剪贴板 |
| `SUGGEST_COMMAND`   | 命令文本      | 填入聊天框  |
| `RUN_COMMAND`       | 命令文本      | 直接执行命令 |

### 组合使用

悬停与点击事件可以嵌套：

```markdown
<hover type="SHOW_TEXT" data="提示"><click type="OPEN_URL" data="https://example.com">点击并悬停</click></hover>
```

---

## 不支持的语法

以下 Markdown 元素当前**不支持**：

- 行尾两个空格的硬换行（按软换行处理）
- 引用块内嵌套块级元素（如引用中的列表、代码块）
- 脚注（Footnotes）
- 定义列表（Definition Lists）
- 数学公式（LaTeX/MathJax）
- HTML 块（Raw HTML Blocks）
- 任务列表复选框点击（仅渲染，不可交互）

