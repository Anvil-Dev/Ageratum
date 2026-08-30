package dev.anvilcraft.resource.ageratum.item.component;

import com.mojang.datafixers.util.Either;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.anvilcraft.resource.ageratum.client.feat.github.GitHubDocUri;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.Identifier;

import javax.annotation.Nullable;

/**
 * 手册指向的文档。
 *
 * <p>值为字符串形式：</p>
 * <ul>
 *   <li>普通资源包文档：{@code ageratum:index}（Identifier 字符串形式）；</li>
 *   <li>GitHub 远程指南：{@code github:user/repo#path:assets/xxx&commit=sha}（见 {@link GitHubDocUri}）。</li>
 * </ul>
 *
 * <p>Codec 同时兼容旧格式（纯 Identifier 或 {@code {"id": "..."}} 对象）。</p>
 */
public record Doc(String value) {
    /**
     * 旧格式的严格 Codec（{@code {"id": "..."}}）。
     */
    public static final Codec<Doc> STRICT = RecordCodecBuilder.create(instance -> instance.group(
        Codec.STRING.fieldOf("id").forGetter(Doc::value)
    ).apply(instance, Doc::new));

    /**
     * 兼容 Codec：优先按普通字符串解码，其次按旧对象格式解码。
     */
    public static final Codec<Doc> CODEC = Codec.either(
        Codec.STRING.xmap(Doc::new, Doc::value),
        STRICT
    ).xmap(e -> e.left().orElseGet(e.right()::get), doc -> Either.left(doc));

    public static final StreamCodec<FriendlyByteBuf, Doc> STREAM_CODEC = StreamCodec.of(
        (buf, doc) -> buf.writeUtf(doc.value()),
        buf -> new Doc(buf.readUtf())
    );

    /**
     * 由 Identifier 构造。
     */
    public static Doc of(Identifier id) {
        return new Doc(id.toString());
    }

    /**
     * 是否为 GitHub 远程指南。
     */
    public boolean isGitHub() {
        return GitHubDocUri.isGitHubDoc(this.value);
    }

    /**
     * 解析为 Identifier；GitHub 远程指南返回 {@code null}。
     */
    @Nullable
    public Identifier id() {
        if (this.isGitHub()) {
            return null;
        }
        return Identifier.tryParse(this.value);
    }
}
