---
title: 最佳实践
navigation:
  title: 最佳实践
---

# 最佳实践指南

如何创建高质量的 Ageratum 文档。

## 命名规范

### 文件命名

- 使用小写英文字母
- 用下划线分隔单词：`getting_started.md`
- 避免中文文件名
- 避免特殊字符

### 目录命名

- 使用单数形式：`guide/` 而非 `guides/`
- 保持简洁：3-4 个字符最佳
- 避免深层嵌套（最多 2 级）

## 文档组织

### 推荐结构

```
ageratum/
├── zh_cn/
│   ├── index.md （首页）
│   ├── guide/
│   │   ├── index.md （导览）
│   │   ├── page1.md
│   │   └── page2.md
│   └── examples/
│       ├── index.md
│       ├── example1.md
│       └── example2.md
```

### 避免的做法

❌ 过深的嵌套：`zh_cn/a/b/c/d/page.md`
❌ 过多的一级文件：超过 20 个 .md
❌ 没有索引页：目录下没有 index.md

## 内容质量

### 标题层级

- 使用 # 作为页面标题
- ## 作为主要部分
- ### 作为小节
- 避免过多级别

### 链接最佳实践

```markdown
[文本](relative/path)     ✓ 相对路径
[文本](../other/path)     ✓ 回退路径
[文本](../../index)       ✓ 回到首页
[文本](namespace:path)    ✓ 跨命名空间
```

## 性能考虑

### 文件大小

- 单个文件 <100KB 最佳
- 超过 50KB 考虑分割
- 避免单个文件超过 1MB

### 图片优化

```markdown
![描述](ageratum:path/to/image.png)
```

- 使用 PNG 格式（推荐）
- 压缩图片大小
- 提供有意义的 alt 文本

## 维护建议

1. **定期更新** - 文档要与代码同步
2. **版本管理** - 在 front matter 记录版本
3. **用户反馈** - 收集用户的改进建议
4. **翻译** - 为多个语言提供文档

[返回示例列表](../index)


