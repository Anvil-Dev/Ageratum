package dev.anvilcraft.resource.ageratum.client.feat.markdown;

import com.mojang.logging.LogUtils;
import dev.anvilcraft.resource.ageratum.client.feat.markdown.component.MDComponent;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.PreparableReloadListener;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimplePreparableReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.world.item.ItemStack;
import org.slf4j.Logger;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import javax.annotation.Nullable;

/**
 * 指南文档预解析缓存。
 *
 * <p>在资源包加载/重载时扫描 {@code ageratum/} 目录下全部 Markdown 文档，
 * 预先构建 {@link MDDocument}，降低首次打开文档时的解析开销。</p>
 */
@SuppressWarnings("unused")
public final class GuideDocumentCache {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final String GUIDE_ROOT = "ageratum";

    private static volatile Map<ResourceLocation, MDDocument> PARSED_DOCUMENT_CACHE = Map.of();
    private static volatile Map<NavigationTreeKey, NavigationTree> NAVIGATION_TREE_CACHE = Map.of();
    private static volatile Map<ResourceLocation, List<ItemDocumentBinding>> ITEM_DOCUMENT_CACHE = Map.of();

    private static final PreparableReloadListener RELOAD_LISTENER =
        new SimplePreparableReloadListener<PreparedGuideData>() {
            @Override
            protected PreparedGuideData prepare(ResourceManager resourceManager, ProfilerFiller profiler) {
                MarkdownParser parser = new MarkdownParser();
                Map<ResourceLocation, Resource> resources = resourceManager.listResources(
                    GUIDE_ROOT,
                    location -> location.getPath().startsWith(GUIDE_ROOT + "/") && location.getPath().endsWith(".md")
                );
                Map<ResourceLocation, MDDocument> prepared = new HashMap<>();
                Map<NavigationTreeKey, MutableDirectoryNode> treeRoots = new HashMap<>();
                Map<ResourceLocation, List<ItemDocumentBinding>> itemDocuments = new HashMap<>();
                for (Map.Entry<ResourceLocation, Resource> entry : resources.entrySet()) {
                    ResourceLocation location = entry.getKey();
                    try (var stream = entry.getValue().open()) {
                        String markdown = new String(stream.readAllBytes(), StandardCharsets.UTF_8);
                        MDDocument document = parser.parseDocument(location, markdown);
                        prepared.put(location, document);
                        registerNavigationNode(treeRoots, location, document);
                        for (GuideItemBinding binding : document.getGuideItemBindings()) {
                            itemDocuments
                                .computeIfAbsent(binding.itemId(), ignored -> new ArrayList<>())
                                .add(new ItemDocumentBinding(location, binding));
                        }
                    } catch (IOException exception) {
                        throw new UncheckedIOException("Failed to preload guide: " + location, exception);
                    } catch (RuntimeException exception) {
                        LOGGER.warn("Skip invalid guide during preload: {}", location, exception);
                    }
                }
                return new PreparedGuideData(prepared, freezeNavigationTrees(treeRoots), freezeItemDocuments(itemDocuments));
            }

            @Override
            protected void apply(
                PreparedGuideData prepared,
                ResourceManager resourceManager,
                ProfilerFiller profiler
            ) {
                PARSED_DOCUMENT_CACHE = Map.copyOf(prepared.documents());
                NAVIGATION_TREE_CACHE = Map.copyOf(prepared.navigationTrees());
                ITEM_DOCUMENT_CACHE = Map.copyOf(prepared.itemDocuments());
                LOGGER.info("Preloaded {} guide markdown files", PARSED_DOCUMENT_CACHE.size());
            }
        };

    private static Map<ResourceLocation, List<ItemDocumentBinding>> freezeItemDocuments(
        Map<ResourceLocation, List<ItemDocumentBinding>> source
    ) {
        Map<ResourceLocation, List<ItemDocumentBinding>> result = new HashMap<>();
        for (Map.Entry<ResourceLocation, List<ItemDocumentBinding>> entry : source.entrySet()) {
            List<ItemDocumentBinding> sorted = new ArrayList<>(entry.getValue());
            sorted.sort(Comparator
                .comparing((ItemDocumentBinding binding) -> binding.location().toString())
                .thenComparing(binding -> binding.binding().toString()));
            result.put(entry.getKey(), List.copyOf(sorted));
        }
        return result;
    }

