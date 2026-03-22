---
title: 高级主题
navigation:
  title: 高级特性
---

# 高级特性

本文档介绍 Ageratum 的高级功能。

## 自定义扩展

### 注册自定义组件

```java
public static final DeferredRegister<MDExtensionComponentFactory> COMPONENTS =
    AgeratumRegistries.createExtensionComponentFactoryRegister("mymod");

public static final DeferredHolder<MDExtensionComponentFactory, MDExtensionComponentFactory> CUSTOM =
    COMPONENTS.register("custom", () ->
        context -> new MyCustomComponent(context.renderedContent())
    );
```

### 使用自定义组件

在 Markdown 中使用你的自定义组件：

```
::: custom param1="value" param2="value2"
组件内容
:::
```

## 性能优化

- 预编译文档以加快加载速度
- 使用缓存减少重复解析
- 对大型文档进行分页处理

## 国际化

Ageratum 支持多语言：

- 在 `assets/<namespace>/ageratum/<language>/` 下创建文档
- 默认语言为 `en_us`
- 自动回退到默认语言

## 高级链接

### 跨文档跳转

```markdown
[文本](namespace:path)
[同命名空间](path)
[带锚点](page#anchor)
```

### 资源位置语法

完整格式：`namespace:ageratum/zh_cn/path/file`

简写格式：`path/file`（自动使用当前命名空间和语言）

## 导航

- [Markdown 语法](introduction)
- [API 参考](api)
- [返回首页](../../index)

