package dev.anvilcraft.resource.ageratum.client.registries;

import dev.anvilcraft.resource.ageratum.client.feat.markdown.component.recipe.MDCraftingTableRecipeComponent;
import dev.anvilcraft.resource.ageratum.client.feat.markdown.component.recipe.MDRecipeComponent;
import net.minecraft.world.item.crafting.RecipeType;
import net.neoforged.neoforge.registries.DeferredHolder;

public class BuiltinRecipeComponentFactories {
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
