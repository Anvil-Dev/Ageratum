---
title: Advanced Tutorial
navigation:
  title: Advanced
---

# Advanced Tutorial: Deep Dive

This tutorial is for developers who want to make the most of Ageratum.

## Custom Component Development

### Creating Extension Components

```java
public class MyCustomComponent extends MDComponent {
    private final String customParam;
    
    public MyCustomComponent(String customParam) {
        super("Custom Component");
        this.customParam = customParam;
    }
    
    @Override
    public void render(GuiGraphicsExtractor guiGraphics, Minecraft minecraft, int maxX, int maxY) {
        // Custom rendering logic
    }
}
```

### Registering to Factory

```java
public static final DeferredRegister<MDExtensionComponentFactory> COMPONENTS =
    AgeratumRegistries.createExtensionComponentFactoryRegister("mymod");

public static final DeferredHolder<MDExtensionComponentFactory, MDExtensionComponentFactory> CUSTOM =
    COMPONENTS.register("custom", () ->
        context -> new MyCustomComponent(context.params().get("param"))
    );
```

## Performance Optimization

### Document Caching

Ageratum automatically caches parsed documents to avoid repeated parsing.

Use `GuideDocumentCache.getParsedDocument()` to read from cache directly.

### Handling Large Documents

For extremely large documents:
1. Split into multiple smaller files
2. Use links to connect them
3. Sidebar navigation handles it automatically

## Internationalization Best Practices

### Multi-language Support

```
ageratum/
├── en_us/
│   ├── index.md
│   └── guide/
│       ├── index.md
│       └── intro.md
├── zh_cn/
│   ├── index.md
│   └── guide/
│       ├── index.md
│       └── intro.md
└── pt_br/
    ├── index.md
    └── guide/
        ├── index.md
        └── intro.md
```

## Debugging Tips

### Enable Logging

```
[23:45:12] [Client thread/INFO] [Ageratum]: Loading guide from ageratum:en_us/index.md
[23:45:12] [Client thread/INFO] [Ageratum]: Parsed 15 components
```

### Check Document Location

Use commands to get the current document's Identifier:

```
/say ageratum:en_us/guide/index
```

## Navigation

- [Beginner's Tutorial](basics)
- [Intermediate Tutorial](intermediate)
- [Back to Homepage](../../index)

