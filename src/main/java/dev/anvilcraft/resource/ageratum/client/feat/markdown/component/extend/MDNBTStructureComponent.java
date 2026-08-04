package dev.anvilcraft.resource.ageratum.client.feat.markdown.component.extend;

import com.mojang.brigadier.StringReader;
import dev.anvilcraft.resource.ageratum.client.AgeratumClient;
import dev.anvilcraft.resource.ageratum.client.constants.AgeratumConstants;
import dev.anvilcraft.resource.ageratum.client.feat.markdown.MDExtensionContext;
import dev.anvilcraft.resource.ageratum.client.feat.markdown.MDRenderContext;
import dev.anvilcraft.resource.ageratum.client.feat.markdown.component.MDComponent;
import dev.anvilcraft.resource.ageratum.client.feat.markdown.component.MDTextComponent;
import dev.anvilcraft.resource.ageratum.client.feat.structure.StructureProjectionApi;
import dev.anvilcraft.resource.ageratum.client.util.RelativePathResolver;
import dev.anvilcraft.resource.ageratum.client.util.ViewportCameraRig;
import dev.anvilcraft.resource.ageratum.client.util.level.SandboxRenderLevel;
import dev.anvilcraft.resource.ageratum.client.util.level.StructurePreviewRenderer;
import dev.anvilcraft.resource.ageratum.client.util.level.StructureSandboxFactory;
import lombok.extern.slf4j.Slf4j;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.Vec3i;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtAccounter;
import net.minecraft.nbt.NbtIo;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.nbt.TagParser;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.util.Mth;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import net.minecraft.world.phys.BlockHitResult;
import org.lwjgl.glfw.GLFW;

import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.annotation.Nullable;

/**
 * NBT 结构文件渲染组件。
 *
 * <p>该组件由扩展标签 {@code <structure id="namespace:path"/>} 创建，
 * 用于在文档中渲染结构文件摘要与 NBT 树状视图。</p>
 */
@Slf4j
public final class MDNBTStructureComponent extends MDComponent {
    private static final int MAX_SNBT_BYTES = 8 * 1024 * 1024;
    private static final float MIN_ZOOM = AgeratumConstants.Structure.Camera.MIN_ZOOM;
    private static final float MAX_ZOOM = AgeratumConstants.Structure.Camera.MAX_ZOOM;
    private static final float ROTATE_YAW_SENSITIVITY = AgeratumConstants.Structure.Sensitivity.ROTATE_YAW;
    private static final float ROTATE_PITCH_SENSITIVITY = AgeratumConstants.Structure.Sensitivity.ROTATE_PITCH;
    private static final float PAN_SENSITIVITY = AgeratumConstants.Structure.Sensitivity.PAN;
    private static final ResourceLocation BUTTON_PROJECTION_LOCATION = AgeratumConstants.Structure.Textures.BUTTON_PROJECTION;

    private final StructureTarget target;
    private final ViewportCameraRig cameraRig = new ViewportCameraRig();
    private @Nullable SandboxRenderLevel previewLevel = null;
    private @Nullable StructureTemplate structureTemplateCache = null;
    private float panOffsetX;
    private float panOffsetY;
    private int dragButton = AgeratumConstants.GuideScreenUI.Positions.INVALID_BUTTON;
    private int visibleMinY;
    private int totalLayerCount = 1;
    private int visibleLayerCount = 1;
    private boolean layerPreviewInitialized;
    private int contentHeight = 220;
    private int bottomHeight = 220;

    private MDNBTStructureComponent(StructureTarget target) {
        super("[结构未加载]");
        this.target = target;
    }

    /**
     * 解析结构扩展标签。
     */
    public static MDComponent parse(MDExtensionContext context) {
        String rawId = context.params().getOrDefault("id", context.params().get("path"));
        if (rawId == null || rawId.isBlank()) {
            return new MDTextComponent("[错误：structure 需要 id 或 path 参数]");
        }

        try {
            StructureTarget target = StructureTarget.resolve(context.sourceLocation(), rawId);
            return new MDNBTStructureComponent(target);
        } catch (Exception exception) {
            return new MDTextComponent("[错误：无法解析结构组件参数 - " + exception.getMessage() + "]");
        }
    }

