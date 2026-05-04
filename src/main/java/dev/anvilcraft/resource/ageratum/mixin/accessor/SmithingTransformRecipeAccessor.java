package dev.anvilcraft.resource.ageratum.mixin.accessor;

import net.minecraft.world.item.ItemStackTemplate;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.SmithingTransformRecipe;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import java.util.Optional;

@Mixin(SmithingTransformRecipe.class)
public interface SmithingTransformRecipeAccessor {
    @Accessor("template")
    Optional<Ingredient> template();

    @Accessor("base")
    Ingredient base();

    @Accessor("addition")
    Optional<Ingredient> addition();

    @Accessor("result")
    ItemStackTemplate result();
}
