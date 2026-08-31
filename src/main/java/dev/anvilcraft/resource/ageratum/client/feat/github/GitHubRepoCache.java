package dev.anvilcraft.resource.ageratum.client.feat.github;

import com.mojang.logging.LogUtils;
import net.neoforged.fml.loading.FMLLoader;
import org.slf4j.Logger;

import javax.annotation.Nullable;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.StandardCopyOption;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

/**
 * GitHub 仓库缓存：负责下载指定 commit 的 zip 包并解压到
 * {@code caches/ageratum/repos/<user>-<repo>/} 目录。
 *
 * <p>解压时会剥离 zip 内不确定的顶层目录（{@code <user>-<repo>-<sha>}），
 * 使缓存目录直接对应仓库根。使用 {@code .commit} 标记文件记录已缓存 commit，
 * 相同 commit 直接命中缓存，避免重复下载。</p>
 *
 * <p>所有下载/解压均在异步线程执行，调用方通过 {@link CompletableFuture} 消费结果。</p>
 */
public final class GitHubRepoCache {
    private static final Logger LOGGER = LogUtils.getLogger();

    /**
     * 缓存根目录（游戏运行目录下）。
     */
    public static Path cacheRoot() {
        return FMLLoader.getCurrent().getGameDir()
            .resolve("caches")
            .resolve("ageratum")
            .resolve("repos");
    }

    /**
     * 记录已缓存 commit 的标记文件名。
     */
    private static final String COMMIT_MARKER = ".commit";

    /**
     * 当前已打开的 github 源文档位置 → 仓库状态（用于图片/结构资源解析）。
     */
    private static final Map<net.minecraft.resources.Identifier, RepoState> ACTIVE_STATES = new ConcurrentHashMap<>();

    private GitHubRepoCache() {
    }

    /**
     * 获取与 github 源文档位置关联的仓库状态（已就绪）。
     */
    @Nullable
    public static RepoState getActiveState(net.minecraft.resources.Identifier location) {
        return ACTIVE_STATES.get(location);
    }

    /**
     * 注册 github 源文档位置与仓库状态的关联（打开文档时调用）。
     */
    public static void registerActiveState(net.minecraft.resources.Identifier location, RepoState state) {
        ACTIVE_STATES.put(location, state);
    }

    /**
     * 移除 github 源文档位置与仓库状态的关联（界面关闭时调用）。
     */
    public static void unregisterActiveState(net.minecraft.resources.Identifier location) {
        ACTIVE_STATES.remove(location);
    }