    @Override
    public void render(MDRenderContext context) {
        Minecraft minecraft = context.minecraft();
        int maxX = context.maxX();
        GuiGraphics graphics = context.graphics();
        if (this.previewLevel == null) {
            this.previewLevel = this.prepare(minecraft.level, this.target);
            this.resetLayerPreview();
        }
        if (this.previewLevel == null) {
            super.render(context.child());
            return;
        }

        this.ensureLayerPreviewInitialized();

        int height = this.getHeight(minecraft, maxX, context.maxY()); // 确保 scale 计算正确
        graphics.renderOutline(0, 0, maxX, height, 0xAA000000);
        graphics.fill(0, 0, maxX, height, 0x55000000);
        context.enableScissor(1, 1, maxX - 1, height - 1);
        this.cameraRig.configureViewport(context.screenWidth(), context.screenHeight());
        this.cameraRig.setZoom(2.0f);
        // 将结构投影居中到组件分配区域的中心（考虑 offsetX + maxX 与屏幕中心的偏移）
        this.cameraRig.setOffsetX(context.offsetX() + maxX / 2.0f - context.screenWidth() / 2.0f + this.panOffsetX);
        this.cameraRig.setOffsetY(context.screenHeight() / 2.0f - this.contentHeight + this.bottomHeight / 2.0f - context.offsetY() + this.panOffsetY);
        StructurePreviewRenderer.getInstance()
            .render(
                this.previewLevel,
                this.cameraRig,
                graphics.bufferSource(),
                this.visibleMinY,
                this.visibleMinY + this.visibleLayerCount
            );
        this.renderLayerIndicator(context, graphics);
        this.renderButton(context);
        context.disableScissor();
    }

    private boolean isHoverProjectionButton(int maxX, float mouseX, float mouseY) {
        return isHover(maxX - 21, 5, 16, 16, mouseX, mouseY);
    }

    /**
     * 计算层数指示器区域中 [+] 按钮的 X 坐标。
     */
    private int getLayerUpButtonX(MDRenderContext context) {
        int padding = AgeratumConstants.GuideScreenUI.Positions.LAYER_INDICATOR_PADDING;
        String layerLabel = "层数: " + this.visibleLayerCount + "/" + this.totalLayerCount;
        int labelWidth = context.minecraft().font.width(layerLabel);
        int btnSize = context.minecraft().font.lineHeight + padding;
        int btnGap = 2;
        return 4 + padding + labelWidth + padding + btnGap;
    }

    private int getLayerButtonSize(MDRenderContext context) {
        return context.minecraft().font.lineHeight + AgeratumConstants.GuideScreenUI.Positions.LAYER_INDICATOR_PADDING;
    }

    private void renderButton(MDRenderContext context) {
        GuiGraphics graphics = context.graphics();
        boolean isHover = isHoverProjectionButton(context.maxX(), context.mouseX(), context.mouseY());
        graphics.blit(
            BUTTON_PROJECTION_LOCATION,
            context.maxX() - AgeratumConstants.GuideScreenUI.Positions.STRUCTURE_BUTTON_RIGHT_MARGIN,
            AgeratumConstants.GuideScreenUI.Positions.STRUCTURE_BUTTON_TOP_MARGIN,
            0,
            0,
            isHover ? AgeratumConstants.GuideScreenUI.Positions.STRUCTURE_BUTTON_HEIGHT : 0,
            AgeratumConstants.GuideScreenUI.Positions.STRUCTURE_BUTTON_WIDTH,
            AgeratumConstants.GuideScreenUI.Positions.STRUCTURE_BUTTON_HEIGHT,
            AgeratumConstants.GuideScreenUI.Positions.STRUCTURE_BUTTON_WIDTH,
            32
        );
        if (isHover) {
            context.addTooltip(List.of(
                Component.translatable(
                    "tooltip.ageratum.structure_projection.layer_shortcut",
                    Component.keybind("key.ageratum.structure_projection.layer_up"),
                    Component.keybind("key.ageratum.structure_projection.layer_down")
                ),
                Component.translatable(
                    "tooltip.ageratum.structure_projection.remove_shortcut",
                    Component.keybind("key.ageratum.structure_projection.remove")
                )
            ));
        }
    }

