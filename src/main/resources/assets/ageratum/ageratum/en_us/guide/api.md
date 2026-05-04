---
title: API Reference
navigation:
  title: API Reference
---

# Ageratum API Reference

## Core Classes

### MDDocument

Document model containing front matter and component list.

```java
public record MDDocument(
    @Nullable Identifier sourceLocation,
    Map<String, Object> frontMatter,
    List<MDComponent> components
)
```

#### Common Methods

- `String getTitle()` - Get document title
- `Optional<String> getSourceFileName()` - Get source file name
- `List<MDComponent> components()` - Get component list

### GuideScreen

Document reading interface.

```java
public class GuideScreen extends Screen {
    public GuideScreen(Identifier documentLocation, List<MDComponent> components)
    public void setAnchor(@Nullable String anchor)
}
```

### GuideDocumentLoader

Document loading utility.

```java
public static class GuideDocumentLoader {
    public static Optional<Identifier> resolveExistingLocation(
        ResourceManager resourceManager,
        String namespace,
        String languageCode,
        @Nullable String fileArgument
    )
    
    public static List<String> listFiles(
        ResourceManager resourceManager,
        String namespace,
        String languageCode
    )
}
```

## Extension Points

### MDExtensionComponentFactory

Custom component factory interface.

```java
@FunctionalInterface
public interface MDExtensionComponentFactory {
    MDComponent create(MDExtensionContext context);
}
```

### MDExtensionContext

Extension component context.

- `String renderedContent()` - Rendered content
- `Map<String, String> params()` - Tag attributes

## Navigation

- [Markdown Syntax](introduction)
- [Advanced Features](advanced)
- [Back to Homepage](../../index)

