package dev.anvilcraft.resource.ageratum.client.feat.markdown.component;

import com.mojang.blaze3d.platform.NativeImage;
import dev.anvilcraft.resource.ageratum.client.constants.AgeratumConstants;
import dev.anvilcraft.resource.ageratum.client.feat.github.GitHubAssetResolver;
import dev.anvilcraft.resource.ageratum.client.feat.github.GitHubRepoCache;
import dev.anvilcraft.resource.ageratum.client.feat.markdown.MDRenderContext;
import lombok.extern.slf4j.Slf4j;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.resources.ResourceLocation;

import javax.annotation.Nullable;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

/**
 * GitHub 远程指南中的图片组件。
 *
 * <p>资源通过 {@link GitHubAssetResolver} 映射到已下载仓库的本地文件，
 * 读取后注册为动态纹理；文件不存在时回退为占位尺寸。</p>
 */
@Slf4j
public class GitHubImageComponent extends MDImageComponent {
    private static final Map<ResourceLocation, CachedTexture> SIZE_CACHE = new HashMap<>();

    private record CachedTexture(long modified, long length, String commit, Size size) {}

    private final ResourceLocation sourceLocation;
    private final String rawTarget;

    private GitHubImageComponent(ResourceLocation sourceLocation, String rawTarget) {
        super(ResourceLocation.fromNamespaceAndPath(
            GitHubAssetResolver.GITHUB_NAMESPACE,
            "asset/" + java.util.UUID.nameUUIDFromBytes((sourceLocation + "\0" + rawTarget).getBytes(java.nio.charset.StandardCharsets.UTF_8))
        ));
        this.sourceLocation = sourceLocation;
        this.rawTarget = rawTarget;
    }

    /**
     * 基于 github 源文档位置与目标引用创建图片组件。
     */
    public static @Nullable GitHubImageComponent of(ResourceLocation sourceLocation, String rawTarget) {
        if (rawTarget == null || rawTarget.isBlank()) {
            return null;
        }
        return new GitHubImageComponent(sourceLocation, rawTarget.trim());
    }

    @Override
    protected Size resolveSize(Minecraft minecraft) {
        Path file = resolveFile();
        if (file == null) {
            return placeholderSize();
        }
        Size size = loadTexture(minecraft, file);
        return size == null ? placeholderSize() : size;
    }

    @Override
    protected void renderContent(MDRenderContext context, Size size, float mouseX, float mouseY) {
        Path file = resolveFile();
        if (file == null) {
            return;
        }
        Minecraft minecraft = context.minecraft();
        Size loadedSize = loadTexture(minecraft, file);
        if (loadedSize == null) {
            return;
        }
        super.innerBlit(
            context.graphics(),
            this.getImageLocation(),
            loadedSize.width(),
            loadedSize.height(),
            loadedSize.width(),
            loadedSize.height()
        );
    }

    @Nullable
    private Path resolveFile() {
        GitHubRepoCache.RepoState state = GitHubRepoCache.getActiveState(this.sourceLocation);
        if (state == null) {
            return null;
        }
        return GitHubAssetResolver.resolve(state, this.sourceLocation, this.rawTarget);
    }

    @Nullable
    private Size loadTexture(Minecraft minecraft, Path file) {
        ResourceLocation textureLocation = this.getImageLocation();
        try {
            long modified = Files.getLastModifiedTime(file).toMillis();
            long length = Files.size(file);
            var state = GitHubRepoCache.getActiveState(this.sourceLocation);
            if (state == null) return null;
            var cached = SIZE_CACHE.get(textureLocation);
            if (cached != null && cached.modified() == modified && cached.length() == length
                && cached.commit().equals(state.commit())) {
                return cached.size();
            }
            try (var stream = Files.newInputStream(file)) {
                NativeImage image = NativeImage.read(stream);
                Size size = new Size(Math.max(1, image.getWidth()), Math.max(1, image.getHeight()), 1.0f);
                minecraft.getTextureManager().register(textureLocation, new DynamicTexture(image));
                SIZE_CACHE.put(textureLocation, new CachedTexture(modified, length, state.commit(), size));
                return size;
            }
        } catch (IOException exception) {
            log.debug("Failed to load GitHub image: {}", file, exception);
            return null;
        }
    }

    private static Size placeholderSize() {
        return new Size(
            AgeratumConstants.Image.DEFAULT_PLACEHOLDER_WIDTH,
            AgeratumConstants.Image.DEFAULT_PLACEHOLDER_HEIGHT,
            1.0f
        );
    }
}
