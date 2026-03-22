package dev.anvilcraft.resource.ageratum.client.registries;

import dev.anvilcraft.resource.ageratum.client.feat.markdown.component.recipe.MDCraftingTableRecipeComponent;
import dev.anvilcraft.resource.ageratum.client.feat.markdown.component.recipe.MDRecipeComponent;
import net.minecraft.world.item.crafting.RecipeType;
import net.neoforged.neoforge.registries.DeferredHolder;

/**
 * 内置配方组件工厂注册。
 *
 * <p>将原版常见配方类型映射到对应的 {@link MDRecipeComponent} 实现。</p>
 */
public class BuiltinRecipeComponentFactories {
    /**
     * 工作台配方渲染工厂（{@code RecipeType.CRAFTING}）。
     */
    public static final DeferredHolder<MDRecipeComponent.RecipeComponentFactory<?>, MDRecipeComponent.RecipeComponentFactory<?>> INFO =
        AgeratumRegistries.RECIPE_COMPONENT_FACTORIES.register(
            "crafting",
            () -> MDRecipeComponent.RecipeComponentFactory.create(RecipeType.CRAFTING, MDCraftingTableRecipeComponent::new)
        );

    /**
     * 触发类加载，确保静态注册项初始化。
     */
    public static void init() {
    }
}
