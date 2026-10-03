package dev.anvilcraft.resource.ageratum.test;

import com.mojang.logging.LogUtils;
import com.mojang.blaze3d.vertex.PoseStack;
import dev.anvilcraft.lib.v2.rendering.projection.ProjectionRenderer;
import dev.anvilcraft.lib.v2.rendering.projection.ProjectionScene;
import dev.anvilcraft.resource.ageratum.client.AgeratumKeyMappings;
import dev.anvilcraft.resource.ageratum.client.constants.AgeratumConstants;
import dev.anvilcraft.resource.ageratum.client.feat.markdown.MDExtensionContext;
import dev.anvilcraft.resource.ageratum.client.feat.markdown.MDRenderContext;
import dev.anvilcraft.resource.ageratum.client.feat.markdown.component.MDComponent;
import dev.anvilcraft.resource.ageratum.client.feat.markdown.component.extend.MDNBTStructureComponent;
import dev.anvilcraft.resource.ageratum.client.feat.structure.StructureProjectionApi;
import dev.anvilcraft.resource.ageratum.client.feat.structure.StructureProjectionManager;
import dev.anvilcraft.resource.ageratum.client.util.ViewportCameraRig;
import dev.anvilcraft.resource.ageratum.client.util.level.SandboxRenderLevel;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.Difficulty;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.LevelSettings;
import net.minecraft.world.level.WorldDataConfiguration;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.levelgen.WorldOptions;
import net.minecraft.world.level.levelgen.presets.WorldPresets;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.InputEvent;
import net.neoforged.neoforge.client.event.ModelEvent;
import net.neoforged.neoforge.client.event.RenderFrameEvent;
import net.neoforged.neoforge.common.NeoForge;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

/** Opt-in real-client preview, input, mesh and resource-lifecycle regression. */
@Mod(value = "ageratum_structure_test", dist = Dist.CLIENT)
public final class StructureTest {
    public static boolean control;
    private boolean ready;
    private int stage;
    private long started = System.currentTimeMillis();
    private long next;
    private MDComponent component;
    private ProjectionRenderer previous;
    private StructureTemplate template;
    private CompletableFuture<Void> reload;
    private Object mesh;
    private int checks;

    public StructureTest(IEventBus bus) {
        if (Boolean.getBoolean("ageratum.layoutTest")) { new LayoutClientTest(bus); return; }
        bus.addListener((ModelEvent.BakingCompleted event) -> this.ready = true);
        NeoForge.EVENT_BUS.addListener(this::tick);
        NeoForge.EVENT_BUS.addListener(this::frame);
    }

    private void tick(ClientTickEvent.Post event) {
        var mc = Minecraft.getInstance();
        mc.options.pauseOnLostFocus = false;
        if (this.stage == 99) return;
        if (System.currentTimeMillis() - this.started > 240000) {
            fail(new IllegalStateException("Timed out at " + this.stage));
            return;
        }
        if (mc.getOverlay() != null || this.stage != 0 || !this.ready) return;
        this.stage = 1;
        String name = "structure-port-" + System.currentTimeMillis();
        mc.createWorldOpenFlows().createFreshLevel(name,
            new LevelSettings(name, GameType.CREATIVE,
                new LevelSettings.DifficultySettings(Difficulty.PEACEFUL, false, false), true, WorldDataConfiguration.DEFAULT),
            new WorldOptions(17, false, false), WorldPresets::createFlatWorldDimensions, mc.screen);
    }

