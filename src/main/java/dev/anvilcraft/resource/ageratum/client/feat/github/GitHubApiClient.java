package dev.anvilcraft.resource.ageratum.client.feat.github;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.logging.LogUtils;
import org.slf4j.Logger;

import javax.annotation.Nullable;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;

/**
 * GitHub API 客户端。
 *
 * <p>负责通过 GitHub REST API 解析默认分支最新 commit 并下载仓库 zip 包。
 * 请求携带 OAuth APP 的 Client ID（{@link #CLIENT_ID}），并在 {@code api.github.com}
 * 失败时依次回退到内置的社区代理前缀。</p>
 *
 * <p>代理可用性随时可能变化，因此每个 host 的响应都会经过严格校验
 * （状态码、Content-Type、JSON/zip 魔数），不满足即视为失败并继续下一个。</p>
 */
public final class GitHubApiClient {
    private static final Logger LOGGER = LogUtils.getLogger();

    /**
     * OAuth APP Client ID（匿名请求身份标识）。
     */
    public static final String CLIENT_ID = "Ov23lisVnunOyzPZWLpo";

    /**
     * GitHub API 请求超时（毫秒）。
     */
    private static final int CONNECT_TIMEOUT_MS = 10_000;

    /**
     * GitHub API 读取超时（毫秒）。
     */
    private static final int READ_TIMEOUT_MS = 60_000;

    /**
     * 请求 User-Agent（GitHub API 强制要求）。
     */
    private static final String USER_AGENT = "Ageratum-Mod/1.0";

    /**
     * API host 列表：直连优先，随后为社区代理前缀。
     */
    public static final List<String> API_HOSTS = List.of(
        "https://api.github.com",
        "https://ghfast.top/https://api.github.com",
        "https://gh-proxy.com/https://api.github.com",
        "https://ghproxy.net/https://api.github.com",
        "https://ghproxy.cc/https://api.github.com",
        "https://mirror.ghproxy.com/https://api.github.com",
        "https://github.moeyy.xyz/https://api.github.com",
        "https://gitproxy.click/https://api.github.com",
        "https://gh.ddlc.top/https://api.github.com",
        "https://ghps.cc/https://api.github.com"
    );

    private GitHubApiClient() {
    }

    /**
     * 获取仓库默认分支的最新 commit SHA。
     *
     * @param user 仓库所有者
     * @param repo 仓库名
     * @return 最新 commit SHA；全部 host 失败时返回 {@code null}
     */
    @Nullable
    public static String resolveLatestCommit(String user, String repo) {
        String defaultBranch = tryHosts(host -> fetchDefaultBranch(host, user, repo));
        if (defaultBranch == null) {
            return null;
        }
        return tryHosts(host -> fetchBranchHeadCommit(host, user, repo, defaultBranch));
    }

    /**
     * 下载指定 commit 的仓库 zip 包到目标文件。
     *
     * <p>GitHub API 会返回 302 跳转到 {@code codeload.github.com}，此处跟随重定向。</p>
     *
     * @param destFile 目标 zip 文件路径
     * @param user     仓库所有者
     * @param repo     仓库名
     * @param commit   commit SHA 或分支名
     * @return 下载成功返回 {@code true}，全部 host 失败返回 {@code false}
     */
    public static boolean downloadZipball(Path destFile, String user, String repo, String commit) {
        return tryHosts(host -> fetchZipball(host, destFile, user, repo, commit));
    }

    // ── 单 host 实现 ────────────────────────────────────────────────────

    @Nullable
    private static String fetchDefaultBranch(String host, String user, String repo) {
        JsonObject root = getJson(host + "/repos/" + user + "/" + repo);
        if (root == null) {
            return null;
        }
        String branch = getString(root, "default_branch");
        if (branch == null || branch.isBlank()) {
            LOGGER.warn("GitHub API {}: missing default_branch", host);
            return null;
        }
        return branch;
    }

    @Nullable
    private static String fetchBranchHeadCommit(String host, String user, String repo, String branch) {
        JsonObject root = getJson(host + "/repos/" + user + "/" + repo + "/commits/" + branch);
        if (root == null) {
            return null;
        }
        String sha = getString(root, "sha");
        if (sha == null || sha.isBlank()) {
            LOGGER.warn("GitHub API {}: missing commit sha", host);
            return null;
        }
        return sha;
    }

