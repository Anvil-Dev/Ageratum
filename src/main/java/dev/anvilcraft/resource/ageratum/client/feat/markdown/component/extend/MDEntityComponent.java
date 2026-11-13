package dev.anvilcraft.resource.ageratum.client.feat.markdown.component.extend;

import dev.anvilcraft.resource.ageratum.client.gui.GuideFont;
import com.mojang.brigadier.StringReader;
import dev.anvilcraft.resource.ageratum.client.feat.markdown.MDExtensionContext;
import dev.anvilcraft.resource.ageratum.client.feat.markdown.MDRenderContext;
import dev.anvilcraft.resource.ageratum.client.feat.markdown.component.MDComponent;
import dev.anvilcraft.resource.ageratum.client.feat.markdown.component.MDTextComponent;
import dev.anvilcraft.resource.ageratum.client.util.level.SandboxRenderLevel;
import lombok.extern.slf4j.Slf4j;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.gui.navigation.ScreenRectangle;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.nbt.TagParser;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.storage.TagValueInput;
import org.joml.Quaternionf;
import org.joml.Vector2f;
import org.joml.Vector3f;

import java.util.Optional;
import javax.annotation.Nullable;

/**
 * 实体展示组件，使用背包界面同款实体预览渲染。
 */
@Slf4j
public final class MDEntityComponent extends MDComponent {
    private final Identifier entityId;
    private final CompoundTag entityNbt;
    private final boolean showText;
    private @Nullable Entity cachedEntity;
    private @Nullable SandboxRenderLevel cachedLevel;
    private Vector2f entityBbSize = new Vector2f(1.0F, 1.0F);

    public MDEntityComponent(Identifier entityId, CompoundTag entityNbt, boolean showText) {
        super(Component.literal("[entity load failed]").withStyle(ChatFormatting.RED));
        this.entityId = entityId;
        this.entityNbt = entityNbt;
        this.showText = showText;
    }

    @Override
    public void extractRenderState(MDRenderContext context) {
        GuiGraphicsExtractor graphics = context.graphics();

        Entity entity = this.getEntity();
        if (entity == null) {
            super.extractRenderState(context);
            return;
        }

        int contentWidth = this.getContentWidth(context.maxX());
        int contentHeight = this.getContentHeight();
        int drawX = Math.max(0, (context.maxX() - contentWidth) / 2);
        int x1 = drawX + 4;
        int y1 = 4;
        int x2 = drawX + contentWidth - 4;
        int y2 = contentHeight - 4;

        graphics.fill(drawX, 0, drawX + contentWidth, contentHeight, 0x22000000);
        graphics.outline(drawX, 0, contentWidth, contentHeight, 0x66000000);

        if (this.showText) {
            Component hoverName = entity.getType().getDescription();
            int nameWidth = GuideFont.get().width(hoverName);
            GuideFont.get().draw(graphics, hoverName, drawX + contentWidth / 2 - nameWidth / 2, contentHeight + 2, context.layout().color("colors.content_text", 0xFF202020), false);
        }

        context.enableScissor(drawX + 1, 1, drawX + contentWidth - 1, contentHeight - 1);
        try {
            MDEntityComponent.renderEntity(context, graphics, x1, y1, x2, y2, entity);
        } finally {
            context.disableScissor();
        }
    }

    private int getContentWidth(int maxX) {
        return Math.max(1, Math.min(Math.max(Math.round(this.entityBbSize.x), 64), maxX));
    }

    private int getContentHeight() {
        return Math.max(Math.round(this.entityBbSize.y), 72);
    }

    @Override
    public int getPreferredWidth(Minecraft minecraft, int maxX, int maxY) {
        Entity entity = this.getEntity();
        if (entity == null) return super.getPreferredWidth(minecraft, maxX, maxY);
        int textWidth = this.showText ? GuideFont.get().width(entity.getName()) : 0;
        return Math.max(this.getContentWidth(maxX), textWidth);
    }

    @Override
    public int getHeight(Minecraft minecraft, int maxX, int maxY) {
        Entity entity = this.getEntity();
        if (entity == null) return super.getHeight(minecraft, maxX, maxY);
        int textHeight = this.showText ? GuideFont.get().lineHeight : 0;
        return this.getContentHeight() + textHeight + 2;
    }

