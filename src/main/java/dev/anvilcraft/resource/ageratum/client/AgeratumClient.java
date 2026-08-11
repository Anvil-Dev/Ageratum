package dev.anvilcraft.resource.ageratum.client;

import com.mojang.brigadier.context.CommandContext;
import com.mojang.logging.LogUtils;
import dev.anvilcraft.lib.v2.config.ConfigManager;
import dev.anvilcraft.resource.ageratum.Ageratum;
import dev.anvilcraft.resource.ageratum.client.constants.AgeratumConstants;
import dev.anvilcraft.resource.ageratum.client.feat.markdown.GuideDocumentCache;
import dev.anvilcraft.resource.ageratum.client.feat.markdown.GuideDocumentLoader;
import dev.anvilcraft.resource.ageratum.client.feat.markdown.MDDocument;
import dev.anvilcraft.resource.ageratum.client.feat.markdown.MarkdownParser;
import dev.anvilcraft.resource.ageratum.client.feat.structure.AgeratumStructureTemplateManager;
import dev.anvilcraft.resource.ageratum.client.gui.GuideScreen;
import dev.anvilcraft.resource.ageratum.client.registries.AgeratumRegistries;
import dev.anvilcraft.resource.ageratum.client.registries.BuiltinExtensionComponents;
import dev.anvilcraft.resource.ageratum.client.registries.BuiltinInlineComponents;
import dev.anvilcraft.resource.ageratum.client.registries.BuiltinInlineStyleParsers;
import dev.anvilcraft.resource.ageratum.client.registries.BuiltinRecipeComponentFactories;
import net.minecraft.client.Minecraft;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.resources.ResourceManager;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.loading.FMLLoader;
import net.neoforged.neoforge.client.event.AddClientReloadListenersEvent;
import org.slf4j.Logger;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import javax.annotation.Nullable;

@Mod(value = Ageratum.MOD_ID, dist = Dist.CLIENT)
@EventBusSubscriber(modid = Ageratum.MOD_ID, value = Dist.CLIENT)
public class AgeratumClient {
    /**
     * 模组日志记录器。
     */
    private static final Logger LOGGER = LogUtils.getLogger();

    public static final AgeratumClientConfig CONFIG = ConfigManager.register(Ageratum.MOD_ID, AgeratumClientConfig::new);

    /**
     * 模组客户端侧构造函数，由 NeoForge 在加载时调用。
     *
     * @param modEventBus  模组专属事件总线
     * @param modContainer 模组容器
     */
    public AgeratumClient(IEventBus modEventBus, ModContainer modContainer) {
        // 注册自定义注册表
        AgeratumRegistries.register(modEventBus);
        // 触发内置扩展组件注册项的类加载
        BuiltinExtensionComponents.init();
        // 触发内置行内样式解析器注册项的类加载
        BuiltinInlineStyleParsers.init();
        // 触发内置行内组件注册项的类加载
        BuiltinInlineComponents.init();
        // 触发内置配方组件解析器注册项的类加载
        BuiltinRecipeComponentFactories.init();
    }

    /**
     * 获取客户端当前语言代码。
     *
     * <p>若无法读取语言管理器，回退到 {@code en_us}。</p>
     */
    public static String getClientLanguageCode(Minecraft minecraft) {
        try {
            return minecraft.getLanguageManager().getSelected();
        } catch (RuntimeException exception) {
            LOGGER.warn("Failed to read client language code, fallback to en_us", exception);
            return GuideDocumentLoader.DEFAULT_LANGUAGE_CODE;
        }
    }

    /**
     * 注册客户端资源重载监听器。
     */
    @SubscribeEvent
    public static void onReloadListenerRegister(AddClientReloadListenersEvent event) {
        event.addListener(Ageratum.location("document"), GuideDocumentCache.reloadListener());
        event.addListener(Ageratum.location("structure"), AgeratumStructureTemplateManager.reloadListener());
    }

    /**
     * 解析命令参数并打开对应的文档界面。
     *
     * <p>若目标文件不存在，向命令发起方发送错误反馈，不打开界面。</p>
     *
     * @param context      命令执行上下文
     * @param namespace    文档所在的资源包命名空间
     * @param fileArgument 文件名参数（可为 {@code null}，此时使用 index.md）
     * @return 命令执行结果码：1 表示成功，0 表示失败
     */
    public static int openGuide(
        CommandContext<CommandSourceStack> context,
        String namespace,
        @Nullable String fileArgument,
        @Nullable String anchor
    ) {
        Minecraft minecraft = Minecraft.getInstance();

        String languageCode = getClientLanguageCode(minecraft);

        // 将 namespace + languageCode + fileArgument 解析为存在的 Identifier（带回退）
        Identifier documentLocation;
        try {
            Optional<Identifier> resolved = GuideDocumentLoader.resolveExistingLocation(
                minecraft.getResourceManager(),
                namespace,
                languageCode,
                fileArgument
            );
            if (resolved.isEmpty()) {
                context.getSource().sendFailure(Component.literal("Guide file not found for language '" + languageCode + "'."));
                return 0;
            }
            documentLocation = resolved.get();
        } catch (RuntimeException exception) {
            context.getSource().sendFailure(Component.literal("Invalid guide path."));
            return 0;
        }
        if (!openGuideOnClient(documentLocation, anchor, List.of())) {
            context.getSource()
                .sendFailure(Component.literal("Guide file not found: assets/" + documentLocation.getNamespace() + "/" + documentLocation.getPath()));
            return 0;
        }
        return 1;
    }

