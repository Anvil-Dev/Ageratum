package dev.anvilcraft.resource.ageratum.client.feat.markdown.component;

import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.BufferUploader;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;
import dev.anvilcraft.resource.ageratum.client.AgeratumClient;
import dev.anvilcraft.resource.ageratum.client.feat.markdown.MDRenderContext;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.network.chat.FormattedText;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.Resource;
import org.joml.Matrix4f;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.annotation.Nullable;

/**
 * 图片组件。
 *
 * <p>支持独占一行的 Markdown 图片语法，图片资源会被映射到
 * {@code textures/} 目录下并按可用区域等比缩放。</p>
 */
@Getter
@Slf4j
public class MDImageComponent extends MDComponent {
    private static final Pattern IMAGE_PATTERN = Pattern.compile("^\\s*!\\[[^]]*]\\(([^):]+):([^)]+)\\)\\s*$");
    private static final Pattern FALLBACK_IMAGE_PATTERN = Pattern.compile("^\\s*!\\[[^]]*]\\(([^)]+)\\)\\s*$");
    private static final Map<ResourceLocation, Size> IMAGE_SIZE_CACHE = new HashMap<>();
    private static final Map<ResourceLocation, PreviewImageState> PREVIEW_IMAGE_CACHE = new HashMap<>();
    protected final ResourceLocation imageLocation;
    protected final boolean shouldScaleUp;
    protected final boolean enableAlignCenter;
    protected float scale = 1.0f;

    /**
     * 创建图片组件。
     */
    public MDImageComponent(ResourceLocation imageLocation) {
        this(imageLocation, false);
    }

    /**
     * 创建图片组件。
     */
    public MDImageComponent(ResourceLocation imageLocation, boolean shouldScaleUp) {
        this(imageLocation, shouldScaleUp, false);
    }

    /**
     * 创建图片组件。
     */
    public MDImageComponent(ResourceLocation imageLocation, boolean shouldScaleUp, boolean enableAlignCenter) {
        super(FormattedText.EMPTY);
        this.imageLocation = imageLocation;
        this.shouldScaleUp = shouldScaleUp;
        this.enableAlignCenter = enableAlignCenter;
    }

    /**
     * 尝试将一行文本解析为图片组件。
     */
    public static @Nullable MDImageComponent parse(ResourceLocation sourceLocation, String text) {
        Matcher matcher = IMAGE_PATTERN.matcher(text);
        if (!matcher.matches()) {
            return MDImageComponent.fallbackParse(sourceLocation, text);
        }
        String namespace = matcher.group(1);
        String file = matcher.group(2).trim().replace('\\', '/');
        while (file.startsWith("/")) {
            file = file.substring(1);
        }
        try {
            ResourceLocation imageLocation = ResourceLocation.fromNamespaceAndPath(namespace, file).withPrefix("textures/");
            return new MDImageComponent(imageLocation);
        } catch (RuntimeException exception) {
            return null;
        }
    }

    public static @Nullable MDImageComponent fallbackParse(ResourceLocation sourceLocation, String text) {
        Matcher matcher = FALLBACK_IMAGE_PATTERN.matcher(text);
        if (!matcher.matches()) {
            return null;
        }
        try {
            String path = getParsedPath(sourceLocation, matcher.group(1));
            return new MDImageComponent(sourceLocation.withPath(path));
        } catch (RuntimeException exception) {
            log.debug(exception.getLocalizedMessage(), exception);
            return null;
        }
    }

    private static String getParsedPath(ResourceLocation sourceLocation, String path) {
        String[] splitPath = path.split("/");
        String locationPath = sourceLocation.getPath();
        LinkedList<String> splitLocationPathList = new LinkedList<>(List.of(locationPath.split("/")));
        splitLocationPathList.pollLast();
        for (String pat : splitPath) {
            if (".".equals(pat)) continue;
            if ("..".equals(pat)) {
                splitLocationPathList.pollLast();
                continue;
            }
            splitLocationPathList.add(pat);
        }
        StringBuilder pathBuilder = new StringBuilder();
        splitLocationPathList.forEach(str -> {
            pathBuilder.append(str);
            pathBuilder.append("/");
        });
        pathBuilder.deleteCharAt(pathBuilder.length() - 1);
        return pathBuilder.toString();
    }

