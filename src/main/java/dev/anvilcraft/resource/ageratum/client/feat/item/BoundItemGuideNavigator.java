package dev.anvilcraft.resource.ageratum.client.feat.item;

import com.mojang.blaze3d.platform.InputConstants;
import com.mojang.datafixers.util.Either;
import dev.anvilcraft.resource.ageratum.Ageratum;
import dev.anvilcraft.resource.ageratum.client.AgeratumClient;
import dev.anvilcraft.resource.ageratum.client.feat.markdown.GuideDocumentCache;
import net.minecraft.ChatFormatting;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.FormattedText;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.inventory.tooltip.TooltipComponent;
import net.minecraft.world.item.ItemStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.neoforge.client.event.RenderTooltipEvent;
import org.lwjgl.glfw.GLFW;

import java.util.List;
import java.util.Optional;
import javax.annotation.Nullable;

@EventBusSubscriber(modid = Ageratum.MOD_ID, value = Dist.CLIENT)
public final class BoundItemGuideNavigator {
    private static final long HOLD_DURATION_MS = 1_500L;
    private static final long HOVER_STALE_MS = 200L;

    @Nullable
    private static ResourceLocation hoveredItemId;
    private static long hoverSeenAtMs;

    @Nullable
    private static ResourceLocation holdingItemId;
    private static long holdStartAtMs = -1L;
    private static boolean openedDuringCurrentHold;

    private BoundItemGuideNavigator() {
    }

    @SubscribeEvent
    public static void onTooltipGather(RenderTooltipEvent.GatherComponents event) {
        ItemStack stack = event.getItemStack();
        if (stack.isEmpty()) {
            hoveredItemId = null;
            return;
        }

        ResourceLocation itemId = BuiltInRegistries.ITEM.getKey(stack.getItem());
        Minecraft minecraft = Minecraft.getInstance();
        String languageCode = AgeratumClient.getClientLanguageCode(minecraft);
        Optional<ResourceLocation> targetDocument = GuideDocumentCache.getFirstDocumentByItemId(itemId, languageCode);
        if (targetDocument.isEmpty()) {
            hoveredItemId = null;
            return;
        }

        long now = System.currentTimeMillis();
        hoveredItemId = itemId;
        hoverSeenAtMs = now;

        double progress = getCurrentProgressPercent(now, itemId, isWDown(minecraft));
        List<Either<FormattedText, TooltipComponent>> tooltipElements = event.getTooltipElements();
        if (progress <= 0) {
            tooltipElements.add(Either.left(
                Component.translatable("tooltip.ageratum.bind_item_hold", Component.keybind("key.ageratum.more_info"))
                    .withStyle(ChatFormatting.GRAY)
            ));
        } else {
            final int MAX_COUNT = 20;
            int count = (int) Math.round(MAX_COUNT * progress);
            StringBuilder processed = new StringBuilder();
            processed.repeat("|", Math.clamp(count, 0, MAX_COUNT));
            StringBuilder remaining = new StringBuilder();
            remaining.repeat("|", Math.clamp(MAX_COUNT - count, 0, MAX_COUNT));
            Component progressComponent = Component.empty()
                .append(Component.literal("[").withStyle(ChatFormatting.WHITE))
                .append(Component.literal(processed.toString()).withStyle(ChatFormatting.WHITE))
                .append(Component.literal(remaining.toString()).withStyle(ChatFormatting.DARK_GRAY))
                .append(Component.literal("]").withStyle(ChatFormatting.WHITE));
            tooltipElements.add(Either.left(progressComponent));
        }
    }

    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null || minecraft.level == null) {
            clearAllState();
            return;
        }

        long now = System.currentTimeMillis();
        if (hoveredItemId == null || now - hoverSeenAtMs > HOVER_STALE_MS) {
            hoveredItemId = null;
            resetHoldState();
            return;
        }

        if (!isWDown(minecraft)) {
            resetHoldState();
            return;
        }

        if (!hoveredItemId.equals(holdingItemId)) {
            holdingItemId = hoveredItemId;
            holdStartAtMs = now;
            openedDuringCurrentHold = false;
            return;
        }

        if (openedDuringCurrentHold || holdStartAtMs < 0L || now - holdStartAtMs < HOLD_DURATION_MS) {
            return;
        }

        String languageCode = AgeratumClient.getClientLanguageCode(minecraft);
        Optional<ResourceLocation> targetDocument = GuideDocumentCache.getFirstDocumentByItemId(holdingItemId, languageCode);
        if (targetDocument.isPresent() && AgeratumClient.openGuideOnClient(targetDocument.get(), List.of())) {
            openedDuringCurrentHold = true;
        }
    }

    private static double getCurrentProgressPercent(long now, ResourceLocation itemId, boolean wDown) {
        if (!wDown || holdStartAtMs < 0L || !itemId.equals(holdingItemId)) {
            return 0;
        }
        long elapsed = Math.max(0L, now - holdStartAtMs);
        return Math.min(100L, (double) elapsed / HOLD_DURATION_MS);
    }

    public static final KeyMapping W_KEY_MAPPING = new KeyMapping("key.ageratum.more_info", GLFW.GLFW_KEY_W, "key.categories.ageratum");

    private static boolean isWDown(Minecraft minecraft) {
        long window = minecraft.getWindow().getWindow();
        return InputConstants.isKeyDown(window, W_KEY_MAPPING.getKey().getValue());
    }

    @SubscribeEvent
    public static void onKetReg(RegisterKeyMappingsEvent event) {
        event.register(W_KEY_MAPPING);
    }

    private static void resetHoldState() {
        holdingItemId = null;
        holdStartAtMs = -1L;
        openedDuringCurrentHold = false;
    }

    private static void clearAllState() {
        hoveredItemId = null;
        hoverSeenAtMs = 0L;
        resetHoldState();
    }
}


