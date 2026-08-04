package dev.anvilcraft.resource.ageratum.client.feat.markdown.component.extend;

import dev.anvilcraft.resource.ageratum.Ageratum;
import dev.anvilcraft.resource.ageratum.client.AgeratumClient;
import dev.anvilcraft.resource.ageratum.client.feat.markdown.GuideDocumentCache;
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
import net.minecraft.world.item.ItemStack;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeManager;
import net.minecraft.world.item.crafting.RecipeType;

import java.util.List;
import java.util.Optional;
import java.util.function.BiFunction;
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
     * 当前鼠标悬停的配方物品绑定的文档位置，用于配方内 W 键跳转。
     */
    protected @javax.annotation.Nullable ResourceLocation hoveredDocLink;

/**
     * 创建配方组件。
     */
    public MDRecipeComponent(ResourceLocation imageLocation, int width, int height, boolean enableAlignCenter) {
        super(imageLocation, false, enableAlignCenter);
        this.width = width;
        this.height = height;
            this.hoveredDocLink = null;
}

    @Override
    protected void renderContent(MDRenderContext context, Size size, float mouseX, float mouseY) {
        GuiGraphics guiGraphics = context.graphics();
        this.innerBlit(guiGraphics, this.getImageLocation(), this.width, this.height, size.width(), size.height());
        // 子类只关心配方元素绘制，底图缩放由基类统一处理。
        this.hoveredDocLink = null;  // 每帧重置
        this.renderRecipe(context, mouseX, mouseY);
    }

    /**
     * 在组件底图上绘制配方具体内容（输入、输出等）。
     */
    protected void renderRecipe(MDRenderContext context, float mouseX, float mouseY) {
    }

    /**
     * 渲染配方物品并检查文档绑定，若存在则记录到 {@link #hoveredDocLink}。
     * W 键提示由 tooltip 事件在渲染物品 tooltip 时统一注入。
     */
    protected void renderRecipeItem(MDRenderContext context, ItemStack stack, int startX, int startY, float mouseX, float mouseY) {
        if (this.isHoverItem(startX, startY, mouseX, mouseY)) {
            context.addTooltip(stack);
            // W 键提示由 BoundItemGuideNavigator 通过 RenderTooltipEvent 注入，这里只记录跳转目标。
            GuideDocumentCache.getFirstDocumentByItemStack(
                stack,
                AgeratumClient.getClientLanguageCode(context.minecraft())
            ).ifPresentOrElse(
                doc -> this.hoveredDocLink = doc,
                () -> this.hoveredDocLink = null
            );
        }
    }

    @Override
    public boolean keyPressed(
        Minecraft minecraft,
        double mouseX,
        double mouseY,
        int keyCode,
        int scanCode,
        int modifiers,
        int maxX
    ) {
        if (this.hoveredDocLink != null
            && keyCode == dev.anvilcraft.resource.ageratum.client.AgeratumKeyMappings.W_KEY_MAPPING.getKey().getValue()) {
            AgeratumClient.openGuideOnClient(this.hoveredDocLink, java.util.List.of());
            return true;
        }
        return false;
    }


    /**
     * 解析 {@code recipe} 扩展标签。
     *
     * <p>要求参数中包含 {@code id}，其值应为合法的 {@link ResourceLocation}。</p>
     */
    public static MDComponent parse(MDExtensionContext context) {
        String id = context.params().get("id");
        boolean enableAlignCenter = "true".equals(context.params().getOrDefault("center", "true"));
        ResourceLocation location = ResourceLocation.parse(id);
        return new MDRecipeComponentProxy(location, enableAlignCenter);
    }

    @Override
    public int getPreferredWidth(Minecraft minecraft, int maxX, int maxY) {
        Size size = new Size(this.width, this.height, 1.0f);
        return this.computeRenderSize(size, maxX, maxY).width();
    }

    /**
     * 返回图片在目标区域中的渲染高度。
     */
    @Override
    public int getHeight(Minecraft minecraft, int maxX, int maxY) {
        Size size = new Size(this.width, this.height, 1.0f);
        return this.computeRenderSize(size, maxX, maxY).height();
    }

    /**
     * 不同 {@link RecipeType} 到具体渲染组件的工厂接口。
     */
    public interface RecipeComponentFactory<T extends Recipe<?>> {
        /**
         * 当前工厂支持的配方类型。
         */
        List<RecipeType<? extends T>> type();

        /**
         * 由具体配方实例创建可渲染组件。
         */
        MDRecipeComponent create(T recipe, boolean enableAlignCenter);

        /**
         * 使用 lambda 快速构造工厂。
         */
        static <R extends Recipe<?>> RecipeComponentFactory<R> create(
            RecipeType<R> type,
            BiFunction<R, Boolean, MDRecipeComponent> function
        ) {
            return RecipeComponentFactory.create(function, type);
        }

        /**
         * 使用 lambda 快速构造工厂。
         */
        @SafeVarargs
        static <R extends Recipe<?>> RecipeComponentFactory<R> create(
            BiFunction<R, Boolean, MDRecipeComponent> function,
            RecipeType<? extends R>... types
        ) {
            return new RecipeComponentFactory<>() {
                @Override
                public List<RecipeType<? extends R>> type() {
                    return List.of(types);
                }

                @Override
                public MDRecipeComponent create(R recipe, boolean enableAlignCenter) {
                    return function.apply(recipe, enableAlignCenter);
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

        public MDRecipeComponentProxy(ResourceLocation location, boolean enableAlignCenter) {
            super(Ageratum.location("empty"), 0, 0, enableAlignCenter);
            this.location = location;
        }

        @Override
        public void render(MDRenderContext context) {
            Minecraft minecraft = context.minecraft();
            if (component != null) {
                this.component.render(context.child());
                return;
            }
            ClientLevel level = minecraft.level;
            if (level == null) {
                emptyComponent.render(context.child());
                return;
            }
            RecipeManager manager = level.getRecipeManager();
            Optional<RecipeHolder<?>> holderOptional = manager.byKey(this.location);
            if (holderOptional.isPresent()) {
                if (this.setComponent(holderOptional.get())) {
                    return;
                }
            }
            emptyComponent.render(context.child());
        }

        /**
         * 按配方类型查找注册工厂并实例化真实组件。
         */
        @SuppressWarnings("unchecked")
        public <T extends Recipe<?>> boolean setComponent(RecipeHolder<?> holder) {
            T value = ((RecipeHolder<T>) holder).value();
            RecipeType<T> type = (RecipeType<T>) value.getType();
            for (RecipeComponentFactory<?> factory : AgeratumRegistries.RECIPE_COMPONENT_FACTORY_REGISTRY) {
                if (factory.type().contains(type)) {
                    RecipeComponentFactory<T> factoryT = (RecipeComponentFactory<T>) factory;
                    this.component = factoryT.create(value, this.enableAlignCenter);
                    return true;
                }
            }
            return false;
        }

        @Override
        public int getPreferredWidth(Minecraft minecraft, int maxX, int maxY) {
            if (this.component == null) {
                return this.emptyComponent.getPreferredWidth(minecraft, maxX, maxY);
            }
            return this.component.getPreferredWidth(minecraft, maxX, maxY);
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
