# Markdown Syntax Reference

Ageratum implements a core subset of CommonMark and extends it with several game-specific syntaxes.

---

## Block Elements

### Headings

Both ATX (`#`) and Setext (underline) styles are supported:

```markdown
# Heading 1

## Heading 2

### Heading 3

#### Heading 4

##### Heading 5

###### Heading 6

Setext Heading 1
================

Setext Heading 2
----------------
```

### Paragraphs & Line Breaks

Consecutive non-empty lines form a single paragraph. An empty line starts a new paragraph:

```markdown
This is the first paragraph.

This is the second paragraph.
This line is in the same paragraph (soft line break, merged when rendered).
```

> **Note**: Hard line breaks (two trailing spaces) are currently treated as soft line breaks.

### Blockquotes

Multi-level nesting is supported using `>` characters:

```markdown
> Level 1 blockquote
>> Level 2 blockquote
>>> Level 3 blockquote
> Back to level 1
```

### Lists

#### Unordered Lists

Supports `-`, `+`, `*` as list markers with multi-level nesting (4 spaces or 1 tab for indentation):

```markdown
- Item A
    - Sub-item A1
        - Sub-sub-item A1a
- Item B
```

Each level uses a different geometric symbol (●, ○, ■) when rendered.

#### Ordered Lists

```markdown
1. First item
2. Second item
    1. Sub-item
3. Third item
```

#### Task Lists

```markdown
- [x] Completed task
- [ ] Incomplete task
    - [x] Nested completed task
```

### Code Blocks

#### Fenced Code Blocks (Backticks)

````markdown
```java
public class Hello {
    public static void main(String[] args) {
        System.out.println("Hello, Ageratum!");
    }
}
```
````

Language tags are supported (currently affects display only, no syntax highlighting).

#### Fenced Code Blocks (Tildes)

```markdown
~~~
Tilde-fenced code blocks work the same as backtick ones.
~~~
```

#### Indented Code Blocks

Prefix lines with 4 spaces or 1 tab:

```markdown
    int x = 42;
    System.out.println(x);
```

> Code block content is **rendered literally** — no Markdown parsing inside.

### Horizontal Rules

Use three or more `-`, `*`, or `_` (spaces allowed between them):

```markdown
---

***
___
```

### Tables

Column alignment is controlled with `:`:

```markdown
| Left-aligned |  Center-aligned  |  Right-aligned |
|:-------------|:----------------:|---------------:|
| Cell A1      |     Cell B1      |        Cell C1 |
| **Bold**     |     `Code`       |        *Italic*|
```

Table cells support inline Markdown (bold, italic, code spans, etc.).

### Images

Images **must be on their own line** and use namespace-qualified paths:

```markdown
![Description](namespace:textures/path/image.png)
```

### Formula Images (LaTeX)

Single-line bracket syntax is supported for compatibility:

```markdown
[latex:E=mc^2]
[tex,\frac{a}{b}]
[formula!\int_0^1 x^2 dx]
```

- `tex`, `latex`, and `formula` are equivalent prefixes
- Delimiters control scale: `,` = `0.75x`, `!` = `1.5x`, `+` = `2.0x`, `:`/`;` = `1.0x`
- This syntax is parsed as a standalone block component (formula line only)

---

## Inline Elements

### Bold

```markdown
**bold**  or  __bold__
```

### Italic

```markdown
*italic*  or  _italic_
```

> Underscore-style italic (`_italic_`) cannot be immediately preceded or followed by letters or digits.

### Strikethrough

```markdown
~~strikethrough~~
```

### Inline Code

```markdown
`code`  or  ``code with a `backtick` inside``
```

### Links

#### Inline Links

```markdown
[link text](https://example.com)
```

#### Reference Links

```markdown
[link text][ref-id]
[collapsed][]
[shortcut]

[ref-id]: https://example.com

[collapsed]: https://example.com/c

[shortcut]: https://example.com/s
```

#### Autolinks

```markdown
<https://example.com>
<user@example.com>
```

### Escape Characters

CommonMark escapable punctuation (`!`, `"`, `#`, `$`, `%`, `&`, `'`, `(`, `)`, `*`, `+`, `,`, `-`, `.`, `/`, `:`, `;`, `<`, `=`, `>`, `?`,
`@`, `[`, `\`, `]`, `^`, `_`, `` ` ``, `{`, `|`, `}`, `~`):

```markdown
\*not italic\*
\[not a link\]
```

---

## Extension Syntax

### Colon Syntax

Define notice boxes with `:::` (4 built-in types):

```markdown
::: info
This is an info notice.
:::

::: tip
This is a tip.
:::

::: warning
This is a warning.
:::

::: danger
This is a danger notice.
:::
```

Content inside colon syntax supports full Markdown syntax (paragraphs, lists, code blocks, etc.).

### Tag Syntax

Define custom block components using XML-style tags:

```markdown
<namespace:component key="value" num=42>
Block content here — supports Markdown syntax.
</namespace:component>

<namespace:component key="value"/>
```