    /**
     * 客户端本地打开文档；若不存在则返回 false。
     */
    public static boolean openGuideOnClientWithoutLanguageCode(Identifier location, List<Identifier> breadCrumbs) {
        Minecraft minecraft = Minecraft.getInstance();
        String languageCode = getClientLanguageCode(minecraft);
        String namespace = location.getNamespace();
        String fileArgument = location.getPath();
        Identifier documentLocation;
        try {
            Optional<Identifier> resolved = GuideDocumentLoader.resolveExistingLocation(
                minecraft.getResourceManager(),
                namespace,
                languageCode,
                fileArgument
            );
            if (resolved.isEmpty()) {
                return false;
            }
            documentLocation = resolved.get();
        } catch (RuntimeException exception) {
            return false;
        }
        return openGuideOnClient(documentLocation, breadCrumbs);
    }

    /**
     * 客户端本地打开文档；若不存在则返回 false。
     */
    public static boolean openGuideOnClient(Identifier location, List<Identifier> breadCrumbs) {
        return openGuideOnClient(location, null, breadCrumbs);
    }

    /**
     * 从侧边栏打开文档，保留当前标签栏滚动和折叠状态。
     */
    public static boolean openGuideOnClientPreservingLabelState(Identifier location, List<Identifier> breadCrumbs) {
        return openGuideOnClientInternal(location, null, breadCrumbs, true);
    }

    /**
     * 客户端本地打开文档，可选指定锚点；若不存在则返回 false。
     *
     * @param location 文档资源位置
     * @param anchor   目标锚点（可为 null）
     */
    public static boolean openGuideOnClient(Identifier location, @Nullable String anchor, List<Identifier> breadCrumbs) {
        return openGuideOnClientInternal(location, anchor, breadCrumbs, false);
    }

    private static boolean openGuideOnClientInternal(
        Identifier location,
        @Nullable String anchor,
        List<Identifier> breadCrumbs,
        boolean preserveLabelState
    ) {
        if (isPreviewLocation(location)) {
            return openPreviewGuideOnClient(location, anchor, breadCrumbs, preserveLabelState);
        }

        Minecraft minecraft = Minecraft.getInstance();
        GuideScreen currentGuideScreen = null;
        if (minecraft.screen instanceof GuideScreen guideScreen) {
            currentGuideScreen = guideScreen;
        }
        ResourceManager resourceManager = minecraft.getResourceManager();
        if (!GuideDocumentLoader.exists(resourceManager, location)) {
            return false;
        }

        int inheritedLabelScrollRows = 0;
        double inheritedLabelScrollRemainder = 0.0d;
        if (currentGuideScreen != null) {
            inheritedLabelScrollRows = currentGuideScreen.getLabelScrollRows();
            inheritedLabelScrollRemainder = currentGuideScreen.getLabelScrollRemainder();
        }

        // 优先使用预解析缓存，缺失时回退为即时解析
        Optional<MDDocument> cachedDocument = GuideDocumentCache.getParsedDocument(location);
        if (cachedDocument.isPresent()) {
            GuideScreen screen = new GuideScreen(location, cachedDocument.get(), breadCrumbs, false);
            screen.setAnchor(anchor);
            screen.setLabelScrollState(inheritedLabelScrollRows, inheritedLabelScrollRemainder);
            if (preserveLabelState && currentGuideScreen != null) {
                screen.setLabelStatePreserved(currentGuideScreen);
            }
            minecraft.setScreen(screen);
            return true;
        }

        String content = GuideDocumentLoader.read(resourceManager, location);
        return AgeratumClient.parseDocumentAndSetScreen(
            location,
            anchor,
            breadCrumbs,
            minecraft,
            inheritedLabelScrollRows,
            inheritedLabelScrollRemainder,
            currentGuideScreen,
            preserveLabelState,
            content,
            false
        );
    }

