package dev.anvilcraft.resource.ageratum.client.gui;

import com.mojang.blaze3d.vertex.PoseStack;
import dev.anvilcraft.resource.ageratum.Ageratum;
import dev.anvilcraft.resource.ageratum.client.feat.markdown.MarkdownParser;
import dev.anvilcraft.resource.ageratum.client.feat.markdown.component.MDComponent;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

public class GuideScreen extends Screen {
    protected static final ResourceLocation GUIDE_LOCATION = Ageratum.location("textures/gui/guide/guide.png");
    protected static final int IMAGE_WIDTH = 392;
    protected static final int IMAGE_HEIGHT = 466;
    protected static final int LABEL_WIDTH = 102;
    protected static final int LABEL_HEIGHT = 32;
    protected final MarkdownParser parser;
    protected int imageWidth = IMAGE_WIDTH / 2;
    protected int imageHeight = IMAGE_HEIGHT / 2;
    protected int labelWidth = LABEL_WIDTH / 2;
    protected int labelHeight = LABEL_HEIGHT / 2;
    protected int leftPos;
    protected int topPos;

    public GuideScreen() {
        super(Component.literal("Guide"));
        this.parser = new MarkdownParser();
    }

    protected void init() {
        this.leftPos = (this.width - this.imageWidth) / 2;
        this.topPos = (this.height - this.imageHeight) / 2;
    }

    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        this.renderTransparentBackground(guiGraphics);
        int i = (this.width - this.imageWidth) / 2;
        int j = (this.height - this.imageHeight) / 2;
        PoseStack pose = guiGraphics.pose();
        pose.pushPose();
        pose.translate(i, j, 0);
        this.renderLabel(guiGraphics, partialTick, mouseX - i, mouseY - j);
        this.renderBg(guiGraphics, partialTick, mouseX - i, mouseY - j);
        this.renderContent(guiGraphics, partialTick, mouseX - i, mouseY - j);
        pose.popPose();
    }

    private void renderBg(GuiGraphics guiGraphics, float partialTick, int mouseX, int mouseY) {
        guiGraphics.blit(GUIDE_LOCATION, 0, 0, 0, 0, this.imageWidth, this.imageHeight);
    }

    private void renderLabel(GuiGraphics guiGraphics, float partialTick, int mouseX, int mouseY) {
        for (int k = 0; k < 11; k++) {
            int l = 5;
            if (k != 0 && k != 3 && k != 9) {
                l += 10;
            }
            int originX = -35 + l;
            int originY = 24 + k * 17;
            if (this.mouseInRange(originX, originY, this.labelWidth, this.labelHeight, mouseX, mouseY) && mouseX < 11) {
                originX -= 5;
            }
            guiGraphics.blit(GUIDE_LOCATION, originX, originY, this.imageWidth, 0, this.labelWidth, this.labelHeight);
        }
    }

    private boolean mouseInRange(int originX, int originY, int width, int height, int mouseX, int mouseY) {
        return mouseX >= originX && mouseX <= originX + width && mouseY >= originY && mouseY <= originY + height;
    }

    private void renderContent(GuiGraphics guiGraphics, float partialTick, int mouseX, int mouseY) {
        PoseStack pose = guiGraphics.pose();
        pose.pushPose();
        pose.translate(19, 18, 0);
        pose.scale(0.6f, 0.6f, 1);
        int maxX = 264;
        int maxY = 328;
        String testText = """
            # Praesent scelerisque
            ## Vivamus euismod
            Donec molestie, sem ac varius vulputate, diam risus pretium
            enim, ut fermentum velit velit id tellus.
            ### Aliquam convallis
            Sed elementum lacinia magna, ut pulvinar justo pharetra at.
            Vestibulum et imperdiet nulla.
            
            # Fusce sit
            ## Vivamus interdum
            Lorem ipsum dolor sit amet, consectetur adipiscing elit. Nunc
            vel nulla nec dui mollis vulputate. Nulla et nulla sodales,
            vulputate urna sed, viverra nulla.
            ### Duis vestibulum
            Nulla tincidunt varius ipsum, id condimentum sapien interdum
            quis. Sed ut odio id velit porta porta eget lobortis sem.
            """;
        if (this.minecraft == null) return;
        for (MDComponent component : this.parser.parse(testText)) {
            pose.pushPose();
            component.render(guiGraphics, this.minecraft, maxX);
            pose.popPose();
            pose.translate(0, component.getHeight(this.minecraft, maxX) + 5, 0);
        }
        pose.popPose();
    }
}