    @Override
    public boolean keyPressed(Minecraft minecraft, double mouseX, double mouseY, int keyCode, int scanCode, int modifiers, int maxX) {
        if (this.previewLevel == null) {
            return false;
        }

        this.ensureLayerPreviewInitialized();
        if (keyCode == GLFW.GLFW_KEY_PAGE_DOWN) {
            this.visibleLayerCount = Math.max(1, this.visibleLayerCount - 1);
            return true;
        }

        if (keyCode == GLFW.GLFW_KEY_PAGE_UP) {
            this.visibleLayerCount = Math.min(this.totalLayerCount, this.visibleLayerCount + 1);
            return true;
        }

        return false;
    }

    @Override
    public boolean blocksParentKeyHandling(
        Minecraft minecraft,
        double mouseX,
        double mouseY,
        int keyCode,
        int scanCode,
        int modifiers,
        int maxX
    ) {
        return this.previewLevel != null && (keyCode == GLFW.GLFW_KEY_PAGE_UP || keyCode == GLFW.GLFW_KEY_PAGE_DOWN);
    }

    @Override
    public boolean mouseScrolled(Minecraft minecraft, double mouseX, double mouseY, double scrollY, int maxX) {
        if (!Screen.hasControlDown() || scrollY == 0.0d) {
            return false;
        }

        float nextZoom = this.cameraRig.getZoom() * (float) Math.pow(1.1d, scrollY);
        this.cameraRig.setZoom(clamp(nextZoom, MIN_ZOOM, MAX_ZOOM));
        return true;
    }

    @Override
    public boolean mouseClicked(Minecraft minecraft, double mouseX, double mouseY, int button, int maxX) {
        if (button != 0 && button != 1) {
            return false;
        }
        // 层数调节按钮
        if (button == 0 && this.previewLevel != null) {
            int padding = AgeratumConstants.GuideScreenUI.Positions.LAYER_INDICATOR_PADDING;
            String layerLabel = "层数: " + this.visibleLayerCount + "/" + this.totalLayerCount;
            int labelWidth = minecraft.font.width(layerLabel);
            int btnSize = minecraft.font.lineHeight + padding;
            int btnGap = 2;
            int btnUpX = 4 + padding + labelWidth + padding + btnGap;
            int btnDownX = btnUpX + btnSize + btnGap;
            if (isHover(btnUpX, 4, btnSize, btnSize, (float) mouseX, (float) mouseY)) {
                this.ensureLayerPreviewInitialized();
                this.visibleLayerCount = Math.min(this.totalLayerCount, this.visibleLayerCount + 1);
                return true;
            }
            if (isHover(btnDownX, 4, btnSize, btnSize, (float) mouseX, (float) mouseY)) {
                this.ensureLayerPreviewInitialized();
                this.visibleLayerCount = Math.max(1, this.visibleLayerCount - 1);
                return true;
            }
        }
        if (this.isHoverProjectionButton(maxX, (float) mouseX, (float) mouseY)) {
            if (this.structureTemplateCache != null && minecraft.cameraEntity != null) {
                BlockPos blockPos;
                if (minecraft.hitResult instanceof BlockHitResult hitResult) {
                    blockPos = hitResult.getBlockPos().relative(hitResult.getDirection());
                } else {
                    blockPos = minecraft.cameraEntity.getOnPos().above();
                }
                StructureProjectionApi.showFloating(this.structureTemplateCache, blockPos);
                minecraft.setScreen(null);
            }
            return true;
        }
        this.dragButton = button;
        return true;
    }

    @Override
    public boolean mouseDragged(Minecraft minecraft, double mouseX, double mouseY, int button, double dragX, double dragY, int maxX) {
        if (button != this.dragButton) {
            return false;
        }

        if (button == 0) {
            this.panOffsetX += (float) (dragX * PAN_SENSITIVITY);
            this.panOffsetY -= (float) (dragY * PAN_SENSITIVITY);
            return true;
        }

        if (button == 1) {
            this.cameraRig.setRotationY(this.cameraRig.getRotationY() + (float) (dragX * ROTATE_YAW_SENSITIVITY));
            float nextPitch = this.cameraRig.getRotationX() - (float) (dragY * ROTATE_PITCH_SENSITIVITY);
            this.cameraRig.setRotationX(clamp(nextPitch, -89.9f, 89.9f));
            return true;
        }

        return false;
    }

    @Override
    public boolean mouseReleased(Minecraft minecraft, double mouseX, double mouseY, int button, int maxX) {
        if (button != this.dragButton) {
            return false;
        }
        this.dragButton = AgeratumConstants.GuideScreenUI.Positions.INVALID_BUTTON;
        return true;
    }

