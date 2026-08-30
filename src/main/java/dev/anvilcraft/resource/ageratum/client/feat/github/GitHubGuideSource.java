package dev.anvilcraft.resource.ageratum.client.feat.github;

import com.mojang.logging.LogUtils;
import dev.anvilcraft.resource.ageratum.client.AgeratumClient;
import dev.anvilcraft.resource.ageratum.client.feat.markdown.MDDocument;
import dev.anvilcraft.resource.ageratum.client.feat.markdown.MarkdownParser;
import dev.anvilcraft.resource.ageratum.client.gui.GuideScreen;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.Identifier;
import org.slf4j.Logger;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

/**
 * GitHub 远程指南打开入口。
 *
 * <p>流程：显示加载界面 → 异步下载/解压仓库 → 解析 {@code ageratum/index.md}
 * 或 {@code ageratum/<语言>/index.md} → 打开 {@link GuideScreen}；
 * 全部失败时显示红色 {@code 加载失败...}。</p>
 */
public final class GitHubGuideSource {
    private static final Logger LOGGER = LogUtils.getLogger();

    private GitHubGuideSource() {
    }

    /**
     * 打开 GitHub 远程指南（从原始 URI 字符串）。
     *
     * @param rawUri 例如 {@code github:Gu-ZT/Ageratum-Resources-Pack-TeaCon2026#path:assets/advanced_clover&commit=798f631}
     */
    public static void open(String rawUri) {
        GitHubDocUri uri = GitHubDocUri.parse(rawUri);
        if (uri == null) {
            showFailure();
            return;
        }
        open(uri);
    }

    /**
     * 打开 GitHub 远程指南（已解析 URI）。
     */
    public static void open(GitHubDocUri uri) {
        GitHubLoadingScreen loadingScreen = GitHubLoadingScreen.open();
        GitHubRepoCache.ensureDownloaded(uri).whenComplete((state, throwable) -> {
            Minecraft minecraft = Minecraft.getInstance();
            minecraft.execute(() -> {
                if (state == null || throwable != null) {
                    LOGGER.warn("Failed to load GitHub guide {}/{}", uri.user(), uri.repo(), throwable);
                    loadingScreen.markFailed();
                    return;
                }
                try {
                    openDocument(minecraft, uri, state);
                } catch (Exception exception) {
                    LOGGER.warn("Failed to open GitHub guide {}/{}", uri.user(), uri.repo(), exception);
                    loadingScreen.markFailed();
                }
            });
        });
    }

    /**
     * 根据已就绪的仓库状态打开文档。
     */
    private static void openDocument(Minecraft minecraft, GitHubDocUri uri, GitHubRepoCache.RepoState state) {
        String languageCode = AgeratumClient.getClientLanguageCode(minecraft);

        // 文档位置：github:<user>/<repo>/<root>/ageratum/<lang>/index.md
        // （user/repo/root 转为小写以满足 Identifier 字符集；资源解析走 state）
        Identifier indexLocation = toDisplayLocation(uri, "ageratum/" + languageCode + "/index.md");
        Path documentFile = GitHubAssetResolver.resolveDocument(state, indexLocation, "index");
        if (documentFile == null) {
            LOGGER.warn("No index document found for {}/{} under {}", uri.user(), uri.repo(), uri.resourceRoot());
            loadingScreenMarkFailed(minecraft);
            return;
        }

        String markdown;
        try {
            markdown = Files.readString(documentFile, StandardCharsets.UTF_8);
        } catch (Exception exception) {
            LOGGER.warn("Failed to read document {}", documentFile, exception);
            loadingScreenMarkFailed(minecraft);
            return;
        }

        MDDocument document = new MarkdownParser().parseDocument(indexLocation, markdown);
        GitHubRepoCache.registerActiveState(indexLocation, state);
        GuideScreen screen = new GuideScreen(indexLocation, document, List.of(), false);
        minecraft.setScreen(screen);
    }

    /**
     * 构造展示用 github 源文档位置。
     *
     * <p>路径：{@code <user>/<repo>/<root>/<file>}，其中 user/repo/root 均转为小写
     * 以满足 {@link Identifier} 字符集要求（仅作展示与注册表键，资源解析走状态数据）。</p>
     */
    public static Identifier toDisplayLocation(GitHubDocUri uri, String fileArgument) {
        String normalizedRoot = uri.resourceRoot().toLowerCase();
        String path = uri.user().toLowerCase() + "/" + uri.repo().toLowerCase();
        if (!normalizedRoot.isBlank()) {
            path += "/" + normalizedRoot;
        }
        if (!fileArgument.isBlank()) {
            path += "/" + fileArgument;
        }
        return Identifier.fromNamespaceAndPath(GitHubAssetResolver.GITHUB_NAMESPACE, path);
    }

    private static void loadingScreenMarkFailed(Minecraft minecraft) {
        if (minecraft.screen instanceof GitHubLoadingScreen loadingScreen) {
            loadingScreen.markFailed();
        }
    }

    private static void showFailure() {
        Minecraft minecraft = Minecraft.getInstance();
        minecraft.execute(() -> {
            if (minecraft.screen instanceof GitHubLoadingScreen loadingScreen) {
                loadingScreen.markFailed();
                return;
            }
            GitHubLoadingScreen screen = GitHubLoadingScreen.open();
            screen.markFailed();
        });
    }
}
