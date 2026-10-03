# Configuration

Ageratum provides a client-side configuration file to adjust the document reader's display behavior.

---

## Configuration File Location

```
<minecraft_dir>/config/ageratum-client.toml
```

---

## Configuration Options

### `darkMode`

**Type**: `boolean`, default `false`. Enables dark layout variants for current and subsequently opened guides. The theme button immediately updates and saves `dark_mode` in `config/ageratum-client.toml`, so navigating, reopening and restarting preserve the selected mode. See [Layouts and dark mode](11-layouts.md).

### `directorySide` (configuration key: `directory_side`)

**Type**: `LEFT` / `RIGHT`, default **`LEFT`**.

```toml
# Navigation on the left, content on the right (default)
directory_side = "LEFT"
# Set to "RIGHT" for navigation on the right and content on the left.
```

Changes reflow the open guide while preserving the reading position. The player preference applies after layout inheritance and dark-variant resolution, overriding the `edge` of panels containing `tree`. Bookmarks/actions in the same panel move with it; independent panels keep their positions. The built-in fullscreen layout also defaults to left navigation.

### `scale`

Hold Ctrl and use the wheel to change guide zoom by 2 percentage points per notch, or 10 with Shift held as well. Scrolling over a structure changes only its preview size. Zooming reveals an overlay slider at the right edge; drag it, seek on its track, or press Default to restore this configured value. Zoom is saved per document namespace in `config/ageratum/zoom.json`, surviving page navigation, closing the guide and game restarts without changing the global configuration. Default and Ctrl+0 (including keypad 0) clear the current namespace override and restore the configured scale. Releasing Ctrl keeps the overlay open; a subsequent outside click retracts it to the screen edge and does not activate content underneath.

**Type**: `double`, default `1.0`, range **0.5–4.0**. Accepts arbitrary fractional values such as `0.75`, `1.25` and `1.375`, without rounding to integer steps.

```toml
scale = 1.25
```

Changes to the configured scale apply to namespaces without a saved override; namespaces with saved zoom retain it until Default or Ctrl+0 is used. Reflow preserves the reading component and folded groups where possible. Minecraft's own GUI scale does not change the guide zoom preference. Mouse input, scrolling, clipping and structure components use the inverse of the drawing transform.

Body text, headings, tables, code blocks, navigation, bookmarks and structure layer labels use **AnvilLib Font**. Select the font in AnvilLib-Font's configuration screen. Wrapping, widths, ellipsis and link hit testing share its SDF glyph metrics, retaining bold, italic, colors and click styles. Native item count decorations and native tooltips remain game-rendered.

### `breadCrumbsHasLabel`

**Type**: `boolean`  
**Default**: `false`  
**Description**: Controls whether clicking sidebar tab labels records a breadcrumb entry.

- `false`: Breadcrumbs only record navigation via in-document links. Clicking sidebar tabs does not affect the breadcrumb trail.
- `true`: Clicking a sidebar tab also appends an entry to the breadcrumb history, allowing the "back" button to return to the previous document.

```toml
# Do breadcrumbs record jumps from sidebar tabs
breadCrumbsHasLabel = false
```

---

### `showCodeBlockLineNumbers`

**Type**: `boolean`  
**Default**: `true`  
**Description**: Controls whether code blocks display line numbers on the left side.

- `true`: A line number column (starting at 1) is shown to the left of code content.
- `false`: No line numbers; code content uses the full available width.

```toml
# Show line numbers in code blocks
showCodeBlockLineNumbers = true
```

---

### `allowCodeBlockLineContentLineBreaks`

**Type**: `boolean`  
**Default**: `true`  
**Description**: Controls whether long lines inside code blocks automatically wrap.

- `true`: Lines exceeding the display width wrap to the next line, ensuring full content visibility.
- `false`: No wrapping; content that exceeds the width is not visible (useful when preserving original code formatting is important).

```toml
# Allow line breaks in code block content
allowCodeBlockLineContentLineBreaks = true
```

---

## Example Configuration File

```toml
# Ageratum Client Configuration

# Do breadcrumbs record jumps from sidebar tabs
breadCrumbsHasLabel = false

# Show line numbers in code blocks
showCodeBlockLineNumbers = true

# Allow line breaks in code block content
allowCodeBlockLineContentLineBreaks = true
```

---

## Accessing Configuration in Code

Ageratum uses the AnvilCraft Lib v2 configuration system. Access the config from client-side code via `AgeratumClient.CONFIG`:

```java
import dev.anvilcraft.resource.ageratum.client.AgeratumClient;

boolean showLineNumbers = AgeratumClient.CONFIG.showCodeBlockLineNumbers;
boolean allowLineBreaks = AgeratumClient.CONFIG.allowCodeBlockLineContentLineBreaks;
```

> **Note**: This config is client-only (`Dist.CLIENT`). Do not reference it from server-side code.

---

## See Also

- [Getting Started](01-getting-started.md)
- [Architecture](08-architecture.md)

