package dev.anvilcraft.resource.ageratum;

import com.mojang.logging.LogUtils;
import dev.anvilcraft.resource.ageratum.client.feat.markdown.MarkdownParser;
import dev.anvilcraft.resource.ageratum.client.feat.markdown.component.MDComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.PreparableReloadListener;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimplePreparableReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;
import org.slf4j.Logger;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * 指南文档预解析缓存。
 *
 * <p>在资源包加载/重载时扫描 {@code ageratum/} 目录下全部 Markdown 文档，
 * 预先构建 {@link MDComponent} 列表，降低首次打开文档时的解析开销。</p>
 */
public final class GuideDocumentCache {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final String GUIDE_ROOT = "ageratum";

    private static volatile Map<ResourceLocation, List<MDComponent>> PARSED_COMPONENT_CACHE = Map.of();

    private static final PreparableReloadListener RELOAD_LISTENER =
        new SimplePreparableReloadListener<Map<ResourceLocation, List<MDComponent>>>() {
            @Override
            protected Map<ResourceLocation, List<MDComponent>> prepare(ResourceManager resourceManager, ProfilerFiller profiler) {
                MarkdownParser parser = new MarkdownParser();
                Map<ResourceLocation, Resource> resources = resourceManager.listResources(
                    GUIDE_ROOT,
                    location -> location.getPath().startsWith(GUIDE_ROOT + "/") && location.getPath().endsWith(".md")
                );
                Map<ResourceLocation, List<MDComponent>> prepared = new HashMap<>();
                for (Map.Entry<ResourceLocation, Resource> entry : resources.entrySet()) {
                    ResourceLocation location = entry.getKey();
                    try (var stream = entry.getValue().open()) {
                        String markdown = new String(stream.readAllBytes(), StandardCharsets.UTF_8);
                        prepared.put(location, List.copyOf(parser.parse(markdown)));
                    } catch (IOException exception) {
                        throw new UncheckedIOException("Failed to preload guide: " + location, exception);
                    } catch (RuntimeException exception) {
                        LOGGER.warn("Skip invalid guide during preload: {}", location, exception);
                    }
                }
                return prepared;
            }

            @Override
            protected void apply(
                Map<ResourceLocation, List<MDComponent>> prepared,
                ResourceManager resourceManager,
                ProfilerFiller profiler
            ) {
                PARSED_COMPONENT_CACHE = Map.copyOf(prepared);
                LOGGER.info("Preloaded {} guide markdown files", PARSED_COMPONENT_CACHE.size());
            }
        };

    private GuideDocumentCache() {
    }

    /**
     * 返回资源重载监听器实例，用于注册到客户端重载事件。
     */
    public static PreparableReloadListener reloadListener() {
        return RELOAD_LISTENER;
    }

    /**
     * 根据文档资源位置读取预解析组件。
     */
    public static Optional<List<MDComponent>> getParsedComponents(ResourceLocation location) {
        List<MDComponent> components = PARSED_COMPONENT_CACHE.get(location);
        if (components == null) {
            return Optional.empty();
        }
        return Optional.of(new ArrayList<>(components));
    }
}