    private void ensureLayerPreviewInitialized() {
        if (this.previewLevel == null) {
            return;
        }

        var bounds = this.previewLevel.getBounds();
        int minY = bounds.min().getY();
        int maxYExclusive = Math.max(minY + 1, bounds.max().getY());
        int fullLayerCount = Math.max(1, maxYExclusive - minY);

        if (!this.layerPreviewInitialized || this.visibleMinY != minY || this.totalLayerCount != fullLayerCount) {
            this.visibleMinY = minY;
            this.totalLayerCount = fullLayerCount;
            if (!this.layerPreviewInitialized) {
                this.visibleLayerCount = this.totalLayerCount;
                this.layerPreviewInitialized = true;
            } else {
                this.visibleLayerCount = Mth.clamp(this.visibleLayerCount, 1, this.totalLayerCount);
            }
        }
    }

    private void resetLayerPreview() {
        this.visibleMinY = 0;
        this.totalLayerCount = 1;
        this.visibleLayerCount = 1;
        this.layerPreviewInitialized = false;
    }

    private void renderLayerIndicator(MDRenderContext context, GuiGraphics graphics) {
        int padding = AgeratumConstants.GuideScreenUI.Positions.LAYER_INDICATOR_PADDING;
        String layerLabel = "层数: " + this.visibleLayerCount + "/" + this.totalLayerCount;
        int fontHeight = context.minecraft().font.lineHeight;
        int labelWidth = context.minecraft().font.width(layerLabel);
        int btnSize = fontHeight + padding;
        int btnGap = 2;
        int totalWidth = padding + labelWidth + padding + btnGap + btnSize + btnGap + btnSize + padding;
        int totalHeight = fontHeight + padding * 2;
        int startX = 4;
        int startY = 4;

        // 背景
        graphics.fill(startX, startY, startX + totalWidth, startY + totalHeight, AgeratumConstants.GuideScreenUI.Colors.LAYER_INDICATOR_BG);
        // 层数文本
        graphics.drawString(
            context.minecraft().font,
            layerLabel,
            startX + padding,
            startY + padding,
            AgeratumConstants.GuideScreenUI.Colors.LAYER_INDICATOR_TEXT,
            false
        );

        // [+] 按钮
        int btnUpX = startX + padding + labelWidth + padding + btnGap;
        int btnDownX = btnUpX + btnSize + btnGap;
        float mouseX = context.mouseX();
        float mouseY = context.mouseY();
        boolean hoverUp = isHover(btnUpX, startY, btnSize, btnSize, mouseX, mouseY);
        boolean hoverDown = isHover(btnDownX, startY, btnSize, btnSize, mouseX, mouseY);

        int btnBgUp = hoverUp ? 0x88AAAAAA : 0x88444444;
        int btnBgDown = hoverDown ? 0x88AAAAAA : 0x88444444;
        graphics.fill(btnUpX, startY, btnUpX + btnSize, startY + btnSize, btnBgUp);
        graphics.fill(btnDownX, startY, btnDownX + btnSize, startY + btnSize, btnBgDown);
        graphics.drawString(context.minecraft().font, "+", btnUpX + 3, startY + 1, 0xFFFFFFFF, false);
        graphics.drawString(context.minecraft().font, "-", btnDownX + 3, startY + 1, 0xFFFFFFFF, false);

        // tooltip
        if (hoverUp || hoverDown) {
            context.addTooltip(Component.literal("快捷键: PageUp/PageDown"));
        }
    }

    private static float clamp(float value, float min, float max) {
        return Mth.clamp(value, min, max);
    }

    public int scale(int maxX, int value) {
        float scale = AgeratumConstants.Structure.Render.SCREEN_WIDTH_SCALE / maxX;
        return Math.round(value * scale);
    }

    @Override
    public int getHeight(Minecraft minecraft, int maxX, int maxY) {
        return this.contentHeight;
    }