    private static void registerNavigationNode(
        Map<NavigationTreeKey, MutableDirectoryNode> treeRoots,
        ResourceLocation location,
        MDDocument document
    ) {
        String path = location.getPath();
        String prefix = GUIDE_ROOT + "/";
        if (!path.startsWith(prefix)) {
            return;
        }
        String withoutRoot = path.substring(prefix.length());
        int slash = withoutRoot.indexOf('/');
        if (slash < 0) {
            return;
        }
        String languageCode = withoutRoot.substring(0, slash);
        String relativePathWithExt = withoutRoot.substring(slash + 1);
        if (!relativePathWithExt.endsWith(".md")) {
            return;
        }

        String fileArgument = relativePathWithExt.substring(0, relativePathWithExt.length() - 3).replace('\\', '/');
        if (fileArgument.isBlank()) {
            return;
        }

        NavigationTreeKey key = new NavigationTreeKey(location.getNamespace(), normalizeLanguageCode(languageCode));
        MutableDirectoryNode root = treeRoots.computeIfAbsent(key, ignored -> new MutableDirectoryNode(location.getNamespace(), ""));
        root.insert(fileArgument, location, document.getTitle(fileArgument));
    }

    private static Map<NavigationTreeKey, NavigationTree> freezeNavigationTrees(
        Map<NavigationTreeKey, MutableDirectoryNode> treeRoots
    ) {
        Map<NavigationTreeKey, NavigationTree> result = new HashMap<>();
        for (Map.Entry<NavigationTreeKey, MutableDirectoryNode> entry : treeRoots.entrySet()) {
            result.put(entry.getKey(), entry.getValue().freezeAsTree());
        }
        return result;
    }

    private static String normalizeLanguageCode(@Nullable String languageCode) {
        if (languageCode == null || languageCode.isBlank()) {
            return GuideDocumentLoader.DEFAULT_LANGUAGE_CODE;
        }
        return languageCode.trim().toLowerCase().replace('-', '_');
    }

    private GuideDocumentCache() {
    }

    /**
     * 返回资源重载监听器实例，用于注册到客户端重载事件。
     */
    public static PreparableReloadListener reloadListener() {
        return RELOAD_LISTENER;
    }

    /**
     * 根据文档资源位置读取预解析文档。
     */
    public static Optional<MDDocument> getParsedDocument(ResourceLocation location) {
        MDDocument document = PARSED_DOCUMENT_CACHE.get(location);
        return Optional.ofNullable(document);
    }

    /**
     * 读取指定命名空间和语言的导航树（目录结构扫描结果）。
     */
    public static Optional<NavigationTree> getNavigationTree(String namespace, String languageCode) {
        NavigationTreeKey key = new NavigationTreeKey(namespace, normalizeLanguageCode(languageCode));
        return Optional.ofNullable(NAVIGATION_TREE_CACHE.get(key));
    }

    /**
     * 根据物品栈返回文档位置（当前语言优先，其次 en_us，最后回退列表中的第一个）。
     */
    public static Optional<ResourceLocation> getFirstDocumentByItemStack(ItemStack stack, @Nullable String languageCode) {
        if (stack.isEmpty()) {
            return Optional.empty();
        }

        ResourceLocation itemId = BuiltInRegistries.ITEM.getKey(stack.getItem());
        List<ItemDocumentBinding> bindings = ITEM_DOCUMENT_CACHE.get(itemId);
        if (bindings == null || bindings.isEmpty()) {
            return Optional.empty();
        }

        List<ResourceLocation> matchedLocations = new ArrayList<>();
        for (ItemDocumentBinding binding : bindings) {
            if (!binding.binding().matches(stack) || matchedLocations.contains(binding.location())) {
                continue;
            }
            matchedLocations.add(binding.location());
        }

        if (matchedLocations.isEmpty()) {
            return Optional.empty();
        }

        return selectPreferredLocation(matchedLocations, languageCode);
    }

