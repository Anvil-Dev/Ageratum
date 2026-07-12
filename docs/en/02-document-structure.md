# Document Structure

This document explains Ageratum's resource pack layout, path conventions, and Front Matter syntax.

---

## Resource Pack Directory Layout

Ageratum documents are stored under the standard Minecraft resource pack `assets/` directory:

```
assets/
└── <namespace>/
    └── ageratum/
        ├── en_us/              ← Default language (required)
        │   ├── index.md        ← Home page document
        │   ├── <file>.md
        │   └── <subdir>/
        │       └── <file>.md
        ├── zh_cn/              ← Chinese (optional)
        │   └── ...
        └── <any_lang>/         ← Any language code (optional)
            └── ...
```

### Namespaces

- `<namespace>` is typically the same as your mod ID.
- Ageratum's own documentation namespace is `ageratum`.
- Multiple namespaces can coexist without interference.

### Language Codes

- Language codes follow the Minecraft standard (e.g. `en_us`, `zh_cn`, `ja_jp`).
- Resolution order: **client's current language** → **`en_us` (fallback)**.
- Language codes are case-insensitive; hyphens `-` are automatically converted to underscores `_`.

---

## File Path Conventions

### Valid Path Examples

| Resource Location                         | Description          |
|-------------------------------------------|----------------------|
| `mymod:ageratum/en_us/index.md`           | English home page    |
| `mymod:ageratum/zh_cn/index.md`           | Chinese home page    |
| `mymod:ageratum/en_us/tutorial/basics.md` | File in subdirectory |
| `mymod:ageratum/en_us/faq.md`             | Top-level document   |

### Path Normalization Rules

`GuideDocumentLoader` automatically applies the following normalization:

1. **Empty path** → defaults to `index` (home page)
2. **Extension completion**: `.md` is appended if not already present
3. **Path separator**: backslashes `\` are converted to forward slashes `/`
4. **Leading slash removal**: `/guide` → `guide`
5. **Language code normalization**: uppercased to lowercase, `-` → `_`

### Command Path Examples

```
/ageratum mymod                     → assets/mymod/ageratum/<lang>/index.md
/ageratum mymod guide               → assets/mymod/ageratum/<lang>/guide.md
/ageratum mymod tutorial/basics     → assets/mymod/ageratum/<lang>/tutorial/basics.md
/ageratum mymod zh_cn/advanced      → assets/mymod/ageratum/zh_cn/advanced.md
```

---

## Front Matter

Ageratum supports YAML-style Front Matter for document metadata. It must appear at the very beginning of the file, delimited by `---`:

```markdown
---
title: "My Document Title"
navigation:
  title: "Sidebar Title"
---

# Document body content

...
```

### Supported Fields

| Field                                               | Type                      | Description                                                         |
|-----------------------------------------------------|---------------------------|---------------------------------------------------------------------|
| `title`                                             | `string`                  | Document title (overrides first heading)                            |
| `navigation.title`                                  | `string`                  | Title shown in the sidebar navigation (takes priority over `title`) |
| `guide.items` or `items`                            | `string` / `list<string>` | Bound item rules used for the "contemplate" feature                 |
| `guide.item_id`, `item_id`, `guide.item`, or `item` | `string`                  | Legacy compatibility fields; still read, but `items` is recommended |

### Title Resolution Priority

Ageratum determines the document title in the following order:

1. `navigation.title` (Front Matter)
2. `title` (Front Matter)
3. First level-1 heading (`# Heading`) in the document
4. File name (without extension)

### Item Binding & "Contemplate" Feature

By declaring `items` (or `guide.items`) in the Front Matter, you can bind a document to specific item rules. When players hover over a
matching item in their inventory, a progress indicator appears in the Tooltip. Holding the <kbd>W</kbd> key automatically opens the
corresponding document.

`items` supports:

- a single item string
- a list of item strings
- item component conditions appended with `{}` such as `minecraft:diamond{\"test\":1}`

Matching rules:

1. If only the item ID is written, such as `minecraft:diamond`, item components are ignored
2. If empty components are written, such as `minecraft:diamond{}`, it behaves the same as item ID only
3. If components are written, such as `minecraft:diamond{\"test\":1}`, only the specified components must match
4. Therefore `minecraft:diamond{\"test\":1}` can match `minecraft:diamond{\"test\":1,\"test2\":2}`, but it cannot match `minecraft:diamond{\"test\":0}`

**Example:**

```markdown
---
title: "Uses of Iron Ingots"
items: "minecraft:iron_ingot"
---

# Iron Ingot Uses

...
```

**List example:**

```markdown
---
title: "Diamond Related Notes"
items:
  - "minecraft:diamond"
  - "minecraft:diamond{\"test\":1}"
  - "minecraft:diamond_sword{damage=0}"
---
```

If multiple documents are bound to the same item, the first one is opened according to the following priority:

1. Client's current language version
2. English (`en_us`) version
3. Within the same language, the binding with more component constraints is preferred (including nested fields and array elements)
4. If specificity is equal, the first document by path lexicographical order

---

## Navigation Tree Structure

Ageratum automatically scans all `.md` files on resource pack load and builds a two-level navigation tree displayed in the GUI sidebar.

### Navigation Tree Rules

- **Directories**: correspond to filesystem subdirectories
- **`index.md`**: serves as the index page for a directory, displayed on the directory entry
- The tree shows at most two levels (top-level directories + subdirectories)
- Files are sorted alphabetically by file name
- Directories are sorted alphabetically by directory name

### Example Structure

```
assets/mymod/ageratum/en_us/
├── index.md          → Root-level document (title from document)
├── guide.md          → Root-level document
├── faq.md            → Root-level document
└── tutorial/
    ├── index.md      → Index page for the tutorial directory
    ├── basics.md     → Document under tutorial/
    └── advanced.md   → Document under tutorial/
```

Sidebar rendering result (schematic):

```
[Home]
[Guide]
[FAQ]
▼ [Tutorial]
   [Tutorial Index]
   [Basics]
   [Advanced]
```

---

## Image Resources

Images referenced in documents use `namespace:path` format, corresponding to `assets/<namespace>/textures/<path>` files.

```markdown
![Description](mymod:gui/my_image.png)
![Description](ageratum:gui/guide/guide.png)
```

Images **must occupy their own line** in the document. Inline images (within a paragraph) are not currently supported.