    private void frame(RenderFrameEvent.Post event) {
        var mc = Minecraft.getInstance();
        if (this.stage == 99 || mc.level == null || mc.player == null || mc.getOverlay() != null
            || System.currentTimeMillis() < this.next) return;
        try {
            if (this.stage == 1) {
                if (mc.screen != null) return;
                mc.options.guiScale().set(1);
                mc.resizeGui();
                this.component = MDNBTStructureComponent.parse(new MDExtensionContext(
                    Identifier.parse("ageratum:ageratum/port.md"), Identifier.parse("ageratum:structure"), "",
                    Map.of("id", "ageratum:ageratum/structure_port.snbt"), List.of(), ""));
                mc.setScreen(new Preview());
                advance();
            } else if (this.stage == 2) {
                this.template = (StructureTemplate) field(this.component, "structureTemplateCache");
                check(this.template != null, "preview loads");
                check((int) field(this.component, "totalLayerCount") == 3, "all three layers are counted");
                capture("preview-full");
                int up = layerUp(mc);
                int size = mc.font.lineHeight + AgeratumConstants.GuideScreenUI.Positions.LAYER_INDICATOR_PADDING;
                check(this.component.mouseClicked(mc, up + size + 3, 6, 0, 320), "layer minus handles click");
                check((int) field(this.component, "visibleLayerCount") == 2, "layer minus removes one layer");
                advance();
            } else if (this.stage == 3) {
                capture("preview-layer");
                this.component.keyPressed(mc, 0, 0, GLFW.GLFW_KEY_PAGE_DOWN, 0, 0, 320);
                this.component.keyPressed(mc, 0, 0, GLFW.GLFW_KEY_PAGE_DOWN, 0, 0, 320);
                check((int) field(this.component, "visibleLayerCount") == 1, "layer lower bound");
                this.component.mouseClicked(mc, layerUp(mc) + 1, 6, 0, 320);
                check((int) field(this.component, "visibleLayerCount") == 2, "layer plus handles click");
                this.component.keyPressed(mc, 0, 0, GLFW.GLFW_KEY_PAGE_UP, 0, 0, 320);
                this.component.mouseClicked(mc, 180, 80, 0, 320);
                this.component.mouseDragged(mc, 180, 80, 0, 24, -12, 320);
                this.component.mouseReleased(mc, 180, 80, 0, 320);
                check((float) field(this.component, "panOffsetX") == 24, "horizontal pan");
                this.component.mouseClicked(mc, 180, 80, 1, 320);
                this.component.mouseDragged(mc, 180, 80, 1, 30, 10, 320);
                this.component.mouseReleased(mc, 180, 80, 1, 320);
                control = true;
                check(this.component.mouseScrolled(mc, 180, 80, 1, 320), "control wheel zoom");
                control = false;
                advance();
            } else if (this.stage == 4) {
                check(Math.abs(((ViewportCameraRig) field(this.component, "cameraRig")).getZoom() - 2.2f) < 0.0001f,
                    "zoom persists after render");
                capture("preview-drag-zoom");
                mc.options.guiScale().set(2);
                mc.resizeGui();
                advance();
            } else if (this.stage == 5) {
                capture("preview-gui2");
                this.component.mouseClicked(mc, 308, 12, 0, 320);
                check(StructureProjectionApi.hasActiveProjection() && mc.screen == null, "button starts floating projection");
                advance();
            } else if (this.stage == 6) {
                this.previous = renderer();
                check(this.previous.isValid(), "floating projection renders");
                check((boolean) field(active(), "floating"), "floating state");
                var input = new InputEvent.InteractionKeyMappingTriggered(1, mc.options.keyUse, net.minecraft.world.InteractionHand.MAIN_HAND);
                StructureProjectionManager.onInteractionKeyMappingTriggered(input);
                check(!(boolean) field(active(), "floating") && input.isCanceled(), "use fixes projection and consumes interaction");
                this.mesh = field(this.previous, "mesh");
                check(this.mesh != null, "native GPU mesh exists");
                capture("projection-fixed");
                this.reload = mc.reloadResourcePacks();
                advance();
            } else if (this.stage == 7) {
                if (!this.reload.isDone()) return;
                this.reload.join();
                check(this.previous.isValid(), "projection renders after reload");
                check(field(this.previous, "mesh") != this.mesh, "reload replaces GPU mesh");
                check(StructureProjectionApi.show(this.template, new BlockPos(0, 0, 0)), "fixed replacement");
                check(!this.previous.isValid(), "replacement closes original renderer");
                this.previous = renderer();
                advance();
            } else if (this.stage == 8) {
                check(this.previous.isValid(), "fixed replacement renders");
                this.mesh = field(this.previous, "mesh");
                mc.player.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND,
                    dev.anvilcraft.resource.ageratum.init.AgeratumItems.DEFAULT_GUIDE_ITEM.get().getDefaultInstance());
                control = true;
                var scroll = new InputEvent.MouseScrollingEvent(0, 1, false, false, false, 0, 0);
                StructureProjectionManager.onMouseScrolling(scroll);
                control = false;
                check(scroll.isCanceled(), "control wheel consumes scroll");
                check(!field(active(), "origin").equals(BlockPos.ZERO), "control wheel moves fixed projection");
                advance();
            } else if (this.stage == 9) {
                check(field(this.previous, "mesh") == this.mesh, "moving projection reuses mesh");
                var method = active().getClass().getDeclaredMethod("shrinkVisibleLayers");
                method.setAccessible(true);
                method.invoke(active());
                advance();
            } else if (this.stage == 10) {
                check(field(this.previous, "mesh") != this.mesh, "layer change rebuilds mesh");
                StructureProjectionApi.clear();
                check(!this.previous.isValid() && !StructureProjectionApi.hasActiveProjection(), "clear releases resources");
                checkLibrary(mc);
                preview(mc, "thin");
                advance();
            } else if (this.stage == 11) {
                capture("preview-thin");
                check((int) field(this.component, "totalLayerCount") == 1, "thin structure keeps its only layer");
                preview(mc, "flat");
                advance();
            } else if (this.stage == 12) {
                capture("preview-flat");
                preview(mc, "single");
                advance();
            } else if (this.stage == 13) {
                capture("preview-single");
                check(this.component.getHeight(mc, 320, 260) >= 46, "small preview leaves space for controls");
                this.component.keyPressed(mc, 0, 0, GLFW.GLFW_KEY_PAGE_DOWN, 0, 0, 320);
                check((int) field(this.component, "visibleLayerCount") == 1, "single block cannot hide its only layer");
                LogUtils.getLogger().info("AGERATUM_STRUCTURE_TEST PASS: {} checks; preview, input, fixed/floating, fluid, BER, entity, cache, reload and cleanup", this.checks);
                this.stage = 99;
                mc.stop();
            }
        } catch (Throwable failure) { fail(failure); }
    }

    private void checkLibrary(Minecraft mc) throws Exception {
        var colors = new java.util.ArrayList<Integer>();
        var sink = (com.mojang.blaze3d.vertex.VertexConsumer) java.lang.reflect.Proxy.newProxyInstance(
            getClass().getClassLoader(), new Class<?>[] {com.mojang.blaze3d.vertex.VertexConsumer.class},
            (proxy, method, args) -> {
                if (method.getName().equals("setColor") && args.length == 4) colors.add((Integer) args[3]);
                return proxy;
            });
        var alpha = new dev.anvilcraft.resource.ageratum.client.util.AlphaVertexConsumer(sink, 0.45f);
        alpha.addVertex(0, 0, 0).setUv(0, 0).setColor(255, 255, 255, 255);
        check(colors.equals(List.of(115)), "chained vertex methods preserve alpha wrapper");
        var scene = new ProjectionScene(mc.level, BlockPos.ZERO);
        scene.put(new BlockPos(-1, 0, 0), Blocks.WATER.defaultBlockState(), null);
        scene.put(new BlockPos(0, 0, 0), Blocks.WATER.defaultBlockState(), null);
        check(scene.shifted(new BlockPos(-1, 0, 0)).getFluidState(new BlockPos(1, 0, 0)).isSource(), "fluid neighbours use local coordinates");
        var entity = EntityType.ARMOR_STAND.create(mc.level, EntitySpawnReason.LOAD);
        entity.setPos(0.5, 0, 0.5);
        scene.addEntity(entity);
        try (var renderer = new ProjectionRenderer()) {
            boolean rejected = false;
            try { renderer.rebuild(scene, 256); } catch (IllegalArgumentException expected) { rejected = true; }
            check(rejected, "invalid opacity is rejected");
            renderer.rebuild(scene, 115);
            check(renderer.isValid(), "independent renderer builds");
            renderer.render(new PoseStack(), mc.gameRenderer.getGameRenderState().levelRenderState.cameraRenderState);
            check(((List<?>) field(renderer, "entities")).size() == 1, "entity rendering is included");
            renderer.close();
            check(!renderer.isValid(), "close invalidates renderer");
        }
    }

    private void preview(Minecraft mc, String name) {
        mc.options.guiScale().set(1);
        mc.resizeGui();
        this.component = MDNBTStructureComponent.parse(new MDExtensionContext(
            Identifier.parse("ageratum:ageratum/port.md"), Identifier.parse("ageratum:structure"), "",
            Map.of("id", "ageratum:ageratum/" + name + ".snbt"), List.of(), ""));
        mc.setScreen(new Preview());
    }

    private int layerUp(Minecraft mc) throws Exception {
        int padding = AgeratumConstants.GuideScreenUI.Positions.LAYER_INDICATOR_PADDING;
        String label = "层数: " + field(this.component, "visibleLayerCount") + "/" + field(this.component, "totalLayerCount");
        return 4 + padding + dev.anvilcraft.resource.ageratum.client.gui.GuideFont.get().width(label) + padding + 2;
    }
    private void advance() { this.stage++; this.next = System.currentTimeMillis() + 1200; }
    private void capture(String name) {
        var mc = Minecraft.getInstance();
        Screenshot.grab(mc.gameDirectory, name + ".png", mc.getMainRenderTarget(), 1, message -> {});
    }
    private static Object field(Object object, String name) throws Exception {
        var field = object.getClass().getDeclaredField(name); field.setAccessible(true); return field.get(object);
    }
    private static Object active() throws Exception {
        var field = StructureProjectionManager.class.getDeclaredField("activeProjection"); field.setAccessible(true); return field.get(null);
    }
    private static ProjectionRenderer renderer() throws Exception { return (ProjectionRenderer) field(active(), "renderer"); }
    private void check(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
        this.checks++;
    }
    private void fail(Throwable failure) {
        control = false;
        this.stage = 99;
        LogUtils.getLogger().error("AGERATUM_STRUCTURE_TEST FAILED", failure);
        Minecraft.getInstance().stop();
    }
    private final class Preview extends Screen {
        Preview() { super(Component.literal("Structure port")); }
        @Override public boolean isPauseScreen() { return false; }
        @Override public void extractRenderState(GuiGraphicsExtractor graphics, int x, int y, float partialTick) {
            graphics.fill(0, 0, this.width, this.height, 0xFFF1E6CD);
            graphics.pose().pushMatrix();
            graphics.pose().translate(20, 30);
            component.extractRenderState(new MDRenderContext(null, Minecraft.getInstance(), graphics, new ArrayList<>(),
                this.width, this.height, 320, 260, -1, -1, 20, 30, 1, 20, 30, new ArrayList<>()));
            graphics.pose().popMatrix();
        }
    }
}
