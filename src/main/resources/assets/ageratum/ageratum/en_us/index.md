# Ageratum Markdown Parser — Full Test Suite

## Implemented: ATX headings (H1-H3 shown here)

### Heading H3

---

## Implemented: Paragraph + inline styles

Plain paragraph with **bold**, *italic*, ~~strikethrough~~, [link text](https://example.com), and <color=#39c5bb>custom color</color>
and <o>obfuscated</o>.

Inline code keeps all Markdown literal: `**not bold** _not italic_ ~~not strike~~ <color=#ff0000>no color</color>`.

Multi-backtick code span: ``code with `backtick` inside``.

Autolink URL: <https://example.com/path?q=1>
Autolink email: <user@example.com>

Escaped punctuation (should all be literal symbols):
\* \_ \~ \` \[ \] \( \) \# \+ \- \. \! \| \{ \} \< \> \@ \\

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
                - Level 4 wraps back to first symbol

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

