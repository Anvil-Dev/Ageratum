package dev.anvilcraft.resource.ageratum.client;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.logging.LogUtils;
import dev.anvilcraft.resource.ageratum.Ageratum;
import dev.anvilcraft.resource.ageratum.client.feat.markdown.BuiltinExtensionComponents;
import dev.anvilcraft.resource.ageratum.client.feat.markdown.GuideDocumentCache;
import dev.anvilcraft.resource.ageratum.client.feat.markdown.GuideDocumentLoader;
import dev.anvilcraft.resource.ageratum.client.feat.markdown.MDDocument;
import dev.anvilcraft.resource.ageratum.client.feat.markdown.MarkdownParser;
import dev.anvilcraft.resource.ageratum.client.gui.GuideScreen;
import dev.anvilcraft.resource.ageratum.client.registries.AgeratumRegistries;
import net.minecraft.client.Minecraft;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.client.event.RegisterClientCommandsEvent;
import net.neoforged.neoforge.client.event.RegisterClientReloadListenersEvent;
import org.slf4j.Logger;

import java.util.Optional;
import javax.annotation.Nullable;

@Mod(value = Ageratum.MOD_ID, dist = Dist.CLIENT)
@EventBusSubscriber(modid = Ageratum.MOD_ID, value = Dist.CLIENT)
public class AgeratumClient {
    /**
     * 模组日志记录器。
     */
    private static final Logger LOGGER = LogUtils.getLogger();

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
    }

    /**
     * 获取客户端当前语言代码。
     *
     * <p>若无法读取语言管理器，回退到 {@code en_us}。</p>
     */
    private static String getClientLanguageCode(Minecraft minecraft) {
        try {
            return minecraft.getLanguageManager().getSelected();
        } catch (RuntimeException exception) {
            LOGGER.warn("Failed to read client language code, fallback to en_us", exception);
            return GuideDocumentLoader.DEFAULT_LANGUAGE_CODE;
        }
    }

    /**
     * 注册客户端命令 {@code /ageratum}。
     *
     * <p>命令格式：</p>
     * <pre>
     *   /ageratum &lt;namespace&gt;               — 打开该命名空间的 index.md
     *   /ageratum &lt;namespace&gt; &lt;file&gt;        — 打开指定文件（不需要 .md 后缀）
     * </pre>
     * <p>两个参数均支持 Tab 补全，仅显示资源包中实际存在的值。</p>
     *
     * @param event 命令注册事件
     */
    @SubscribeEvent
    public static void onCommandRegister(RegisterClientCommandsEvent event) {
        event.getDispatcher().register(
            Commands.literal("ageratum")
                .then(
                    // ── 第一个参数：命名空间 ──────────────────────────
                    Commands.argument("namespace", StringArgumentType.word())
                        .suggests((context, builder) -> {
                            Minecraft minecraft = Minecraft.getInstance();
                            // 枚举资源包中所有含有 ageratum/*.md 的命名空间
                            return SharedSuggestionProvider.suggest(
                                GuideDocumentLoader.listNamespaces(minecraft.getResourceManager(), getClientLanguageCode(minecraft)),
                                builder
                            );
                        })
                        // 仅提供 namespace，file 缺省为 index.md
                        .executes(context -> openGuide(context, StringArgumentType.getString(context, "namespace"), null))
                        .then(
                            // ── 第二个参数（可选）：文件名 ──────────────
                            Commands.argument("file", StringArgumentType.word())
                                .suggests((context, builder) -> {
                                    Minecraft minecraft = Minecraft.getInstance();
                                    String namespace = StringArgumentType.getString(context, "namespace");
                                    // 枚举该命名空间下的所有 .md 文件（返回不含扩展名的相对路径）
                                    return SharedSuggestionProvider.suggest(
                                        GuideDocumentLoader.listFiles(
                                            minecraft.getResourceManager(),
                                            namespace,
                                            getClientLanguageCode(minecraft)
                                        ),
                                        builder
                                    );
                                })
                                .executes(context -> openGuide(
                                    context,
                                    StringArgumentType.getString(context, "namespace"),
                                    StringArgumentType.getString(context, "file")
                                ))
                        )
                ));
    }

    /**
     * 注册客户端资源重载监听器。
     */
    @SubscribeEvent
    public static void onReloadListenerRegister(RegisterClientReloadListenersEvent event) {
        event.registerReloadListener(GuideDocumentCache.reloadListener());
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
    private static int openGuide(CommandContext<CommandSourceStack> context, String namespace, @Nullable String fileArgument) {
        Minecraft minecraft = Minecraft.getInstance();

        String languageCode = getClientLanguageCode(minecraft);

        // 将 namespace + languageCode + fileArgument 解析为存在的 ResourceLocation（带回退）
        ResourceLocation documentLocation;
        try {
            Optional<ResourceLocation> resolved = GuideDocumentLoader.resolveExistingLocation(
                minecraft.getResourceManager(),
                namespace,
                languageCode,
                fileArgument
            );
            if (resolved.isEmpty()) {
                context.getSource().sendFailure(Component.literal(
                    "Guide file not found for language '" + languageCode + "'."
                ));
                return 0;
            }
            documentLocation = resolved.get();
        } catch (RuntimeException exception) {
            context.getSource().sendFailure(Component.literal("Invalid guide path."));
            return 0;
        }
        if (!openGuideOnClient(documentLocation)) {
            context.getSource().sendFailure(
                Component.literal(
                    "Guide file not found: assets/" + documentLocation.getNamespace() + "/" + documentLocation.getPath()
                )
            );
            return 0;
        }
        return 1;
    }

    /**
     * 客户端本地打开文档；若不存在则返回 false。
     */
    public static boolean openGuideOnClient(ResourceLocation location) {
        return openGuideOnClient(location, null);
    }

    /**
     * 客户端本地打开文档，可选指定锚点；若不存在则返回 false。
     *
     * @param location 文档资源位置
     * @param anchor   目标锚点（可为 null）
     */
    public static boolean openGuideOnClient(ResourceLocation location, @Nullable String anchor) {
        Minecraft minecraft = Minecraft.getInstance();
        ResourceManager resourceManager = minecraft.getResourceManager();
        if (!GuideDocumentLoader.exists(resourceManager, location)) {
            return false;
        }

        int inheritedLabelScrollRows = 0;
        double inheritedLabelScrollRemainder = 0.0d;
        if (minecraft.screen instanceof GuideScreen currentGuideScreen) {
            inheritedLabelScrollRows = currentGuideScreen.getLabelScrollRows();
            inheritedLabelScrollRemainder = currentGuideScreen.getLabelScrollRemainder();
        }

        // 优先使用预解析缓存，缺失时回退为即时解析
        Optional<MDDocument> cachedDocument = GuideDocumentCache.getParsedDocument(location);
        if (cachedDocument.isPresent()) {
            GuideScreen screen = new GuideScreen(location, cachedDocument.get().components());
            screen.setAnchor(anchor);
            screen.setLabelScrollState(inheritedLabelScrollRows, inheritedLabelScrollRemainder);
            minecraft.setScreen(screen);
            return true;
        }

        String content = GuideDocumentLoader.read(resourceManager, location);
        MDDocument parsedDocument = new MarkdownParser().parseDocument(location, content);
        GuideScreen screen = new GuideScreen(location, parsedDocument.components());
        screen.setAnchor(anchor);
        screen.setLabelScrollState(inheritedLabelScrollRows, inheritedLabelScrollRemainder);
        minecraft.setScreen(screen);
        return true;
    }
}