    /**
     * 加载 NBT 结构模板并将其放入沙盒关卡，供后续渲染使用。
     */
    private @Nullable SandboxRenderLevel prepare(@Nullable Level clientLevel, StructureTarget target) {
        if (clientLevel == null) return null;
        try {
            StructureTemplate template = new StructureTemplate();
            HolderLookup.RegistryLookup<Block> blocks = clientLevel.registryAccess().registryOrThrow(Registries.BLOCK).asLookup();
            CompoundTag root = readStructureRoot(target);
            if (root == null) {
                return null;
            }
            root = normalizeStructureRoot(root);
            template.load(blocks, root);
            Vec3i size = template.getSize();
            BlockPos pos = StructureSandboxFactory.centeredPlacement(template);
            this.contentHeight = (int) (AgeratumConstants.Structure.Render.CONTENT_HEIGHT_FACTOR * Math.sqrt(BlockPos.ZERO.distSqr(size)));
            this.bottomHeight = (int) (AgeratumConstants.Structure.Render.BOTTOM_HEIGHT_FACTOR * Math.sqrt(BlockPos.ZERO.distSqr(pos)));
            this.structureTemplateCache = template;
            return StructureSandboxFactory.create(clientLevel, template, pos);
        } catch (Exception exception) {
            log.warn("Failed to load structure preview from '{}'", target.displayPath(), exception);
            return null;
        }
    }

    private static @Nullable CompoundTag readStructureRoot(StructureTarget target) {
        ParseMode preferredMode;
        try (InputStream stream = MDNBTStructureComponent.openStructureStream(target)) {
            if (stream == null) {
                return null;
            }
            preferredMode = detectParseMode(stream);
        } catch (IOException exception) {
            log.warn("Failed to open structure file '{}'", target.displayPath(), exception);
            return null;
        }

        CompoundTag parsed = parseStructureRoot(target, preferredMode);
        if (parsed != null) {
            return parsed;
        }

        ParseMode fallbackMode = preferredMode == ParseMode.COMPRESSED_NBT ? ParseMode.SNBT : ParseMode.COMPRESSED_NBT;
        parsed = parseStructureRoot(target, fallbackMode);
        if (parsed != null) {
            log.warn(
                "Structure file '{}' failed {} parsing and was loaded as {}",
                target.displayPath(),
                preferredMode.description,
                fallbackMode.description
            );
            return parsed;
        }

        log.warn(
            "Failed to parse structure file '{}' as {} or {}",
            target.displayPath(),
            preferredMode.description,
            fallbackMode.description
        );
        return null;
    }

    private static @Nullable CompoundTag parseStructureRoot(StructureTarget target, ParseMode mode) {
        try (InputStream stream = MDNBTStructureComponent.openStructureStream(target)) {
            if (stream == null) {
                return null;
            }
            return switch (mode) {
                case COMPRESSED_NBT -> NbtIo.readCompressed(stream, NbtAccounter.unlimitedHeap());
                case SNBT -> readSnbtRoot(stream);
            };
        } catch (Exception exception) {
            log.debug("Failed to parse structure '{}' as {}", target.displayPath(), mode.description, exception);
            return null;
        }
    }

    private static CompoundTag readSnbtRoot(InputStream stream) throws Exception {
        String snbt = readUtf8WithLimit(stream, MAX_SNBT_BYTES);
        if (!snbt.isEmpty() && snbt.charAt(0) == '\ufeff') {
            snbt = snbt.substring(1);
        }
        return new TagParser(new StringReader(snbt)).readStruct();
    }

    private static ParseMode detectParseMode(InputStream stream) throws IOException {
        BufferedInputStream buffered = stream instanceof BufferedInputStream b ? b : new BufferedInputStream(stream);
        buffered.mark(2);
        int first = buffered.read();
        int second = buffered.read();
        buffered.reset();
        return first == 0x1f && second == 0x8b ? ParseMode.COMPRESSED_NBT : ParseMode.SNBT;
    }

    private static String readUtf8WithLimit(InputStream stream, int maxBytes) throws IOException {
        ByteArrayOutputStream output = new ByteArrayOutputStream(Math.min(maxBytes, 8192));
        byte[] buffer = new byte[8192];
        int total = 0;
        int read;
        while ((read = stream.read(buffer)) != -1) {
            total += read;
            if (total > maxBytes) {
                throw new IOException("SNBT payload exceeds " + maxBytes + " bytes limit");
            }
            output.write(buffer, 0, read);
        }
        return output.toString(StandardCharsets.UTF_8);
    }

    private enum ParseMode {
        COMPRESSED_NBT("compressed NBT"), SNBT("SNBT");

        private final String description;

        ParseMode(String description) {
            this.description = description;
        }
    }

