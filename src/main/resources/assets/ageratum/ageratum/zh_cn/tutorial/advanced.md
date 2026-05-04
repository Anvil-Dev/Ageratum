---
title: 高级教程
navigation:
  title: 高级
---

# 高级教程：深入了解

本教程适合想要充分利用 Ageratum 的开发者。

## 自定义组件开发

### 创建扩展组件

```java
public class MyCustomComponent extends MDComponent {
    private final String customParam;
    
    public MyCustomComponent(String customParam) {
        super("Custom Component");
        this.customParam = customParam;
    }
    
    @Override
    public void render(GuiGraphicsExtractor guiGraphics, Minecraft minecraft, int maxX, int maxY) {
        // 自定义渲染逻辑
    }
}
```

### 注册到工厂

```java
public static final DeferredRegister<MDExtensionComponentFactory> COMPONENTS =
    AgeratumRegistries.createExtensionComponentFactoryRegister("mymod");

public static final DeferredHolder<MDExtensionComponentFactory, MDExtensionComponentFactory> CUSTOM =
    COMPONENTS.register("custom", () ->
        context -> new MyCustomComponent(context.params().get("param"))
    );
```

## 性能优化

### 文档缓存

Ageratum 自动缓存已解析的文档，避免重复解析。

使用 `GuideDocumentCache.getParsedDocument()` 直接读取缓存。

### 大型文档处理

对于超大文档：
1. 分割成多个小文件
2. 使用链接连接它们
3. 侧栏导航会自动处理

## 国际化最佳实践

### 多语言支持

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

## 调试技巧

### 启用日志

```
[23:45:12] [Client thread/INFO] [Ageratum]: Loading guide from ageratum:en_us/index.md
[23:45:12] [Client thread/INFO] [Ageratum]: Parsed 15 components
```

### 检查文档位置

使用命令获取当前文档的 Identifier：

```
/say ageratum:en_us/guide/index
```

## 导航

- [初级教程](basics)
- [中级教程](intermediate)
- [返回首页](../../index)

