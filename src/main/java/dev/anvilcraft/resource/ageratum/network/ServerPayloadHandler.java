package dev.anvilcraft.resource.ageratum.network;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.players.PlayerList;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import java.util.List;
import java.util.Objects;

public class ServerPayloadHandler {
    public static void handleShareGuide(ShareGuidePayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (!(context.player() instanceof ServerPlayer player)) return;
            MinecraftServer server = player.level().getServer();
            PlayerList playerList = server.getPlayerList();
            List<ServerPlayer> players = playerList.getPlayers();
            for (ServerPlayer sp : players) {
                if (payload.sameTeam() && !Objects.equals(sp.getTeam(), player.getTeam())) {
                    continue;
                }
                MutableComponent component = Component.empty();
                component.append(Component.translatable("system.ageratum.share.tip", player.getName()).withStyle(ChatFormatting.GRAY));
                component.append("\n");
                String command = "/ageratum \"%s\" \"%s\"".formatted(
                    payload.location().getNamespace(),
                    payload.location().getPath()
                );
                MutableComponent hover = Component.empty();
                hover.append(Component.literal(payload.location().toString()).withStyle(ChatFormatting.BLUE));
                String anchor = payload.anchor().trim();
                if (!anchor.isBlank()) {
                    command += " \"" + anchor + "\"";
                    hover.append("\n");
                    hover.append(Component.literal("#" + anchor).withStyle(ChatFormatting.GRAY));
                }
                component.append(
                    Component.translatable("system.ageratum.share.button")
                        .withStyle(
                            Style.EMPTY
                                .applyFormats(ChatFormatting.GREEN)
                                .withClickEvent(new ClickEvent.SuggestCommand(command))
                                .withHoverEvent(new HoverEvent.ShowText(hover))
                        )
                );
                sp.sendSystemMessage(component);
            }
        });
    }
}
