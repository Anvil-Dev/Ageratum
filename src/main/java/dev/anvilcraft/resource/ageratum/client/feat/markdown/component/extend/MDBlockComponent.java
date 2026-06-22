package dev.anvilcraft.resource.ageratum.client.feat.markdown.component.extend;

import dev.anvilcraft.resource.ageratum.client.AgeratumClient;
import dev.anvilcraft.resource.ageratum.client.feat.markdown.GuideDocumentCache;
import dev.anvilcraft.resource.ageratum.client.feat.markdown.MDExtensionContext;
import dev.anvilcraft.resource.ageratum.client.feat.markdown.MDRenderContext;
import dev.anvilcraft.resource.ageratum.client.feat.markdown.component.MDComponent;
import dev.anvilcraft.resource.ageratum.client.feat.markdown.component.MDImageComponent;
import dev.anvilcraft.resource.ageratum.client.feat.markdown.component.MDTextComponent;
import dev.anvilcraft.resource.ageratum.client.util.ViewportCameraRig;
import dev.anvilcraft.resource.ageratum.client.util.level.SandboxRenderLevel;
import dev.anvilcraft.resource.ageratum.client.util.level.StructurePreviewRenderer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.Property;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import javax.annotation.Nullable;

/**
 * 方块渲染组件。
 *
 * <p>外观与 item 组件保持一致，使用槽位背景 + 16x16 方块预览。</p>
 */
public class MDBlockComponent extends MDImageComponent {
    private static final int SLOT_SIZE = 32;

    private final Identifier blockLoc;
    private final Map<String, String> stateProps;
    private final boolean showText;
    private final ViewportCameraRig cameraRig = new ViewportCameraRig();
    private @Nullable BlockState blockState;
        /**
     * 当前悬停的方块关联文档位置，用于 W 键跳转。
     */
    private @Nullable Identifier hoveredDocLink;
private @Nullable SandboxRenderLevel sandboxRenderLevel;

    public MDBlockComponent(Identifier blockLoc, Map<String, String> stateProps, boolean showText) {
        super(MDItemComponent.SLOT_COMPONENT_TEXTURE, false, true);
        this.blockLoc = blockLoc;
        this.stateProps = stateProps;
        this.showText = showText;
    }

    private @Nullable SandboxRenderLevel getSandboxRenderLevel(BlockState state) {
        if (this.sandboxRenderLevel != null) return this.sandboxRenderLevel;
        if (Minecraft.getInstance().level == null) return null;
        this.sandboxRenderLevel = new SandboxRenderLevel();
        this.sandboxRenderLevel.setBlock(BlockPos.ZERO, state, Block.UPDATE_ALL);
        return this.sandboxRenderLevel;
    }

    @Override
    protected void renderContent(MDRenderContext context, Size size, float mouseX, float mouseY) {
        GuiGraphicsExtractor graphics = context.graphics();
        this.innerBlit(graphics, this.getImageLocation(), SLOT_SIZE, SLOT_SIZE, size.width(), size.height());
        this.renderBlock(context, mouseX, mouseY);
    }

    private void renderBlock(MDRenderContext context, float mouseX, float mouseY) {
        BlockState state = this.getBlockState();
        if (state == null) return;

        
        this.hoveredDocLink = null;  // 每帧重置
GuiGraphicsExtractor graphics = context.graphics();
        Font font = context.minecraft().font;

        SandboxRenderLevel level = this.getSandboxRenderLevel(state);
        if (level != null) {
            this.cameraRig.configureViewport(context.screenWidth(), context.screenHeight());
            this.cameraRig.setOffsetY(context.screenHeight() / 2.0f - context.offsetY() - context.topPos() - 24.25f);
            this.cameraRig.setOffsetX(-context.screenWidth() / 2.0f + context.leftPos() + context.offsetX() + context.maxX() / 2.0f);
            StructurePreviewRenderer.getInstance().render(level, this.cameraRig);
        }

        ItemStack tooltipStack = state.getBlock().asItem().getDefaultInstance();
        if (!tooltipStack.isEmpty()) {
            this.renderBlockItem(context, tooltipStack, 8, 8, mouseX, mouseY);
        }

        
        if (this.showText) {
            Component hoverName = state.getBlock().getName();
            int width = font.width(hoverName);
            graphics.drawString(font, hoverName, 16 - width / 2, 32, 0x00000000, false);
        }
    }