    /**
     * 按缩放后的尺寸渲染图片。
     */
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
        Size size = this.resolveSize(minecraft);
        Size renderSize = this.computeRenderSize(size, maxX, maxY);
        if (renderSize.width() <= 0 || renderSize.height() <= 0) {
            return;
        }
        PoseStack pose = guiGraphics.pose();
        pose.pushPose();
        if (this.enableAlignCenter) {
            float translateX = (maxX - renderSize.width()) / 2.0f;
            pose.translate(translateX, 0, 0);
            mouseX -= translateX;
        }
        pose.scale(renderSize.scale(), renderSize.scale(), renderSize.scale());
        this.renderContent(context, size, mouseX / renderSize.scale(), mouseY / renderSize.scale());
        pose.popPose();
    }

    protected void renderContent(MDRenderContext context, Size size, float mouseX, float mouseY) {
        GuiGraphics guiGraphics = context.graphics();
        this.innerBlit(guiGraphics, this.getImageLocation(), size.width(), size.height(), size.width(), size.height());
    }

    protected void innerBlit(
        GuiGraphics guiGraphics,
        ResourceLocation atlasLocation,
        int width,
        int height,
        int textureWidth,
        int textureHeight
    ) {
        float minU = ((float) 0.0 + 0.0F) / (float) textureWidth;
        float maxU = ((float) 0.0 + (float) width) / (float) textureWidth;
        float minV = (0.0F + 0.0F) / (float) textureHeight;
        float maxV = (0.0F + (float) height) / (float) textureHeight;
        RenderSystem.enableBlend();
        RenderSystem.setShaderTexture(0, atlasLocation);
        RenderSystem.setShader(GameRenderer::getPositionTexShader);
        Matrix4f matrix4f = guiGraphics.pose().last().pose();
        BufferBuilder bufferbuilder = Tesselator.getInstance().begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX);
        bufferbuilder.addVertex(matrix4f, (float) 0, (float) 0, (float) 0).setUv(minU, minV);
        bufferbuilder.addVertex(matrix4f, (float) 0, (float) height, (float) 0).setUv(minU, maxV);
        bufferbuilder.addVertex(matrix4f, (float) width, (float) height, (float) 0).setUv(maxU, maxV);
        bufferbuilder.addVertex(matrix4f, (float) width, (float) 0, (float) 0).setUv(maxU, minV);
        BufferUploader.drawWithShader(bufferbuilder.buildOrThrow());
        RenderSystem.disableBlend();
    }

    /**
     * 返回图片在目标区域中的渲染高度。
     */
    @Override
    public int getHeight(Minecraft minecraft, int maxX, int maxY) {
        Size size = this.resolveSize(minecraft);
        return this.computeRenderSize(size, maxX, maxY).height();
    }

    protected boolean shouldScaleUp() {
        return this.shouldScaleUp;
    }

    /**
     * 在可用宽高约束下计算等比缩放后的尺寸。
     */
    protected Size computeRenderSize(Size source, int maxX, int maxY) {
        float scale = computeScale(source, maxX, maxY);
        this.scale = scale;
        int width = Math.max(1, Math.round(source.width() * scale));
        int height = Math.max(1, Math.round(source.height() * scale));
        return new Size(width, height, scale);
    }

    protected float computeScale(Size source, int maxX, int maxY) {
        int availableWidth = Math.max(1, maxX);
        int availableHeight = maxY <= 0 ? Integer.MAX_VALUE : availableWidth;
        float scale = Math.min((float) availableWidth / source.width(), (float) availableHeight / source.height());
        if (!this.shouldScaleUp()) scale = Math.min(1.0f, scale);
        return scale;
    }

    protected float getScaleInRender() {
        return this.scale;
    }

    /**
     * 获取图片原始尺寸，缺失时使用缓存或回退默认值。
     */
    protected Size resolveSize(Minecraft minecraft) {
        if (AgeratumClient.PREVIEW_NAMESPACE.equals(this.getImageLocation().getNamespace())) {
            return this.resolvePreviewSize(minecraft);
        }

        Size cachedSize = IMAGE_SIZE_CACHE.get(this.getImageLocation());
        if (cachedSize != null) {
            return cachedSize;
        }
        Size size = new Size(16, 16, 1.0f);
        try {
            Resource resource = minecraft.getResourceManager().getResource(this.getImageLocation()).orElse(null);
            if (resource != null) {
                try (NativeImage image = NativeImage.read(resource.open())) {
                    size = new Size(Math.max(1, image.getWidth()), Math.max(1, image.getHeight()), 1.0f);
                }
            }
        } catch (IOException ignored) {
            // Missing or invalid textures fall back to a tiny placeholder size.
        }
        IMAGE_SIZE_CACHE.put(this.getImageLocation(), size);
        return size;
    }

    private Size resolvePreviewSize(Minecraft minecraft) {
        Path imagePath = AgeratumClient.resolvePreviewAssetPath(this.getImageLocation().getPath());
        if (!Files.isRegularFile(imagePath)) {
            return new Size(16, 16, 1.0f);
        }

        long modifiedMillis;
        long fileSize;
        try {
            modifiedMillis = Files.getLastModifiedTime(imagePath).toMillis();
            fileSize = Files.size(imagePath);
        } catch (IOException ignored) {
            return new Size(16, 16, 1.0f);
        }

        PreviewImageState cached = PREVIEW_IMAGE_CACHE.get(this.getImageLocation());
        if (cached != null && cached.modifiedMillis() == modifiedMillis && cached.fileSize() == fileSize) {
            return cached.size();
        }

        try {
            NativeImage image = NativeImage.read(Files.newInputStream(imagePath));
            Size size = new Size(Math.max(1, image.getWidth()), Math.max(1, image.getHeight()), 1.0f);
            minecraft.getTextureManager().register(this.getImageLocation(), new DynamicTexture(image));
            PREVIEW_IMAGE_CACHE.put(this.getImageLocation(), new PreviewImageState(modifiedMillis, fileSize, size));
            return size;
        } catch (IOException exception) {
            log.debug("Failed to load preview image: {}", imagePath, exception);
            return new Size(16, 16, 1.0f);
        }
    }

    /**
     * 简单尺寸值对象。
     */
    public record Size(int width, int height, float scale) {
    }

    private record PreviewImageState(long modifiedMillis, long fileSize, Size size) {
    }
}

