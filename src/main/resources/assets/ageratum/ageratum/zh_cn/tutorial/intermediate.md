---
title: 中级教程
navigation:
  title: 中级
---

# 中级教程：进阶技巧

本教程涵盖更高级的主题和最佳实践。

## 组织大型文档

### 目录结构

当你有很多文档时，使用目录组织：

```
ageratum/zh_cn/
├── index.md （一级：首页）
├── guide/
│   ├── index.md （一级：指南总览）
│   ├── introduction.md （二级）
│   ├── advanced.md （二级）
│   └── api.md （二级）
└── tutorial/
    ├── index.md （一级：教程总览）
    ├── basics.md （二级）
    ├── intermediate.md （二级）
    └── advanced.md （二级）
```

## 使用 Front Matter

### 完整示例

```yaml
---
title: 我的文档
navigation:
  title: 自定义标题
description: 文档描述
author: 你的名字
version: 1.0.0
---
```

## 自定义样式

### 使用 HTML 标签

```markdown
<color=#FF0000>红色文本</color>
<o>混淆文本</o>
```

## 最佳实践

1. **保持结构简洁** - 最多两级导航
2. **使用清晰的标题** - 避免过长或模糊的名称
3. **添加索引页** - 每个目录都用 index.md 作为入口
4. **内部链接** - 使用相对路径链接其他文档

## 导航

- [初级教程](basics)
- [高级教程](advanced)
- [返回首页](../../index)

