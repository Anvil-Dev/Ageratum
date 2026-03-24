# Configuration

Ageratum provides a client-side configuration file to adjust the document reader's display behavior.

---

## Configuration File Location

```
<minecraft_dir>/config/ageratum-client.toml
```

---

## Configuration Options

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

