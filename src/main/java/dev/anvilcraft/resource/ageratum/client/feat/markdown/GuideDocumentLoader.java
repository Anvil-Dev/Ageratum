package dev.anvilcraft.resource.ageratum.client.feat.markdown;

import dev.anvilcraft.resource.ageratum.client.constants.AgeratumConstants;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.TreeSet;
import javax.annotation.Nullable;

/**
 * 文档加载工具类，负责从资源包中读取 Markdown 文档。
 *
 * <p>文档路径约定：{@code assets/<namespace>/ageratum/<languageCode>/<file>.md}</p>
 *
 * <p>该类为纯工具类，不可实例化。</p>
 */
public final class GuideDocumentLoader {
    /**
     * Markdown 文档在各命名空间内的根目录名称。
     */
    private static final String GUIDE_ROOT = AgeratumConstants.Guide.ROOT_FOLDER;

    /**
     * 默认语言目录。
     */
    public static final String DEFAULT_LANGUAGE_CODE = AgeratumConstants.I18n.DEFAULT_LANGUAGE_CODE;

    /**
     * 工具类，禁止实例化。
     */
    private GuideDocumentLoader() {
    }

    /**
     * 将命名空间与文件参数组合为标准 {@link ResourceLocation}。
     *
     * <p>该重载默认使用 {@link #DEFAULT_LANGUAGE_CODE} 目录。</p>
     */
    public static ResourceLocation toDocumentLocation(String namespace, String fileArgument) {
        return toDocumentLocation(namespace, DEFAULT_LANGUAGE_CODE, fileArgument);
    }

    /**
     * 将命名空间、语言代码与文件参数组合为标准 {@link ResourceLocation}。
     *
     * <p>文件名会经过规范化处理（见 {@link #normalizeFileArgument}）:
     * 缺省时使用 {@code index}，自动补全 {@code .md} 后缀。</p>
     *
     * @param namespace    文档所属命名空间
     * @param languageCode 语言代码（如 {@code en_us}、{@code zh_cn}）
     * @param fileArgument 文件名参数（可为 {@code null} 或空字符串）
     * @return 指向该文档的资源位置，格式为
     * {@code namespace:ageratum/<languageCode>/<normalizedFile>.md}
     */
    public static ResourceLocation toDocumentLocation(String namespace, String languageCode, @Nullable String fileArgument) {
        String normalizedLanguage = normalizeLanguageCode(languageCode);
        String normalizedFile = normalizeFileArgument(fileArgument);
        return ResourceLocation.fromNamespaceAndPath(namespace, GUIDE_ROOT + "/" + normalizedLanguage + "/" + normalizedFile);
    }

    /**
     * 按“当前语言 -> en_us”顺序解析第一个存在的文档位置。
     */
    public static Optional<ResourceLocation> resolveExistingLocation(
        ResourceManager resourceManager,
        String namespace,
        String languageCode,
        @Nullable String fileArgument
    ) {
        String normalizedLanguage = normalizeLanguageCode(languageCode);
        List<ResourceLocation> candidates = new ArrayList<>();
        candidates.add(toDocumentLocation(namespace, normalizedLanguage, fileArgument));
        if (!DEFAULT_LANGUAGE_CODE.equals(normalizedLanguage)) {
            candidates.add(toDocumentLocation(namespace, DEFAULT_LANGUAGE_CODE, fileArgument));
        }
        for (ResourceLocation location : candidates) {
            if (exists(resourceManager, location)) {
                return Optional.of(location);
            }
        }
        return Optional.empty();
    }

    /**
     * 检查指定资源位置的文档是否存在于当前资源包中。
     *
     * @param resourceManager 当前游戏资源管理器
     * @param location        要检查的资源位置
     * @return 存在返回 {@code true}，否则返回 {@code false}
     */
    public static boolean exists(ResourceManager resourceManager, ResourceLocation location) {
        return resourceManager.getResource(location).isPresent();
    }

