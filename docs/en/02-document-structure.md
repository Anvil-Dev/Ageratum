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

| Resource Location | Description |
|------------------|-------------|
| `mymod:ageratum/en_us/index.md` | English home page |
| `mymod:ageratum/zh_cn/index.md` | Chinese home page |
| `mymod:ageratum/en_us/tutorial/basics.md` | File in subdirectory |
| `mymod:ageratum/en_us/faq.md` | Top-level document |

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

| Field | Type | Description |
|-------|------|-------------|
| `title` | `string` | Document title (overrides first heading) |
| `navigation.title` | `string` | Title shown in the sidebar navigation (takes priority over `title`) |

### Title Resolution Priority

Ageratum determines the document title in the following order:

1. `navigation.title` (Front Matter)
2. `title` (Front Matter)
3. First level-1 heading (`# Heading`) in the document
4. File name (without extension)

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