    /**
     * 渲染方块物品 tooltip，并检查文档绑定添加 W 键提示。
     */
    private void renderBlockItem(MDRenderContext context, ItemStack stack, int startX, int startY, float mouseX, float mouseY) {
        if (this.isHoverItem(startX, startY, mouseX, mouseY)) {
            context.addTooltip(stack);
            Minecraft minecraft = context.minecraft();
            String languageCode = AgeratumClient.getClientLanguageCode(minecraft);
            GuideDocumentCache.getFirstDocumentByItemStack(stack, languageCode).ifPresentOrElse(
                doc -> {
                    this.hoveredDocLink = doc;
                    context.addTooltip(Component.translatable(
                        "tooltip.ageratum.bind_item_hold",
                        Component.keybind("key.ageratum.more_info")
                    ));
                },
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
            AgeratumClient.openGuideOnClient(this.hoveredDocLink, List.of());
            return true;
        }
        return false;
    }


    protected @Nullable BlockState getBlockState() {
        if (this.blockState != null) return this.blockState;

        ClientLevel level = Minecraft.getInstance().level;
        if (level == null) return null;

        Optional<HolderLookup.RegistryLookup<Block>> lookup = level.registryAccess().lookup(Registries.BLOCK);
        if (lookup.isEmpty()) return null;

        Optional<Holder.Reference<Block>> blockReference = lookup.get().get(ResourceKey.create(Registries.BLOCK, this.blockLoc));
        if (blockReference.isEmpty()) return null;

        Block block = blockReference.get().value();
        this.blockState = resolveBlockState(block, this.stateProps);
        return this.blockState;
    }

    private static BlockState resolveBlockState(Block block, Map<String, String> stateProps) {
        BlockState state = block.defaultBlockState();
        if (stateProps.isEmpty()) return state;
        for (Property<?> property : state.getProperties()) {
            String rawValue = stateProps.get(property.getName());
            if (rawValue != null) {
                state = applyProperty(state, property, rawValue);
            }
        }
        return state;
    }

    private static <T extends Comparable<T>> BlockState applyProperty(
        BlockState state, Property<T> property, String rawValue
    ) {
        return property.getValue(rawValue)
            .map(v -> state.setValue(property, v))
            .orElse(state);
    }

    @Override
    public int getPreferredWidth(Minecraft minecraft, int maxX, int maxY) {
        int textWidth = this.showText ? this.blockState != null ? minecraft.font.width(this.blockState.getBlock().getName()) : 0 : 0;
        return Math.max(32, textWidth);
    }

    @Override
    public int getHeight(Minecraft minecraft, int maxX, int maxY) {
        int textHeight = this.showText ? minecraft.font.lineHeight : 0;
        Size size = new Size(SLOT_SIZE, SLOT_SIZE + textHeight, 1.0f);
        return this.computeRenderSize(size, maxX, maxY).height();
    }

    private static Map<String, String> parseStateProps(@Nullable String rawState) {
        Map<String, String> props = new HashMap<>();
        if (rawState == null || rawState.isBlank()) return props;
        for (String entry : rawState.split(",")) {
            String trimmed = entry.trim();
            int eq = trimmed.indexOf('=');
            if (eq > 0) {
                props.put(trimmed.substring(0, eq).trim(), trimmed.substring(eq + 1).trim());
            }
        }
        return props;
    }

    public static MDComponent parse(MDExtensionContext context) {
        String rawId = context.params().get("id");
        if (rawId == null || rawId.isBlank()) {
            return new MDTextComponent("[错误：block 需要 id 参数]");
        }

        Identifier id;
        try {
            id = Identifier.parse(rawId);
        } catch (Exception e) {
            return new MDTextComponent("[错误：block 的 id 参数格式无效]");
        }

        Map<String, String> stateProps = parseStateProps(context.params().get("state"));
        boolean showText = Boolean.parseBoolean(context.params().getOrDefault("showText", "true"));
        return new MDBlockComponent(id, stateProps, showText);
    }
}
