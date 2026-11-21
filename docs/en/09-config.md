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

**Type**: `boolean`
**Default**: `false`
**Description**: Use dark backgrounds, tabs, buttons, item slots and recipe textures with matching default text colors. The theme button near the bottom of the guide's right sidebar switches immediately and saves the preference across pages and restarts.

```toml
# Use dark guide textures and colors
dark_mode = false
```

---

### `scale`

Sets the default guide zoom. It accepts decimal values from `0.5` (50%) to `4.0` (400%) and defaults to `2.0` (200%).

Hold Ctrl and scroll to change the guide zoom by 2 percentage points per notch, or 10 with Shift held as well. Scrolling over a structure changes its preview size. When zooming the guide, a horizontal slider fades in on the top roll, or at the top of the stone tablet in dark mode. Click the track to select a zoom level or drag the thumb. The slider stays visible during a drag and fades out after a short pause. With Ctrl released, clicking outside dismisses the slider and consumes the click.

Zoom is saved per document namespace in `config/ageratum/zoom.json` across page changes, closing the guide and game restarts. Press Default or Ctrl+0 (including keypad 0) to clear the current namespace override and restore the configured scale.

```toml
scale = 1.25
```

---

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

