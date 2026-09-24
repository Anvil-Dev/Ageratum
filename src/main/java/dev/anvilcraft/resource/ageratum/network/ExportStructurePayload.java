package dev.anvilcraft.resource.ageratum.network;

import dev.anvilcraft.resource.ageratum.Ageratum;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

/** Bounded chunks keep structure exports below the serverbound custom payload limit. */
public record ExportStructurePayload(ResourceLocation location, int totalBytes, int offset, byte[] data)
    implements CustomPacketPayload {
    public static final int CHUNK_BYTES = 24 * 1024;
    public static final int MAX_BYTES = 8 * 1024 * 1024;
    public static final Type<ExportStructurePayload> TYPE = new Type<>(Ageratum.location("export_structure"));
    public static final StreamCodec<RegistryFriendlyByteBuf, ExportStructurePayload> STREAM_CODEC = StreamCodec.of(
        (buffer, payload) -> {
            buffer.writeResourceLocation(payload.location());
            buffer.writeVarInt(payload.totalBytes());
            buffer.writeVarInt(payload.offset());
            buffer.writeByteArray(payload.data());
        },
        buffer -> new ExportStructurePayload(buffer.readResourceLocation(), buffer.readVarInt(), buffer.readVarInt(),
            buffer.readByteArray(CHUNK_BYTES))
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
