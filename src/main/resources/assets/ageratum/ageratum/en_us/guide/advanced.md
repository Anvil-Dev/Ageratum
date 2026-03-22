---
title: Advanced Topics
navigation:
  title: Advanced Features
---

# Advanced Features

This document covers advanced features of Ageratum.

## Custom Extensions

### Registering Custom Components

```java
public static final DeferredRegister<MDExtensionComponentFactory> COMPONENTS =
    AgeratumRegistries.createExtensionComponentFactoryRegister("mymod");

public static final DeferredHolder<MDExtensionComponentFactory, MDExtensionComponentFactory> CUSTOM =
    COMPONENTS.register("custom", () ->
        context -> new MyCustomComponent(context.renderedContent())
    );
```

### Using Custom Components

Use your custom component in Markdown:

```
::: custom param1="value" param2="value2"
Component content
:::
```

## Performance Optimization

- Pre-compile documents for faster loading
- Use caching to reduce repeated parsing
- Split large documents into pages

## Internationalization

Ageratum supports multiple languages:

- Create documents in `assets/<namespace>/ageratum/<language>/`
- Default language is `en_us`
- Automatically falls back to default language

## Advanced Linking

### Cross-Document Jumps

```markdown
[text](namespace:path)
[same namespace](path)
[with anchor](page#anchor)
```

### ResourceLocation Syntax

Full format: `namespace:ageratum/en_us/path/file`

Short format: `path/file` (uses current namespace and language)

## Navigation

- [Markdown Syntax](introduction)
- [API Reference](api)
- [Back to Homepage](../../index)

