package dev.anvilcraft.resource.ageratum.client.feat.markdown.component.extend;

import com.mojang.blaze3d.platform.Lighting;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.brigadier.StringReader;
import dev.anvilcraft.resource.ageratum.client.feat.markdown.MDExtensionContext;
import dev.anvilcraft.resource.ageratum.client.feat.markdown.MDRenderContext;
import dev.anvilcraft.resource.ageratum.client.feat.markdown.component.MDComponent;
import dev.anvilcraft.resource.ageratum.client.feat.markdown.component.MDTextComponent;
import dev.anvilcraft.resource.ageratum.client.util.level.SandboxRenderLevel;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.TagParser;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import org.joml.Quaternionf;
import org.joml.Vector2f;
import org.joml.Vector3f;

import java.util.Optional;
import javax.annotation.Nullable;

/**
 * 实体展示组件，使用背包界面同款实体预览渲染。
 */
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
    public void render(MDRenderContext context) {
        GuiGraphics graphics = context.graphics();
        Minecraft minecraft = context.minecraft();

        Entity entity = this.getEntity();
        if (entity == null) {
            super.render(context);
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
        graphics.renderOutline(drawX, 0, contentWidth, contentHeight, 0x66000000);

        if (this.showText) {
            Component hoverName = entity.getType().getDescription();
            int nameWidth = minecraft.font.width(hoverName);
            graphics.drawString(minecraft.font, hoverName, drawX + contentWidth / 2 - nameWidth / 2, contentHeight + 2, 0x000000, false);
        }

        context.enableScissor(drawX + 1, 1, drawX + contentWidth - 1, contentHeight - 1);
        PoseStack pose = graphics.pose();
        pose.pushPose();
        MDEntityComponent.renderEntity(context, graphics, x1, y1, x2, y2, this.getScale(entity, context.scale()), entity);
        pose.popPose();
        context.disableScissor();
    }

    @SuppressWarnings("MathClampMigration")
    private int getContentWidth(int maxX) {
        return Math.min(Math.max(Math.round(this.entityBbSize.x), 64), maxX);
    }

    private int getContentHeight() {
        return Math.max(Math.round(this.entityBbSize.y), 72);
    }

    private float getScale(Entity entity, float contextScale) {
        return 30F / (entity instanceof LivingEntity living ? living.getScale() : (0.55F * contextScale));
    }

    @Override
    public int getPreferredWidth(Minecraft minecraft, int maxX, int maxY) {
        Entity entity = this.cachedEntity;
        if (entity == null) return super.getPreferredWidth(minecraft, maxX, maxY);
        int textWidth = this.showText ? minecraft.font.width(entity.getName()) : 0;
        return Math.max(this.getContentWidth(maxX), textWidth);
    }

    @Override
    public int getHeight(Minecraft minecraft, int maxX, int maxY) {
        Entity entity = this.cachedEntity;
        if (entity == null) return super.getHeight(minecraft, maxX, maxY);
        int textHeight = this.showText ? minecraft.font.lineHeight : 0;
        return this.getContentHeight() + textHeight + 2;
    }

    private static void renderEntity(
        MDRenderContext context,
        GuiGraphics graphics,
        int x1,
        int y1,
        int x2,
        int y2,
        float scale,
        Entity entity
    ) {
        float centerX = (float) (x1 + x2) / 2.0F;
        float centerY = (float) (y1 + y2) / 2.0F;
        float f2 = (float) Math.atan((centerX - context.mouseX()) / 40.0F);
        float f3 = (float) Math.atan((centerY - context.mouseY()) / 40.0F);
        Quaternionf pose = new Quaternionf().rotateZ((float) Math.PI);
        Quaternionf cameraOrientation = new Quaternionf().rotateX(f3 * 20.0F * (float) (Math.PI / 180.0));
        pose.mul(cameraOrientation);
        float yRot = entity.getYRot();
        float xRot = entity.getXRot();
        entity.setYRot(180.0F + f2 * 40.0F);
        entity.setXRot(-f3 * 20.0F);
        float yBodyRot = 0F;
        float yHeadRot = 0F;
        float yHeadRotO = 0F;
        if (entity instanceof LivingEntity living) {
            yBodyRot = living.yBodyRot;
            yHeadRot = living.yHeadRot;
            yHeadRotO = living.yHeadRotO;
            living.yBodyRot = 180.0F + f2 * 20.0F;
            living.yHeadRot = living.getYRot();
            living.yHeadRotO = living.getYRot();
        }
        Vector3f translate = new Vector3f(0.0F, entity.getBbHeight() / 2.0F, entity.getBbWidth() / -2.0F);
        MDEntityComponent.renderEntity(
            graphics,
            centerX,
            centerY,
            scale,
            translate,
            pose,
            cameraOrientation,
            entity
        );
        entity.setYRot(yRot);
        entity.setXRot(xRot);
        if (entity instanceof LivingEntity living) {
            living.yBodyRot = yBodyRot;
            living.yHeadRotO = yHeadRotO;
            living.yHeadRot = yHeadRot;
        }
    }

    private static void renderEntity(
        GuiGraphics guiGraphics,
        float x,
        float y,
        float scale,
        Vector3f translate,
        Quaternionf pose,
        @Nullable Quaternionf cameraOrientation,
        Entity entity
    ) {
        guiGraphics.pose().pushPose();
        guiGraphics.pose().translate(x, y, 50.0);
        guiGraphics.pose().scale(scale, scale, -scale);
        guiGraphics.pose().translate(translate.x, translate.y, translate.z);
        guiGraphics.pose().mulPose(pose);
        Lighting.setupForEntityInInventory();
        EntityRenderDispatcher entityrenderdispatcher = Minecraft.getInstance().getEntityRenderDispatcher();
        if (cameraOrientation != null) {
            entityrenderdispatcher.overrideCameraOrientation(cameraOrientation.conjugate(new Quaternionf()).rotateY((float) Math.PI));
        }

        entityrenderdispatcher.setRenderShadow(false);
        // noinspection deprecation
        RenderSystem.runAsFancy(() -> entityrenderdispatcher.render(
            entity,
            0.0,
            0.0,
            0.0,
            0.0F,
            1.0F,
            guiGraphics.pose(),
            guiGraphics.bufferSource(),
            15728880
        ));
        guiGraphics.flush();
        entityrenderdispatcher.setRenderShadow(true);
        guiGraphics.pose().popPose();
        Lighting.setupFor3DItems();
    }

    private @Nullable Entity getEntity() {
        if (this.cachedEntity != null && this.cachedLevel != null) {
            return this.cachedEntity;
        }

        if (Minecraft.getInstance().level == null) return null;
        SandboxRenderLevel level = new SandboxRenderLevel();

        Optional<HolderLookup.RegistryLookup<EntityType<?>>> lookup = level.registryAccess().lookup(Registries.ENTITY_TYPE);
        if (lookup.isEmpty()) {
            return null;
        }

        Optional<Holder.Reference<EntityType<?>>> entityTypeRef = lookup.get().get(
            ResourceKey.create(Registries.ENTITY_TYPE, this.entityId));
        if (entityTypeRef.isEmpty()) {
            return null;
        }

        Entity entity = entityTypeRef.get().value().create(level);
        if (entity == null) return null;
        entity.load(this.entityNbt);

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
            nbt = new TagParser(new StringReader(rawNbt)).readStruct();
        } catch (Exception ignore) {
        }

        boolean showText = Boolean.parseBoolean(context.params().getOrDefault("showText", "true"));

        return new MDEntityComponent(id, nbt, showText);
    }
}

