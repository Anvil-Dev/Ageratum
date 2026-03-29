# 扩展组件开发

本文档介绍如何注册自定义块级扩展组件，让你的模组可以在 Ageratum 文档中使用专属的渲染块。

---

## 概念介绍

**扩展组件**（Extension Component）是 Ageratum 文档中可渲染的自定义块级元素。通过注册 `MDExtensionComponentFactory`，开发者可以使用两种语法在文档中插入自定义渲染内容：

```markdown
<!-- 冒号语法 -->
::: mymod:my_component
内容
:::

<!-- 标签语法 -->
<mymod:my_component key="value"/>
```

---

## 核心接口

### `MDExtensionComponentFactory`

```java
package dev.anvilcraft.resource.ageratum.client.feat.markdown;

@FunctionalInterface
public interface MDExtensionComponentFactory {
    /**
     * 根据扩展上下文创建 Markdown 组件。
     *
     * @param context 包含组件 ID、参数、渲染内容等信息的上下文
     * @return 要渲染的组件实例
     */
    MDComponent create(MDExtensionContext context);
}
```

### `MDExtensionContext`

```java
public record MDExtensionContext(
    ResourceLocation sourceLocation, // 当前文档位置
    ResourceLocation id,          // 组件 ID，如 mymod:my_component
    String rawParams,             // 原始参数字符串（冒号语法时可用）
    Map<String, String> params,   // 解析后的参数键值对（标签语法时可用）
    List<MDComponent> renderedContent, // 块内容解析后的子组件列表
    String rawContent             // 块内容的原始文本
) {}
```

### `MDComponent`（基类）

所有组件都需要继承 `MDComponent`，实现以下方法：

```java
public abstract class MDComponent {
    /** 在给定区域内渲染组件内容 */
    public void render(GuiGraphics guiGraphics, Minecraft minecraft,
                       int maxX, int maxY, float mouseX, float mouseY);

    /** 计算组件在指定宽度下的渲染高度 */
    public int getHeight(Minecraft minecraft, int maxX, int maxY);

    /** 获取指定坐标对应的文本样式（用于点击/悬停交互） */
    @Nullable
    public Style getStyleAtPosition(Minecraft minecraft, double mouseX,
                                    double mouseY, int maxX);
}
```

---

## 注册步骤

### 1. 创建组件类

实现一个继承 `MDComponent` 的自定义组件：

```java
package com.example.mymod.client.markdown;

import dev.anvilcraft.resource.ageratum.client.feat.markdown.component.MDComponent;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.FormattedText;
import net.minecraft.network.chat.Style;

import java.util.List;
import java.util.Map;
import javax.annotation.Nullable;

public class MyCustomComponent extends MDComponent {

    private final String label;
    private final List<MDComponent> children;

    public MyCustomComponent(String label, List<MDComponent> children) {
        super(FormattedText.EMPTY); // 若你的渲染不依赖 text 字段，传 EMPTY
        this.label = label;
        this.children = children;
    }

    @Override
    public void render(GuiGraphics guiGraphics, Minecraft minecraft,
                       int maxX, int maxY, float mouseX, float mouseY) {
        // 绘制标签头部
        guiGraphics.drawString(minecraft.font, "▶ " + this.label, 0, 0, 0xFF5500, false);
        int yOffset = minecraft.font.lineHeight + 2;

        // 绘制子组件
        for (MDComponent child : this.children) {
            int childHeight = child.getHeight(minecraft, maxX - 8, maxY - yOffset);
            if (yOffset + childHeight > maxY) break;
            guiGraphics.pose().pushPose();
            guiGraphics.pose().translate(8, yOffset, 0);
            child.render(guiGraphics, minecraft, maxX - 8, maxY - yOffset, mouseX - 8, mouseY - yOffset);
            guiGraphics.pose().popPose();
            yOffset += childHeight;
        }
    }

    @Override
    public int getHeight(Minecraft minecraft, int maxX, int maxY) {
        int height = minecraft.font.lineHeight + 2;
        for (MDComponent child : this.children) {
            height += child.getHeight(minecraft, maxX - 8, maxY);
        }
        return height;
    }

    @Nullable
    @Override
    public Style getStyleAtPosition(Minecraft minecraft, double mouseX, double mouseY, int maxX) {
        int yOffset = minecraft.font.lineHeight + 2;
        for (MDComponent child : this.children) {
            int childHeight = child.getHeight(minecraft, maxX - 8, Integer.MAX_VALUE);
            if (mouseY >= yOffset && mouseY < yOffset + childHeight) {
                return child.getStyleAtPosition(minecraft, mouseX - 8, mouseY - yOffset, maxX - 8);
            }
            yOffset += childHeight;
        }
        return null;
    }
}
```

### 2. 注册工厂

使用 `DeferredRegister`（推荐）：

