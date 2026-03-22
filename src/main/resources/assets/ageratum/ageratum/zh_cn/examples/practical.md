---
title: 实战示例
navigation:
  title: 实战
---

# 实战示例：完整的模组指南

这是一个真实项目中使用的完整指南结构示例。

## 项目结构

```
mods/mymod/
├── src/
│   └── main/
│       └── resources/
│           └── assets/
│               └── mymod/
│                   └── ageratum/
│                       ├── en_us/
│                       │   ├── index.md
│                       │   ├── features.md
│                       │   ├── guide/
│                       │   │   ├── index.md
│                       │   │   ├── installation.md
│                       │   │   └── usage.md
│                       │   └── api/
│                       │       ├── index.md
│                       │       ├── classes.md
│                       │       └── interfaces.md
│                       └── zh_cn/
│                           └── ... （相同结构）
```

## Front Matter 完整示例

```yaml
---
title: 我的模组 - 完整指南
navigation:
  title: 首页
description: 一个强大的 Minecraft 模组
author: 你的名字
version: 1.0.0
keywords:
  - 模组
  - 指南
  - Minecraft
---
```

## 内容示例

### 使用所有功能

```markdown
# 标题

## 小标题

**粗体** *斜体* ~~删除线~~

> 引用块

- 列表项 1
- 列表项 2

```java
// 代码块
System.out.println("Hello");
```

[链接](./other_page)

::: tip
提示框
:::
```

## 最佳实践

1. 为每个目录创建 index.md
2. 使用一致的 front matter 格式
3. 添加内部链接指引用户
4. 定期更新文档

[返回示例列表](../index)


