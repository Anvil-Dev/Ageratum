---
title: 高级示例
navigation:
  title: 高级
---

# 高级示例

## 自定义组件示例

### 注册扩展

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
        // 在 mod 构造函数中调用此方法
    }
}
```

### 使用自定义组件

在 Markdown 中：

```markdown
::: highlight color="#FF0000"
这是红色高亮的内容
:::

::: highlight color="#00FF00"
这是绿色高亮的内容
:::
```

## 高级链接用法

### 跨文档导航

```markdown
[相同命名空间](guide/page)
[不同命名空间](othermod:guide/page)
[完整路径](othermod:ageratum/zh_cn/guide/page)
```

### 带参数的链接

```markdown
[点击查看详情](page#section)
```

## 动态内容

### 使用变量

虽然 Ageratum 本身不支持变量，但可以通过以下方式模拟：

1. 为每个版本维护不同的文档分支
2. 使用 front matter 存储版本信息
3. 在代码中动态选择文档

## 内容缓存控制

Ageratum 会自动缓存已解析的文档。如果需要强制重新加载：

```java
GuideDocumentCache.getParsedDocument(location).clear();
```

## 性能监控

### 检查加载时间

```
[0ms] 首次打开
[0ms] 第二次打开（来自缓存）
```

### 大型文档优化

对于包含大量图片或代码的文档：
1. 分割成多个部分
2. 使用目录索引
3. 实现惰性加载

## 常见陷阱

❌ 过度嵌套目录
❌ 忘记添加 front matter
❌ 链接路径错误
❌ 混用相对和绝对路径

✓ 使用清晰的目录名称
✓ 完整的 front matter 定义
✓ 测试所有链接
✓ 一致的路径格式

[返回示例列表](../index)


