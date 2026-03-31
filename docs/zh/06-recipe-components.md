# 配方组件开发

本文档介绍如何注册自定义配方类型渲染器，让 Ageratum 能够在文档中渲染你模组的专属配方。

---

## 概念介绍

Ageratum 提供了 `<recipe id="..."/>` 扩展标签，可以在文档中内嵌游戏配方的渲染结果。  
通过注册 `MDRecipeComponent.RecipeComponentFactory<T>`，可以为任意 `RecipeType<T>` 提供对应的渲染实现。

**内置支持**：`RecipeType.CRAFTING`（工作台合成配方）→ `MDCraftingTableRecipeComponent`

---

## 核心接口

### `MDRecipeComponent.RecipeComponentFactory<T>`

```java
public interface RecipeComponentFactory<T extends Recipe<?>> {
    /** 当前工厂支持的配方类型 */
    RecipeType<T> type();

    /** 由具体配方实例创建可渲染组件 */
    MDRecipeComponent create(T recipe);

    /** 静态工厂方法：便于 lambda 方式注册 */
    static <T extends Recipe<?>> RecipeComponentFactory<T> create(
        RecipeType<T> type,
        Function<T, MDRecipeComponent> factory
    ) { ... }
}
```

### `MDRecipeComponent`（抽象基类）

```java
public abstract class MDRecipeComponent extends MDImageComponent {
    /** 原始材质宽度（像素） */
    protected final int width;
    /** 原始材质高度（像素） */
    protected final int height;

    public MDRecipeComponent(ResourceLocation imageLocation, int width, int height) { ... }

    /** 子类实现：在底图上绘制配方内容 */
    protected void renderRecipe(GuiGraphics guiGraphics, float mouseX, float mouseY) {}
}
```

---

## 注册步骤

### 1. 准备背景纹理

将你的配方背景图片放置在资源包中：

```
src/main/resources/
└── assets/
    └── mymod/
        └── textures/
            └── gui/
                └── component/
                    └── my_furnace.png   ← 背景纹理（建议 2 的幂次）
```

### 2. 创建渲染组件

```java
package com.example.mymod.client.markdown.recipe;

import dev.anvilcraft.resource.ageratum.client.feat.markdown.component.extend.recipe.MDRecipeComponent;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.SmeltingRecipe;  // 假设是熔炉配方

public class MDFurnaceRecipeComponent extends MDRecipeComponent {

    /** 背景纹理，宽 256px，高 128px */
    public static final ResourceLocation TEXTURE =
        ResourceLocation.fromNamespaceAndPath("mymod", "gui/component/my_furnace.png");

    private final ItemStack input;
    private final ItemStack fuel;
    private final ItemStack output;

    public MDFurnaceRecipeComponent(SmeltingRecipe recipe) {
        super(TEXTURE, 256, 128);  // 纹理尺寸（原始像素）
        Ingredient[] ingredients = recipe.getIngredients().toArray(Ingredient[]::new);
        this.input = ingredients.length > 0 && !ingredients[0].isEmpty()
                     ? ingredients[0].getItems()[0] : ItemStack.EMPTY;
        this.fuel = new ItemStack(net.minecraft.world.item.Items.COAL);
        // 获取输出需要访问 registryAccess
        var level = Minecraft.getInstance().level;
        this.output = level != null
                      ? recipe.getResultItem(level.registryAccess())
                      : ItemStack.EMPTY;
    }

    @Override
    protected void renderRecipe(GuiGraphics g, float mouseX, float mouseY) {
        // 在背景图上按位置绘制物品
        // 假设背景图中：输入格在 (8, 24)，燃料格在 (8, 60)，输出格在 (96, 36)
        renderItem(g, this.input, 8, 24, mouseX, mouseY);
        renderItem(g, this.fuel, 8, 60, mouseX, mouseY);
        renderItem(g, this.output, 96, 36, mouseX, mouseY);
    }

    private void renderItem(
        GuiGraphics g, ItemStack stack, int x, int y,
        float mouseX, float mouseY
    ) {
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

### 3. 注册工厂

```java
package com.example.mymod.client;

import com.example.mymod.client.markdown.recipe.MDFurnaceRecipeComponent;
import dev.anvilcraft.resource.ageratum.client.feat.markdown.component.extend.recipe.MDRecipeComponent;
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

    /** 注册熔炉配方渲染工厂 */
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

    private MyModRecipeComponentFactories() {
    }
}
```

### 4. 在客户端初始化中绑定

```java
@Mod(value = "mymod", dist = Dist.CLIENT)
public class MyModClient {
    public MyModClient(IEventBus modEventBus, ModContainer modContainer) {
        MyModRecipeComponentFactories.RECIPE_COMPONENT_FACTORIES.register(modEventBus);
    }
}
```

---

## 在文档中使用

```markdown
## 熔炼配方

以下是铁锭的冶炼配方：

<recipe id="minecraft:iron_ingot_from_smelting_iron_ore"/>

## 合成配方

以下是工作台的合成配方：

<recipe id="minecraft:crafting_table"/>
```

Ageratum 会根据配方 ID 查找对应的 `RecipeType`，然后选择匹配的 `RecipeComponentFactory` 渲染。

---

## 渲染行为说明

| 情形 | 渲染结果 |
|------|---------|
| 配方不存在 | 组件高度为 0，不占位 |
| 无匹配工厂 | 组件高度为 0，不占位 |
| 客户端世界未就绪（`level == null`） | 物品可能为空，组件仍可渲染背景图 |
| 正常 | 渲染背景图 + 物品图标 + 鼠标悬停 tooltip |

---

## 背景纹理设计建议

- 使用 **2 的幂次** 尺寸（如 256×128、512×256）
- 建议在纹理中保留 1px 间距，避免物品渲染溢出
- 参考内置工作台纹理：`assets/ageratum/textures/gui/component/crafting_table.png`

---

## 物品渲染缩放

`MDRecipeComponent` 基类提供 `computeRenderSize(size, maxX, maxY)` 来等比缩放底图，适应文档可用宽度。`renderRecipe` 中的坐标基于**原始纹理像素**，缩放由基类的 `innerBlit` 统一处理。

如需在子类中调整渲染偏移，使用 `PoseStack.scale()` 配合 `pose.translate()` 进行仿射变换。

---

## 参见

- [扩展组件开发](04-extension-components.md)
- [Markdown 语法参考 → 配方组件](03-markdown-syntax.md#内置扩展组件)
- [API 参考](07-api-reference.md)

