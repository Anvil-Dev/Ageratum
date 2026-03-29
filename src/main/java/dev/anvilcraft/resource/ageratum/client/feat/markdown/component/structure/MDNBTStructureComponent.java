package dev.anvilcraft.resource.ageratum.client.feat.markdown.component.structure;

import dev.anvilcraft.resource.ageratum.client.AgeratumClient;
import dev.anvilcraft.resource.ageratum.client.feat.markdown.MDExtensionContext;
import dev.anvilcraft.resource.ageratum.client.feat.markdown.component.MDBlockComponent;
import dev.anvilcraft.resource.ageratum.client.feat.markdown.component.MDComponent;
import dev.anvilcraft.resource.ageratum.client.feat.markdown.component.MDTextComponent;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
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
import net.minecraft.network.chat.FormattedText;
import net.minecraft.network.chat.Style;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.Resource;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import javax.annotation.Nullable;

/**
 * NBT 结构文件渲染组件。
 *
 * <p>该组件由扩展标签 {@code <structure id="namespace:path"/>} 或
 * {@code <nbt_structure id="namespace:path"/>} 创建，
 * 用于在文档中渲染结构文件摘要与 NBT 树状视图。</p>
 */
public final class MDNBTStructureComponent extends MDBlockComponent<MDNBTStructureComponent.StructureLine> {
    private static final int INDENT_WIDTH = 10;
    private static final int TEXT_PADDING = 6;
    private static final int DEFAULT_MAX_DEPTH = 2;
    private static final int DEFAULT_MAX_ENTRIES = 12;
    private static final int TITLE_COLOR = 0x1F2937;
    private static final int SUMMARY_COLOR = 0x334155;
    private static final int NUMBER_COLOR = 0xB45309;
    private static final int STRING_COLOR = 0x15803D;
    private static final int CONTAINER_COLOR = 0x0F766E;
    private static final int ARRAY_COLOR = 0x7C3AED;
    private static final int NULL_COLOR = 0x6B7280;
    private static final int ERROR_COLOR = 0xB91C1C;
    private static final int MAX_TEXT_PREVIEW_LENGTH = 80;

    private MDNBTStructureComponent(StructureTarget target, int maxDepth, int maxEntries) {
        this(prepare(target, Math.max(1, maxDepth), Math.max(1, maxEntries)));
    }

    private MDNBTStructureComponent(PreparedData preparedData) {
        super(preparedData.componentText(), preparedData.lines());
    }

    /**
     * 解析结构扩展标签。
     */
    public static MDComponent parse(MDExtensionContext context) {
        String rawId = context.params().getOrDefault("id", context.params().get("path"));
        if (rawId == null || rawId.isBlank()) {
            return new MDTextComponent("[错误：structure / nbt_structure 需要 id 或 path 参数]");
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
            guiGraphics.vLine(lineX, y, lineBottom, getLevelLineColor(level));
        }
    }

    private static PreparedData prepare(StructureTarget target, int maxDepth, int maxEntries) {
        List<StructureLine> lines = new ArrayList<>();
        lines.add(new StructureLine(0, TITLE_COLOR, "Structure: " + target.displayPath()));

        try (InputStream inputStream = openStructureStream(target)) {
            if (inputStream == null) {
                lines.add(new StructureLine(0, ERROR_COLOR, "无法找到结构文件：" + target.displayPath()));
                return toPreparedData(lines);
            }

            CompoundTag root = NbtIo.readCompressed(inputStream, NbtAccounter.unlimitedHeap());
            appendStructureSummary(lines, root);
            lines.add(new StructureLine(0, SUMMARY_COLOR, "Root tags:"));

            List<String> keys = new ArrayList<>(root.getAllKeys());
            Collections.sort(keys);
            int limit = Math.min(keys.size(), maxEntries);
            for (int i = 0; i < limit; i++) {
                String key = keys.get(i);
                appendNamedTag(lines, 1, key, root.get(key), 1, maxDepth, maxEntries);
            }
            appendOverflowLine(lines, 1, keys.size(), limit);
        } catch (Exception exception) {
            lines.add(new StructureLine(0, ERROR_COLOR, "加载失败：" + exception.getMessage()));
        }

        return toPreparedData(lines);
    }

    private static PreparedData toPreparedData(List<StructureLine> sourceLines) {
        List<CachedItem<StructureLine>> cachedItems = new ArrayList<>(sourceLines.size());
        for (StructureLine line : sourceLines) {
            Style style = Style.EMPTY.withColor(line.color());
            cachedItems.add(new CachedItem<>(line.level(), line, FormattedText.of(line.text(), style)));
        }
        List<CachedItem<StructureLine>> immutableLines = List.copyOf(cachedItems);
        return new PreparedData(immutableLines, composeBlockText(immutableLines));
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

    private static int getLevelLineColor(int level) {
        return LEVEL_LINE_COLORS[level % LEVEL_LINE_COLORS.length] | 0xFF000000;
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

    private record PreparedData(List<CachedItem<StructureLine>> lines, FormattedText componentText) {
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
}

