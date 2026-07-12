package dev.anvilcraft.resource.ageratum.client.feat.markdown;

import com.mojang.logging.LogUtils;
import dev.anvilcraft.resource.ageratum.client.command.AgeratumCommand;
import dev.anvilcraft.resource.ageratum.client.constants.AgeratumConstants;
import dev.anvilcraft.resource.ageratum.client.feat.markdown.component.MDComponent;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
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
 */
@SuppressWarnings("unused")
public final class GuideDocumentCache {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final String GUIDE_ROOT = AgeratumConstants.Guide.ROOT_FOLDER;

    private static volatile Map<Identifier, MDDocument> PARSED_DOCUMENT_CACHE = Map.of();
    private static volatile Map<NavigationTreeKey, NavigationTree> NAVIGATION_TREE_CACHE = Map.of();
    private static volatile Map<Identifier, List<ItemDocumentBinding>> ITEM_DOCUMENT_CACHE = Map.of();

    private static final PreparableReloadListener RELOAD_LISTENER =
        new SimplePreparableReloadListener<PreparedGuideData>() {
            @Override
            protected PreparedGuideData prepare(ResourceManager resourceManager, ProfilerFiller profiler) {
                MarkdownParser parser = new MarkdownParser();
                Map<Identifier, Resource> resources = resourceManager.listResources(
                    GUIDE_ROOT,
                    location -> location.getPath().startsWith(GUIDE_ROOT + "/")
                                && location.getPath().endsWith(AgeratumConstants.Guide.MARKDOWN_EXTENSION)
                );
                // Pass 1: extract front matter, build item binding cache
                Map<Identifier, String> rawMarkdowns = new LinkedHashMap<>();
                Map<Identifier, List<ItemDocumentBinding>> itemDocuments = new HashMap<>();
                for (Map.Entry<Identifier, Resource> entry : resources.entrySet()) {
                    Identifier location = entry.getKey();
                    try (var stream = entry.getValue().open()) {
                        String markdown = new String(stream.readAllBytes(), StandardCharsets.UTF_8);
                        rawMarkdowns.put(location, markdown);
                        Map<String, Object> frontMatter = MarkdownParser.extractFrontMatter(markdown).frontMatter();
                        MDDocument tempDoc = new MDDocument(location, frontMatter, List.of());
                        for (GuideItemBinding binding : tempDoc.getGuideItemBindings()) {
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
                // Early apply item cache so <ref> resolution in pass 2 works
                ITEM_DOCUMENT_CACHE = Map.copyOf(freezeItemDocuments(itemDocuments));

                // Pass 2: full parse with item cache available
                Map<Identifier, MDDocument> prepared = new HashMap<>();
                Map<NavigationTreeKey, MutableDirectoryNode> treeRoots = new HashMap<>();
                for (Map.Entry<Identifier, String> entry : rawMarkdowns.entrySet()) {
                    Identifier location = entry.getKey();
                    try {
                        MDDocument document = parser.parseDocument(location, entry.getValue());
                        prepared.put(location, document);
                        registerNavigationNode(treeRoots, location, document);
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
                AgeratumCommand.warmSuggestionCache(PARSED_DOCUMENT_CACHE);
                LOGGER.info("Preloaded {} guide markdown files", PARSED_DOCUMENT_CACHE.size());
            }
        };

    private static Map<Identifier, List<ItemDocumentBinding>> freezeItemDocuments(
        Map<Identifier, List<ItemDocumentBinding>> source
    ) {
        Map<Identifier, List<ItemDocumentBinding>> result = new HashMap<>();
        for (Map.Entry<Identifier, List<ItemDocumentBinding>> entry : source.entrySet()) {
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
        Identifier location,
        MDDocument document
    ) {
        String path = location.getPath();
        String prefix = GUIDE_ROOT + "/";
        if (!path.startsWith(prefix)) return;
        String withoutRoot = path.substring(prefix.length());
        int slash = withoutRoot.indexOf('/');
        if (slash < 0) return;
        String languageCode = withoutRoot.substring(0, slash);
        String relativePathWithExt = withoutRoot.substring(slash + 1);
        if (!relativePathWithExt.endsWith(AgeratumConstants.Guide.MARKDOWN_EXTENSION)) return;

        String fileArgument = relativePathWithExt
            .substring(0, relativePathWithExt.length() - AgeratumConstants.Guide.MARKDOWN_EXTENSION.length())
            .replace('\\', '/');
        if (fileArgument.isBlank()) return;

        NavigationTreeKey key = new NavigationTreeKey(location.getNamespace(), normalizeLanguageCode(languageCode));
        MutableDirectoryNode root = treeRoots.computeIfAbsent(key, ignored -> new MutableDirectoryNode(location.getNamespace(), ""));
        root.insert(fileArgument, location, document.getTitle(fileArgument), document.getWeight(), document.getNavigationColor());
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
        if (languageCode == null || languageCode.isBlank()) return GuideDocumentLoader.DEFAULT_LANGUAGE_CODE;
        return languageCode.trim().toLowerCase().replace('-', '_');
    }

    private GuideDocumentCache() {}

    public static PreparableReloadListener reloadListener() {
        return RELOAD_LISTENER;
    }

    public static Optional<MDDocument> getParsedDocument(Identifier location) {
        return Optional.ofNullable(PARSED_DOCUMENT_CACHE.get(location));
    }

    public static Optional<NavigationTree> getNavigationTree(String namespace, String languageCode) {
        NavigationTreeKey key = new NavigationTreeKey(namespace, normalizeLanguageCode(languageCode));
        return Optional.ofNullable(NAVIGATION_TREE_CACHE.get(key));
    }

    public static Optional<Identifier> getFirstDocumentByItemStack(ItemStack stack, @Nullable String languageCode) {
        if (stack.isEmpty()) return Optional.empty();
        Identifier itemId = BuiltInRegistries.ITEM.getKey(stack.getItem());
        List<ItemDocumentBinding> bindings = ITEM_DOCUMENT_CACHE.get(itemId);
        if (bindings == null || bindings.isEmpty()) return Optional.empty();

        Map<Identifier, Integer> matchedLocations = new HashMap<>();
        for (ItemDocumentBinding binding : bindings) {
            int specificity = binding.binding().matchSpecificity(stack);
            if (specificity < 0) continue;
            matchedLocations.merge(binding.location(), specificity, Math::max);
        }
        if (matchedLocations.isEmpty()) return Optional.empty();
        return selectPreferredLocation(matchedLocations, languageCode);
    }

    private static Optional<Identifier> selectPreferredLocation(
        Map<Identifier, Integer> locations,
        @Nullable String languageCode
    ) {
        String preferredLanguage = normalizeLanguageCode(languageCode);
        List<Map.Entry<Identifier, Integer>> candidates = locations.entrySet().stream()
            .filter(entry -> preferredLanguage.equals(extractLanguageCode(entry.getKey())))
            .toList();
        if (candidates.isEmpty() && !GuideDocumentLoader.DEFAULT_LANGUAGE_CODE.equals(preferredLanguage)) {
            candidates = locations.entrySet().stream()
                .filter(entry -> GuideDocumentLoader.DEFAULT_LANGUAGE_CODE.equals(extractLanguageCode(entry.getKey())))
                .toList();
        }
        if (candidates.isEmpty()) {
            candidates = List.copyOf(locations.entrySet());
        }
        return candidates.stream()
            .min(Comparator
                .<Map.Entry<Identifier, Integer>>comparingInt(Map.Entry::getValue)
                .reversed()
                .thenComparing(entry -> entry.getKey().toString()))
            .map(Map.Entry::getKey);
    }

    private static String extractLanguageCode(Identifier location) {
        String path = location.getPath().replace('\\', '/');
        String prefix = GUIDE_ROOT + "/";
        if (!path.startsWith(prefix)) return "";
        String withoutRoot = path.substring(prefix.length());
        int slash = withoutRoot.indexOf('/');
        if (slash < 0) return "";
        return normalizeLanguageCode(withoutRoot.substring(0, slash));
    }

    public static Optional<List<MDComponent>> getParsedComponents(Identifier location) {
        return getParsedDocument(location).map(MDDocument::components).map(ArrayList::new);
    }

    private record PreparedGuideData(
        Map<Identifier, MDDocument> documents,
        Map<NavigationTreeKey, NavigationTree> navigationTrees,
        Map<Identifier, List<ItemDocumentBinding>> itemDocuments
    ) {}

    private record ItemDocumentBinding(Identifier location, GuideItemBinding binding) {}
    private record NavigationTreeKey(String namespace, String languageCode) {}

    public record NavigationTree(
        List<NavigationDocument> rootDocuments,
        List<NavigationDirectory> rootDirectories
    ) {}

    public record NavigationDirectory(
        String namespace,
        String name,
        @Nullable NavigationDocument indexDocument,
        List<NavigationDocument> documents,
        List<NavigationDirectory> children
    ) {}

    public record NavigationDocument(
        String fileArgument,
        String title,
        Identifier location,
        int weight,
        @Nullable String color
    ) {
        public NavigationDocument(String fileArgument, String title, Identifier location) {
            this(fileArgument, title, location, 0, null);
        }
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

        private void insert(String fileArgument, Identifier location, String title, int weight, @Nullable String color) {
            String[] segments = fileArgument.split("/");
            MutableDirectoryNode current = this;
            for (int i = 0; i < segments.length - 1; i++) {
                String segment = segments[i];
                current = current.children.computeIfAbsent(segment, name -> new MutableDirectoryNode(this.namespace, name));
            }
            String fileName = segments[segments.length - 1];
            NavigationDocument document = new NavigationDocument(fileArgument, title, location, weight, color);
            if (AgeratumConstants.Guide.INDEX_FILE.equalsIgnoreCase(fileName)) {
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
            directoryDocuments.sort(
                Comparator.comparingInt(NavigationDocument::weight)
                    .thenComparing(NavigationDocument::fileArgument)
            );
            if (level <= 1 && this.indexDocument != null) {
                directoryDocuments.addFirst(this.indexDocument);
            }
            List<NavigationDirectory> frozenChildren = new ArrayList<>();
            for (MutableDirectoryNode child : this.children.values()) {
                frozenChildren.add(child.freezeAsDirectory(level + 1));
            }
            frozenChildren.sort(Comparator.comparing(NavigationDirectory::name));
            return new NavigationDirectory(
                this.namespace, this.name, this.indexDocument,
                List.copyOf(directoryDocuments), List.copyOf(frozenChildren)
            );
        }
    }
}
