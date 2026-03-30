package dev.anvilcraft.resource.ageratum.client.feat.markdown.component.structure;

import dev.anvilcraft.resource.ageratum.client.AgeratumClient;
import dev.anvilcraft.resource.ageratum.client.feat.markdown.MDExtensionContext;
import dev.anvilcraft.resource.ageratum.client.feat.markdown.MDRenderContext;
import dev.anvilcraft.resource.ageratum.client.feat.markdown.component.MDComponent;
import dev.anvilcraft.resource.ageratum.client.feat.markdown.component.MDTextComponent;
import dev.anvilcraft.resource.ageratum.client.util.RelativePathResolver;
import dev.anvilcraft.resource.ageratum.client.util.ViewportCameraRig;
import dev.anvilcraft.resource.ageratum.client.util.level.DelegatingServerLevelAccessor;
import dev.anvilcraft.resource.ageratum.client.util.level.SandboxRenderLevel;
import dev.anvilcraft.resource.ageratum.client.util.level.StructurePreviewRenderer;
import lombok.extern.slf4j.Slf4j;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtAccounter;
import net.minecraft.nbt.NbtIo;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.levelgen.SingleThreadedRandomSource;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import javax.annotation.Nullable;

/**
 * NBT 结构文件渲染组件。
 *
 * <p>该组件由扩展标签 {@code <structure id="namespace:path"/>} 创建，
 * 用于在文档中渲染结构文件摘要与 NBT 树状视图。</p>
 */
@Slf4j
public final class MDNBTStructureComponent extends MDComponent {
    private final StructureTarget target;
    private @Nullable SandboxRenderLevel previewLevel = null;

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
            this.previewLevel = MDNBTStructureComponent.prepare(minecraft.level, this.target);
        }
        if (this.previewLevel == null) {
            super.render(context.child());
        }
        graphics.renderOutline(0, 0, maxX, this.scale(maxX, 150), 0xAA000000);
        graphics.fill(0, 0, maxX, this.scale(maxX, 150), 0x55000000);
        context.enableScissor(1, 1, maxX - 1, this.scale(maxX, 150) - 1);
        ViewportCameraRig cameraRig = new ViewportCameraRig();
        cameraRig.configureViewport(context.screenWidth(), context.screenHeight());
        cameraRig.setOffsetY(-graphics.pose().last().pose().m31());
        StructurePreviewRenderer.getInstance().render(this.previewLevel, cameraRig, graphics.bufferSource());
        context.disableScissor();
    }

    public int scale(int maxX, int value) {
        float scale = 330.f / maxX;
        return Math.round(value * scale);
    }

    @Override
    public int getHeight(Minecraft minecraft, int maxX, int maxY) {
        return this.scale(maxX, 150);
    }

    /**
     * 加载 NBT 结构模板并将其放入沙盒关卡，供后续渲染使用。
     */
    private static @Nullable SandboxRenderLevel prepare(@Nullable Level clientLevel, StructureTarget target) {
        if (clientLevel == null) return null;
        try (InputStream inputStream = MDNBTStructureComponent.openStructureStream(target)) {
            if (inputStream == null) {
                return null;
            }

            var template = new StructureTemplate();
            var blocks = clientLevel.registryAccess().registryOrThrow(Registries.BLOCK).asLookup();
            CompoundTag root = NbtIo.readCompressed(inputStream, NbtAccounter.unlimitedHeap());
            template.load(blocks, root);
            var random = new SingleThreadedRandomSource(0L);
            var settings = new StructurePlaceSettings();
            settings.setIgnoreEntities(true);
            SandboxRenderLevel level = new SandboxRenderLevel();
            var fakeServerLevel = new DelegatingServerLevelAccessor(level);
            if (!template.placeInWorld(fakeServerLevel, BlockPos.ZERO, BlockPos.ZERO, settings, random, 0)) {
                log.debug("Failed to place structure.");
            }
            return level;
        } catch (Exception exception) {
            return null;
        }
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

    /**
     * 生成结构文件在 classpath 中的回退搜索路径。
     */
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

    public record StructureTarget(ResourceLocation location, String displayPath, List<String> previewCandidatePaths) {
        /**
         * 基于 markdown 源文档位置解析显式或相对的结构引用。
         */
        public static StructureTarget resolve(ResourceLocation sourceLocation, String rawTarget) {
            String trimmed = rawTarget.trim();
            if (trimmed.contains(":")) {
                ResourceLocation location = ResourceLocation.parse(trimmed);
                List<String> previewPaths = AgeratumClient.isPreviewLocation(location)
                                            ? List.of(ensureNbtExtension(RelativePathResolver.resolveWithinBase("", location.getPath())))
                                            : List.of();
                return new StructureTarget(location, trimmed, previewPaths);
            }

            String resolvedPath = RelativePathResolver.resolveWithinBase(getCurrentDirectoryPath(sourceLocation), trimmed);
            ResourceLocation location = ResourceLocation.fromNamespaceAndPath(sourceLocation.getNamespace(), resolvedPath);
            List<String> previewPaths = AgeratumClient.isPreviewLocation(sourceLocation)
                                        ? List.of(ensureNbtExtension(resolvedPath))
                                        : List.of();
            return new StructureTarget(location, trimmed, previewPaths);
        }
    }
}

