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

    // ── 主流程 ──────────────────────────────────────────────────────────

    @Nullable
    private static RepoState downloadAndExtract(GitHubDocUri uri) {
        String targetCommit = uri.commit();
        if (targetCommit == null) {
            targetCommit = GitHubApiClient.resolveLatestCommit(uri.user(), uri.repo());
            if (targetCommit == null) {
                LOGGER.warn("Failed to resolve latest commit for {}/{}", uri.user(), uri.repo());
                return null;
            }
        }

        Path repoDir = cacheRoot().resolve(sanitizeDirName(uri.user()) + "-" + sanitizeDirName(uri.repo()));
        String cachedCommit = readCommitMarker(repoDir);
        if (targetCommit.equals(cachedCommit) && isNonEmptyDirectory(repoDir)) {
            return new RepoState(
                uri.user(),
                uri.repo(),
                repoDir,
                resolveResourceRoot(repoDir, uri.resourceRoot()),
                targetCommit
            );
        }

        if (!extractZipForCommit(uri.user(), uri.repo(), targetCommit, repoDir)) {
            return null;
        }

        writeCommitMarker(repoDir, targetCommit);
        LOGGER.info("GitHub repo {}/{}@{} cached at {}", uri.user(), uri.repo(), targetCommit, repoDir);
        return new RepoState(
            uri.user(),
            uri.repo(),
            repoDir,
            resolveResourceRoot(repoDir, uri.resourceRoot()),
            targetCommit
        );
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
     * 下载并解压指定 commit 的 zip 包；成功返回 {@code true}。
     */
    private static boolean extractZipForCommit(String user, String repo, String commit, Path repoDir) {
        try {
            Files.createDirectories(cacheRoot());
        } catch (IOException exception) {
            LOGGER.warn("Failed to create cache root {}", cacheRoot(), exception);
            return false;
        }

        Path zipFile = cacheRoot().resolve(repoDir.getFileName() + ".zip");
        try {
            Files.deleteIfExists(zipFile);
        } catch (IOException exception) {
            LOGGER.warn("Failed to clean stale zip {}", zipFile, exception);
        }

        if (!GitHubApiClient.downloadZipball(zipFile, user, repo, commit)) {
            return false;
        }

        // 先解压到临时目录，成功后再原子替换目标目录
        Path tempDir = repoDir.resolveSibling(repoDir.getFileName() + ".tmp");
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

        deleteRecursively(repoDir);
        try {
            Files.move(tempDir, repoDir, StandardCopyOption.ATOMIC_MOVE);
        } catch (IOException atomicFailure) {
            LOGGER.debug("Atomic move failed, fallback to plain move", atomicFailure);
            try {
                Files.move(tempDir, repoDir);
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