    /**
     * 确保指定 URI 对应的仓库 commit 已下载并解压。
     *
     * <p>返回的 future 完成时：{@code state != null} 表示成功
     * （{@code state.root()} 为仓库根目录，{@code state.commit()} 为实际使用的 commit），
     * {@code null} 表示所有尝试均失败。</p>
     *
     * <p>行为：</p>
     * <ul>
     *   <li>已生效缓存（commit 匹配）或待切换缓存（pending 完整）→ 立即返回，不发起网络请求；</li>
     *   <li>未缓存且 URI 显式指定 commit → 下载该 commit 后返回；</li>
     *   <li>未缓存且 URI 缺省 commit → 解析最新 commit 并下载后返回。</li>
     * </ul>
     *
     * @param uri 已解析的 GitHub 指南 URI
     */
    public static CompletableFuture<RepoState> ensureDownloaded(GitHubDocUri uri) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                return downloadAndExtract(uri);
            } catch (Exception exception) {
                LOGGER.warn("Failed to prepare GitHub repo {}/{}", uri.user(), uri.repo(), exception);
                return null;
            }
        });
    }

    /**
     * 静默检查更新：缺省 commit 时在后台解析最新 commit，与当前生效缓存不同则下载到
     * 待切换目录，不打扰当前展示；下次 {@link #ensureDownloaded} 时自动切换生效。
     *
     * @param uri 已解析的 GitHub 指南 URI（缺省 commit）
     */
    public static void refreshInBackground(GitHubDocUri uri) {
        if (uri.commit() != null) {
            return; // 显式 commit 无需静默更新
        }
        CompletableFuture.runAsync(() -> {
            try {
                refreshQuietly(uri);
            } catch (Exception exception) {
                LOGGER.debug("Silent refresh failed for {}/{}", uri.user(), uri.repo(), exception);
            }
        });
    }

    // ── 主流程 ──────────────────────────────────────────────────────────

    @Nullable
    private static RepoState downloadAndExtract(GitHubDocUri uri) {
        Path repoDir = cacheRoot().resolve(sanitizeDirName(uri.user()) + "-" + sanitizeDirName(uri.repo()));

        // 1) 待切换缓存：上次静默下载完成，直接切换生效
        if (activatePendingIfReady(repoDir, uri)) {
            RepoState state = buildState(uri, repoDir);
            if (state != null) {
                return state;
            }
        }

        String targetCommit = uri.commit();
        if (targetCommit == null) {
            targetCommit = GitHubApiClient.resolveLatestCommit(uri.user(), uri.repo());
            if (targetCommit == null) {
                LOGGER.warn("Failed to resolve latest commit for {}/{}", uri.user(), uri.repo());
                return null;
            }
        }

        String cachedCommit = readCommitMarker(repoDir);
        if (targetCommit.equals(cachedCommit) && isNonEmptyDirectory(repoDir)) {
            return buildState(uri, repoDir);
        }

        if (!extractZipForCommit(uri.user(), uri.repo(), targetCommit, repoDir)) {
            return null;
        }

        writeCommitMarker(repoDir, targetCommit);
        LOGGER.info("GitHub repo {}/{}@{} cached at {}", uri.user(), uri.repo(), targetCommit, repoDir);
        return buildState(uri, repoDir);
    }

    /**
     * 静默更新：解析最新 commit，与当前生效缓存不同则下载到 pending 目录。
     */
    private static void refreshQuietly(GitHubDocUri uri) {
        Path repoDir = cacheRoot().resolve(sanitizeDirName(uri.user()) + "-" + sanitizeDirName(uri.repo()));
        String latestCommit = GitHubApiClient.resolveLatestCommit(uri.user(), uri.repo());
        if (latestCommit == null) {
            return;
        }
        String cachedCommit = readCommitMarker(repoDir);
        if (latestCommit.equals(cachedCommit) && isNonEmptyDirectory(repoDir)) {
            return; // 已是最新，无需更新
        }
        // 已在下载中（pending 标记指向同一 commit）则跳过
        if (latestCommit.equals(readPendingCommitMarker(repoDir)) && isNonEmptyDirectory(pendingDir(repoDir))) {
            return;
        }
        if (extractZipForCommit(uri.user(), uri.repo(), latestCommit, pendingDir(repoDir))) {
            writePendingCommitMarker(repoDir, latestCommit);
            LOGGER.info("Silent refresh ready for {}/{}@{} (applies on next open)", uri.user(), uri.repo(), latestCommit);
        }
    }

    /**
     * 若 pending 目录完整且与目标 commit 匹配，则切换为生效缓存。
     */
    private static boolean activatePendingIfReady(Path repoDir, GitHubDocUri uri) {
        String targetCommit = uri.commit();
        if (targetCommit == null) {
            // 缺省 commit：pending 存在且完整即可切换
            String pendingCommit = readPendingCommitMarker(repoDir);
            if (pendingCommit == null || !isNonEmptyDirectory(pendingDir(repoDir))) {
                return false;
            }
            targetCommit = pendingCommit;
        } else {
            // 显式 commit：pending 必须匹配该 commit
            String pendingCommit = readPendingCommitMarker(repoDir);
            if (!targetCommit.equals(pendingCommit) || !isNonEmptyDirectory(pendingDir(repoDir))) {
                return false;
            }
        }

        deleteRecursively(repoDir);
        try {
            Files.move(pendingDir(repoDir), repoDir, StandardCopyOption.ATOMIC_MOVE);
        } catch (IOException atomicFailure) {
            LOGGER.debug("Atomic pending activation failed, fallback to plain move", atomicFailure);
            try {
                Files.move(pendingDir(repoDir), repoDir);
            } catch (IOException exception) {
                LOGGER.warn("Failed to activate pending repo for {}", repoDir, exception);
                return false;
            }
        }
        writeCommitMarker(repoDir, targetCommit);
        deletePendingMarker(repoDir);
        LOGGER.info("Activated pending GitHub repo {}/{}@{}", uri.user(), uri.repo(), targetCommit);
        return true;
    }

    @Nullable
    private static RepoState buildState(GitHubDocUri uri, Path repoDir) {
        String commit = readCommitMarker(repoDir);
        if (commit == null || !isNonEmptyDirectory(repoDir)) {
            return null;
        }
        return new RepoState(
            uri.user(),
            uri.repo(),
            repoDir,
            resolveResourceRoot(repoDir, uri.resourceRoot()),
            commit
        );
    }

    // ── pending 目录与标记 ──────────────────────────────────────────────

    private static Path pendingDir(Path repoDir) {
        return repoDir.resolveSibling(repoDir.getFileName() + ".pending");
    }

    private static final String PENDING_COMMIT_MARKER = ".commit.pending";

    @Nullable
    private static String readPendingCommitMarker(Path repoDir) {
        Path marker = pendingDir(repoDir).resolve(PENDING_COMMIT_MARKER);
        if (!Files.isRegularFile(marker)) {
            return null;
        }
        try {
            String content = Files.readString(marker, StandardCharsets.UTF_8).trim();
            return content.isEmpty() ? null : content;
        } catch (IOException exception) {
            return null;
        }
    }

    private static void writePendingCommitMarker(Path repoDir, String commit) {
        try {
            Files.writeString(pendingDir(repoDir).resolve(PENDING_COMMIT_MARKER), commit, StandardCharsets.UTF_8);
        } catch (IOException exception) {
            LOGGER.warn("Failed to write pending commit marker for {}", repoDir, exception);
        }
    }

    private static void deletePendingMarker(Path repoDir) {
        try {
            Files.deleteIfExists(pendingDir(repoDir).resolve(PENDING_COMMIT_MARKER));
        } catch (IOException exception) {
            LOGGER.debug("Failed to delete pending marker for {}", repoDir, exception);
        }
    }

    /**
     * 计算资源根相对仓库根的路径（如 {@code assets/advanced_clover}）。
     */
    public static String relativizeRoot(RepoState state) {
        try {
            return state.root().relativize(state.resourceRoot()).toString().replace('\\', '/');
        } catch (Exception exception) {
            return "";
        }
    }

    private static Path resolveResourceRoot(Path repoRoot, String resourceRootRelative) {
        if (resourceRootRelative == null || resourceRootRelative.isBlank()) {
            return repoRoot;
        }
        return repoRoot.resolve(resourceRootRelative).normalize();
    }

    /**
     * 下载并解压指定 commit 的 zip 包到目标目录。
     *
     * <p>目标目录既可以是正式缓存目录（{@code <dir>}），也可以是待切换目录
     * （{@code <dir>.pending}）；下载解压完成后以原子方式替换目标目录。</p>
     *
     * @param user     仓库所有者
     * @param repo     仓库名
     * @param commit   commit SHA
     * @param targetDir 目标目录（下载内容最终落点）
     * @return 成功返回 {@code true}
     */
    private static boolean extractZipForCommit(String user, String repo, String commit, Path targetDir) {
        try {
            Files.createDirectories(cacheRoot());
        } catch (IOException exception) {
            LOGGER.warn("Failed to create cache root {}", cacheRoot(), exception);
            return false;
        }

        Path zipFile = cacheRoot().resolve(targetDir.getFileName() + ".zip");
        try {
            Files.deleteIfExists(zipFile);
        } catch (IOException exception) {
            LOGGER.warn("Failed to clean stale zip {}", zipFile, exception);
        }

        if (!GitHubApiClient.downloadZipball(zipFile, user, repo, commit)) {
            return false;
        }

        // 先解压到临时目录，成功后再原子替换目标目录
        Path tempDir = targetDir.resolveSibling(targetDir.getFileName() + ".tmp");
        deleteRecursively(tempDir);
        try {
            Files.createDirectories(tempDir);
        } catch (IOException exception) {
            LOGGER.warn("Failed to create temp dir {}", tempDir, exception);
            return false;
        }

        boolean extracted;
        try {
            extracted = extractZip(zipFile, tempDir);
        } finally {
            try {
                Files.deleteIfExists(zipFile);
            } catch (IOException exception) {
                LOGGER.debug("Failed to delete zip {}", zipFile, exception);
            }
        }
        if (!extracted) {
            deleteRecursively(tempDir);
            return false;
        }

        deleteRecursively(targetDir);
        try {
            Files.move(tempDir, targetDir, StandardCopyOption.ATOMIC_MOVE);
        } catch (IOException atomicFailure) {
            LOGGER.debug("Atomic move failed, fallback to plain move", atomicFailure);
            try {
                Files.move(tempDir, targetDir);
            } catch (IOException exception) {
                LOGGER.warn("Failed to move extracted repo into place", exception);
                deleteRecursively(tempDir);
                return false;
            }
        }
        return true;
    }

    /**
     * 解压 zip 到目标目录，剥离顶层单一目录（GitHub zipball 的固定布局）。
     *
     * @return 全部成功返回 {@code true}
     */
    private static boolean extractZip(Path zipFile, Path targetDir) {
        try (ZipInputStream zip = new ZipInputStream(Files.newInputStream(zipFile), StandardCharsets.UTF_8)) {
            String topLevel = null;
            ZipEntry entry;
            while ((entry = zip.getNextEntry()) != null) {
                if (entry.isDirectory()) {
                    continue;
                }
                String name = entry.getName().replace('\\', '/');
                if (name.startsWith("__MACOSX/")) {
                    continue;
                }
                if (topLevel == null) {
                    int slash = name.indexOf('/');
                    topLevel = slash >= 0 ? name.substring(0, slash) : "";
                }
                String relative = name;
                if (!topLevel.isEmpty() && relative.startsWith(topLevel + "/")) {
                    relative = relative.substring(topLevel.length() + 1);
                }
                if (relative.isEmpty()) {
                    continue;
                }
                Path output = resolveSafely(targetDir, relative);
                if (output == null) {
                    LOGGER.warn("Zip entry escapes target dir, skipped: {}", name);
                    continue;
                }
                Files.createDirectories(output.getParent());
                try (OutputStream out = Files.newOutputStream(output)) {
                    zip.transferTo(out);
                }
            }
            return true;
        } catch (IOException exception) {
            LOGGER.warn("Failed to extract zip {}", zipFile, exception);
            return false;
        }
    }

    /**
     * 将相对路径安全地解析到目标目录内，防止 Zip Slip。
     *
     * @return 越界时返回 {@code null}
     */
    @Nullable
    private static Path resolveSafely(Path base, String relative) {
        Path resolved = base.resolve(relative).normalize();
        if (!resolved.startsWith(base.normalize())) {
            return null;
        }
        return resolved;
    }

    // ── 标记文件与工具 ──────────────────────────────────────────────────

    @Nullable
    private static String readCommitMarker(Path repoDir) {
        Path marker = repoDir.resolve(COMMIT_MARKER);
        if (!Files.isRegularFile(marker)) {
            return null;
        }
        try {
            String content = Files.readString(marker, StandardCharsets.UTF_8).trim();
            return content.isEmpty() ? null : content;
        } catch (IOException exception) {
            return null;
        }
    }

    private static void writeCommitMarker(Path repoDir, String commit) {
        try {
            Files.writeString(repoDir.resolve(COMMIT_MARKER), commit, StandardCharsets.UTF_8);
        } catch (IOException exception) {
            LOGGER.warn("Failed to write commit marker for {}", repoDir, exception);
        }
    }

    private static boolean isNonEmptyDirectory(Path dir) {
        if (!Files.isDirectory(dir)) {
            return false;
        }
        try (var stream = Files.list(dir)) {
            return stream.findAny().isPresent();
        } catch (IOException exception) {
            return false;
        }
    }

    private static void deleteRecursively(Path dir) {
        if (!Files.exists(dir)) {
            return;
        }
        try {
            Files.walkFileTree(dir, new SimpleFileVisitor<>() {
                @Override
                public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                    Files.delete(file);
                    return FileVisitResult.CONTINUE;
                }

                @Override
                public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {
                    Files.delete(dir);
                    return FileVisitResult.CONTINUE;
                }
            });
        } catch (IOException exception) {
            LOGGER.debug("Failed to delete {}", dir, exception);
        }
    }

    private static String sanitizeDirName(String name) {
        String sanitized = name.replace('\\', '_').replace('/', '_');
        if (sanitized.isBlank()) {
            return "unknown";
        }
        return sanitized;
    }

    /**
     * 已就绪的仓库状态。
     *
     * @param user         仓库所有者
     * @param repo         仓库名
     * @param root         仓库根目录（zip 顶层目录已剥离）
     * @param resourceRoot 资源根目录（{@code path} 参数指向的目录；缺省为仓库根）
     * @param commit       实际使用的 commit SHA
     */
    public record RepoState(
        String user,
        String repo,
        Path root,
        Path resourceRoot,
        String commit
    ) {
    }
}
