package dev.anvilcraft.resource.ageratum.client.feat.github;

import dev.anvilcraft.resource.ageratum.client.util.RelativePathResolver;
import net.minecraft.resources.Identifier;

import javax.annotation.Nullable;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * GitHub 远程指南资源解析器。
 *
 * <p>将文档中的资源引用（图片、结构等）映射到已下载仓库的本地文件：</p>
 * <ul>
 *   <li>{@code advanced_clover:textures/foo.png} —— 命名空间为资源根目录名，
 *       路径相对于资源根（即仓库中 {@code path:assets/advanced_clover} 指向的目录）；</li>
 *   <li>{@code ./foo.png}、{@code ../bar/x.nbt} —— 相对路径，先相对于当前文档所在目录，
 *       再回退到资源根；</li>
 *   <li>{@code ageratum/...} —— 无命名空间时按相对路径处理。</li>
 * </ul>
 *
 * <p>github 源文档使用特殊命名空间 {@code github:} 标记，
 * 通过 {@link #isGitHubLocation(Identifier)} 判断。</p>
 */
public final class GitHubAssetResolver {
    /**
     * github 源文档的命名空间。
     */
    public static final String GITHUB_NAMESPACE = "github";

    /**
     * 文档在资源根下的根目录名。
     */
    public static final String GUIDE_ROOT = "ageratum";

    private GitHubAssetResolver() {
    }

    /**
     * 判断一个资源位置是否属于 GitHub 远程指南源。
     */
    public static boolean isGitHubLocation(Identifier location) {
        return GITHUB_NAMESPACE.equals(location.getNamespace());
    }

    /**
     * 判断一个原始字符串是否是 GitHub 远程指南 URI。
     */
    public static boolean isGitHubLocation(@Nullable String raw) {
        return GitHubDocUri.isGitHubDoc(raw);
    }

    /**
     * 根据仓库状态与当前文档位置解析资源文件。
     *
     * @param state          已就绪的仓库状态（含资源根）
     * @param sourceLocation 当前文档的位置（github 源）
     * @param rawTarget      资源引用（如 {@code textures/foo.png}、{@code advanced_clover:textures/foo.png}）
     * @return 存在的本地文件路径，无法解析或文件不存在时返回 {@code null}
     */
    @Nullable
    public static Path resolve(GitHubRepoCache.RepoState state, Identifier sourceLocation, String rawTarget) {
        if (rawTarget == null || rawTarget.isBlank()) {
            return null;
        }
        String target = rawTarget.trim().replace('\\', '/');
        while (target.startsWith("/")) {
            target = target.substring(1);
        }
        if (target.isEmpty()) {
            return null;
        }

        Path resourceRoot = state.resourceRoot();

        // 1) 显式命名空间：<resourceRootName>:path
        int colon = target.indexOf(':');
        if (colon > 0) {
            String namespace = target.substring(0, colon);
            String path = target.substring(colon + 1);
            Path direct = resolveWithinRoot(resourceRoot, "", path);
            if (direct != null && Files.isRegularFile(direct)) {
                return direct;
            }
            // 命名空间匹配当前资源根时，也允许相对于当前文档目录
            if (namespace.equals(resourceRoot.getFileName().toString())) {
                Path relativeToDoc = resolveWithinRoot(resourceRoot, currentDocumentDirectory(sourceLocation), path);
                if (relativeToDoc != null && Files.isRegularFile(relativeToDoc)) {
                    return relativeToDoc;
                }
            }
            return null;
        }

        // 2) 相对路径：先相对当前文档目录，再相对资源根
        Path relativeToDoc = resolveWithinRoot(resourceRoot, currentDocumentDirectory(sourceLocation), target);
        if (relativeToDoc != null && Files.isRegularFile(relativeToDoc)) {
            return relativeToDoc;
        }
        Path relativeToRoot = resolveWithinRoot(resourceRoot, "", target);
        if (relativeToRoot != null && Files.isRegularFile(relativeToRoot)) {
            return relativeToRoot;
        }
        return null;
    }

    /**
     * 解析 github 源文档对应的文档文件路径（如 {@code ageratum/zh_cn/index.md}）。
     *
     * @param state          已就绪的仓库状态（含资源根）
     * @param sourceLocation 文档位置（github 源）
     * @param fileArgument   文件名（可为 {@code null} 表示 index）
     * @return 存在的文件路径，否则 {@code null}
     */
    @Nullable
    public static Path resolveDocument(GitHubRepoCache.RepoState state, Identifier sourceLocation, @Nullable String fileArgument) {
        String normalizedFile = normalizeFileArgument(fileArgument);
        Path root = state.resourceRoot();

        List<Path> candidates = new ArrayList<>();
        String languageCode = extractLanguageCode(sourceLocation);
        if (languageCode != null) {
            candidates.add(root.resolve(GUIDE_ROOT).resolve(languageCode).resolve(normalizedFile));
        }
        candidates.add(root.resolve(GUIDE_ROOT).resolve("en_us").resolve(normalizedFile));
        candidates.add(root.resolve(GUIDE_ROOT).resolve(normalizedFile));
        candidates.add(root.resolve(normalizedFile));
        for (Path candidate : candidates) {
            if (Files.isRegularFile(candidate)) {
                return candidate;
            }
        }
        return null;
    }

    /**
     * 从 github 源文档位置提取语言代码（{@code ageratum/<lang>/...}）。
     */
    @Nullable
    public static String extractLanguageCode(Identifier sourceLocation) {
        String path = sourceLocation.getPath();
        String[] segments = path.split("/");
        // 从后往前找最后一个 ageratum 段（资源根也可能叫 ageratum）
        for (int i = segments.length - 2; i >= 0; i--) {
            if (GUIDE_ROOT.equals(segments[i])) {
                String language = segments[i + 1];
                return language.isBlank() ? null : language.toLowerCase();
            }
        }
        return null;
    }

    // ── 内部工具 ────────────────────────────────────────────────────────

    @Nullable
    private static Path resolveWithinRoot(Path root, String baseDir, String target) {
        String combined = baseDir.isBlank() ? target : baseDir + "/" + target;
        String normalized = RelativePathResolver.resolveWithinBase("", combined);
        if (normalized.isBlank()) {
            return null;
        }
        Path resolved = root.resolve(normalized).normalize();
        if (!resolved.startsWith(root.normalize())) {
            return null;
        }
        return resolved;
    }

    /**
     * 当前文档在资源根内的相对目录（路径为 {@code ageratum/<lang>/<file>.md} 结构）。
     */
    private static String currentDocumentDirectory(Identifier sourceLocation) {
        String path = sourceLocation.getPath();
        // github 源文档路径格式：<user>/<repo>/<root>/ageratum/<lang>/<file>.md
        // 去除 user/repo 与资源根前缀，得到 ageratum/<lang>/<file>.md
        int guideIndex = path.indexOf("/" + GUIDE_ROOT + "/");
        String relative;
        if (guideIndex >= 0) {
            relative = path.substring(guideIndex + 1);
        } else {
            relative = path;
        }
        int slash = relative.lastIndexOf('/');
        if (slash < 0) {
            return "";
        }
        return relative.substring(0, slash);
    }

    private static String normalizeFileArgument(@Nullable String fileArgument) {
        String file = fileArgument;
        if (file == null || file.isBlank()) {
            file = "index";
        }
        file = file.trim().replace('\\', '/');
        while (file.startsWith("/")) {
            file = file.substring(1);
        }
        if (!file.endsWith(".md")) {
            file += ".md";
        }
        return file;
    }
}
