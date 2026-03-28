package dev.anvilcraft.resource.ageratum.client.feat.markdown.component.recipe;

import dev.anvilcraft.resource.ageratum.Ageratum;
import dev.anvilcraft.resource.ageratum.client.feat.markdown.MDExtensionContext;
import dev.anvilcraft.resource.ageratum.client.feat.markdown.MDRenderContext;
import dev.anvilcraft.resource.ageratum.client.feat.markdown.component.MDComponent;
import dev.anvilcraft.resource.ageratum.client.feat.markdown.component.MDImageComponent;
import dev.anvilcraft.resource.ageratum.client.feat.markdown.component.MDTextComponent;
import dev.anvilcraft.resource.ageratum.client.registries.AgeratumRegistries;
import lombok.Getter;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeManager;
import net.minecraft.world.item.crafting.RecipeType;

import java.util.Optional;
import java.util.function.Function;
import javax.annotation.Nullable;

/**
 * Markdown 配方渲染组件基类。
 *
 * <p>该组件由扩展标签 {@code <recipe id="namespace:path"/>} 创建，
 * 负责在文档中渲染某个配方对应的 GUI 贴图与配方内容。</p>
 *
 * <p>配方类型到具体组件的映射通过
 * {@link dev.anvilcraft.resource.ageratum.client.registries.AgeratumRegistries#RECIPE_COMPONENT_FACTORY_REGISTRY}
 * 动态查找。</p>
 */
@Getter
public abstract class MDRecipeComponent extends MDImageComponent {
    /**
     * 原始材质尺寸宽度（像素）。
     */
    private final int width;
    /**
     * 原始材质尺寸高度（像素）。
     */
    private final int height;

    /**
     * 创建配方组件。
     */
    public MDRecipeComponent(ResourceLocation imageLocation, int width, int height) {
        super(imageLocation, true);
        this.width = width;
        this.height = height;
    }

    @Override
    protected void renderContent(MDRenderContext context, Size size, float mouseX, float mouseY) {
        GuiGraphics guiGraphics = context.graphics();
        this.innerBlit(guiGraphics, this.getImageLocation(), this.width, this.height, size.width(), size.height());
        // 子类只关心配方元素绘制，底图缩放由基类统一处理。
        this.renderRecipe(context, mouseX, mouseY);
    }

    /**
     * 在组件底图上绘制配方具体内容（输入、输出等）。
     */
    protected void renderRecipe(MDRenderContext context, float mouseX, float mouseY) {
    }

    /**
     * 解析 {@code recipe} 扩展标签。
     *
     * <p>要求参数中包含 {@code id}，其值应为合法的 {@link ResourceLocation}。</p>
     */
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

    /**
     * 不同 {@link RecipeType} 到具体渲染组件的工厂接口。
     */
    public interface RecipeComponentFactory<T extends Recipe<?>> {
        /**
         * 当前工厂支持的配方类型。
         */
        RecipeType<T> type();

        /**
         * 由具体配方实例创建可渲染组件。
         */
        MDRecipeComponent create(T recipe);

        /**
         * 使用 lambda 快速构造工厂。
         */
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

    /**
     * 配方占位代理。
     *
     * <p>文档解析阶段仅保存配方 ID；渲染阶段按需查询配方管理器，
     * 找到配方后再委托给实际组件渲染并缓存结果。</p>
     */
    static class MDRecipeComponentProxy extends MDRecipeComponent {
        /**
         * 无法解析配方时的回退组件（空文本，占位高度为 0）。
         */
        private final MDComponent emptyComponent = new MDTextComponent("");
        /**
         * 已解析出的真实渲染组件；命中后会复用。
         */
        private @Nullable MDRecipeComponent component = null;
        /**
         * 文档中声明的配方资源 ID。
         */
        private final ResourceLocation location;

        public MDRecipeComponentProxy(ResourceLocation location) {
            super(Ageratum.location("empty"), 0, 0);
            this.location = location;
        }

        @Override
        public void render(
            MDRenderContext context,
            Minecraft minecraft,
            int maxX,
            int maxY,
            float mouseX,
            float mouseY
        ) {
            if (component != null) {
                this.component.render(context, minecraft, maxX, maxY, mouseX, mouseY);
                return;
            }
            ClientLevel level = minecraft.level;
            if (level == null) {
                emptyComponent.render(context, minecraft, maxX, maxY, mouseX, mouseY);
                return;
            }
            RecipeManager manager = level.getRecipeManager();
            Optional<RecipeHolder<?>> holderOptional = manager.byKey(this.location);
            if (holderOptional.isPresent()) {
                if (this.setComponent(holderOptional.get())) {
                    return;
                }
            }
            emptyComponent.render(context, minecraft, maxX, maxY, mouseX, mouseY);
        }

        /**
         * 按配方类型查找注册工厂并实例化真实组件。
         */
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

    public boolean isHoverItem(int startX, int startY, float mouseX, float mouseY) {
        return this.isHover(startX, startY, 16, 16, mouseX, mouseY);
    }

    public boolean isHover(int startX, int startY, int width, int height, float mouseX, float mouseY) {
        return mouseX >= startX && mouseX <= startX + width && mouseY >= startY && mouseY <= startY + height;
    }

    protected void renderTooltip(
        MDRenderContext context,
        ItemStack stack,
        int startX,
        int startY,
        float mouseX,
        float mouseY
    ) {
        if (this.isHoverItem(startX, startY, mouseX, mouseY)) {
            context.addTooltip(stack);
        }
    }
}