    private static boolean fetchZipball(String host, Path destFile, String user, String repo, String commit) {
        String url = host + "/repos/" + user + "/" + repo + "/zipball/" + commit;
        HttpURLConnection connection = null;
        try {
            connection = openConnection(url);
            connection.setInstanceFollowRedirects(true);
            connection.setRequestMethod("GET");
            int status = connection.getResponseCode();
            if (status < 200 || status >= 300) {
                LOGGER.warn("GitHub API {}: zipball returned status {}", host, status);
                return false;
            }
            String contentType = connection.getContentType();
            if (contentType != null && contentType.toLowerCase().contains("text/html")) {
                LOGGER.warn("GitHub API {}: zipball returned HTML (challenge page?), abort", host);
                return false;
            }

            Path tempFile = destFile.resolveSibling(destFile.getFileName() + ".tmp");
            try (InputStream input = connection.getInputStream()) {
                Files.copy(input, tempFile, StandardCopyOption.REPLACE_EXISTING);
            }
            if (!isZipFile(tempFile)) {
                LOGGER.warn("GitHub API {}: downloaded file is not a valid zip", host);
                Files.deleteIfExists(tempFile);
                return false;
            }
            Files.move(tempFile, destFile, StandardCopyOption.REPLACE_EXISTING);
            LOGGER.info("Downloaded GitHub zipball from {} -> {}", host, destFile);
            return true;
        } catch (IOException exception) {
            LOGGER.debug("GitHub API {}: zipball request failed", host, exception);
            return false;
        } finally {
            if (connection != null) {
                connection.disconnect();
            }
        }
    }

    // ── HTTP 工具 ───────────────────────────────────────────────────────

    @Nullable
    private static JsonObject getJson(String url) {
        HttpURLConnection connection = null;
        try {
            connection = openConnection(url);
            connection.setRequestMethod("GET");
            int status = connection.getResponseCode();
            if (status < 200 || status >= 300) {
                LOGGER.warn("GitHub API {}: status {}", url, status);
                return null;
            }
            String contentType = connection.getContentType();
            if (contentType != null && contentType.toLowerCase().contains("text/html")) {
                LOGGER.warn("GitHub API {}: returned HTML, abort", url);
                return null;
            }
            try (InputStream input = connection.getInputStream()) {
                String body = new String(input.readAllBytes(), StandardCharsets.UTF_8);
                JsonElement element = JsonParser.parseString(body);
                if (!(element instanceof JsonObject object)) {
                    LOGGER.warn("GitHub API {}: response is not a JSON object", url);
                    return null;
                }
                return object;
            }
        } catch (Exception exception) {
            LOGGER.debug("GitHub API {}: request failed", url, exception);
            return null;
        } finally {
            if (connection != null) {
                connection.disconnect();
            }
        }
    }

    private static HttpURLConnection openConnection(String url) throws IOException {
        HttpURLConnection connection = (HttpURLConnection) URI.create(url).toURL().openConnection();
        connection.setConnectTimeout(CONNECT_TIMEOUT_MS);
        connection.setReadTimeout(READ_TIMEOUT_MS);
        connection.setRequestProperty("User-Agent", USER_AGENT);
        connection.setRequestProperty("Accept", "application/vnd.github+json");
        connection.setRequestProperty("X-GitHub-Api-Version", "2022-11-28");
        connection.setRequestProperty("X-GitHub-Client-Id", CLIENT_ID);
        return connection;
    }

    private static boolean isZipFile(Path file) {
        try (InputStream input = Files.newInputStream(file)) {
            byte[] magic = new byte[4];
            int read = input.read(magic);
            return read == 4 && magic[0] == 'P' && magic[1] == 'K' && magic[2] == 3 && magic[3] == 4;
        } catch (IOException exception) {
            return false;
        }
    }

    // ── host fallback ───────────────────────────────────────────────────

    /**
     * 依次尝试每个 API host，返回第一个成功的调用结果。
     */
    @Nullable
    private static <T> T tryHosts(Function<String, T> action) {
        for (String host : API_HOSTS) {
            try {
                T result = action.apply(host);
                if (result != null) {
                    return result;
                }
            } catch (RuntimeException exception) {
                LOGGER.debug("GitHub API host {} failed", host, exception);
            }
        }
        return null;
    }

    @Nullable
    private static String getString(JsonObject object, String key) {
        JsonElement element = object.get(key);
        if (element == null || !element.isJsonPrimitive()) {
            return null;
        }
        return element.getAsString();
    }
}
