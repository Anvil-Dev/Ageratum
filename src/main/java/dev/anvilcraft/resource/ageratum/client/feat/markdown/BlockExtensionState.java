package dev.anvilcraft.resource.ageratum.client.feat.markdown;

import net.minecraft.resources.Identifier;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 块级扩展块状态。
 *
 * <p>用于表示 {@code ::: namespace:location ...} 或 {@code <namespace:location>...</namespace:location>}
 * 形式的块级扩展块。追踪打开状态和未分析的内容，用于块级语法解析。</p>
 */
public final class BlockExtensionState extends SelfClosingBlockExtensionState {
    private final ExtensionBlockType type;
    private final String closeTagWithNamespace;
    private final String closeTagWithoutNamespace;
    private final StringBuilder content = new StringBuilder();

    /**
     * 扩展块类型枚举。
     */
    public enum ExtensionBlockType {
        /**
         * {@code ::: ... :::} 冒号语法。
         */
        COLON,
        /**
         * {@code <<...>...</...>>} 标签语法。
         */
        TAG
    }

    /**
     * 创建块级扩展块状态。
     */
    private BlockExtensionState(
        ExtensionBlockType type,
        Identifier id,
        String rawParams,
        Map<String, String> params,
        String closeTagWithNamespace,
        String closeTagWithoutNamespace
    ) {
        super(id, rawParams, params);
        this.type = type;
        this.closeTagWithNamespace = closeTagWithNamespace;
        this.closeTagWithoutNamespace = closeTagWithoutNamespace;
    }

    /**
     * 创建冒号语法（{@code :::}）的扩展块。
     */
    public static BlockExtensionState colon(Identifier id, String rawParams) {
        return new BlockExtensionState(
            ExtensionBlockType.COLON,
            id,
            rawParams,
            Map.of(),
            ":::",
            ":::"
        );
    }

    /**
     * 创建标签语法（{@code <...>}）的扩展块。
     */
    public static BlockExtensionState tag(
        Identifier id,
        String rawParams,
        Map<String, String> params
    ) {
        return new BlockExtensionState(
            ExtensionBlockType.TAG,
            id,
            rawParams,
            Collections.unmodifiableMap(new LinkedHashMap<>(params)),
            "</" + id + ">",
            "</" + id.getPath() + ">"
        );
    }

    /**
     * 获取扩展块类型。
     */
    public ExtensionBlockType type() {
        return this.type;
    }

    /**
     * 检查给定的一行是否为此块的闭合标签。
     */
    public boolean matchesCloseLine(String line) {
        String trimmed = line.trim();
        if (this.type == ExtensionBlockType.COLON) {
            return ":::".equals(trimmed);
        }
        return this.closeTagWithNamespace.equals(trimmed) || this.closeTagWithoutNamespace.equals(trimmed);
    }

    /**
     * 追加一行内容到块的缓冲区。
     */
    public void appendLine(String line) {
        this.content.append(line).append("\n");
    }

    /**
     * 获取块的完整内容（去除末尾换行）。
     */
    public String rawContent() {
        if (this.content.isEmpty()) {
            return "";
        }
        String text = this.content.toString();
        return text.endsWith("\n") ? text.substring(0, text.length() - 1) : text;
    }
}

