package dev.anvilcraft.resource.ageratum;

import com.mojang.logging.LogUtils;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
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
import net.neoforged.neoforge.common.NeoForge;
import org.slf4j.Logger;

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
                                GuideDocumentLoader.listNamespaces(minecraft.getResourceManager()),
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
                                        GuideDocumentLoader.listFiles(minecraft.getResourceManager(), namespace),
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

        // 将 namespace + fileArgument 规范化为完整 ResourceLocation
        ResourceLocation documentLocation;
        try {
            documentLocation = GuideDocumentLoader.toDocumentLocation(namespace, fileArgument);
        } catch (RuntimeException exception) {
            context.getSource().sendFailure(Component.literal("Invalid guide path."));
            return 0;
        }

        ResourceManager resourceManager = minecraft.getResourceManager();

        // 检查文件是否存在于当前资源包中
        if (!GuideDocumentLoader.exists(resourceManager, documentLocation)) {
            context.getSource().sendFailure(Component.literal("Guide file not found: assets/"
                + documentLocation.getNamespace() + "/" + documentLocation.getPath()));
            return 0;
        }

        // 读取文件内容并打开文档界面
        String content = GuideDocumentLoader.read(resourceManager, documentLocation);
        minecraft.setScreen(new GuideScreen(documentLocation, content));
        return 1;
    }
}