    /**
     * 从资源包中读取指定文档的全部文本内容（UTF-8 编码）。
     *
     * @param resourceManager 当前游戏资源管理器
     * @param location        目标文档的资源位置
     * @return 文档的完整文本内容
     * @throws IllegalArgumentException 若文档不存在
     * @throws UncheckedIOException     若读取时发生 I/O 错误
     */
    public static String read(ResourceManager resourceManager, ResourceLocation location) {
        Resource resource = resourceManager.getResource(location)
            .orElseThrow(() -> new IllegalArgumentException("Missing guide: " + location));
        try (var stream = resource.open()) {
            return new String(stream.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException exception) {
            throw new UncheckedIOException(exception);
        }
    }

    /**
     * 枚举所有资源包中含有 {@code ageratum/*.md} 文件的命名空间列表。
     *
     * <p>返回结果按字母顺序排序，可直接用于命令补全。</p>
     *
     * @param resourceManager 当前游戏资源管理器
     * @return 包含至少一个文档的命名空间名称列表（升序排列）
     */
    public static List<String> listNamespaces(ResourceManager resourceManager, String languageCode) {
        String normalizedLanguage = normalizeLanguageCode(languageCode);
        Set<String> namespaces = new TreeSet<>(listNamespacesForLanguage(resourceManager, normalizedLanguage));
        if (!DEFAULT_LANGUAGE_CODE.equals(normalizedLanguage)) {
            namespaces.addAll(listNamespacesForLanguage(resourceManager, DEFAULT_LANGUAGE_CODE));
        }
        return new ArrayList<>(namespaces);
    }

    /**
     * 枚举指定命名空间下所有可用的文档文件名（不含 {@code .md} 后缀）。
     *
     * <p>返回结果按字母顺序排序，可直接用于命令补全。</p>
     *
     * @param resourceManager 当前游戏资源管理器
     * @param namespace       目标命名空间
     * @return 该命名空间下所有文档的相对路径列表（不含扩展名，升序排列）
     */
    public static List<String> listFiles(ResourceManager resourceManager, String namespace, String languageCode) {
        String normalizedLanguage = normalizeLanguageCode(languageCode);
        Set<String> result = new TreeSet<>(listFilesForRoot(resourceManager, namespace, GUIDE_ROOT + "/" + normalizedLanguage + "/"));
        if (!DEFAULT_LANGUAGE_CODE.equals(normalizedLanguage)) {
            result.addAll(listFilesForRoot(resourceManager, namespace, GUIDE_ROOT + "/" + DEFAULT_LANGUAGE_CODE + "/"));
        }
        return new ArrayList<>(result);
    }

    private static List<String> listNamespacesForLanguage(ResourceManager resourceManager, String languageCode) {
        return listNamespacesForRoot(resourceManager, GUIDE_ROOT + "/" + languageCode + "/");
    }

    private static List<String> listNamespacesForRoot(ResourceManager resourceManager, String rootPrefix) {
        String rootDirectory = rootPrefix.substring(0, rootPrefix.length() - 1);
        Map<ResourceLocation, Resource> files = resourceManager.listResources(
            rootDirectory,
            location -> location.getPath().startsWith(rootPrefix)
                        && location.getPath().endsWith(AgeratumConstants.Guide.MARKDOWN_EXTENSION)
        );
        Set<String> namespaces = new TreeSet<>();
        for (ResourceLocation location : files.keySet()) {
            namespaces.add(location.getNamespace());
        }
        return new ArrayList<>(namespaces);
    }

    private static List<String> listFilesForRoot(ResourceManager resourceManager, String namespace, String rootPrefix) {
        String rootDirectory = rootPrefix.substring(0, rootPrefix.length() - 1);
        Map<ResourceLocation, Resource> files = resourceManager.listResources(
            rootDirectory,
            location -> location.getNamespace().equals(namespace)
                        && location.getPath().startsWith(rootPrefix)
                        && location.getPath().endsWith(AgeratumConstants.Guide.MARKDOWN_EXTENSION)
        );
        List<String> result = new ArrayList<>();
        for (ResourceLocation location : files.keySet()) {
            String path = location.getPath();
            String relativePath = path.substring(rootPrefix.length());
            if (relativePath.endsWith(AgeratumConstants.Guide.MARKDOWN_EXTENSION)) {
                relativePath = relativePath.substring(
                    0,
                    relativePath.length() - AgeratumConstants.Guide.MARKDOWN_EXTENSION.length()
                );
            }
            result.add(relativePath);
        }
        result.sort(Comparator.naturalOrder());
        return result;
    }

    /**
     * 规范化文件名参数。
     *
     * <ul>
     *   <li>空值或空白字符串 → {@code index}</li>
     *   <li>反斜杠替换为正斜杠</li>
     *   <li>去除开头的 {@code /}</li>
     *   <li>若不以 {@code .md} 结尾则自动追加</li>
     * </ul>
     *
     * @param fileArgument 原始文件名参数（可为 {@code null}）
     * @return 规范化后的文件名（含 {@code .md} 后缀）
     */
    private static String normalizeFileArgument(@Nullable String fileArgument) {
        String file = fileArgument;
        // 缺省：使用首页文档
        if (file == null || file.isBlank()) {
            file = AgeratumConstants.Guide.INDEX_FILE;
        }
        // 统一路径分隔符，去除开头斜杠
        file = file.trim().replace('\\', '/');
        while (file.startsWith("/")) {
            file = file.substring(1);
        }
        // 补全 .md 后缀
        if (!file.endsWith(AgeratumConstants.Guide.MARKDOWN_EXTENSION)) {
            file += AgeratumConstants.Guide.MARKDOWN_EXTENSION;
        }
        return file;
    }

    /**
     * 规范化语言代码（小写并使用下划线分隔）。
     */
    private static String normalizeLanguageCode(@Nullable String languageCode) {
        if (languageCode == null || languageCode.isBlank()) {
            return DEFAULT_LANGUAGE_CODE;
        }
        return languageCode.trim().toLowerCase().replace('-', '_');
    }
}

