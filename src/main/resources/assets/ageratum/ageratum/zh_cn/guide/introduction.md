---
title: Markdown 语法完整指南
navigation:
  title: Markdown 语法
---

# Markdown 完整指南

本文档介绍 Ageratum 支持的所有 Markdown 语法。

## 标题

# 一级标题
## 二级标题
### 三级标题

## 文本样式

这是 **粗体** 和 *斜体* 以及 ~~删除线~~。

你也可以使用 __粗体__ 或 _斜体_。

## 列表

### 无序列表

- 项目 1
- 项目 2
  - 嵌套项目 2.1
  - 嵌套项目 2.2
- 项目 3

### 有序列表

1. 第一项
2. 第二项
   1. 嵌套项 2.1
   2. 嵌套项 2.2
3. 第三项

### 任务列表

- [x] 已完成任务
- [ ] 未完成任务
- [x] 另一个已完成任务

## 链接和图片

[点击跳转到快速指南](../guide)

自动链接：<https://example.com>

## 代码

行内代码：`console.log("hello")`

代码块：

```javascript
function greet(name) {
  return "Hello, " + name;
}
```

## 引用

> 这是一个引用块
> 可以包含多行内容
> 
> > 也可以嵌套

## 导航

- [高级特性](advanced)
- [API 参考](api)
- [返回首页](../../index)