```java
package com.example.mymod.client;

import com.example.mymod.client.markdown.MyCustomComponent;
import dev.anvilcraft.resource.ageratum.client.feat.markdown.MDExtensionComponentFactory;
import dev.anvilcraft.resource.ageratum.client.registries.AgeratumRegistries;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class MyModExtensionComponents {

    public static final DeferredRegister<MDExtensionComponentFactory> EXTENSION_COMPONENTS =
        DeferredRegister.create(
            AgeratumRegistries.EXTENSION_COMPONENT_FACTORY_REGISTRY_KEY,
            "mymod"  // 使用你的 mod id
        );

    /**
     * 注册名称 "section" 对应标签 <mymod:section label="...">
     */
    public static final DeferredHolder<MDExtensionComponentFactory, MDExtensionComponentFactory> SECTION =
        EXTENSION_COMPONENTS.register(
            "section",
            () -> context -> {
                String label = context.params().getOrDefault("label", "Section");
                return new MyCustomComponent(label, context.renderedContent());
            }
        );

    private MyModExtensionComponents() {}
}
```

### 3. 在客户端初始化中绑定事件总线

```java
package com.example.mymod.client;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;

@Mod(value = "mymod", dist = Dist.CLIENT)
public class MyModClient {
    public MyModClient(IEventBus modEventBus, ModContainer modContainer) {
        MyModExtensionComponents.EXTENSION_COMPONENTS.register(modEventBus);
    }
}
```

---

## 在文档中使用

注册完成后，在你的 Markdown 文档中即可使用：

```markdown
<!-- 标签语法（支持参数） -->
<mymod:section label="重要说明">
这里是 section 的内容，支持完整 Markdown 语法。

- 列表项 1
- 列表项 2
</mymod:section>

<!-- 冒号语法（参数以空格分隔的原始字符串传入） -->
::: mymod:section label="重要说明"
这里是 section 的内容。
:::

<!-- 自闭合（无块内容） -->
<mymod:section label="空节"/>
```

---

## 参数解析说明

### 标签语法参数（`context.params()`）

标签语法 `<mymod:component key="value" flag=true>` 的参数会被解析为 `Map<String, String>`：

```java
Map<String, String> params = context.params();
String id = params.get("id");            // "value"
String flag = params.get("flag");        // "true"（字符串形式）
```

- 参数名支持字母、数字、`_`、`:` 和 `-`
- 参数值可以带双引号或不带引号
- 带引号的值中不能包含 `"`

### 冒号语法参数（`context.rawParams()`）

冒号语法 `::: mymod:component raw param string` 的所有参数以原始字符串形式提供：

```java
String raw = context.rawParams(); // "raw param string"
// 需要自行解析
```

> 推荐使用标签语法，参数更结构化，便于解析。

---

## 块内容

`context.renderedContent()` 包含块内容解析后的 `MDComponent` 列表，顺序与文档中一致。可以直接将它们传给子容器组件渲染。

`context.rawContent()` 包含块内容的原始文本，适合需要再次解析或处理的场景。

---

## 完整示例：注意框变种

下面实现一个可以带自定义颜色和图标的注意框：

```java
public class ColoredNoticeComponent extends MDComponent {
    private final int borderColor;
    private final String icon;
    private final List<MDComponent> children;

    public ColoredNoticeComponent(int borderColor, String icon, List<MDComponent> children) {
        super(FormattedText.EMPTY);
        this.borderColor = borderColor;
        this.icon = icon;
        this.children = children;
    }

    @Override
    public void render(GuiGraphics g, Minecraft mc, int maxX, int maxY, float mx, float my) {
        // 绘制左边彩色竖条
        g.fill(0, 0, 2, getHeight(mc, maxX, maxY), this.borderColor | 0xFF000000);
        int y = 2;
        g.drawString(mc.font, this.icon, 4, 0, this.borderColor, false);
        y += mc.font.lineHeight + 2;
        for (MDComponent child : this.children) {
            int h = child.getHeight(mc, maxX - 8, maxY - y);
            if (y + h > maxY) break;
            g.pose().pushPose();
            g.pose().translate(8, y, 0);
            child.render(g, mc, maxX - 8, maxY - y, mx - 8, my - y);
            g.pose().popPose();
            y += h;
        }
    }

    @Override
    public int getHeight(Minecraft mc, int maxX, int maxY) {
        int h = mc.font.lineHeight + 4;
        for (MDComponent child : this.children) {
            h += child.getHeight(mc, maxX - 8, maxY);
        }
        return h;
    }
}

// 注册
public static final DeferredHolder<MDExtensionComponentFactory, MDExtensionComponentFactory> CUSTOM_NOTICE =
    EXTENSION_COMPONENTS.register("notice", () -> context -> {
        String colorStr = context.params().getOrDefault("color", "5555FF");
        String icon = context.params().getOrDefault("icon", "ℹ");
        int color = Integer.parseInt(colorStr, 16);
        return new ColoredNoticeComponent(color, icon, context.renderedContent());
    });
```

文档使用：

```markdown
<mymod:notice color="FF5500" icon="⚠">
这是一个橙色警告框，带自定义图标。
</mymod:notice>
```

---

## 参见

- [行内样式解析器开发](05-inline-style-parsers.md)
- [配方组件开发](06-recipe-components.md)
- [API 参考](07-api-reference.md)

