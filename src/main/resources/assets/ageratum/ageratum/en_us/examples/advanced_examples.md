---
title: Advanced Examples
navigation:
  title: Advanced
---

# Advanced Examples

## Custom Component Example

### Registering Extensions

```java
public class MyModComponents {
    public static final DeferredRegister<MDExtensionComponentFactory> COMPONENTS =
        AgeratumRegistries.createExtensionComponentFactoryRegister("mymod");

    public static final DeferredHolder<MDExtensionComponentFactory, MDExtensionComponentFactory> HIGHLIGHT =
        COMPONENTS.register("highlight", () ->
            context -> {
                String text = context.renderedContent();
                String color = context.params().getOrDefault("color", "#FFFF00");
                return new ColorHighlightComponent(text, color);
            }
        );

    public static void init() {
        // Call this method in your mod's constructor
    }
}
```

### Using Custom Components

In Markdown:

```markdown
::: highlight color="#FF0000"
This is red highlighted content
:::

::: highlight color="#00FF00"
This is green highlighted content
:::
```

## Advanced Link Usage

### Cross-Document Navigation

```markdown
[same namespace](guide/page)
[different namespace](othermod:guide/page)
[full path](othermod:ageratum/en_us/guide/page)
```

### Links with Anchors

```markdown
[Click for details](page#section)
```

## Dynamic Content

### Using Variables

While Ageratum doesn't support variables directly, you can simulate them:

1. Maintain separate document branches for each version
2. Store version info in front matter
3. Dynamically choose documents in code

## Cache Control

Ageratum automatically caches parsed documents. To force reload:

```java
GuideDocumentCache.getParsedDocument(location).clear();
```

## Performance Monitoring

### Check Load Times

```
[0ms] First time open
[0ms] Second time open (from cache)
```

### Optimize Large Documents

For documents with many images or code:
1. Split into multiple parts
2. Use directory index
3. Implement lazy loading

## Common Pitfalls

❌ Over-nested directories
❌ Forgot to add front matter
❌ Wrong link paths
❌ Mixing relative and absolute paths

✓ Use clear directory names
✓ Complete front matter definition
✓ Test all links
✓ Consistent path format

[Back to Examples](../index)


