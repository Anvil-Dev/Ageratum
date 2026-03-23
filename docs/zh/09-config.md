# 配置参考

Ageratum 提供了一个客户端配置文件，用于调整文档阅读界面的显示行为。

---

## 配置文件位置

```
<minecraft_dir>/config/ageratum-client.toml
```

---

## 配置项说明

### `breadCrumbsHasLabel`

**类型**：`boolean`  
**默认值**：`false`  
**说明**：控制面包屑导航（顶部路径栏）是否将侧边标签（Tab）的跳转记录在内。

- `false`：面包屑仅记录通过文档内链接进行的跳转，侧边标签点击不影响面包屑。
- `true`：点击侧边标签也会向面包屑历史追加一条记录，可以用"返回"按钮回到之前的文档。

```toml
# Do breadcrumbs record jumps from sidebar tabs
breadCrumbsHasLabel = false
```

---

### `showCodeBlockLineNumbers`

**类型**：`boolean`  
**默认值**：`true`  
**说明**：控制代码块是否在左侧显示行号。

- `true`：代码块左侧显示行号列（从 1 开始）。
- `false`：不显示行号，代码内容占用全部宽度。

```toml
# Show line numbers in code blocks
showCodeBlockLineNumbers = true
```

---

### `allowCodeBlockLineContentLineBreaks`

**类型**：`boolean`  
**默认值**：`true`  
**说明**：控制代码块内超长行是否允许自动换行。

- `true`：代码行超出显示宽度时自动折行，确保内容完整可见。
- `false`：不换行，超出部分不可见（适合需要保留代码原始格式的场景）。

```toml
# Allow line breaks in code block content
allowCodeBlockLineContentLineBreaks = true
```

---

## 示例配置文件

```toml
# Ageratum 客户端配置

# Do breadcrumbs record jumps from sidebar tabs
breadCrumbsHasLabel = false

# Show line numbers in code blocks
showCodeBlockLineNumbers = true

# Allow line breaks in code block content
allowCodeBlockLineContentLineBreaks = true
```

---

## 在代码中访问配置

Ageratum 使用 AnvilCraft Lib v2 的配置系统。在客户端代码中可通过 `AgeratumClient.CONFIG` 访问：

```java
import dev.anvilcraft.resource.ageratum.client.AgeratumClient;

// 读取配置值
boolean showLineNumbers = AgeratumClient.CONFIG.showCodeBlockLineNumbers;
boolean allowLineBreaks = AgeratumClient.CONFIG.allowCodeBlockLineContentLineBreaks;
```

> **注意**：该配置仅在客户端可用（`Dist.CLIENT`），不要在服务端代码中引用。

---

## 参见

- [快速入门](01-getting-started.md)
- [架构设计](08-architecture.md)