    private static void renderEntity(
        MDRenderContext context, GuiGraphicsExtractor graphics, int x1, int y1, int x2, int y2, Entity entity
    ) {
        if (x2 <= x1 || y2 <= y1) return;
        // entity 的画中画接口接收屏幕坐标，不会应用当前 GUI pose。
        ScreenRectangle bounds = new ScreenRectangle(x1, y1, x2 - x1, y2 - y1).transformMaxBounds(graphics.pose());
        Vector2f mouse = graphics.pose().transformPosition(context.mouseX(), context.mouseY(), new Vector2f());
        float parentScale = (float) Math.hypot(graphics.pose().m00(), graphics.pose().m01());
        if (entity instanceof LivingEntity living) {
            InventoryScreen.extractEntityInInventoryFollowsMouse(graphics, bounds.left(), bounds.top(), bounds.right(), bounds.bottom(),
                Math.max(1, Math.round(30 * parentScale)), 0, mouse.x, mouse.y, living);
        } else {
            float centerX = (x1 + x2) / 2f;
            float centerY = (y1 + y2) / 2f;
            float yaw = (float) Math.atan((centerX - context.mouseX()) / 40f);
            float pitch = (float) Math.atan((centerY - context.mouseY()) / 40f);
            Quaternionf camera = new Quaternionf().rotateX(pitch * 20 * (float) Math.PI / 180);
            Quaternionf rotation = new Quaternionf().rotateZ((float) Math.PI).mul(camera).rotateY(yaw);
            EntityRenderState state = context.minecraft().getEntityRenderDispatcher().extractEntity(entity, 0);
            state.shadowPieces.clear();
            state.outlineColor = 0;
            graphics.entity(state, 30 * parentScale,
                new Vector3f(0, entity.getBbHeight() / 2, 0), rotation, camera,
                bounds.left(), bounds.top(), bounds.right(), bounds.bottom());
        }
    }

    private @Nullable Entity getEntity() {
        if (this.cachedEntity != null && this.cachedLevel != null) {
            return this.cachedEntity;
        }

        if (Minecraft.getInstance().level == null) return null;
        SandboxRenderLevel level = new SandboxRenderLevel();

        Optional<Registry<EntityType<?>>> lookup = level.registryAccess().lookup(Registries.ENTITY_TYPE);
        if (lookup.isEmpty()) {
            return null;
        }

        Optional<Holder.Reference<EntityType<?>>> entityTypeRef = lookup.get()
            .get(ResourceKey.create(Registries.ENTITY_TYPE, this.entityId));
        if (entityTypeRef.isEmpty()) {
            return null;
        }

        Entity entity = entityTypeRef.get().value().create(level, EntitySpawnReason.LOAD);
        if (entity == null) return null;

        try (ProblemReporter.ScopedCollector reporter = new ProblemReporter.ScopedCollector(entity.problemPath(), log)) {
            entity.load(TagValueInput.create(reporter, entity.registryAccess(), this.entityNbt));
        }

        this.cachedLevel = level;
        this.cachedEntity = entity;
        this.entityBbSize = new Vector2f(entity.getBbWidth() * 30, entity.getBbHeight() * 30);
        return this.cachedEntity;
    }

    public static MDComponent parse(MDExtensionContext context) {
        String rawId = context.params().get("id");
        if (rawId == null || rawId.isBlank()) {
            return new MDTextComponent("[错误：entity 需要 id 参数]");
        }

        Identifier id;
        try {
            id = Identifier.parse(rawId);
        } catch (Exception exception) {
            return new MDTextComponent("[错误：entity 的 id 参数格式无效]");
        }

        CompoundTag nbt = new CompoundTag();
        String rawNbt = context.params().get("nbt");
        try {
            Tag tag = TagParser.create(NbtOps.INSTANCE).parseAsArgument(new StringReader(rawNbt));
            if (tag instanceof CompoundTag compoundTag) nbt = compoundTag;
        } catch (Exception ignore) {
        }

        boolean showText = Boolean.parseBoolean(context.params().getOrDefault("showText", "true"));

        return new MDEntityComponent(id, nbt, showText);
    }
}

