---
title: 常见问题
navigation:
  title: FAQ
---

# 常见问题 (FAQ)

## Ageratum 是什么？

Ageratum 是一个为 Minecraft 模组开发者和玩家设计的文档/指南框架。

## 它支持哪些格式？

支持完整的 CommonMark Markdown 格式，以及一些扩展功能如：
- 自定义颜色标签
- 悬停和点击事件
- 提示框容器
- 表格

## 如何创建我自己的指南？

1. 在资源包中创建 `assets/<namespace>/ageratum/<lang>/` 目录
2. 添加 `.md` 文件
3. 使用命令 `/ageratum <namespace>` 打开

## 支持哪些语言？

默认支持英文 (`en_us`) 和简体中文 (`zh_cn`)。

你可以在资源包中添加其他语言的文件，如 `pt_br/`、`es_es/` 等。

## 会不会掉帧？

不会。文档在加载时解析一次，之后直接渲染已解析的组件。

## 我能用它做什么？

- 制作模组说明手册
- 创建教程
- 编写 API 文档
- 制作游戏指南

## 相关链接

- [返回首页](index)
- [快速开始](guide)
- [查看教程](tutorial/index)

