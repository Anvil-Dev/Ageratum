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
 * <p>缓存优先：打开时若本地已有可用缓存（缺省 commit 或显式 commit 匹配），
 * 立即展示缓存内容，不显示加载界面、不阻塞网络；随后在后台静默检查更新，
 * 有新版本时下载到 pending 目录，下次打开自动生效。仅在无缓存可用时才显示
 * 「加载中」过渡界面并现场下载；全部失败显示红色 {@code 加载失败...}。</p>
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
        Minecraft minecraft = Minecraft.getInstance();

        // 1) 缓存命中：立即打开，无需等待网络
        GitHubRepoCache.RepoState cachedState = GitHubRepoCache.tryLoadCached(uri);
        if (cachedState != null) {
            if (openDocument(minecraft, uri, cachedState)) {
                // 2) 打开后后台静默检查更新：下次打开生效（不打断当前阅读）
                GitHubRepoCache.refreshInBackground(uri);
                return;
            }
            // 缓存内容异常（文档缺失等），回退到现场下载
            LOGGER.info("Cached GitHub guide {}/{} cannot be opened, fallback to download", uri.user(), uri.repo());
        }

        showLoadingAndFetch(uri);
    }

    /**
     * 无可用缓存：显示「加载中」过渡界面，异步下载后打开；全部失败显示「加载失败」。
     */
    private static void showLoadingAndFetch(GitHubDocUri uri) {
        GitHubLoadingScreen loadingScreen = GitHubLoadingScreen.open();
        GitHubRepoCache.ensureDownloaded(uri).whenComplete((state, throwable) -> {
            Minecraft minecraft = Minecraft.getInstance();
            minecraft.execute(() -> {
                if (state == null || throwable != null) {
                    LOGGER.warn("Failed to load GitHub guide {}/{}", uri.user(), uri.repo(), throwable);
                    loadingScreen.markFailed();
                    return;
                }
                boolean opened = openDocument(minecraft, uri, state);
                if (!opened) {
                    LOGGER.warn("Failed to open GitHub guide {}/{}", uri.user(), uri.repo());
                    loadingScreen.markFailed();
                }
            });
        });
    }

    /**
     * 根据已就绪的仓库状态打开文档。
     *
     * @return 打开成功返回 {@code true}；文档缺失/读取失败返回 {@code false}
     */
    private static boolean openDocument(Minecraft minecraft, GitHubDocUri uri, GitHubRepoCache.RepoState state) {
        String languageCode = AgeratumClient.getClientLanguageCode(minecraft);

        // 文档位置：github:<user>/<repo>/<root>/ageratum/<lang>/index.md
        // （user/repo/root 转为小写以满足 Identifier 字符集；资源解析走 state）
        Identifier indexLocation = toDisplayLocation(uri, "ageratum/" + languageCode + "/index.md");
        Path documentFile = GitHubAssetResolver.resolveDocument(state, indexLocation, "index");
        if (documentFile == null) {
            LOGGER.warn("No index document found for {}/{} under {}", uri.user(), uri.repo(), uri.resourceRoot());
            return false;
        }

        String markdown;
        try {
            markdown = Files.readString(documentFile, StandardCharsets.UTF_8);
        } catch (Exception exception) {
            LOGGER.warn("Failed to read document {}", documentFile, exception);
            return false;
        }

        MDDocument document = new MarkdownParser().parseDocument(indexLocation, markdown);
        GitHubRepoCache.registerActiveState(indexLocation, state);
        GuideScreen screen = new GuideScreen(indexLocation, document, List.of(), false);
        minecraft.setScreen(screen);
        return true;
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
