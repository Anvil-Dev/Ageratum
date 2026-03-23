# Recipe Components

This document explains how to register custom recipe type renderers, allowing Ageratum to render your mod's custom recipes inside documents.

---

## Overview

Ageratum provides the `<recipe id="..."/>` extension tag to embed in-game recipe rendering directly in documents.  
By registering an `MDRecipeComponent.RecipeComponentFactory<T>`, you can provide a rendering implementation for any `RecipeType<T>`.

**Built-in support**: `RecipeType.CRAFTING` (crafting table recipes) → `MDCraftingTableRecipeComponent`

---

## Core Interface

### `MDRecipeComponent.RecipeComponentFactory<T>`

```java
public interface RecipeComponentFactory<T extends Recipe<?>> {
    /** The recipe type this factory handles */
    RecipeType<T> type();

    /** Create a renderable component from a specific recipe instance */
    MDRecipeComponent create(T recipe);

    /** Static factory method for lambda-style registration */
    static <T extends Recipe<?>> RecipeComponentFactory<T> create(
        RecipeType<T> type,
        Function<T, MDRecipeComponent> factory
    ) { ... }
}
```

### `MDRecipeComponent` (Abstract Base Class)

```java
public abstract class MDRecipeComponent extends MDImageComponent {
    protected final int width;   // Background texture width (pixels)
    protected final int height;  // Background texture height (pixels)

    public MDRecipeComponent(ResourceLocation imageLocation, int width, int height) { ... }

    /** Subclass implements: draw recipe content on top of the background */
    protected void renderRecipe(GuiGraphics guiGraphics, float mouseX, float mouseY) {}
}
```

---

## Registration Steps

### 1. Prepare the Background Texture

Place your recipe background image in your resource pack:

```
src/main/resources/
└── assets/
    └── mymod/
        └── textures/
            └── gui/
                └── component/
                    └── my_furnace.png   ← Background texture (power of 2 recommended)
```

### 2. Create the Renderer Component

```java
package com.example.mymod.client.markdown.recipe;

import dev.anvilcraft.resource.ageratum.client.feat.markdown.component.recipe.MDRecipeComponent;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.SmeltingRecipe;

public class MDFurnaceRecipeComponent extends MDRecipeComponent {

    public static final ResourceLocation TEXTURE =
        ResourceLocation.fromNamespaceAndPath("mymod", "gui/component/my_furnace.png");

    private final ItemStack input;
    private final ItemStack fuel;
    private final ItemStack output;

    public MDFurnaceRecipeComponent(SmeltingRecipe recipe) {
        super(TEXTURE, 256, 128);  // Texture dimensions in pixels
        Ingredient[] ingredients = recipe.getIngredients().toArray(Ingredient[]::new);
        this.input = ingredients.length > 0 && !ingredients[0].isEmpty()
            ? ingredients[0].getItems()[0] : ItemStack.EMPTY;
        this.fuel  = new ItemStack(net.minecraft.world.item.Items.COAL);
        var level  = Minecraft.getInstance().level;
        this.output = level != null
            ? recipe.getResultItem(level.registryAccess()) : ItemStack.EMPTY;
    }

    @Override
    protected void renderRecipe(GuiGraphics g, float mouseX, float mouseY) {
        // Draw items at their positions in the background image
        // Adjust coordinates to match your texture layout
        renderItem(g, this.input,   8, 24, mouseX, mouseY);
        renderItem(g, this.fuel,    8, 60, mouseX, mouseY);
        renderItem(g, this.output, 96, 36, mouseX, mouseY);
    }

    private void renderItem(GuiGraphics g, ItemStack stack, int x, int y,
                             float mouseX, float mouseY) {
        if (stack.isEmpty()) return;
        Minecraft mc = Minecraft.getInstance();
        g.renderItem(stack, x, y);
        g.renderItemDecorations(mc.font, stack, x, y);
        if (mouseX >= x && mouseX < x + 16 && mouseY >= y && mouseY < y + 16) {
            g.renderTooltip(mc.font, stack, (int) mouseX, (int) mouseY);
        }
    }
}
```

### 3. Register the Factory

```java
package com.example.mymod.client;

import com.example.mymod.client.markdown.recipe.MDFurnaceRecipeComponent;
import dev.anvilcraft.resource.ageratum.client.feat.markdown.component.recipe.MDRecipeComponent;
import dev.anvilcraft.resource.ageratum.client.registries.AgeratumRegistries;
import net.minecraft.world.item.crafting.RecipeType;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class MyModRecipeComponentFactories {

    public static final DeferredRegister<MDRecipeComponent.RecipeComponentFactory<?>>
        RECIPE_COMPONENT_FACTORIES = DeferredRegister.create(
            AgeratumRegistries.RECIPE_COMPONENT_FACTORY_REGISTRY_KEY,
            "mymod"
        );

    public static final DeferredHolder<
        MDRecipeComponent.RecipeComponentFactory<?>,
        MDRecipeComponent.RecipeComponentFactory<?>
    > SMELTING = RECIPE_COMPONENT_FACTORIES.register(
        "smelting",
        () -> MDRecipeComponent.RecipeComponentFactory.create(
            RecipeType.SMELTING,
            MDFurnaceRecipeComponent::new
        )
    );

    private MyModRecipeComponentFactories() {}
}
```

### 4. Bind to the Event Bus

```java
@Mod(value = "mymod", dist = Dist.CLIENT)
public class MyModClient {
    public MyModClient(IEventBus modEventBus, ModContainer modContainer) {
        MyModRecipeComponentFactories.RECIPE_COMPONENT_FACTORIES.register(modEventBus);
    }
}
```

---

## Using in Documents

```markdown
## Smelting Recipe

Here is the smelting recipe for iron ingots:

<recipe id="minecraft:iron_ingot_from_smelting_iron_ore"/>

## Crafting Recipe

Here is the crafting recipe for a crafting table:

<recipe id="minecraft:crafting_table"/>
```

Ageratum looks up the recipe type by ID, then selects the matching `RecipeComponentFactory` to render it.

---

## Rendering Behavior

| Situation | Rendering Result |
|-----------|-----------------|
| Recipe not found | Component height = 0, invisible |
| No matching factory | Component height = 0, invisible |
| Client level not ready (`level == null`) | Items may be empty, background still renders |
| Normal | Background + item icons + hover tooltips |

---

## Background Texture Design Tips

- Use **power-of-2** dimensions (e.g. 256×128, 512×256)
- Leave at least 1px padding to prevent item rendering overflow
- Reference the built-in crafting table texture: `assets/ageratum/textures/gui/component/crafting_table.png`

---

## Item Rendering & Scaling

The `MDRecipeComponent` base class provides `computeRenderSize(size, maxX, maxY)` to proportionally scale the background image to fit the available document width. Coordinates in `renderRecipe` are in **original texture pixels** — scaling is handled uniformly by the base class's `innerBlit`.

To adjust item render offsets, use `PoseStack.scale()` combined with `pose.translate()` for affine transforms, as demonstrated in `MDCraftingTableRecipeComponent`.

---

## See Also

- [Extension Components](04-extension-components.md)
- [Markdown Syntax Reference → Recipe Component](03-markdown-syntax.md#built-in-extension-components)
- [API Reference](07-api-reference.md)

