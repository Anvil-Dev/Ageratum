package dev.anvilcraft.resource.ageratum.client.feat.structure;

import com.mojang.logging.LogUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtAccounter;
import net.minecraft.nbt.NbtIo;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.resources.PreparableReloadListener;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimplePreparableReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import org.slf4j.Logger;

import java.io.IOException;
import java.io.InputStream;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 客户端结构模板管理器。
 *
 * <p>在资源重载时扫描并缓存所有资源包中 {@code assets/<namespace>/ageratum/} 目录下的
 * {@code .nbt} 文件。</p>
 *
 * <p>模板 Identifier 格式为 {@code namespace:relative/path}（相对于 {@code ageratum/}
 * 目录，不含 {@code .nbt} 后缀）。</p>
 *
 * <p>例如：{@code assets/minecraft/ageratum/village/house.nbt} → {@code minecraft:village/house}</p>
 */
public final class AgeratumStructureTemplateManager {
    private static final Logger LOGGER = LogUtils.getLogger();

    /**
     * 在 {@code assets/<namespace>/} 下扫描结构文件的目录名。
     */
    public static final String ASSET_FOLDER = "ageratum";

    /**
     * 原始 NBT 数据，在资源重载时以原子方式整体替换。
     */
    private static volatile Map<Identifier, CompoundTag> nbtCache = Map.of();

    /**
     * 编译后的 StructureTemplate 懒加载缓存，资源重载时清空。
     */
    private static final Map<Identifier, StructureTemplate> templateCache = new ConcurrentHashMap<>();

    private AgeratumStructureTemplateManager() {
    }

    // ──────────────────────────────────────────────────────────────────────────
    // 公开 API
    // ──────────────────────────────────────────────────────────────────────────

    /**
     * 根据 Identifier 获取结构模板。
     *
     * <p>首次调用时从缓存的 CompoundTag 编译模板；编译结果会被缓存供后续调用复用。
     * 调用时需要游戏关卡已加载（{@code Minecraft.getInstance().level != null}）。</p>
     *
     * @param location 模板标识，格式见类文档
     * @return 模板，若未找到或当前无法编译则为 empty
     */
    public static Optional<StructureTemplate> get(Identifier location) {
        StructureTemplate cached = templateCache.get(location);
        if (cached != null) {
            return Optional.of(cached);
        }

        CompoundTag nbt = nbtCache.get(location);
        if (nbt == null) {
            return Optional.empty();
        }

        var level = Minecraft.getInstance().level;
        if (level == null) {
            return Optional.empty();
        }

        try {
            StructureTemplate template = new StructureTemplate();
            HolderLookup.RegistryLookup<Block> blockLookup = level.registryAccess().lookupOrThrow(Registries.BLOCK);
            template.load(blockLookup, nbt);
            templateCache.put(location, template);
            return Optional.of(template);
        } catch (Exception e) {
            LOGGER.warn("Failed to compile structure template {}: {}", location, e.getMessage());
            return Optional.empty();
        }
    }

    /**
     * 返回所有已发现的结构模板 Identifier 集合（不可修改视图）。
     */
    public static Set<Identifier> listAll() {
        return nbtCache.keySet();
    }

    /**
     * 返回可注册到客户端资源重载系统的监听器实例。
     */
    public static PreparableReloadListener reloadListener() {
        return RELOAD_LISTENER;
    }

    // ──────────────────────────────────────────────────────────────────────────
    // 重载监听器
    // ──────────────────────────────────────────────────────────────────────────

    private static final PreparableReloadListener RELOAD_LISTENER = new SimplePreparableReloadListener<Map<Identifier, CompoundTag>>() {
        @Override
        protected Map<Identifier, CompoundTag> prepare(ResourceManager manager, ProfilerFiller profiler) {
            String prefix = ASSET_FOLDER + "/";
            int prefixLen = prefix.length();
            int nbtSuffixLen = ".nbt".length();

            Map<Identifier, CompoundTag> result = new HashMap<>();

            manager.listResources(ASSET_FOLDER, rl -> rl.getPath().startsWith(prefix) && rl.getPath().endsWith(".nbt"))
                .forEach((rl, resource) -> {
                    String rawPath = rl.getPath(); // e.g. "ageratum/village/house.nbt"

                    // 路径必须在 "ageratum/" 之后还有至少一个字符（加上 ".nbt" 后缀）
                    if (rawPath.length() <= prefixLen + nbtSuffixLen) {
                        return;
                    }

                    String relative = rawPath.substring(prefixLen, rawPath.length() - nbtSuffixLen);
                    Identifier location = Identifier.fromNamespaceAndPath(rl.getNamespace(), relative);

                    try (InputStream stream = resource.open()) {
                        CompoundTag tag = NbtIo.readCompressed(stream, NbtAccounter.unlimitedHeap());
                        result.put(location, tag);
                        LOGGER.debug("Discovered structure template: {}", location);
                    } catch (IOException e) {
                        LOGGER.warn("Failed to read structure NBT at {}: {}", rl, e.getMessage());
                    }
                });

            return result;
        }

        @Override
        protected void apply(Map<Identifier, CompoundTag> data, ResourceManager manager, ProfilerFiller profiler) {
            nbtCache = Collections.unmodifiableMap(data);
            templateCache.clear();
            LOGGER.info("Loaded {} structure template(s) from assets/{}/", data.size(), ASSET_FOLDER);
        }
    };
}

