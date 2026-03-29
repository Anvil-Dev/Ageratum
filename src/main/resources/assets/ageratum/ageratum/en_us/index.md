# Ageratum Documentation System

![](ageratum:gui/mod_icon.png)

Welcome to Ageratum! A powerful documentation framework designed for Minecraft mods.

## Key Features

- 📖 Complete Markdown support
- 🎯 Smart two-level navigation system
- 🔗 Flexible cross-document jumping
- 🌐 Multi-language internationalization
- ⚡ High-performance caching mechanism

## Quick Navigation

### 📚 Guides & Tutorials

- **[Quick Start](guide)** - Get started with Ageratum
  - [Markdown Syntax](guide/introduction)
  - [Advanced Features](guide/advanced)
  - [API Reference](guide/api)

- **[Tutorial Series](tutorial)** - Progressive learning path
  - [Beginner's Tutorial](tutorial/basics)
  - [Intermediate Tutorial](tutorial/intermediate)
  - [Advanced Tutorial](tutorial/advanced)

### 💡 Examples & References

- **[Code Examples](examples)** - Practical examples and best practices
  - [Simple Example](examples/simple)
  - [Practical Example](examples/practical)
  - [Best Practices](examples/best_practices)
  - [Advanced Examples](examples/advanced_examples)

### ❓ FAQ & More

- **[FAQ](faq)** - Frequently asked questions
- **[Changelog](changelog)** - Version history and updates
- **[Quick Guide](guide)** - Quick reference

Multi-backtick code span: ``code with `backtick` inside``.

Autolink URL: <https://example.com/path?q=1>
Autolink email: <user@example.com>

Escaped punctuation (should all be literal symbols):
\* \_ \~ \` \[ \] \( \) \# \+ \- \. \! \| \{ \} \< \> \@ \\

---

## Implemented: Hover and Click Events

Hover events display additional text when you hover over text:
- <hover type="SHOW_TEXT" data="This is a helpful tooltip!">hover over me</hover>

Click events allow text to respond to clicks:
- <click type="OPEN_URL" data="https://example.com">click to open URL</click>
- <click type="COPY_TO_CLIPBOARD" data="Some text to copy">click to copy text</click>
- <click type="RUN_COMMAND" data="/ageratum ageratum">click to run command</click>

You can combine multiple styles in the same text:
- <hover type="SHOW_TEXT" data="Combined events!"><click type="OPEN_URL" data="https://example.com">hover and click me!</click></hover>

---

::: info
This is an info box.
:::

::: tip
This is a tip.
:::

::: warning
This is a warning.
:::

::: danger
This is a dangerous warning.
:::

---

## Implemented: Setext headings

Setext heading H1
=================

Setext heading H2
-----------------

---

## Implemented: Blockquote (multi-level)

> Level 1 quote with **bold** and `code`.
>> Level 2 quote with ~~strike~~ and [link](https://example.com/quote).
>>> Level 3 quote with escaped \*asterisk\* and `literal`.
> Back to level 1 after level 3.

---

## Implemented: Lists

### Unordered (multi-level, geometric markers)

- Level 0 bullet with *italic*
    - Level 1 item with ~~strike~~
        - Level 2 item with **bold**
            - Level 3 item with `code`
                - Level 4

- Level 0 bullet with *italic*
  - Level 1 item with ~~strike~~
    - Level 2 item with **bold**
      - Level 3 item with `code`
        - Level 4

### Ordered (multi-level)

1. Ordered level 0, item 1
2. Ordered level 0, item 2
1. Ordered level 1, item 1
    1. Ordered level 2, item 1

### Task list

- [x] Task done with **bold** and `code`
- [ ] Task pending with *italic*
    - [x] Nested task done
        - [ ] Double-nested task pending

---

## Implemented: Fenced code block — backtick

```java
// Everything inside is literal
**not bold**
_not italic_ ~~
not strike~~
    [not-link](https://example.com)
    >
not a
quote
-[x]
not a
task list
\* \
_ escaped
but literal
inside code
block
```

## Implemented: Fenced code block — tilde

~~~
Tilde fenced code block is now supported.
**still literal** _still literal_
~~~

## Implemented: Indented code block (4 spaces)

    int x = 42;
    System.out.println("indented code block");
    // Consecutive indented lines become one block

---

## Implemented: Tables

| Left aligned | Center aligned | Right aligned |
|:-------------|:--------------:|--------------:|
| cell A1      |    cell B1     |       cell C1 |
| **bold**     |     `code`     |      *italic* |
| cell A3      |    cell B3     |       cell C3 |

---

## Implemented: Link reference definitions

[ref-link][example-ref]

[collapsed-ref][]

[shortcut]

[example-ref]: https://example.com

[collapsed-ref]: https://example.com/collapsed

[shortcut]: https://example.com/shortcut

---

## 已实现：结构NBT

<structure id="minecraft:village/plains/houses/plains_small_house_1"/>

<structure id="./test.nbt"/>

---

## Implemented: Image (line-only, namespace:path)

![](ageratum:gui/guide/guide.png)

---

## Not yet implemented

### Hard line break

Line one  
Line two (trailing two-space hard break — currently treated as soft wrap)

### Nested block elements inside blockquote

> - list inside quote (not supported as block)
    > ```code inside quote``` (not supported as block)

### Footnotes, definition lists, math, front matter

Extended Markdown features not in CommonMark core — not planned.

