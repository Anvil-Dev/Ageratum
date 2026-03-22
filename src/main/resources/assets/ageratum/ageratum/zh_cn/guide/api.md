---
title: API 参考
navigation:
  title: API 参考
---

# Ageratum API 参考

## 核心类

### MDDocument

文档模型，包含 front matter 和组件列表。

```java
public record MDDocument(
    @Nullable ResourceLocation sourceLocation,
    Map<String, Object> frontMatter,
    List<MDComponent> components
)
```

#### 常用方法

- `String getTitle()` - 获取文档标题
- `Optional<String> getSourceFileName()` - 获取源文件名
- `List<MDComponent> components()` - 获取组件列表

### GuideScreen

文档阅读界面。

```java
public class GuideScreen extends Screen {
    public GuideScreen(ResourceLocation documentLocation, List<MDComponent> components)
    public void setAnchor(@Nullable String anchor)
}
```

### GuideDocumentLoader

文档加载工具。

```java
public static class GuideDocumentLoader {
    public static Optional<ResourceLocation> resolveExistingLocation(
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

## 扩展点

### MDExtensionComponentFactory

自定义组件工厂接口。

```java
@FunctionalInterface
public interface MDExtensionComponentFactory {
    MDComponent create(MDExtensionContext context);
}
```

### MDExtensionContext

扩展组件上下文。

- `String renderedContent()` - 已渲染的内容
- `Map<String, String> params()` - 标签属性

## 导航

- [Markdown 语法](introduction)
- [高级特性](advanced)
- [返回首页](../../index)