    private static Optional<ResourceLocation> selectPreferredLocation(List<ResourceLocation> locations, @Nullable String languageCode) {
        String preferredLanguage = normalizeLanguageCode(languageCode);
        if (!preferredLanguage.isEmpty()) {
            for (ResourceLocation location : locations) {
                if (preferredLanguage.equals(extractLanguageCode(location))) {
                    return Optional.of(location);
                }
            }
        }

        if (!GuideDocumentLoader.DEFAULT_LANGUAGE_CODE.equals(preferredLanguage)) {
            for (ResourceLocation location : locations) {
                if (GuideDocumentLoader.DEFAULT_LANGUAGE_CODE.equals(extractLanguageCode(location))) {
                    return Optional.of(location);
                }
            }
        }

        return Optional.of(locations.getFirst());
    }

    private static String extractLanguageCode(ResourceLocation location) {
        String path = location.getPath().replace('\\', '/');
        String prefix = GUIDE_ROOT + "/";
        if (!path.startsWith(prefix)) {
            return "";
        }
        String withoutRoot = path.substring(prefix.length());
        int slash = withoutRoot.indexOf('/');
        if (slash < 0) {
            return "";
        }
        return normalizeLanguageCode(withoutRoot.substring(0, slash));
    }

    /**
     * 根据文档资源位置读取预解析组件。
     */
    public static Optional<List<MDComponent>> getParsedComponents(ResourceLocation location) {
        return getParsedDocument(location).map(MDDocument::components).map(ArrayList::new);
    }

    private record PreparedGuideData(
        Map<ResourceLocation, MDDocument> documents,
        Map<NavigationTreeKey, NavigationTree> navigationTrees,
        Map<ResourceLocation, List<ItemDocumentBinding>> itemDocuments
    ) {
    }

    private record ItemDocumentBinding(ResourceLocation location, GuideItemBinding binding) {
    }

    private record NavigationTreeKey(String namespace, String languageCode) {
    }

    public record NavigationTree(
        List<NavigationDocument> rootDocuments,
        List<NavigationDirectory> rootDirectories
    ) {
    }

    public record NavigationDirectory(
        String namespace,
        String name,
        @Nullable NavigationDocument indexDocument,
        List<NavigationDocument> documents,
        List<NavigationDirectory> children
    ) {
    }

    public record NavigationDocument(String fileArgument, String title, ResourceLocation location) {
    }

    private static final class MutableDirectoryNode {
        private final String namespace;
        private final String name;
        private final Map<String, MutableDirectoryNode> children = new LinkedHashMap<>();
        private final List<NavigationDocument> documents = new ArrayList<>();
        @Nullable
        private NavigationDocument indexDocument;

        private MutableDirectoryNode(String namespace, String name) {
            this.namespace = namespace;
            this.name = name;
        }

        private void insert(String fileArgument, ResourceLocation location, String title) {
            String[] segments = fileArgument.split("/");
            MutableDirectoryNode current = this;
            for (int i = 0; i < segments.length - 1; i++) {
                String segment = segments[i];
                current = current.children.computeIfAbsent(segment, name -> new MutableDirectoryNode(this.namespace, name));
            }
            String fileName = segments[segments.length - 1];
            NavigationDocument document = new NavigationDocument(fileArgument, title, location);
            if ("index".equalsIgnoreCase(fileName)) {
                current.indexDocument = document;
            } else {
                current.documents.add(document);
            }
        }

        private NavigationTree freezeAsTree() {
            NavigationDirectory directory = this.freezeAsDirectory(1);
            return new NavigationTree(directory.documents, directory.children);
        }

        private NavigationDirectory freezeAsDirectory(int level) {
            List<NavigationDocument> directoryDocuments = new ArrayList<>(this.documents);

            directoryDocuments.sort(Comparator.comparing(NavigationDocument::fileArgument));

            if (level <= 1 && this.indexDocument != null) {
                directoryDocuments.addFirst(this.indexDocument);
            }

            List<NavigationDirectory> frozenChildren = new ArrayList<>();
            for (MutableDirectoryNode child : this.children.values()) {
                frozenChildren.add(child.freezeAsDirectory(level + 1));
            }
            frozenChildren.sort(Comparator.comparing(NavigationDirectory::name));

            return new NavigationDirectory(
                this.namespace,
                this.name,
                this.indexDocument,
                List.copyOf(directoryDocuments),
                List.copyOf(frozenChildren)
            );
        }
    }
}

