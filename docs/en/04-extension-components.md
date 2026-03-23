# Extension Components

This document explains how to register custom block-level extension components, allowing your mod to use custom rendering blocks inside Ageratum documents.

---

## Overview

**Extension Components** are custom block-level elements that can be rendered in Ageratum documents. By registering an `MDExtensionComponentFactory`, developers can insert custom rendering content into documents using either of two syntaxes:

```markdown
<!-- Colon syntax -->
::: mymod:my_component
Content
:::

<!-- Tag syntax -->
<mymod:my_component key="value"/>
```

---

## Core Interfaces

### `MDExtensionComponentFactory`

```java
package dev.anvilcraft.resource.ageratum.client.feat.markdown;

@FunctionalInterface
public interface MDExtensionComponentFactory {
    /**
     * Creates a Markdown component from the extension context.
     *
     * @param context Context containing component ID, parameters, content, etc.
     * @return The component instance to render
     */
    MDComponent create(MDExtensionContext context);
}
```

### `MDExtensionContext`

```java
public record MDExtensionContext(
    ResourceLocation id,               // Component ID, e.g. mymod:section
    String rawParams,                  // Raw parameter string (colon syntax)
    Map<String, String> params,        // Parsed key-value pairs (tag syntax)
    List<MDComponent> renderedContent, // Parsed child components from block content
    String rawContent                  // Raw block content text
) {}
```

### `MDComponent` (Base Class)

All components must extend `MDComponent` and implement:

```java
public abstract class MDComponent {
    /** Render the component within the given bounds */
    public void render(GuiGraphics guiGraphics, Minecraft minecraft,
                       int maxX, int maxY, float mouseX, float mouseY);

    /** Calculate the component's rendered height within the given width */
    public int getHeight(Minecraft minecraft, int maxX, int maxY);

    /** Get the text style at a given coordinate (for click/hover) */
    @Nullable
    public Style getStyleAtPosition(Minecraft minecraft, double mouseX,
                                    double mouseY, int maxX);
}
```

---

## Registration Steps

### 1. Create the Component Class

Implement a custom component by extending `MDComponent`:

```java
package com.example.mymod.client.markdown;

import dev.anvilcraft.resource.ageratum.client.feat.markdown.component.MDComponent;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.FormattedText;
import net.minecraft.network.chat.Style;

import java.util.List;
import javax.annotation.Nullable;

public class MyCustomComponent extends MDComponent {

    private final String label;
    private final List<MDComponent> children;

    public MyCustomComponent(String label, List<MDComponent> children) {
        super(FormattedText.EMPTY); // Pass EMPTY if you don't use the text field
        this.label = label;
        this.children = children;
    }

    @Override
    public void render(GuiGraphics guiGraphics, Minecraft minecraft,
                       int maxX, int maxY, float mouseX, float mouseY) {
        // Draw label header
        guiGraphics.drawString(minecraft.font, "▶ " + this.label, 0, 0, 0xFF5500, false);
        int yOffset = minecraft.font.lineHeight + 2;

        // Draw child components
        for (MDComponent child : this.children) {
            int childHeight = child.getHeight(minecraft, maxX - 8, maxY - yOffset);
            if (yOffset + childHeight > maxY) break;
            guiGraphics.pose().pushPose();
            guiGraphics.pose().translate(8, yOffset, 0);
            child.render(guiGraphics, minecraft, maxX - 8, maxY - yOffset,
                         mouseX - 8, mouseY - yOffset);
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
    public Style getStyleAtPosition(Minecraft minecraft,
                                    double mouseX, double mouseY, int maxX) {
        int yOffset = minecraft.font.lineHeight + 2;
        for (MDComponent child : this.children) {
            int childHeight = child.getHeight(minecraft, maxX - 8, Integer.MAX_VALUE);
            if (mouseY >= yOffset && mouseY < yOffset + childHeight) {
                return child.getStyleAtPosition(minecraft,
                                                mouseX - 8, mouseY - yOffset, maxX - 8);
            }
            yOffset += childHeight;
        }
        return null;
    }
}
```

### 2. Register the Factory

Use `DeferredRegister` (recommended):

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
            "mymod"
        );

    /**
     * Registration "section" maps to <mymod:section label="...">
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

### 3. Bind to the Event Bus in Client Init

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

## Using in Documents

Once registered, use your component in any Markdown document:

```markdown
<!-- Tag syntax (supports structured parameters) -->
<mymod:section label="Important Notice">
This is the section content. Full Markdown is supported here.

- List item 1
- List item 2
</mymod:section>

<!-- Colon syntax (params passed as raw string) -->
::: mymod:section label="Important Notice"
This is the section content.
:::

<!-- Self-closing (no block content) -->
<mymod:section label="Empty Section"/>
```

---

## Parameter Parsing

### Tag Syntax Parameters (`context.params()`)

Tag syntax `<mymod:component key="value" flag=true>` params are parsed into a `Map<String, String>`:

```java
Map<String, String> params = context.params();
String id    = params.get("id");     // "value"
String flag  = params.get("flag");   // "true" (as String)
```

- Parameter names support letters, digits, `_`, `:`, and `-`
- Values can be quoted (`"value"`) or unquoted (`value`)
- Quoted values cannot contain `"`

### Colon Syntax Parameters (`context.rawParams()`)

Colon syntax `::: mymod:component raw param string` provides all parameters as a raw string:

```java
String raw = context.rawParams(); // "raw param string"
// Parse manually
```

> Tag syntax is recommended for structured parameter parsing.

---

## Block Content

`context.renderedContent()` contains the parsed `MDComponent` list from the block content, in document order. Pass it directly to container components for rendering.

`context.rawContent()` contains the raw text of the block content, useful when you need to re-parse or further process it.

---

## Full Example: Custom Notice Box Variant

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
        // Draw colored left border strip
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
        for (MDComponent child : this.children) h += child.getHeight(mc, maxX - 8, maxY);
        return h;
    }
}

// Registration
public static final DeferredHolder<MDExtensionComponentFactory, MDExtensionComponentFactory> CUSTOM_NOTICE =
    EXTENSION_COMPONENTS.register("notice", () -> context -> {
        String colorStr = context.params().getOrDefault("color", "5555FF");
        String icon     = context.params().getOrDefault("icon", "ℹ");
        int color = Integer.parseInt(colorStr, 16);
        return new ColoredNoticeComponent(color, icon, context.renderedContent());
    });
```

Document usage:

```markdown
<mymod:notice color="FF5500" icon="⚠">
This is an orange warning box with a custom icon.
</mymod:notice>
```

---

## See Also

- [Inline Style Parsers](05-inline-style-parsers.md)
- [Recipe Components](06-recipe-components.md)
- [API Reference](07-api-reference.md)

