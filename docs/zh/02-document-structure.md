# 文档结构规范

本文档说明 Ageratum 文档的资源包布局规则、路径约定及 Front Matter 语法。

---

## 资源包目录结构

Ageratum 文档存放在标准 Minecraft 资源包的 `assets/` 目录下：

```
assets/
└── <namespace>/
    └── ageratum/
        ├── en_us/              ← 默认语言（必须提供）
        │   ├── index.md        ← 首页文档
        │   ├── <file>.md
        │   └── <subdir>/
        │       └── <file>.md
        ├── zh_cn/              ← 中文（可选）
        │   └── ...
        └── <any_lang>/         ← 任意语言代码（可选）
            └── ...
```

### 命名空间

- `<namespace>` 通常与你的模组 ID 相同。
- Ageratum 自身的文档命名空间为 `ageratum`。
- 多个命名空间可以共存，互不干扰。

### 语言代码

- 语言代码遵循 Minecraft 标准，如 `en_us`、`zh_cn`、`ja_jp`。
- 查找顺序：**当前客户端语言** → **`en_us`（回退）**。
- 语言代码大小写不敏感，连字符 `-` 会自动转换为下划线 `_`。

---

## 文件路径规范

### 有效路径示例

| 资源位置 | 说明 |
|---------|------|
| `mymod:ageratum/en_us/index.md` | 英文首页 |
| `mymod:ageratum/zh_cn/index.md` | 中文首页 |
| `mymod:ageratum/en_us/tutorial/basics.md` | 子目录文档 |
| `mymod:ageratum/en_us/faq.md` | 顶层文档 |

### 路径规范化规则

`GuideDocumentLoader` 在解析路径时会自动进行以下处理：

1. **空路径** → 自动使用 `index`（首页）
2. **补全扩展名**：若路径不以 `.md` 结尾则自动追加
3. **路径分隔符**：反斜杠 `\` 自动转换为正斜杠 `/`
4. **去除前导斜杠**：`/guide` → `guide`
5. **语言代码规范化**：大写转小写，`-` 转 `_`

### 命令行路径示例

```
/ageratum mymod                     → assets/mymod/ageratum/<lang>/index.md
/ageratum mymod guide               → assets/mymod/ageratum/<lang>/guide.md
/ageratum mymod tutorial/basics     → assets/mymod/ageratum/<lang>/tutorial/basics.md
/ageratum mymod zh_cn/advanced      → assets/mymod/ageratum/zh_cn/advanced.md
```

> **注意**：当第二个参数包含 `/` 时，`/` 前面的部分如果是合法语言代码会被识别为语言代码覆盖（当前实现中直接透传，不做分割）。

---

## Front Matter

Ageratum 支持 YAML 风格的 Front Matter，用于为文档设置元数据。Front Matter 位于文件开头，以 `---` 为定界符：

```markdown
---
title: "我的文档标题"
navigation:
  title: "侧边栏标题"
---

# 文档正文内容

...
```

### 支持的字段

| 字段 | 类型 | 说明 |
|------|------|------|
| `title` | `string` | 文档标题（覆盖一级标题） |
| `navigation.title` | `string` | 侧边栏导航中显示的标题（优先于 `title`） |

### 标题解析优先级

Ageratum 按以下顺序确定文档标题：

1. `navigation.title`（Front Matter）
2. `title`（Front Matter）
3. 文档中第一个一级标题（`# 标题`）
4. 文件名（不含扩展名）

---

## 导航树结构

Ageratum 在资源包加载时自动扫描所有 `.md` 文件并构建两级导航树，显示在 GUI 侧边栏。

### 导航树规则

- **目录**：对应文件系统中的子目录
- **`index.md`**：作为目录的索引页，显示在目录条目上
- 导航树最多显示两级（顶级目录 + 子目录）
- 文件按文件名字母顺序排序
- 目录按目录名字母顺序排序

### 示例结构

```
assets/mymod/ageratum/en_us/
├── index.md          → 显示为根级文档（标题来自文档）
├── guide.md          → 显示为根级文档
├── faq.md            → 显示为根级文档
└── tutorial/
    ├── index.md      → tutorial 目录的索引页
    ├── basics.md     → tutorial 目录下的文档
    └── advanced.md   → tutorial 目录下的文档
```

侧边栏渲染结果（示意）：

```
[首页]
[指南]
[FAQ]
▼ [Tutorial]
   [Tutorial 索引]
   [基础]
   [进阶]
```

---

## 图片资源

文档中引用的图片使用 `命名空间:路径` 格式，对应 `assets/<namespace>/textures/<path>` 文件。

```markdown
![描述](mymod:gui/my_image.png)
![描述](ageratum:gui/guide/guide.png)
```

图片在文档中**独占一行**，内联图片（段落内）当前不支持。

