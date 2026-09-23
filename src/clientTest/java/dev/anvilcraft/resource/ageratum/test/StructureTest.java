package dev.anvilcraft.resource.ageratum.test;

import com.mojang.logging.LogUtils;
import dev.anvilcraft.lib.v2.renderer.projection.ProjectionRenderer;
import dev.anvilcraft.resource.ageratum.client.feat.structure.StructureExportClient;
import dev.anvilcraft.resource.ageratum.client.feat.structure.StructureProjectionApi;
import dev.anvilcraft.resource.ageratum.client.feat.structure.StructureProjectionManager;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtAccounter;
import net.minecraft.nbt.NbtIo;
import net.minecraft.nbt.TagParser;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.Difficulty;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.LevelSettings;
import net.minecraft.world.level.WorldDataConfiguration;
import net.minecraft.world.level.levelgen.WorldOptions;
import net.minecraft.world.level.levelgen.presets.WorldPresets;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import net.minecraft.world.level.storage.LevelResource;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.ModelEvent;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import net.neoforged.neoforge.common.NeoForge;
import org.lwjgl.glfw.GLFW;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Random;
import java.util.concurrent.CompletableFuture;

/** Opt-in real client, renderer and client-to-server export regression. */
@Mod(value = "ageratum_structure_test", dist = Dist.CLIENT)
public final class StructureTest {
    private boolean ready;
    private int stage;
    private int frames;
    private long started = System.nanoTime();
    private CompletableFuture<Void> reload;
    private ProjectionRenderer originalRenderer;
    private StructureTemplate template;
    private CompoundTag expected;
    private Path export;

    public StructureTest(IEventBus bus) {
        bus.addListener((ModelEvent.BakingCompleted event) -> this.ready = true);
        NeoForge.EVENT_BUS.addListener(this::tick);
        NeoForge.EVENT_BUS.addListener(this::render);
    }

    private void tick(ClientTickEvent.Post event) {
        Minecraft mc = Minecraft.getInstance();
        mc.options.pauseOnLostFocus = false;
        GLFW.glfwHideWindow(mc.getWindow().getWindow());
        if (this.stage == 5) return;
        if (System.nanoTime() - this.started > 180_000_000_000L) {
            fail(new IllegalStateException("Timed out"));
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

    private void render(RenderLevelStageEvent event) {
        Minecraft mc = Minecraft.getInstance();
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_TRANSLUCENT_BLOCKS || mc.level == null
            || mc.player == null || mc.screen != null || mc.getOverlay() != null || this.stage == 5) return;
        try {
            if (this.stage == 1) {
                CompoundTag root = TagParser.parseTag("""
                    {size:[2,2,2],palette:[{Name:"minecraft:stone"},{Name:"minecraft:glass"},
                    {Name:"minecraft:water"},{Name:"minecraft:chest"}],blocks:[
                    {pos:[0,0,0],state:0},{pos:[0,1,0],state:1},{pos:[1,0,0],state:2},
                    {pos:[1,1,1],state:3,nbt:{id:"minecraft:chest"}}],entities:[]}
                    """);
                byte[] extra = new byte[70_000];
                new Random(17).nextBytes(extra);
                root.getList("blocks", 10).getCompound(3).getCompound("nbt").putByteArray("export_test", extra);
                this.template = new StructureTemplate();
                this.template.load(mc.level.registryAccess().lookupOrThrow(Registries.BLOCK), root);
                this.expected = this.template.save(new CompoundTag());
                check(StructureProjectionApi.show(this.template, mc.player.blockPosition()), "fixed projection opens");
                this.originalRenderer = renderer();
                this.export = mc.getSingleplayerServer().getWorldPath(LevelResource.ROOT)
                    .resolve("data/ageratum/test_fixture.nbt");
                StructureExportClient.export(ResourceLocation.parse("test:fixture.snbt"), this.template);
                this.stage = 2;
            } else if (this.stage == 2 && ++this.frames > 3 && Files.isRegularFile(this.export)) {
                check(this.originalRenderer.isValid(), "projection mesh rendered");
                check(this.expected.equals(NbtIo.readCompressed(this.export, NbtAccounter.unlimitedHeap())),
                    "multi-chunk client/server export preserves NBT");
                this.reload = mc.reloadResourcePacks();
                this.stage = 3;
            } else if (this.stage == 3 && this.reload.isDone()) {
                this.reload.join();
                // Call the public render event handler to avoid event registration order assumptions.
                StructureProjectionManager.onRenderLevelStage(event);
                check(this.originalRenderer.isValid(), "projection rebuilds after resource reload");
                check(StructureProjectionApi.showFloating(this.template, mc.player.blockPosition()), "replacement opens");
                check(!this.originalRenderer.isValid(), "replacement frees previous mesh");
                this.stage = 4;
                this.frames = 0;
            } else if (this.stage == 4 && ++this.frames > 3) {
                ProjectionRenderer renderer = renderer();
                check(renderer.isValid(), "floating projection renders");
                StructureProjectionApi.clear();
                check(!StructureProjectionApi.hasActiveProjection() && !renderer.isValid(), "clear releases mesh");
                LogUtils.getLogger().info("AGERATUM_STRUCTURE_TEST PASS: fixed/floating rendering, replacement, reload, cleanup, multi-chunk export");
                this.stage = 5;
                mc.stop();
            }
        } catch (Throwable exception) {
            fail(exception);
        }
    }

    private static ProjectionRenderer renderer() throws Exception {
        var active = StructureProjectionManager.class.getDeclaredField("activeProjection");
        active.setAccessible(true);
        Object projection = active.get(null);
        var field = projection.getClass().getDeclaredField("renderer");
        field.setAccessible(true);
        return (ProjectionRenderer) field.get(projection);
    }

    private void fail(Throwable exception) {
        LogUtils.getLogger().error("AGERATUM_STRUCTURE_TEST FAILED", exception);
        this.stage = 5;
        Minecraft.getInstance().stop();
    }

    private static void check(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }
}
