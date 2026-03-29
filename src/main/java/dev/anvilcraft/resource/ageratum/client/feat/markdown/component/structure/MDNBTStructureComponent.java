package dev.anvilcraft.resource.ageratum.client.feat.markdown.component.structure;

import com.mojang.blaze3d.vertex.PoseStack;
import dev.anvilcraft.resource.ageratum.client.AgeratumClient;
import dev.anvilcraft.resource.ageratum.client.feat.markdown.MDExtensionContext;
import dev.anvilcraft.resource.ageratum.client.feat.markdown.MDRenderContext;
import dev.anvilcraft.resource.ageratum.client.feat.markdown.component.MDBlockComponent;
import dev.anvilcraft.resource.ageratum.client.feat.markdown.component.MDComponent;
import dev.anvilcraft.resource.ageratum.client.feat.markdown.component.MDTextComponent;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.ByteArrayTag;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.IntArrayTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.LongArrayTag;
import net.minecraft.nbt.NbtAccounter;
import net.minecraft.nbt.NbtIo;
import net.minecraft.nbt.NumericTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.FormattedText;
import net.minecraft.network.chat.Style;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;

import java.awt.Color;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import javax.annotation.Nullable;

/**
 * NBT 结构文件渲染组件。
 *
 * <p>该组件由扩展标签 {@code <structure id="namespace:path"/>} 创建，
 * 用于在文档中渲染结构文件摘要与 NBT 树状视图。</p>
 */
public final class MDNBTStructureComponent extends MDComponent {
    private static final int INDENT_WIDTH = 10;
    private static final int TEXT_PADDING = 6;
    private static final int DEFAULT_MAX_DEPTH = 2;
    private static final int DEFAULT_MAX_ENTRIES = 12;
    private static final int SECTION_SPACING = 6;
    private static final int PREVIEW_PANEL_PADDING = 6;
    private static final int PREVIEW_PANEL_BORDER = 0x88333333;
    private static final int PREVIEW_PANEL_BACKGROUND = 0x11A0A0A0;
    private static final int PREVIEW_CELL_BACKGROUND_A = 0x11D0D0D0;
    private static final int PREVIEW_CELL_BACKGROUND_B = 0x11888888;
    private static final int PREVIEW_CELL_BORDER = 0x22000000;
    private static final int PREVIEW_LABEL_COLOR = 0x334155;
    private static final int PREVIEW_MAX_GRID_PIXELS = 128;
    private static final int PREVIEW_MAX_CELL_SIZE = 16;
    private static final int PREVIEW_MIN_ICON_CELL_SIZE = 7;
    private static final int TITLE_COLOR = 0x1F2937;
    private static final int SUMMARY_COLOR = 0x334155;
    private static final int NUMBER_COLOR = 0xB45309;
    private static final int STRING_COLOR = 0x15803D;
    private static final int CONTAINER_COLOR = 0x0F766E;
    private static final int ARRAY_COLOR = 0x7C3AED;
    private static final int NULL_COLOR = 0x6B7280;
    private static final int ERROR_COLOR = 0xB91C1C;
    private static final int MAX_TEXT_PREVIEW_LENGTH = 80;

    private final StructureTreeComponent summaryComponent;
    private final StructureTreeComponent detailComponent;
    private final @Nullable StructurePreview preview;

    private MDNBTStructureComponent(StructureTarget target, int maxDepth, int maxEntries) {
        this(prepare(target, Math.max(1, maxDepth), Math.max(1, maxEntries)));
    }

    private MDNBTStructureComponent(PreparedData preparedData) {
        super(preparedData.componentText());
        this.summaryComponent = preparedData.summaryComponent();
        this.detailComponent = preparedData.detailComponent();
        this.preview = preparedData.preview();
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
            int maxDepth = parsePositiveInt(context, "maxDepth", DEFAULT_MAX_DEPTH);
            int maxEntries = parsePositiveInt(context, "maxEntries", DEFAULT_MAX_ENTRIES);
            return new MDNBTStructureComponent(target, maxDepth, maxEntries);
        } catch (Exception exception) {
            return new MDTextComponent("[错误：无法解析结构组件参数 - " + exception.getMessage() + "]");
        }
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
        GuiGraphics guiGraphics = context.graphics();
        PoseStack pose = guiGraphics.pose();
        int currentY = 0;

