package dev.anvilcraft.resource.ageratum.client.feat.markdown.component.recipe;

import dev.anvilcraft.resource.ageratum.Ageratum;
import dev.anvilcraft.resource.ageratum.client.feat.markdown.MDExtensionContext;
import dev.anvilcraft.resource.ageratum.client.feat.markdown.component.MDComponent;
import dev.anvilcraft.resource.ageratum.client.feat.markdown.component.MDImageComponent;
import dev.anvilcraft.resource.ageratum.client.feat.markdown.component.MDTextComponent;
import dev.anvilcraft.resource.ageratum.client.registries.AgeratumRegistries;
import lombok.Getter;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeManager;
import net.minecraft.world.item.crafting.RecipeType;

import java.util.Optional;
import java.util.function.Function;
import javax.annotation.Nullable;

@Getter
public abstract class MDRecipeComponent extends MDImageComponent {
    private final int width;
    private final int height;

    /**
     * 创建配方组件。
     */
    public MDRecipeComponent(ResourceLocation imageLocation, int width, int height) {
        super(imageLocation);
        this.width = width;
        this.height = height;
    }

    @Override
    protected void renderContent(GuiGraphics guiGraphics, Size size) {
        this.innerBlit(guiGraphics, this.getImageLocation(), this.width, this.height, size.width(), size.height());
        this.renderRecipe(guiGraphics);
    }

    protected void renderRecipe(GuiGraphics guiGraphics) {
    }

    public static MDComponent parse(MDExtensionContext context) {
        String id = context.params().get("id");
        ResourceLocation location = ResourceLocation.parse(id);
        return new MDRecipeComponentProxy(location);
    }

    /**
     * 返回图片在目标区域中的渲染高度。
     */
    @Override
    public int getHeight(Minecraft minecraft, int maxX, int maxY) {
        Size size = new Size(this.width, this.height);
        return this.computeRenderSize(size, maxX, maxY).height();
    }

    public interface RecipeComponentFactory<T extends Recipe<?>> {
        RecipeType<T> type();

        MDRecipeComponent create(T recipe);

        static <R extends Recipe<?>> RecipeComponentFactory<R> create(
            RecipeType<R> type,
            Function<R, MDRecipeComponent> function
        ) {
            return new RecipeComponentFactory<>() {
                @Override
                public RecipeType<R> type() {
                    return type;
                }

                @Override
                public MDRecipeComponent create(R recipe) {
                    return function.apply(recipe);
                }
            };
        }
    }

    static class MDRecipeComponentProxy extends MDRecipeComponent {
        private final MDComponent emptyComponent = new MDTextComponent("");
        private @Nullable MDRecipeComponent component = null;
        private final ResourceLocation location;

        public MDRecipeComponentProxy(ResourceLocation location) {
            super(Ageratum.location("empty"), 0, 0);
            this.location = location;
        }

        @Override
        public void render(GuiGraphics guiGraphics, Minecraft minecraft, int maxX, int maxY) {
            if (component != null) {
                this.component.render(guiGraphics, minecraft, maxX, maxY);
                return;
            }
            ClientLevel level = minecraft.level;
            if (level == null) {
                emptyComponent.render(guiGraphics, minecraft, maxX, maxY);
                return;
            }
            RecipeManager manager = level.getRecipeManager();
            Optional<RecipeHolder<?>> holderOptional = manager.byKey(this.location);
            if (holderOptional.isPresent()) {
                if (this.setComponent(holderOptional.get())) {
                    return;
                }
            }
            emptyComponent.render(guiGraphics, minecraft, maxX, maxY);
        }

        @SuppressWarnings("unchecked")
        public <T extends Recipe<?>> boolean setComponent(RecipeHolder<?> holder) {
            T value = ((RecipeHolder<T>) holder).value();
            RecipeType<T> type = (RecipeType<T>) value.getType();
            for (RecipeComponentFactory<?> factory : AgeratumRegistries.RECIPE_COMPONENT_FACTORY_REGISTRY) {
                if (factory.type() == type) {
                    RecipeComponentFactory<T> factoryT = (RecipeComponentFactory<T>) factory;
                    this.component = factoryT.create(value);
                    return true;
                }
            }
            return false;
        }

        @Override
        public int getHeight(Minecraft minecraft, int maxX, int maxY) {
            if (this.component == null) {
                return this.emptyComponent.getHeight(minecraft, maxX, maxY);
            }
            return this.component.getHeight(minecraft, maxX, maxY);
        }
    }
}
