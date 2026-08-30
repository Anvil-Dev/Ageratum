package dev.anvilcraft.resource.ageratum.client.feat.github;

import javax.annotation.Nullable;

/**
 * GitHub 远程指南 URI。
 *
 * <p>支持如下语法（{@code github:} 前缀）：</p>
 * <ul>
 *   <li>{@code github:user/repo} —— 使用仓库根目录作为资源根，最新 commit</li>
 *   <li>{@code github:user/repo#path:assets/advanced_clover} —— 指定资源包命名空间根</li>
 *   <li>{@code github:user/repo#commit=798f631} —— 指定 commit</li>
 *   <li>{@code github:user/repo#path:assets/advanced_clover&commit=798f631} —— 组合形式</li>
 * </ul>
 *
 * <p>参数分隔符同时兼容 {@code =} 与 {@code :}（如 {@code path:assets/xxx}、
 * {@code commit:798f631}），以 {@code =} 优先。</p>
 */
public record GitHubDocUri(String user, String repo, String resourceRoot, @Nullable String commit) {

    /**
     * 资源根目录名称（用于命名空间解析与展示）。
     */
    public String rootName() {
        String normalized = this.resourceRoot.replace('\\', '/');
        while (normalized.endsWith("/")) {
            normalized = normalized.substring(0, normalized.length() - 1);
        }
        int slash = normalized.lastIndexOf('/');
        return slash >= 0 ? normalized.substring(slash + 1) : normalized;
    }

    /**
     * 判断原始值是否为 GitHub 远程指南 URI。
     */
    public static boolean isGitHubDoc(@Nullable String raw) {
        return raw != null && raw.trim().startsWith(GITHUB_PREFIX);
    }

    /**
     * 解析 GitHub 远程指南 URI。
     *
     * @return 解析成功返回 URI，否则返回 {@code null}
     */
    @Nullable
    public static GitHubDocUri parse(@Nullable String raw) {
        if (!isGitHubDoc(raw)) {
            return null;
        }
        String text = raw.trim().substring(GITHUB_PREFIX.length());
        String repoPart = text;
        String paramPart = "";

        int hash = text.indexOf('#');
        if (hash >= 0) {
            repoPart = text.substring(0, hash);
            paramPart = text.substring(hash + 1);
        }

        repoPart = repoPart.trim();
        int slash = repoPart.indexOf('/');
        if (slash <= 0 || slash == repoPart.length() - 1) {
            return null;
        }
        String user = repoPart.substring(0, slash).trim();
        String repo = repoPart.substring(slash + 1).trim();
        if (user.isEmpty() || repo.isEmpty() || user.contains("/") || repo.contains("/")) {
            return null;
        }

        String resourceRoot = "";
        String commit = null;
        for (String pair : paramPart.split("&")) {
            String trimmed = pair.trim();
            if (trimmed.isEmpty()) {
                continue;
            }
            int eq = trimmed.indexOf('=');
            int colon = trimmed.indexOf(':');
            int separator = -1;
            if (eq >= 0 && (colon < 0 || eq < colon)) {
                separator = eq;
            } else if (colon >= 0) {
                separator = colon;
            }
            if (separator <= 0 || separator == trimmed.length() - 1) {
                continue;
            }
            String key = trimmed.substring(0, separator).trim().toLowerCase();
            String value = trimmed.substring(separator + 1).trim();
            switch (key) {
                case "path" -> resourceRoot = normalizeResourceRoot(value);
                case "commit" -> {
                    if (isValidCommit(value)) {
                        commit = value;
                    }
                }
                default -> {
                }
            }
        }

        return new GitHubDocUri(user, repo, resourceRoot, commit);
    }

    private static String normalizeResourceRoot(String value) {
        String normalized = value.trim().replace('\\', '/');
        while (normalized.startsWith("/")) {
            normalized = normalized.substring(1);
        }
        while (normalized.endsWith("/")) {
            normalized = normalized.substring(0, normalized.length() - 1);
        }
        if (normalized.equals(".")) {
            return "";
        }
        // 防御：禁止向上越界
        if (normalized.contains("..")) {
            return "";
        }
        return normalized;
    }

    private static boolean isValidCommit(String value) {
        if (value == null || value.isBlank()) {
            return false;
        }
        if (value.contains("/") || value.contains("\\") || value.contains("..")) {
            return false;
        }
        for (int i = 0; i < value.length(); i++) {
            char ch = value.charAt(i);
            boolean valid = (ch >= 'a' && ch <= 'z')
                            || (ch >= 'A' && ch <= 'Z')
                            || (ch >= '0' && ch <= '9')
                            || ch == '-' || ch == '_' || ch == '.';
            if (!valid) {
                return false;
            }
        }
        return true;
    }

    private static final String GITHUB_PREFIX = "github:";
}
