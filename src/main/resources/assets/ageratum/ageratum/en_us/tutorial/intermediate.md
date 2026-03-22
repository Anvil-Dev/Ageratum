---
title: Intermediate Tutorial
navigation:
  title: Intermediate
---

# Intermediate Tutorial: Advanced Techniques

This tutorial covers more advanced topics and best practices.

## Organizing Large Documents

### Directory Structure

When you have many documents, organize them with directories:

```
ageratum/en_us/
├── index.md (Top-level: Homepage)
├── guide/
│   ├── index.md (Top-level: Guide Overview)
│   ├── introduction.md (Second-level)
│   ├── advanced.md (Second-level)
│   └── api.md (Second-level)
└── tutorial/
    ├── index.md (Top-level: Tutorial Overview)
    ├── basics.md (Second-level)
    ├── intermediate.md (Second-level)
    └── advanced.md (Second-level)
```

## Using Front Matter

### Complete Example

```yaml
---
title: My Document
navigation:
  title: Custom Title
description: Document description
author: Your Name
version: 1.0.0
---
```

## Custom Styling

### Using HTML Tags

```markdown
<color=#FF0000>Red text</color>
<o>Obfuscated text</o>
```

## Best Practices

1. **Keep structure simple** - Maximum two levels of navigation
2. **Use clear titles** - Avoid long or vague names
3. **Add index pages** - Use index.md as entry point for each directory
4. **Internal links** - Use relative paths to link other documents

## Navigation

- [Beginner's Tutorial](basics)
- [Advanced Tutorial](advanced)
- [Back to Homepage](../../index)

