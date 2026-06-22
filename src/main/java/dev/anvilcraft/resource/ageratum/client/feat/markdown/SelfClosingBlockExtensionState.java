package dev.anvilcraft.resource.ageratum.client.feat.markdown;

import net.minecraft.resources.Identifier;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 自闭合扩展块状态。
 *
 * <p>用于表示 {@code <<namespace:location key=value .../>>} 形式的自闭合扩展块，
 * 提供与 {@link BlockExtensionState} 兼容的接口以便统一处理。</p>
 */
public class SelfClosingBlockExtensionState {
    private final Identifier id;
    private final String rawParams;
    private final Map<String, String> params;

    /**
     * 创建自闭合扩展块状态。
     */
    public SelfClosingBlockExtensionState(
        Identifier id,
        String rawParams,
        Map<String, String> params
    ) {
        this.id = id;
        this.rawParams = rawParams;
        this.params = Collections.unmodifiableMap(new LinkedHashMap<>(params));
    }

    /**
     * 获取扩展组件 ID。
     */
    public Identifier id() {
        return this.id;
    }

    /**
     * 获取参数的原始字符串。
     */
    public String rawParams() {
        return this.rawParams;
    }

    /**
     * 获取解析后的参数键值对。
     */
    public Map<String, String> params() {
        return this.params;
    }
}

