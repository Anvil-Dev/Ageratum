package dev.anvilcraft.resource.ageratum;

import com.mojang.logging.LogUtils;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import dev.anvilcraft.resource.ageratum.client.feat.markdown.component.MDComponent;
import dev.anvilcraft.resource.ageratum.client.gui.GuideScreen;
import net.minecraft.client.Minecraft;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.client.event.RegisterClientCommandsEvent;
import net.neoforged.neoforge.client.event.RegisterClientReloadListenersEvent;
import net.neoforged.neoforge.common.NeoForge;
import org.slf4j.Logger;

import java.util.List;
import java.util.Optional;

/**
 * Ageratum 模组主类。
 *
 * <p>负责模组的初始化以及客户端命令 {@code /ageratum} 的注册。
 * 该命令允许玩家从资源包中打开指定命名空间下的 Markdown 文档。</p>
 */
@Mod(Ageratum.MOD_ID)
public class Ageratum {

    /** 模组 ID，也是默认命名空间。 */
    public static final String MOD_ID = "ageratum";

    /** 模组日志记录器。 */
    private static final Logger LOGGER = LogUtils.getLogger();

    /**
     * 模组构造函数，由 NeoForge 在加载时调用。
     *
     * @param modEventBus   模组专属事件总线
     * @param modContainer  模组容器
     */
    public Ageratum(IEventBus modEventBus, ModContainer modContainer) {
        // 向 NeoForge 公共事件总线注册客户端命令监听器
        NeoForge.EVENT_BUS.addListener(Ageratum::onCommandRegister);
        // 在资源包加载/重载时预构建 Markdown 组件缓存
        modEventBus.addListener(Ageratum::onReloadListenerRegister);
    }

    /**
     * 生成以本模组 ID 为命名空间的 {@link ResourceLocation}。
     *
     * @param path 资源路径（不含命名空间前缀）
     * @return 完整的资源位置
     */
    public static ResourceLocation location(String path) {
        return ResourceLocation.fromNamespaceAndPath(Ageratum.MOD_ID, path);
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
    public static void onCommandRegister(RegisterClientCommandsEvent event) {
        event.getDispatcher().register(
            Commands.literal("ageratum")
                .then(
                    // ── 第一个参数：命名空间 ──────────────────────────
                    Commands.argument("namespace", StringArgumentType.word())
                        .suggests((context, builder) -> {
                            Minecraft minecraft = Minecraft.getInstance();
                            if (minecraft == null) {
                                return builder.buildFuture();
                            }
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
                                    if (minecraft == null) {
                                        return builder.buildFuture();
                                    }
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
    private static int openGuide(CommandContext<CommandSourceStack> context, String namespace, String fileArgument) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft == null) {
            return 0;
        }

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

        ResourceManager resourceManager = minecraft.getResourceManager();

        // 优先使用预解析缓存，缺失时回退为即时解析
        Optional<List<MDComponent>> cachedComponents = GuideDocumentCache.getParsedComponents(documentLocation);
        if (cachedComponents.isPresent()) {
            minecraft.setScreen(new GuideScreen(documentLocation, cachedComponents.get()));
            return 1;
        }

        String content = GuideDocumentLoader.read(resourceManager, documentLocation);
        minecraft.setScreen(new GuideScreen(documentLocation, content));
        return 1;
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
}
