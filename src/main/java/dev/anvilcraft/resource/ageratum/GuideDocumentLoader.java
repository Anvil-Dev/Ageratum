package dev.anvilcraft.resource.ageratum;

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

/**
 * 文档加载工具类，负责从资源包中读取 Markdown 文档。
 *
 * <p>文档路径约定：{@code assets/<namespace>/ageratum/<file>.md}</p>
 *
 * <p>该类为纯工具类，不可实例化。</p>
 */
public final class GuideDocumentLoader {

    /** Markdown 文档在各命名空间内的根目录名称。 */
    private static final String GUIDE_ROOT = "ageratum";

    /** 工具类，禁止实例化。 */
    private GuideDocumentLoader() {
    }

    /**
     * 将命名空间与文件参数组合为标准 {@link ResourceLocation}。
     *
     * <p>文件名会经过规范化处理（见 {@link #normalizeFileArgument}）:
     * 缺省时使用 {@code index}，自动补全 {@code .md} 后缀。</p>
     *
     * @param namespace    文档所属命名空间
     * @param fileArgument 文件名参数（可为 {@code null} 或空字符串）
     * @return 指向该文档的资源位置，格式为
     *         {@code namespace:ageratum/<normalizedFile>.md}
     */
    public static ResourceLocation toDocumentLocation(String namespace, String fileArgument) {
        String normalizedFile = normalizeFileArgument(fileArgument);
        return ResourceLocation.fromNamespaceAndPath(namespace, GUIDE_ROOT + "/" + normalizedFile);
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
    public static List<String> listNamespaces(ResourceManager resourceManager) {
        // 扫描所有命名空间下 ageratum/ 目录中的 .md 文件
        Map<ResourceLocation, Resource> files = resourceManager.listResources(
            GUIDE_ROOT,
            location -> location.getPath().endsWith(".md")
        );
        List<String> namespaces = new ArrayList<>();
        for (ResourceLocation location : files.keySet()) {
            // 去重：每个命名空间只添加一次
            if (!namespaces.contains(location.getNamespace())) {
                namespaces.add(location.getNamespace());
            }
        }
        namespaces.sort(Comparator.naturalOrder());
        return namespaces;
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
    public static List<String> listFiles(ResourceManager resourceManager, String namespace) {
        // 仅扫描目标命名空间下的 .md 文件
        Map<ResourceLocation, Resource> files = resourceManager.listResources(
            GUIDE_ROOT,
            location -> location.getNamespace().equals(namespace) && location.getPath().endsWith(".md")
        );
        List<String> result = new ArrayList<>();
        for (ResourceLocation location : files.keySet()) {
            // 去掉 "ageratum/" 前缀和 ".md" 后缀，得到相对文件名
            String path = location.getPath();
            String relativePath = path.substring((GUIDE_ROOT + "/").length());
            if (relativePath.endsWith(".md")) {
                relativePath = relativePath.substring(0, relativePath.length() - 3);
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
    private static String normalizeFileArgument(String fileArgument) {
        String file = fileArgument;
        // 缺省：使用首页文档
        if (file == null || file.isBlank()) {
            file = "index";
        }
        // 统一路径分隔符，去除开头斜杠
        file = file.trim().replace('\\', '/');
        while (file.startsWith("/")) {
            file = file.substring(1);
        }
        // 补全 .md 后缀
        if (!file.endsWith(".md")) {
            file += ".md";
        }
        return file;
    }
}