    /**
     * Normalize SNBT variants into the vanilla StructureTemplate NBT format.
     *
     * <p>In addition to vanilla structure NBT (compressed or SNBT), we also support a simplified SNBT format:
     * <pre>
     * {
     *   size: [x, y, z],
     *   palette: ["minecraft:stone", "minecraft:barrier{waterlogged:false}"],
     *   data: [{pos:[0,0,0], state:"minecraft:stone"}, ...]
     * }
     * </pre>
     * This method converts it to {@code palette: [{Name:"...", Properties:{...}}, ...]} and
     * {@code blocks: [{pos:[...], state:<index>}, ...]}.
     */
    private static CompoundTag normalizeStructureRoot(CompoundTag root) {
        Tag paletteTag = root.get("palette");
        boolean paletteIsStringList = paletteTag instanceof ListTag list && list.getElementType() == Tag.TAG_STRING;
        Tag dataTag = root.get("data");
        boolean hasSimplifiedData = dataTag instanceof ListTag list && list.getElementType() == Tag.TAG_COMPOUND;

        if (!paletteIsStringList && !hasSimplifiedData) {
            return root;
        }

        CompoundTag converted = root.copy();

        // Build palette entries in a deterministic order.
        List<String> paletteStates = new ArrayList<>();
        Set<String> seenPaletteStates = new HashSet<>();
        if (paletteIsStringList) {
            ListTag paletteStrings = (ListTag) converted.get("palette");
            for (int i = 0; i < paletteStrings.size(); i++) {
                String state = paletteStrings.getString(i);
                if (!state.isBlank() && seenPaletteStates.add(state)) {
                    paletteStates.add(state);
                }
            }
        }

        ListTag dataList = hasSimplifiedData ? (ListTag) converted.get("data") : null;
        if (paletteStates.isEmpty() && dataList != null) {
            for (int i = 0; i < dataList.size(); i++) {
                CompoundTag entry = dataList.getCompound(i);
                String state = entry.getString("state");
                if (!state.isBlank() && seenPaletteStates.add(state)) {
                    paletteStates.add(state);
                }
            }
        }

        ListTag palette = new ListTag();
        Map<String, Integer> paletteIndex = new LinkedHashMap<>();
        for (String state : paletteStates) {
            paletteIndex.put(state, palette.size());
            palette.add(toStructurePaletteEntry(state));
        }
        converted.put("palette", palette);

        if (dataList != null && !converted.contains("blocks")) {
            ListTag blocks = new ListTag();
            for (int i = 0; i < dataList.size(); i++) {
                CompoundTag entry = dataList.getCompound(i);
                String state = entry.getString("state");

                Integer index = paletteIndex.get(state);
                if (index == null) {
                    index = palette.size();
                    paletteIndex.put(state, index);
                    palette.add(toStructurePaletteEntry(state));
                }

                CompoundTag block = new CompoundTag();
                Tag pos = entry.get("pos");
                if (pos != null) {
                    block.put("pos", pos.copy());
                }
                block.putInt("state", index);

                Tag nbt = entry.get("nbt");
                if (nbt != null) {
                    block.put("nbt", nbt.copy());
                }
                blocks.add(block);
            }
            converted.remove("data");
            converted.put("blocks", blocks);
        }

        return converted;
    }

    private static CompoundTag toStructurePaletteEntry(String stateString) {
        String raw = stateString.trim();
        String name = raw;
        String props = "";

        int braceIndex = raw.indexOf('{');
        if (braceIndex >= 0 && raw.endsWith("}")) {
            name = raw.substring(0, braceIndex).trim();
            props = raw.substring(braceIndex + 1, raw.length() - 1).trim();
        }

        CompoundTag entry = new CompoundTag();
        entry.putString("Name", name);

        if (!props.isEmpty()) {
            CompoundTag properties = new CompoundTag();
            String[] pairs = props.split(",");
            for (String pair : pairs) {
                int colon = pair.indexOf(':');
                if (colon < 0) {
                    continue;
                }
                String key = pair.substring(0, colon).trim();
                String value = pair.substring(colon + 1).trim();
                if (!key.isEmpty() && !value.isEmpty()) {
                    properties.put(key, StringTag.valueOf(value));
                }
            }
            if (!properties.isEmpty()) {
                entry.put("Properties", properties);
            }
        }
        return entry;
    }

