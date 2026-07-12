package dev.anvilcraft.resource.ageratum.item.component;

import com.mojang.datafixers.util.Either;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.Identifier;

public record Doc(Identifier id) {
    public static final Codec<Doc> STRICT = RecordCodecBuilder.create(instance -> instance.group(
        Identifier.CODEC.fieldOf("id").forGetter(Doc::id)
    ).apply(instance, Doc::new));
    public static final Codec<Doc> CODEC = Codec.either(
        Identifier.CODEC.xmap(Doc::new, Doc::id),
        STRICT
    ).xmap(e -> e.left().orElseGet(e.right()::get), Either::left);
    public static final StreamCodec<FriendlyByteBuf, Doc> STREAM_CODEC = StreamCodec.composite(
        Identifier.STREAM_CODEC,
        Doc::id,
        Doc::new
    );
}
