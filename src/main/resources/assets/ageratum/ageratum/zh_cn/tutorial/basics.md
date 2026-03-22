---
title: 初级教程
navigation:
  title: 初级
---

# 初级教程：从零开始

本教程适合刚开始使用 Ageratum 的开发者。

## 创建你的第一个文档

### 步骤 1：创建文件结构

```
src/main/resources/
└── assets/
    └── mymod/
        └── ageratum/
            └── zh_cn/
                └── index.md
```

### 步骤 2：编写 index.md

```markdown
---
title: 我的模组指南
---

# 欢迎

这是我的模组指南。

## 开始使用

[查看快速开始指南](../guide)
```

### 步骤 3：运行游戏

```
/ageratum mymod
```

## 常见问题

**Q: 文件名区分大小写吗？**

A: 取决于你的操作系统。建议使用小写和下划线。

**Q: 可以使用中文文件名吗？**

A: 不建议。推荐使用英文文件名，用 front matter 设置中文标题。

**Q: 多少个文档合适？**

A: 没有限制，但建议不超过 50 个一级文档以保证性能。

 ## 导航

- [中级教程](intermediate)
- [高级教程](advanced)
- [返回首页](../../index)