        int summaryHeight = this.summaryComponent.getHeight(minecraft, maxX, maxY);
        this.summaryComponent.render(context, minecraft, maxX, maxY, mouseX, mouseY);
        currentY += summaryHeight;
        maxY -= summaryHeight;

        int previewHeight = this.getPreviewHeight(minecraft, maxX);
        if (previewHeight > 0 && maxY > 0) {
            pose.pushPose();
            pose.translate(0, currentY + SECTION_SPACING, 0);
            this.renderPreview(context, minecraft, maxX, mouseX, mouseY - currentY - SECTION_SPACING);
            pose.popPose();
            currentY += SECTION_SPACING + previewHeight;
            maxY -= SECTION_SPACING + previewHeight;
        }

        if (this.detailComponent.getHeight(minecraft, maxX, Integer.MAX_VALUE) <= 0) {
            return;
        }

        pose.pushPose();
        pose.translate(0, currentY + SECTION_SPACING, 0);
        this.detailComponent.render(context, minecraft, maxX, maxY, mouseX, mouseY - currentY - SECTION_SPACING);
        pose.popPose();
    }

    @Override
    public int getHeight(Minecraft minecraft, int maxX, int maxY) {
        int total = this.summaryComponent.getHeight(minecraft, maxX, maxY);
        int previewHeight = this.getPreviewHeight(minecraft, maxX);
        if (previewHeight > 0) {
            total += SECTION_SPACING + previewHeight;
        }
        int detailHeight = this.detailComponent.getHeight(minecraft, maxX, maxY);
        if (detailHeight > 0) {
            total += SECTION_SPACING + detailHeight;
        }
        return total;
    }

    @Override
    public @Nullable Style getStyleAtPosition(Minecraft minecraft, double mouseX, double mouseY, int maxX) {
        int summaryHeight = this.summaryComponent.getHeight(minecraft, maxX, Integer.MAX_VALUE);
        if (mouseY >= 0 && mouseY < summaryHeight) {
            return this.summaryComponent.getStyleAtPosition(minecraft, mouseX, mouseY, maxX);
        }

        double currentY = summaryHeight;
        int previewHeight = this.getPreviewHeight(minecraft, maxX);
        if (previewHeight > 0) {
            currentY += SECTION_SPACING + previewHeight;
        }

        int detailHeight = this.detailComponent.getHeight(minecraft, maxX, Integer.MAX_VALUE);
        double detailStartY = currentY + (detailHeight > 0 ? SECTION_SPACING : 0);
        if (detailHeight > 0 && mouseY >= detailStartY) {
            return this.detailComponent.getStyleAtPosition(minecraft, mouseX, mouseY - detailStartY, maxX);
        }
        return null;
    }

    private static PreparedData prepare(StructureTarget target, int maxDepth, int maxEntries) {
        List<StructureLine> summaryLines = new ArrayList<>();
        List<StructureLine> detailLines = new ArrayList<>();
        summaryLines.add(new StructureLine(0, TITLE_COLOR, "Structure: " + target.displayPath()));
        StructurePreview preview = null;

        try (InputStream inputStream = openStructureStream(target)) {
            if (inputStream == null) {
                summaryLines.add(new StructureLine(0, ERROR_COLOR, "无法找到结构文件：" + target.displayPath()));
                return toPreparedData(summaryLines, detailLines, null);
            }

            CompoundTag root = NbtIo.readCompressed(inputStream, NbtAccounter.unlimitedHeap());
            appendStructureSummary(summaryLines, root);
            preview = buildPreview(root);
            detailLines.add(new StructureLine(0, SUMMARY_COLOR, "Root tags:"));

            List<String> keys = new ArrayList<>(root.getAllKeys());
            Collections.sort(keys);
            int limit = Math.min(keys.size(), maxEntries);
            for (int i = 0; i < limit; i++) {
                String key = keys.get(i);
                appendNamedTag(detailLines, 1, key, root.get(key), 1, maxDepth, maxEntries);
            }
            appendOverflowLine(detailLines, 1, keys.size(), limit);
        } catch (Exception exception) {
            summaryLines.add(new StructureLine(0, ERROR_COLOR, "加载失败：" + exception.getMessage()));
        }

        return toPreparedData(summaryLines, detailLines, preview);
    }

    private static PreparedData toPreparedData(
        List<StructureLine> summaryLines,
        List<StructureLine> detailLines,
        @Nullable StructurePreview preview
    ) {
        StructureTreeComponent summaryComponent = StructureTreeComponent.create(summaryLines);
        StructureTreeComponent detailComponent = StructureTreeComponent.create(detailLines);
        return new PreparedData(
            summaryComponent,
            detailComponent,
            preview,
            composeComponentText(summaryComponent.getText(), detailComponent.getText())
        );
    }

    private static void appendStructureSummary(List<StructureLine> lines, CompoundTag root) {
        if (root.contains("size", Tag.TAG_LIST)) {
            ListTag size = root.getList("size", Tag.TAG_INT);
            if (size.size() >= 3) {
                lines.add(new StructureLine(0, SUMMARY_COLOR, "Size: " + readInt(size, 0) + " x " + readInt(size, 1) + " x " + readInt(size, 2)));
            }
        }

        if (root.contains("palette", Tag.TAG_LIST)) {
            lines.add(new StructureLine(0, SUMMARY_COLOR, "Palette entries: " + root.getList("palette", Tag.TAG_COMPOUND).size()));
        }
        if (root.contains("palettes", Tag.TAG_LIST)) {
            lines.add(new StructureLine(0, SUMMARY_COLOR, "Palette variants: " + root.getList("palettes", Tag.TAG_LIST).size()));
        }
        if (root.contains("blocks", Tag.TAG_LIST)) {
            lines.add(new StructureLine(0, SUMMARY_COLOR, "Blocks: " + root.getList("blocks", Tag.TAG_COMPOUND).size()));
        }
        if (root.contains("entities", Tag.TAG_LIST)) {
            lines.add(new StructureLine(0, SUMMARY_COLOR, "Entities: " + root.getList("entities", Tag.TAG_COMPOUND).size()));
        }
        if (root.contains("DataVersion", Tag.TAG_INT)) {
            lines.add(new StructureLine(0, SUMMARY_COLOR, "DataVersion: " + root.getInt("DataVersion")));
        }
    }

    private static void appendNamedTag(
        List<StructureLine> lines,
        int level,
        String name,
        @Nullable Tag tag,
        int depth,
        int maxDepth,
        int maxEntries
    ) {
        if (tag == null) {
            lines.add(new StructureLine(level, NULL_COLOR, name + ": null"));
            return;
        }

        if (tag instanceof CompoundTag compound) {
            List<String> keys = new ArrayList<>(compound.getAllKeys());
            Collections.sort(keys);
            lines.add(new StructureLine(level, CONTAINER_COLOR, name + ": {" + keys.size() + " keys}"));
            if (depth >= maxDepth) {
                return;
            }
            int limit = Math.min(keys.size(), maxEntries);
            for (int i = 0; i < limit; i++) {
                String childKey = keys.get(i);
                appendNamedTag(lines, level + 1, childKey, compound.get(childKey), depth + 1, maxDepth, maxEntries);
            }
            appendOverflowLine(lines, level + 1, keys.size(), limit);
            return;
        }

        if (tag instanceof ListTag list) {
            lines.add(new StructureLine(level, CONTAINER_COLOR, name + ": [" + list.size() + " items]"));
            if (depth >= maxDepth) {
                return;
            }
            int limit = Math.min(list.size(), maxEntries);
            for (int i = 0; i < limit; i++) {
                appendNamedTag(lines, level + 1, "[" + i + "]", list.get(i), depth + 1, maxDepth, maxEntries);
            }
            appendOverflowLine(lines, level + 1, list.size(), limit);
            return;
        }

        if (tag instanceof ByteArrayTag byteArrayTag) {
            lines.add(new StructureLine(level, ARRAY_COLOR, name + ": byte[" + byteArrayTag.getAsByteArray().length + "]"));
            return;
        }
        if (tag instanceof IntArrayTag intArrayTag) {
            lines.add(new StructureLine(level, ARRAY_COLOR, name + ": int[" + intArrayTag.getAsIntArray().length + "]"));
            return;
        }
        if (tag instanceof LongArrayTag longArrayTag) {
            lines.add(new StructureLine(level, ARRAY_COLOR, name + ": long[" + longArrayTag.getAsLongArray().length + "]"));
            return;
        }

        lines.add(new StructureLine(level, getScalarColor(tag), name + ": " + formatScalar(tag)));
    }

    private static void appendOverflowLine(List<StructureLine> lines, int level, int total, int shown) {
        if (total > shown) {
            lines.add(new StructureLine(level, NULL_COLOR, "... +" + (total - shown) + " more"));
        }
    }

    private static int parsePositiveInt(MDExtensionContext context, String key, int defaultValue) {
        String value = context.params().get(key);
        if (value == null || value.isBlank()) {
            return defaultValue;
        }
        return Math.max(1, Integer.parseInt(value));
    }

    private static @Nullable InputStream openStructureStream(StructureTarget target) throws IOException {
        if (AgeratumClient.isPreviewLocation(target.location())) {
            for (String candidate : target.previewCandidatePaths()) {
                Path previewPath = AgeratumClient.resolvePreviewAssetPath(candidate);
                if (Files.isRegularFile(previewPath)) {
                    return Files.newInputStream(previewPath);
                }
            }
        }

        Resource directResource = Minecraft.getInstance().getResourceManager().getResource(target.location()).orElse(null);
        if (directResource != null) {
            return directResource.open();
        }

        for (String candidate : candidateResourcePaths(target.location())) {
            InputStream stream = MDNBTStructureComponent.class.getClassLoader().getResourceAsStream(candidate);
            if (stream != null) {
                return stream;
            }
        }
        return null;
    }

    private void renderPreview(MDRenderContext context, Minecraft minecraft, int maxX, float mouseX, float mouseY) {
        StructurePreview currentPreview = this.preview;
        if (currentPreview == null) {
            return;
        }

        GuiGraphics guiGraphics = context.graphics();
        PreviewLayout layout = this.getPreviewLayout(minecraft, maxX, currentPreview);
        guiGraphics.fill(0, 0, maxX, layout.panelHeight(), PREVIEW_PANEL_BACKGROUND);
        guiGraphics.renderOutline(0, 0, maxX, layout.panelHeight(), PREVIEW_PANEL_BORDER);
        guiGraphics.drawString(minecraft.font, "Top view preview", PREVIEW_PANEL_PADDING, PREVIEW_PANEL_PADDING, PREVIEW_LABEL_COLOR, false);

        for (int z = 0; z < currentPreview.depth(); z++) {
            for (int x = 0; x < currentPreview.width(); x++) {
                int cellX = layout.gridStartX() + x * layout.cellSize();
                int cellY = layout.gridStartY() + z * layout.cellSize();
                int background = ((x + z) & 1) == 0 ? PREVIEW_CELL_BACKGROUND_A : PREVIEW_CELL_BACKGROUND_B;
                guiGraphics.fill(cellX, cellY, cellX + layout.cellSize(), cellY + layout.cellSize(), background);
            }
        }

        PreviewCell hoveredCell = null;
        for (PreviewCell cell : currentPreview.cells()) {
            int cellX = layout.gridStartX() + cell.gridX() * layout.cellSize();
            int cellY = layout.gridStartY() + cell.gridZ() * layout.cellSize();
            guiGraphics.fill(cellX, cellY, cellX + layout.cellSize(), cellY + layout.cellSize(), cell.paletteEntry().previewColor());

            if (layout.cellSize() >= PREVIEW_MIN_ICON_CELL_SIZE && !cell.paletteEntry().displayStack().isEmpty()) {
                PoseStack pose = guiGraphics.pose();
                pose.pushPose();
                pose.translate(cellX, cellY, 0);
                float scale = (float) layout.cellSize() / 16.0f;
                pose.scale(scale, scale, 1.0f);
                guiGraphics.renderItem(cell.paletteEntry().displayStack(), 0, 0);
                pose.popPose();
            }

            if (layout.cellSize() >= 3) {
                guiGraphics.renderOutline(cellX, cellY, layout.cellSize(), layout.cellSize(), PREVIEW_CELL_BORDER);
            }

            if (isHover(cellX, cellY, layout.cellSize(), layout.cellSize(), mouseX, mouseY)) {
                hoveredCell = cell;
            }
        }

        if (hoveredCell != null) {
            context.addTooltip(Component.literal(hoveredCell.tooltip()));
        }
    }

    private int getPreviewHeight(Minecraft minecraft, int maxX) {
        if (this.preview == null) {
            return 0;
        }
        return this.getPreviewLayout(minecraft, maxX, this.preview).panelHeight();
    }

    private PreviewLayout getPreviewLayout(Minecraft minecraft, int maxX, StructurePreview currentPreview) {
        int availableGridWidth = Math.max(1, Math.min(PREVIEW_MAX_GRID_PIXELS, maxX - PREVIEW_PANEL_PADDING * 2));
        int maxDimension = Math.max(1, Math.max(currentPreview.width(), currentPreview.depth()));
        int cellSize = Math.max(1, Math.min(PREVIEW_MAX_CELL_SIZE, availableGridWidth / maxDimension));
        int gridWidth = Math.max(1, currentPreview.width() * cellSize);
        int gridHeight = Math.max(1, currentPreview.depth() * cellSize);
        int gridStartX = Math.max(PREVIEW_PANEL_PADDING, (maxX - gridWidth) / 2);
        int gridStartY = PREVIEW_PANEL_PADDING + minecraft.font.lineHeight + 4;
        int panelHeight = gridStartY + gridHeight + PREVIEW_PANEL_PADDING;
        return new PreviewLayout(cellSize, gridStartX, gridStartY, panelHeight);
    }

    private static boolean isHover(int startX, int startY, int width, int height, float mouseX, float mouseY) {
        return mouseX >= startX && mouseX <= startX + width && mouseY >= startY && mouseY <= startY + height;
    }

    private static @Nullable StructurePreview buildPreview(CompoundTag root) {
        List<PaletteEntry> palette = readPalette(root);
        if (palette.isEmpty() || !root.contains("blocks", Tag.TAG_LIST)) {
            return null;
        }

        ListTag blocks = root.getList("blocks", Tag.TAG_COMPOUND);
        if (blocks.isEmpty()) {
            return null;
        }

        StructureBounds bounds = readBounds(root, blocks);
        if (bounds.width() <= 0 || bounds.depth() <= 0) {
            return null;
        }

        PreviewCell[] topCells = new PreviewCell[bounds.width() * bounds.depth()];
        for (int i = 0; i < blocks.size(); i++) {
            CompoundTag blockTag = blocks.getCompound(i);
            int stateIndex = blockTag.getInt("state");
            if (stateIndex < 0 || stateIndex >= palette.size()) {
                continue;
            }

            PaletteEntry paletteEntry = palette.get(stateIndex);
            if (paletteEntry.air()) {
                continue;
            }

            ListTag pos = blockTag.getList("pos", Tag.TAG_INT);
            if (pos.size() < 3) {
                continue;
            }
            int worldX = readInt(pos, 0);
            int worldY = readInt(pos, 1);
            int worldZ = readInt(pos, 2);
            int gridX = worldX - bounds.minX();
            int gridZ = worldZ - bounds.minZ();
            if (gridX < 0 || gridZ < 0 || gridX >= bounds.width() || gridZ >= bounds.depth()) {
                continue;
            }

            int cellIndex = gridZ * bounds.width() + gridX;
            PreviewCell existing = topCells[cellIndex];
            if (existing != null && existing.worldY() >= worldY) {
                continue;
            }

            boolean hasBlockEntity = blockTag.contains("nbt", Tag.TAG_COMPOUND);
            String tooltip = formatPreviewTooltip(paletteEntry, worldX, worldY, worldZ, stateIndex, hasBlockEntity);
            topCells[cellIndex] = new PreviewCell(gridX, gridZ, worldX, worldY, worldZ, paletteEntry, tooltip);
        }

        List<PreviewCell> cells = new ArrayList<>();
        for (PreviewCell cell : topCells) {
            if (cell != null) {
                cells.add(cell);
            }
        }
        if (cells.isEmpty()) {
            return null;
        }
        cells.sort(Comparator.comparingInt(PreviewCell::gridZ).thenComparingInt(PreviewCell::gridX));
        return new StructurePreview(bounds.width(), bounds.depth(), List.copyOf(cells));
    }

    private static StructureBounds readBounds(CompoundTag root, ListTag blocks) {
        int minX = Integer.MAX_VALUE;
        int minZ = Integer.MAX_VALUE;
        int maxX = Integer.MIN_VALUE;
        int maxZ = Integer.MIN_VALUE;
        for (int i = 0; i < blocks.size(); i++) {
            ListTag pos = blocks.getCompound(i).getList("pos", Tag.TAG_INT);
            if (pos.size() < 3) {
                continue;
            }
            int x = readInt(pos, 0);
            int z = readInt(pos, 2);
            minX = Math.min(minX, x);
            minZ = Math.min(minZ, z);
            maxX = Math.max(maxX, x);
            maxZ = Math.max(maxZ, z);
        }

        if (minX == Integer.MAX_VALUE || minZ == Integer.MAX_VALUE) {
            return new StructureBounds(0, 0, 0, 0);
        }

        if (root.contains("size", Tag.TAG_LIST)) {
            ListTag size = root.getList("size", Tag.TAG_INT);
            if (size.size() >= 3) {
                int width = Math.max(1, readInt(size, 0));
                int depth = Math.max(1, readInt(size, 2));
                return new StructureBounds(0, 0, width, depth);
            }
        }

        return new StructureBounds(minX, minZ, maxX - minX + 1, maxZ - minZ + 1);
    }

    private static List<PaletteEntry> readPalette(CompoundTag root) {
        if (root.contains("palettes", Tag.TAG_LIST)) {
            ListTag palettes = root.getList("palettes", Tag.TAG_LIST);
            if (!palettes.isEmpty() && palettes.get(0) instanceof ListTag firstPalette) {
                return readPalette(firstPalette);
            }
        }
        if (root.contains("palette", Tag.TAG_LIST)) {
            return readPalette(root.getList("palette", Tag.TAG_COMPOUND));
        }
        return List.of();
    }

    private static List<PaletteEntry> readPalette(ListTag paletteTag) {
        List<PaletteEntry> palette = new ArrayList<>(paletteTag.size());
        for (int i = 0; i < paletteTag.size(); i++) {
            if (!(paletteTag.get(i) instanceof CompoundTag entry)) {
                continue;
            }
            String blockId = entry.getString("Name");
            String displayName = blockId + formatPaletteProperties(entry.getCompound("Properties"));
            Block block = Blocks.AIR;
            try {
                ResourceLocation location = ResourceLocation.parse(blockId);
                block = BuiltInRegistries.BLOCK.get(location);
            } catch (RuntimeException ignored) {
            }
            boolean air = block == Blocks.AIR;
            Item item = block.asItem();
            ItemStack displayStack = air || item == Items.AIR ? ItemStack.EMPTY : new ItemStack(item);
            palette.add(new PaletteEntry(i, blockId, displayName, displayStack, computePreviewColor(displayName), air));
        }
        return List.copyOf(palette);
    }

    private static String formatPaletteProperties(CompoundTag properties) {
        if (properties.isEmpty()) {
            return "";
        }
        List<String> entries = new ArrayList<>();
        for (String key : properties.getAllKeys()) {
            entries.add(key + '=' + properties.getString(key));
        }
        entries.sort(String::compareTo);
        return '[' + String.join(",", entries) + ']';
    }

    private static int computePreviewColor(String seed) {
        int hash = seed.toLowerCase(Locale.ROOT).hashCode();
        float hue = (hash & 0xFFFF) / 65535.0f;
        return 0x88000000 | (Color.HSBtoRGB(hue, 0.45f, 0.85f) & 0x00FFFFFF);
    }

    private static String formatPreviewTooltip(
        PaletteEntry paletteEntry,
        int worldX,
        int worldY,
        int worldZ,
        int stateIndex,
        boolean hasBlockEntity
    ) {
        String blockEntityPart = hasBlockEntity ? " | block entity" : "";
        return paletteEntry.displayName() + " @ (" + worldX + ", " + worldY + ", " + worldZ + ") | palette=" + stateIndex + blockEntityPart;
    }

    private static List<String> candidateResourcePaths(ResourceLocation location) {
        String normalizedPath = normalizeStructurePath(location.getPath());
        return List.of(
            "data/" + location.getNamespace() + "/structure/" + normalizedPath + ".nbt",
            "data/" + location.getNamespace() + "/structures/" + normalizedPath + ".nbt"
        );
    }

    private static String normalizeStructurePath(String path) {
        String normalized = path.replace('\\', '/');
        while (normalized.startsWith("/")) {
            normalized = normalized.substring(1);
        }
        if (normalized.endsWith(".nbt")) {
            normalized = normalized.substring(0, normalized.length() - 4);
        }
        return normalized;
    }

    private static String ensureNbtExtension(String path) {
        return path.endsWith(".nbt") ? path : path + ".nbt";
    }

    private static String getCurrentDirectoryPath(ResourceLocation location) {
        String currentFile = location.getPath();
        int slash = currentFile.lastIndexOf('/');
        if (slash < 0) {
            return "";
        }
        return currentFile.substring(0, slash);
    }

    private static String normalizePathAgainstBase(String baseDir, String target) {
        String source = target.replace('\\', '/').trim();
        while (source.startsWith("/")) {
            source = source.substring(1);
        }
        String combined = baseDir.isEmpty() ? source : baseDir + "/" + source;

        ArrayList<String> parts = new ArrayList<>();
        for (String segment : combined.split("/")) {
            if (segment.isEmpty() || ".".equals(segment)) {
                continue;
            }
            if ("..".equals(segment)) {
                if (!parts.isEmpty()) {
                    parts.removeLast();
                }
                continue;
            }
            parts.add(segment);
        }
        return String.join("/", parts);
    }


    private static int getScalarColor(Tag tag) {
        if (tag instanceof NumericTag) {
            return NUMBER_COLOR;
        }
        if (tag instanceof StringTag) {
            return STRING_COLOR;
        }
        return TITLE_COLOR;
    }

    private static String formatScalar(Tag tag) {
        if (tag instanceof StringTag stringTag) {
            return '"' + abbreviate(stringTag.getAsString()) + '"';
        }
        if (tag instanceof NumericTag numericTag) {
            return switch (tag.getId()) {
                case Tag.TAG_BYTE -> numericTag.getAsByte() + "b";
                case Tag.TAG_SHORT -> numericTag.getAsShort() + "s";
                case Tag.TAG_INT -> Integer.toString(numericTag.getAsInt());
                case Tag.TAG_LONG -> numericTag.getAsLong() + "L";
                case Tag.TAG_FLOAT -> numericTag.getAsFloat() + "f";
                case Tag.TAG_DOUBLE -> numericTag.getAsDouble() + "d";
                default -> numericTag.getAsNumber().toString();
            };
        }
        return abbreviate(tag.getAsString());
    }

    private static int readInt(ListTag list, int index) {
        Tag tag = list.get(index);
        if (tag instanceof NumericTag numericTag) {
            return numericTag.getAsInt();
        }
        return 0;
    }

    private static String abbreviate(String text) {
        if (text.length() <= MAX_TEXT_PREVIEW_LENGTH) {
            return text;
        }
        return text.substring(0, Math.max(0, MAX_TEXT_PREVIEW_LENGTH - 3)) + "...";
    }

    private static FormattedText composeComponentText(FormattedText summaryText, FormattedText detailText) {
        List<FormattedText> parts = new ArrayList<>(3);
        if (summaryText != FormattedText.EMPTY) {
            parts.add(summaryText);
        }
        if (detailText != FormattedText.EMPTY) {
            if (!parts.isEmpty()) {
                parts.add(FormattedText.of("\n"));
            }
            parts.add(detailText);
        }
        if (parts.isEmpty()) {
            return FormattedText.EMPTY;
        }
        return FormattedText.composite(parts);
    }

    private record PreparedData(
        StructureTreeComponent summaryComponent,
        StructureTreeComponent detailComponent,
        @Nullable StructurePreview preview,
        FormattedText componentText
    ) {
    }

    public record StructureTarget(ResourceLocation location, String displayPath, List<String> previewCandidatePaths) {
        public static StructureTarget resolve(ResourceLocation sourceLocation, String rawTarget) {
            String trimmed = rawTarget.trim();
            if (trimmed.contains(":")) {
                ResourceLocation location = ResourceLocation.parse(trimmed);
                List<String> previewPaths = AgeratumClient.isPreviewLocation(location)
                    ? List.of(ensureNbtExtension(normalizePathAgainstBase("", location.getPath())))
                    : List.of();
                return new StructureTarget(location, trimmed, previewPaths);
            }

            String resolvedPath = normalizePathAgainstBase(getCurrentDirectoryPath(sourceLocation), trimmed);
            ResourceLocation location = ResourceLocation.fromNamespaceAndPath(sourceLocation.getNamespace(), resolvedPath);
            List<String> previewPaths = AgeratumClient.isPreviewLocation(sourceLocation)
                ? List.of(ensureNbtExtension(resolvedPath))
                : List.of();
            return new StructureTarget(location, trimmed, previewPaths);
        }
    }

    public record StructureLine(int level, int color, String text) {
    }

    private record PreviewLayout(int cellSize, int gridStartX, int gridStartY, int panelHeight) {
    }

    private record StructureBounds(int minX, int minZ, int width, int depth) {
    }

    private record PaletteEntry(
        int index,
        String blockId,
        String displayName,
        ItemStack displayStack,
        int previewColor,
        boolean air
    ) {
    }

    private record PreviewCell(
        int gridX,
        int gridZ,
        int worldX,
        int worldY,
        int worldZ,
        PaletteEntry paletteEntry,
        String tooltip
    ) {
    }

    private record StructurePreview(int width, int depth, List<PreviewCell> cells) {
    }

    private static final class StructureTreeComponent extends MDBlockComponent<StructureLine> {
        private StructureTreeComponent(TreePreparedData preparedData) {
            super(preparedData.componentText(), preparedData.lines());
        }

        public static StructureTreeComponent create(List<StructureLine> sourceLines) {
            List<CachedItem<StructureLine>> cachedItems = new ArrayList<>(sourceLines.size());
            for (StructureLine line : sourceLines) {
                Style style = Style.EMPTY.withColor(line.color());
                cachedItems.add(new CachedItem<>(line.level(), line, FormattedText.of(line.text(), style)));
            }
            List<CachedItem<StructureLine>> immutableLines = List.copyOf(cachedItems);
            return new StructureTreeComponent(new TreePreparedData(immutableLines, composeBlockText(immutableLines)));
        }

        @Override
        protected int getTextX(CachedItem<StructureLine> cachedItem) {
            return cachedItem.level() * INDENT_WIDTH + TEXT_PADDING;
        }

        @Override
        protected void renderDecoration(
            GuiGraphics guiGraphics,
            Minecraft minecraft,
            CachedItem<StructureLine> cachedItem,
            int y,
            int lineHeight,
            int maxX
        ) {
            int lineBottom = y + lineHeight - 1;
            for (int level = 0; level < cachedItem.level(); level++) {
                int lineX = level * INDENT_WIDTH + 1;
                guiGraphics.vLine(lineX, y, lineBottom, LEVEL_LINE_COLORS[level % LEVEL_LINE_COLORS.length] | 0xFF000000);
            }
        }
    }

    private record TreePreparedData(List<MDBlockComponent.CachedItem<StructureLine>> lines, FormattedText componentText) {
    }
}

