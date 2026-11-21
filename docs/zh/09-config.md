# 配置参考

Ageratum 提供了一个客户端配置文件，用于调整文档阅读界面的显示行为。

---

## 配置文件位置

```
<minecraft_dir>/config/ageratum-client.toml
```

---

## 配置项说明

### `darkMode`

**类型**：`boolean`
**默认值**：`false`
**说明**：启用深色手册，切换背景、标签、按钮、物品槽与配方底图，并调整默认文字配色。手册右侧底部的主题按钮可即时切换，偏好会保存到客户端配置，在切换页面和重启后保留。

```toml
# Use dark guide textures and colors
dark_mode = false
```

---

### `scale`

设置手册的默认缩放比例，支持 `0.5`（50%）到 `4.0`（400%）之间的小数，默认值为 `2.0`（200%）。

按住 Ctrl 滚动滚轮，每格调整 2 个百分点，同时按住 Shift 时每格调整 10 个百分点。在结构预览区域滚动只调整结构大小。缩放手册时，横向滑条在日间卷轴或夜间石板的顶部淡入。点击轨道可直接选定比例，也可拖动滑块调整。拖动期间滑条保持显示，停止操作片刻后自动淡出。松开 Ctrl 后点击滑条外可提前收起，该次点击不会触发下方的内容。

缩放比例按文档命名空间保存到 `config/ageratum/zoom.json`，翻页、关闭手册及重启游戏后仍然保持。点击“默认”或按 Ctrl＋0（含小键盘 0）可清除当前命名空间的缩放记录，恢复配置中的比例。

```toml
scale = 1.25
```

---

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