    private static boolean openPreviewGuideOnClient(
        Identifier location,
        @Nullable String anchor,
        List<Identifier> breadCrumbs,
        boolean preserveLabelState
    ) {
        Minecraft minecraft = Minecraft.getInstance();
        GuideScreen currentGuideScreen = null;
        if (minecraft.screen instanceof GuideScreen guideScreen) {
            currentGuideScreen = guideScreen;
        }
        Path previewFile = resolvePreviewDocumentPath(location);
        if (!Files.isRegularFile(previewFile)) {
            return false;
        }

        int inheritedLabelScrollRows = 0;
        double inheritedLabelScrollRemainder = 0.0d;
        if (currentGuideScreen != null) {
            inheritedLabelScrollRows = currentGuideScreen.getLabelScrollRows();
            inheritedLabelScrollRemainder = currentGuideScreen.getLabelScrollRemainder();
        }

        String content;
        try {
            content = Files.readString(previewFile, StandardCharsets.UTF_8);
        } catch (IOException exception) {
            LOGGER.warn("Failed to read preview document: {}", previewFile, exception);
            return false;
        }

        return AgeratumClient.parseDocumentAndSetScreen(
            location,
            anchor,
            breadCrumbs,
            minecraft,
            inheritedLabelScrollRows,
            inheritedLabelScrollRemainder,
            currentGuideScreen,
            preserveLabelState,
            content,
            true
        );
    }

    private static boolean parseDocumentAndSetScreen(
        Identifier location,
        @Nullable String anchor,
        List<Identifier> breadCrumbs,
        Minecraft minecraft,
        int inheritedLabelScrollRows,
        double inheritedLabelScrollRemainder,
        @Nullable GuideScreen currentGuideScreen,
        boolean preserveLabelState,
        String content,
        boolean preview
    ) {
        MDDocument parsedDocument = new MarkdownParser().parseDocument(location, content);
        GuideScreen screen = new GuideScreen(location, parsedDocument, breadCrumbs, preview);
        screen.setAnchor(anchor);
        screen.setLabelScrollState(inheritedLabelScrollRows, inheritedLabelScrollRemainder);
        if (preserveLabelState && currentGuideScreen != null) {
            screen.setLabelStatePreserved(currentGuideScreen);
        }
        minecraft.setScreen(screen);
        return true;
    }

    public static boolean isPreviewLocation(Identifier location) {
        return AgeratumConstants.Preview.NAMESPACE.equals(location.getNamespace());
    }

    public static Identifier toPreviewLocation(@Nullable String fileArgument) {
        String normalized = normalizePreviewFileArgument(fileArgument);
        return Identifier.fromNamespaceAndPath(AgeratumConstants.Preview.NAMESPACE, normalized);
    }

    public static Path getPreviewRootPath() {
        return FMLLoader.getCurrent().getGameDir().resolve(AgeratumClient.CONFIG.previewPath).normalize();
    }

    public static Path resolvePreviewDocumentPath(Identifier location) {
        String path = location.getPath();
        if (!path.endsWith(AgeratumConstants.Guide.MARKDOWN_EXTENSION)) {
            path += AgeratumConstants.Guide.MARKDOWN_EXTENSION;
        }
        return resolvePreviewPath(path);
    }

    public static Path resolvePreviewAssetPath(String relativePath) {
        return resolvePreviewPath(relativePath);
    }

    private static String normalizePreviewFileArgument(@Nullable String fileArgument) {
        String file = fileArgument;
        if (file == null || file.isBlank()) {
            file = AgeratumConstants.Guide.INDEX_FILE;
        }
        file = file.trim().replace('\\', '/');
        while (file.startsWith("/")) {
            file = file.substring(1);
        }
        if (file.endsWith(AgeratumConstants.Guide.MARKDOWN_EXTENSION)) {
            file = file.substring(0, file.length() - AgeratumConstants.Guide.MARKDOWN_EXTENSION.length());
        }
        String[] segments = file.split("/");
        List<String> normalizedSegments = new java.util.ArrayList<>();
        for (String segment : segments) {
            if (segment.isEmpty() || ".".equals(segment)) {
                continue;
            }
            if ("..".equals(segment)) {
                if (!normalizedSegments.isEmpty()) {
                    normalizedSegments.removeLast();
                }
                continue;
            }
            normalizedSegments.add(segment.toLowerCase(Locale.ROOT));
        }
        if (normalizedSegments.isEmpty()) {
            return AgeratumConstants.Guide.INDEX_FILE;
        }
        return String.join("/", normalizedSegments);
    }

    private static Path resolvePreviewPath(String relativePath) {
        String normalized = relativePath.replace('\\', '/');
        while (normalized.startsWith("/")) {
            normalized = normalized.substring(1);
        }
        return getPreviewRootPath().resolve(normalized).normalize();
    }
}
