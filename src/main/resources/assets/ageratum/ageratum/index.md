# Ageratum Markdown Parser Test Suite

This document is for manual verification of current markdown behavior.

---

## Implemented: block-level

### Heading level 3

> Blockquote line 1
> Blockquote line 2 with **bold** and `code`.
>> Nested quote level 2 with ~~strike~~ and [link](https://example.com/quote).
>>> Nested quote level 3 with escaped \*asterisk\* and `literal`.

- Unordered item A
  - Unordered level 2 item with *italic*
    - Unordered level 3 item with ~~strike~~
      - Unordered level 4 item with **bold**

1. Ordered item one
  1. Ordered nested level 2
    1. Ordered nested level 3
2. Ordered item two
3. Ordered item three

### Task list

- [ ] Task level 1 unchecked
  - [x] Task level 2 checked
    - [ ] Task level 3 unchecked with `code`
- [x] Task level 1 checked with **bold**

### Code block in implemented section

```
// fenced code block should stay literal
**not bold** and [not-link](https://example.com)
> not a quote in code
- [x] not a task list in code
```

---

## Implemented: inline-level

Plain paragraph with **bold**, *italic*, ~~strikethrough~~, [styled link text](https://example.com),
and ![inline image placeholder](assets/test.png).

Custom tags still supported: <color=#39c5bb>color text</color> and <o>obfuscated text</o>.

Escapes that should render as literal symbols:
\* \_ \~ \` \[ \] \( \) \# \+ \- \. \! \| \{ \} \< \> \@ \\ \\*literal star\\*

Inline code should be literal (no markdown inside):
`**not bold** _not italic_ ~~not strike~~ <color=#ff0000>not color</color> \* \_ \~ \``

---

## Implemented: image component (line-only)

![](ageratum:gui/guide/guide.png)

The line above should render an image from:
assets/ageratum/textures/gui/guide/guide.png

---

## Not Implemented (expected to fail or stay literal)

### Setext headings (not supported)

Setext heading style
====================

Subheading setext style
-----------------------

### Indented code block (not supported)

    int x = 1;
    System.out.println(x);

### Tilde fenced code block (~~~ not supported)

~~~
this fence style is not implemented
~~~

### Link reference definitions (not supported)

[ref-link][doc-ref]

[doc-ref]: https://example.com "title"

### Autolink angle brackets (not supported)

<https://example.com>
<user@example.com>

### Tables (not supported)

| name | value |
|------|-------|
| a    | 1     |
| b    | 2     |

### Task list / nested list (not supported)

- [ ] todo item
- [x] done item
    - nested item

### Multi-backtick code span (not supported)

``code with `backtick` inside``

### HTML markdown tags intentionally disabled for style parsing

<b>should not become bold</b>
<i>should not become italic</i>
<u>should not become underline</u>
<s>should not become strike</s>

---

## Mixed stress test

> Quote + list marker text: - this is still quote text
> 1. also quote text, not a real ordered list in this parser mode
>> nested quote + task marker text: - [x] still quote text

- level 1 unordered
  - level 2 unordered
    1. level 3 ordered mixed
      - [x] level 4 task mixed

Final mixed line: **bold + *italic* + ~~strike~~ + [link](https://example.com)** and escaped punctuation \? \: \; \" \' .

