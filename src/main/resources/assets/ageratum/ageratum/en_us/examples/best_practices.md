---
title: Best Practices
navigation:
  title: Best Practices
---

# Best Practices Guide

How to create high-quality Ageratum documentation.

## Naming Conventions

### File Naming

- Use lowercase English letters
- Separate words with underscores: `getting_started.md`
- Avoid Chinese file names
- Avoid special characters

### Directory Naming

- Use singular forms: `guide/` instead of `guides/`
- Keep it simple: 3-4 characters is best
- Avoid deep nesting (maximum 2 levels)

## Document Organization

### Recommended Structure

```
ageratum/
├── en_us/
│   ├── index.md (Homepage)
│   ├── guide/
│   │   ├── index.md (Overview)
│   │   ├── page1.md
│   │   └── page2.md
│   └── examples/
│       ├── index.md
│       ├── example1.md
│       └── example2.md
```

### Practices to Avoid

❌ Too deep nesting: `en_us/a/b/c/d/page.md`
❌ Too many top-level files: more than 20 .md
❌ No index page: directory without index.md

## Content Quality

### Heading Levels

- Use # for page title
- Use ## for main sections
- Use ### for subsections
- Avoid too many levels

### Link Best Practices

```markdown
[text](relative/path)     ✓ Relative path
[text](../other/path)     ✓ Fallback path
[text](../../index)       ✓ Back to homepage
[text](namespace:path)    ✓ Cross-namespace
```

## Performance Considerations

### File Size

- < 100KB is optimal
- Consider splitting if > 50KB
- Avoid single files over 1MB

### Image Optimization

```markdown
![description](ageratum:path/to/image.png)
```

- Use PNG format (recommended)
- Compress image size
- Provide meaningful alt text

## Maintenance Recommendations

1. **Update regularly** - Documentation should stay in sync with code
2. **Version management** - Record version in front matter
3. **User feedback** - Collect improvement suggestions from users
4. **Translations** - Provide documentation in multiple languages

[Back to Examples](../index)


