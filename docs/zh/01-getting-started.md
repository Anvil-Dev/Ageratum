# 快速入门

本指南将帮助你在 5 分钟内为你的 NeoForge 模组接入 Ageratum 文档系统。

---

## 1. 添加依赖

在你的 `build.gradle` 中引入 Ageratum：

```groovy
repositories {
    maven {
        name = "Ageratum"
        url = "https://maven.anvilcraft.dev/releases"
    }
}

dependencies {
    // 仅作为 API 依赖（不打包进你的 jar）
    compileOnly "dev.anvilcraft.resource:ageratum-neoforge-1.21.1:0.0.1"
    // 如果需要运行时测试，也可以加 runtimeOnly
    runtimeOnly "dev.anvilcraft.resource:ageratum-neoforge-1.21.1:0.0.1"
}
```

---

## 2. 创建文档文件

在你的资源包中按照以下目录结构创建 Markdown 文档：

```
src/main/resources/
└── assets/
    └── <your_modid>/
        └── ageratum/
            ├── en_us/
            │   └── index.md        ← 英文首页（必须）
            └── zh_cn/
                └── index.md        ← 中文首页（可选）
```

**`assets/<your_modid>/ageratum/en_us/index.md` 示例：**

```markdown
# My Mod Guide

Welcome to My Mod! This guide will help you get started.

## Features

- Feature A
- Feature B

## Quick Links

- [Getting Started](getting_started)
- [FAQ](faq)
```

---

## 3. 用命令打开文档

在游戏中执行客户端命令：

```
/ageratum <your_modid>
```

Ageratum 会自动根据客户端当前语言寻找对应的文档文件，找不到时回退到 `en_us`。

### 命令语法

```
/ageratum <namespace>               # 打开 <namespace>:ageratum/<lang>/index.md
/ageratum <namespace> <file>        # 打开 <namespace>:ageratum/<lang>/<file>.md
/ageratum <namespace> <dir>/<file>  # 打开子目录文档
```

> 两个参数均支持 Tab 补全，自动列出资源包中存在的命名空间和文件名。

---

## 4. 从服务端触发打开

如果你的模组需要从服务端（或在 SSP 服务端线程中）触发客户端打开文档：

```java
import dev.anvilcraft.resource.ageratum.Ageratum;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;

// 在你的事件处理器或命令中调用
public void onPlayerAction(ServerPlayer player) {
    ResourceLocation docLocation = ResourceLocation.fromNamespaceAndPath(
        "mymod", "ageratum/en_us/getting_started.md"
    );
    Ageratum.openGuide(player, docLocation);
}
```

Ageratum 会通过网络包通知客户端打开指定文档。

---

## 5. 添加更多文档

你可以在 `ageratum/<lang>/` 目录下自由创建子目录和更多 `.md` 文件：

```
assets/<your_modid>/ageratum/
├── en_us/
│   ├── index.md
│   ├── getting_started.md
│   ├── faq.md
│   └── tutorial/
│       ├── basics.md
│       └── advanced.md
└── zh_cn/
    ├── index.md
    └── ...
```

在文档中可以用 **相对路径** 互相链接：

```markdown
[快速开始](getting_started)
[基础教程](tutorial/basics)
```

---

## 下一步

- 查看 [Markdown 语法参考](03-markdown-syntax.md) 了解支持的语法
- 查看 [扩展组件开发](04-extension-components.md) 添加自定义块组件
- 查看 [API 参考](07-api-reference.md) 了解完整 API