    /**
     * 按优先级打开结构输入流：先尝试 preview 工作区，再尝试资源管理器，
     * 最后回退到 classpath 路径。
     */
    private static @Nullable InputStream openStructureStream(StructureTarget target) throws IOException {
        if (AgeratumClient.isPreviewLocation(target.location())) {
            for (String candidate : target.previewCandidatePaths()) {
                Path previewPath = AgeratumClient.resolvePreviewAssetPath(candidate);
                if (Files.isRegularFile(previewPath)) {
                    return Files.newInputStream(previewPath);
                }
            }
        }

        for (ResourceLocation candidate : candidateResourceLocations(target.location())) {
            Resource directResource = Minecraft.getInstance().getResourceManager().getResource(candidate).orElse(null);
            if (directResource != null) {
                return directResource.open();
            }
        }

        for (String candidate : candidateResourcePaths(target.location())) {
            InputStream stream = MDNBTStructureComponent.class.getClassLoader().getResourceAsStream(candidate);
            if (stream != null) {
                return stream;
            }
        }
        return null;
    }

    private static List<ResourceLocation> candidateResourceLocations(ResourceLocation location) {
        String path = location.getPath();
        if (endsWithStructureExtension(path)) {
            return List.of(location);
        }
        return List.of(
            location,
            ResourceLocation.fromNamespaceAndPath(location.getNamespace(), path + ".nbt"),
            ResourceLocation.fromNamespaceAndPath(location.getNamespace(), path + ".snbt")
        );
    }

    /**
     * 生成结构文件在 classpath 中的回退搜索路径。
     */
    private static List<String> candidateResourcePaths(ResourceLocation location) {
        String normalizedPath = normalizeStructurePath(location.getPath());
        return List.of(
            "data/" + location.getNamespace() + "/structure/" + normalizedPath + ".nbt",
            "data/" + location.getNamespace() + "/structures/" + normalizedPath + ".nbt",
            "data/" + location.getNamespace() + "/structure/" + normalizedPath + ".snbt",
            "data/" + location.getNamespace() + "/structures/" + normalizedPath + ".snbt"
        );
    }

    private static String normalizeStructurePath(String path) {
        String normalized = path.replace('\\', '/');
        while (normalized.startsWith("/")) {
            normalized = normalized.substring(1);
        }
        if (endsWithStructureExtension(normalized)) {
            int dotIndex = normalized.lastIndexOf('.');
            normalized = dotIndex >= 0 ? normalized.substring(0, dotIndex) : normalized;
        }
        return normalized;
    }

    private static boolean endsWithStructureExtension(String path) {
        return path.endsWith(".nbt") || path.endsWith(".snbt");
    }

    private static List<String> expandStructureExtensions(String path) {
        String normalized = path.replace('\\', '/');
        return endsWithStructureExtension(normalized) ? List.of(normalized) : List.of(normalized + ".nbt", normalized + ".snbt");
    }

    private static String getCurrentDirectoryPath(ResourceLocation location) {
        String currentFile = location.getPath();
        int slash = currentFile.lastIndexOf('/');
        if (slash < 0) {
            return "";
        }
        return currentFile.substring(0, slash);
    }

    public record StructureTarget(ResourceLocation location, String displayPath, List<String> previewCandidatePaths) {
        /**
         * 基于 markdown 源文档位置解析显式或相对的结构引用。
         */
        public static StructureTarget resolve(ResourceLocation sourceLocation, String rawTarget) {
            String trimmed = rawTarget.trim();
            if (trimmed.contains(":")) {
                ResourceLocation location = ResourceLocation.parse(trimmed);
                List<String> previewPaths = AgeratumClient.isPreviewLocation(location)
                                            ? expandStructureExtensions(RelativePathResolver.resolveWithinBase("", location.getPath()))
                                            : List.of();
                return new StructureTarget(location, trimmed, previewPaths);
            }

            String resolvedPath = RelativePathResolver.resolveWithinBase(getCurrentDirectoryPath(sourceLocation), trimmed);
            ResourceLocation location = ResourceLocation.fromNamespaceAndPath(sourceLocation.getNamespace(), resolvedPath);
            List<String> previewPaths = AgeratumClient.isPreviewLocation(sourceLocation)
                                        ? expandStructureExtensions(resolvedPath)
                                        : List.of();
            return new StructureTarget(location, trimmed, previewPaths);
        }
    }
}

