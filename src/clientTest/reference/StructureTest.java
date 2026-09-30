package dev.anvilcraft.resource.ageratum.test;

import com.mojang.logging.LogUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.Difficulty;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.LevelSettings;
import net.minecraft.world.level.WorldDataConfiguration;
import net.minecraft.world.level.levelgen.WorldOptions;
import net.minecraft.world.level.levelgen.presets.WorldPresets;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.ModelEvent;
import net.neoforged.neoforge.common.NeoForge;
import org.lwjgl.glfw.GLFW;


/** 1.21.1 reference captures; copied into the isolated source checkout only. */
@Mod(value = "ageratum_structure_test", dist = Dist.CLIENT)
public final class StructureTest {
    private boolean ready;
    private int stage;
    private int frames;
    private long started = System.nanoTime();

    public StructureTest(IEventBus bus) {
        bus.addListener((ModelEvent.BakingCompleted event) -> this.ready = true);
        NeoForge.EVENT_BUS.addListener(this::tick);
        NeoForge.EVENT_BUS.addListener(this::render);
    }

    private void tick(ClientTickEvent.Post event) {
        Minecraft mc = Minecraft.getInstance();
        mc.options.pauseOnLostFocus = false;
        GLFW.glfwHideWindow(mc.getWindow().getWindow());
        if (this.stage == 9) return;
        if (System.nanoTime() - this.started > 180_000_000_000L) {
            LogUtils.getLogger().error("AGERATUM_STRUCTURE_TEST FAILED: Timed out");
            this.stage = 9; Minecraft.getInstance().stop();
            return;
        }
        if (mc.getOverlay() != null || this.stage != 0 || !this.ready) return;
        this.stage = 1;
        GameRules rules = new GameRules();
        rules.getRule(GameRules.RULE_SPAWN_CHUNK_RADIUS).set(0, null);
        LevelSettings settings = new LevelSettings("Structure regression", GameType.CREATIVE,
            false, Difficulty.PEACEFUL, false, rules, WorldDataConfiguration.DEFAULT);
        mc.createWorldOpenFlows().createFreshLevel("structure-" + System.currentTimeMillis(), settings,
            new WorldOptions(17, false, false),
            access -> access.registryOrThrow(Registries.WORLD_PRESET).getHolderOrThrow(WorldPresets.FLAT)
                .value().createWorldDimensions(), mc.screen);
    }

    private void render(net.neoforged.neoforge.client.event.RenderFrameEvent.Post event) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null
            || mc.player == null || mc.getOverlay() != null || this.stage == 9) return;
        try {
            if (this.stage == 1) {
                mc.options.guiScale().set(1);
                mc.resizeDisplay();
                component = dev.anvilcraft.resource.ageratum.client.feat.markdown.component.extend.MDNBTStructureComponent.parse(
                    new dev.anvilcraft.resource.ageratum.client.feat.markdown.MDExtensionContext(
                        ResourceLocation.parse("ageratum:ageratum/port.md"), ResourceLocation.parse("ageratum:structure"), "",
                        java.util.Map.of("id", "ageratum:ageratum/structure_port.snbt"), java.util.List.of(), ""));
                mc.setScreen(new Preview());
                this.stage = 2;
                this.frames = 0;
            } else if (this.stage == 2 && ++this.frames > 80) {
                net.minecraft.client.Screenshot.grab(mc.gameDirectory, "preview-full.png", mc.getMainRenderTarget(), message -> {});
                component.keyPressed(mc, 0, 0, GLFW.GLFW_KEY_PAGE_DOWN, 0, 0, 320);
                this.stage = 3;
                this.frames = 0;
            } else if (this.stage == 3 && ++this.frames > 80) {
                net.minecraft.client.Screenshot.grab(mc.gameDirectory, "preview-layer.png", mc.getMainRenderTarget(), message -> {});
                this.stage = 4;
                this.frames = 0;
            } else if (this.stage == 4 && ++this.frames > 30) {
                mc.options.guiScale().set(2);
                mc.resizeDisplay();
                component.mouseClicked(mc, 308, 12, 0, 320);
                this.stage = 5;
                this.frames = 0;
            } else if (this.stage == 5 && ++this.frames > 80) {
                net.minecraft.client.Screenshot.grab(mc.gameDirectory, "projection-fixed.png", mc.getMainRenderTarget(), message -> {});
                this.stage = 6;
                this.frames = 0;
            } else if (this.stage == 6 && ++this.frames > 30) {
                LogUtils.getLogger().info("AGERATUM_STRUCTURE_TEST PASS: reference preview and projection captures");
                this.stage = 9;
                mc.stop();
            }

        } catch (Throwable exception) {
            LogUtils.getLogger().error("AGERATUM_STRUCTURE_TEST FAILED", exception);
            this.stage = 9;
            mc.stop();
        }
    }
    private static dev.anvilcraft.resource.ageratum.client.feat.markdown.component.MDComponent component;
    private static final class Preview extends net.minecraft.client.gui.screens.Screen {
        Preview() { super(net.minecraft.network.chat.Component.literal("Structure port")); }
        @Override public boolean isPauseScreen() { return false; }
        @Override public void render(net.minecraft.client.gui.GuiGraphics graphics, int x, int y, float partialTick) {
            graphics.fill(0, 0, this.width, this.height, 0xFFF1E6CD);
            graphics.pose().pushPose();
            graphics.pose().translate(20, 30, 0);
            component.render(new dev.anvilcraft.resource.ageratum.client.feat.markdown.MDRenderContext(
                null, Minecraft.getInstance(), graphics, new java.util.ArrayList<>(), this.width, this.height,
                320, 260, -1, -1, 20, 30, 1, 20, 30, new java.util.ArrayList<>()));
            graphics.pose().popPose();
        }
    }
}
