---
title: Practical Example
navigation:
  title: Practical
---

# Practical Example: Complete Mod Guide

This is a complete guide structure example used in real projects.

## Project Structure

```
mods/mymod/
├── src/
│   └── main/
│       └── resources/
│           └── assets/
│               └── mymod/
│                   └── ageratum/
│                       ├── en_us/
│                       │   ├── index.md
│                       │   ├── features.md
│                       │   ├── guide/
│                       │   │   ├── index.md
│                       │   │   ├── installation.md
│                       │   │   └── usage.md
│                       │   └── api/
│                       │       ├── index.md
│                       │       ├── classes.md
│                       │       └── interfaces.md
│                       └── zh_cn/
│                           └── ... (same structure)
```

## Complete Front Matter Example

```yaml
---
title: My Mod - Complete Guide
navigation:
  title: Homepage
description: A powerful Minecraft mod
author: Your Name
version: 1.0.0
keywords:
  - mod
  - guide
  - minecraft
---
```

## Content Example

### Using All Features

```markdown
# Title

## Subtitle

**Bold** *Italic* ~~Strikethrough~~

> Blockquote

- List item 1
- List item 2

```java
// Code block
System.out.println("Hello");
```

[Link](./other_page)

::: tip
Tip box
:::
```

## Best Practices

1. Create index.md for each directory
2. Use consistent front matter format
3. Add internal links to guide users
4. Update documentation regularly

[Back to Examples](../index)


