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

**类型**：`boolean`，默认 `false`。对当前及之后打开的手册启用布局的深色变体。明暗按钮会立即修改并保存 `config/ageratum-client.toml` 的 `dark_mode`，切换页面、关闭重开及重启游戏后均保持模式。详见[布局与深色模式](11-layouts.md)。

### `directorySide`（配置文件键：`directory_side`）

**类型**：`LEFT` / `RIGHT`，默认 **`LEFT`**。

```toml
# 左侧目录、右侧内容（默认）
directory_side = "LEFT"
# 如需右侧目录、左侧内容，改为 "RIGHT"
```

修改后当前手册会直接重排并保留阅读位置。该玩家偏好在布局继承和深色变体解析之后生效，覆盖包含 `tree` 的面板的 `edge`；与目录同一面板的书签和按钮一起移动，其他独立面板保留原位置。内置全屏布局也默认使用左侧目录。

### `scale`

**类型**：`double`，默认 `1.0`，范围 **0.5～4.0**。支持任意小数比例，如 `0.75`、`1.25`、`1.375`，不会取整为整数档位。

按住 Ctrl 滚动滚轮调整手册缩放，每格增减 2 个百分点；同时按住 Shift 时每格增减 10 个百分点。在结构预览区域内滚动只调整结构大小。缩放时右侧显示覆盖页面的滑条，可拖动或点击轨道，并用“默认”按钮恢复本配置的值；缩放按文档命名空间保存到 `config/ageratum/zoom.json`，跨页、关闭手册及重启游戏后仍然保持，不改写全局配置。默认按钮与 Ctrl＋0（也支持小键盘 0）共用重置逻辑：清除当前命名空间的覆盖记录并恢复配置值。松开 Ctrl 后浮层仍保持显示，点击框外后才收回屏幕边缘，且该次点击不会穿透到正文。

```toml
scale = 1.25
```

修改配置缩放后，没有独立记录的命名空间会自动应用；已有缩放记录的命名空间保持原比例，直到点击默认或按 Ctrl＋0。重排时尽量保持当前阅读段落及目录折叠状态。Minecraft 自身的 GUI 缩放不会改变手册配置所表达的比例；鼠标、滚轮、裁剪和结构组件使用与绘制互逆的坐标转换。

手册正文、标题、表格、代码块、目录、书签和结构层数文字使用 **AnvilLib Font**。字体由 AnvilLib-Font 的配置界面选择；换行、宽度、省略号和链接命中使用同一套 SDF 字形度量，保留粗体、斜体、颜色和点击样式。原版物品数量装饰和原版 tooltip 仍交由游戏渲染。

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