- `namespace` can be omitted, defaulting to `ageratum`
- Parameters use `key="value"` or `key=value` format
- Self-closing tags (`/>`) have no block content

### Structure NBT Component

Use the `structure` extension to render a summary, top-down block preview, and bounded NBT tree view for
structure files stored under `data/<namespace>/structure/*.nbt`:

```markdown
<structure id="minecraft:village/plains/houses/plains_small_house_1"/>

<structure id="./test.nbt"/>

```

- `id` / `path`: required, target structure file `ResourceLocation`
- `maxDepth`: optional, maximum expansion depth, default `2`
- `maxEntries`: optional, maximum number of keys/list entries shown per level, default `12`
- Relative paths are supported, for example `./test.nbt` or `../shared/demo.nbt`, resolved against the current document directory and able
  to point to `.nbt` files placed next to the Markdown document inside the resource pack
- In preview mode, relative paths are loaded from the matching location under `run/ageratum_review/`
- Rendered content: structure size, palette/block/entity counts, a top-down block preview, and a depth-limited NBT tree
- Hovering a block in the preview shows its block ID, structure coordinates, palette index, and whether it carries block-entity NBT
- Fallback behavior: if the structure file is missing or cannot be read, the component shows an inline error message

---

## Built-in Extension Components

| Component ID         | Trigger                                   | Appearance                   |
|----------------------|-------------------------------------------|------------------------------|
| `ageratum:info`      | `::: info` or `<info/>`                   | 🔵 Blue info box             |
| `ageratum:tip`       | `::: tip` or `<tip/>`                     | 🟢 Green tip box             |
| `ageratum:warning`   | `::: warning` or `<warning/>`             | 🟠 Orange warning box        |
| `ageratum:danger`    | `::: danger` or `<danger/>`               | 🔴 Red danger box            |
| `ageratum:recipe`    | `<recipe id="..."/>`                      | Recipe rendering             |
| `ageratum:structure` | `<structure id="..."/>`                   | Structure NBT summary + tree |
| `ageratum:latex`     | `<latex formula="..."/>` or `[latex:...]` | LaTeX formula image          |

### Recipe Component

```markdown
<recipe id="minecraft:crafting_table"/>
```

Parameters:

- `id`: **Required** — target recipe `ResourceLocation`

---

## Built-in Inline Tags

Inline components use self-closing tag syntax:

```markdown
<namespace:id key="value"/>
```

- `namespace` is optional and defaults to `ageratum`
- Parameters use `key="value"` or `key=value`
- Tags are dispatched by `namespace:id` to the corresponding inline component implementation

### Color Tag

```markdown
<color=#FF5500>Orange text</color>
```

`#` followed by a 6-digit hex color code (RGB).

### Obfuscated Tag

```markdown
<o>Obfuscated text</o>
```

Renders as randomly-changing "scrambled" characters.

### Translate Tag

```markdown
<translate key="item.minecraft.diamond"/>
<translate key="ageratum.guide.missing" fallback="Missing translation"/>
```

- `key`: **Required** translation key
- `fallback`: Optional fallback text when the key has no translation

### Hover Events

```markdown
<hover type="SHOW_TEXT" data="Tooltip content">Hover over me</hover>
<hover type="SHOW_ITEM" data="{"id":"minecraft:diamond","count":1}">Hover for item</hover>
<hover type="SHOW_ENTITY" data="{"type":"minecraft:zombie","id":"...","name":"Zombie"}">Hover for entity</hover>
```

Supported types:

| `type`        | `data` format          | Description         |
|---------------|------------------------|---------------------|
| `SHOW_TEXT`   | Plain string           | Show text tooltip   |
| `SHOW_ITEM`   | ItemStackInfo JSON     | Show item tooltip   |
| `SHOW_ENTITY` | EntityTooltipInfo JSON | Show entity tooltip |

### Click Events

```markdown
<click type="OPEN_URL" data="https://example.com">Click to open</click>
<click type="COPY_TO_CLIPBOARD" data="Text to copy">Click to copy</click>
<click type="SUGGEST_COMMAND" data="/say hello">Click to suggest command</click>
<click type="RUN_COMMAND" data="/ageratum ageratum">Click to run command</click>
```

Supported types:

| `type`              | `data` format | Description         |
|---------------------|---------------|---------------------|
| `OPEN_URL`          | Full URL      | Open a web page     |
| `COPY_TO_CLIPBOARD` | Any text      | Copy to clipboard   |
| `SUGGEST_COMMAND`   | Command text  | Fill into chat box  |
| `RUN_COMMAND`       | Command text  | Execute immediately |

### Combining Styles

Hover and click events can be nested:

```markdown
<hover type="SHOW_TEXT" data="Tooltip"><click type="OPEN_URL" data="https://example.com">Hover or click!</click></hover>
```

---

## Unsupported Syntax

The following Markdown elements are **not currently supported**:

- Hard line breaks via two trailing spaces (treated as soft breaks)
- Block elements nested inside blockquotes (e.g. lists or code blocks within `>`)
- Footnotes
- Definition Lists
- Raw HTML blocks
- Interactive task list checkboxes (rendered only, not clickable)

