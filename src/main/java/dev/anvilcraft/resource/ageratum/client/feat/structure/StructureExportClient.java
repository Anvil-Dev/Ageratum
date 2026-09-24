package dev.anvilcraft.resource.ageratum.client.feat.structure;

import dev.anvilcraft.resource.ageratum.network.ExportStructurePayload;
import lombok.extern.slf4j.Slf4j;
import net.minecraft.client.Minecraft;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtIo;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import net.neoforged.neoforge.network.PacketDistributor;

import java.io.ByteArrayOutputStream;
import java.util.Arrays;

@Slf4j
public final class StructureExportClient {
    private StructureExportClient() {
    }

    public static void export(ResourceLocation location, StructureTemplate template) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null || minecraft.getConnection() == null) return;
        try {
            ByteArrayOutputStream output = new ByteArrayOutputStream();
            NbtIo.writeCompressed(template.save(new CompoundTag()), output);
            byte[] data = output.toByteArray();
            if (data.length > ExportStructurePayload.MAX_BYTES) {
                minecraft.player.displayClientMessage(Component.translatable("system.ageratum.structure_export.too_large"), false);
                return;
            }
            for (int offset = 0; offset < data.length; offset += ExportStructurePayload.CHUNK_BYTES) {
                byte[] chunk = Arrays.copyOfRange(data, offset, Math.min(data.length, offset + ExportStructurePayload.CHUNK_BYTES));
                PacketDistributor.sendToServer(new ExportStructurePayload(location, data.length, offset, chunk));
            }
            minecraft.player.displayClientMessage(Component.translatable("system.ageratum.structure_export.sending"), false);
        } catch (Exception exception) {
            log.warn("Failed to send structure export {}", location, exception);
            minecraft.player.displayClientMessage(Component.translatable("system.ageratum.structure_export.failed"), false);
        }
    }
}
