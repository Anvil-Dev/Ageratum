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
        pose.translate(19 + 3, 18 + 3, 0);
        pose.scale(0.6f, 0.6f, 1);
        int maxX = 264 - 6;
        int maxY = 328 - 6;
        String testText = """
            # Praesent scelerisque
            ## Vivamus euismod
            Donec <color=#66ccff>molestie, sem ac varius
            <color=#39c5bb>vulputate, <i>diam</i> <b>risus</b> pretium
            enim</color>, ut fermentum velit velit</color> id tellus.
            ### Aliquam convallis
            Sed elementum lacinia magna, ut pulvinar justo pharetra at.
            Vestibulum et imperdiet nulla.
            
            # Fusce sit
            ## Vivamus interdum
            Lorem ipsum dolor sit amet, consectetur adipiscing elit. Nunc
            vel nulla nec dui mollis vulputate. Nulla et nulla sodales,
            vulputate urna sed, viverra nulla.
            
            
            Vivamus euismod, nulla molestie vestibulum faucibus, est purus
            dapibus mauris, sit amet pellentesque nulla diam quis nulla.
            Vestibulum ut erat semper, ultrices lacus id, maximus mi.
            Integer ut pretium orci.
            ### Duis vestibulum
            Aliquam <color=#39c5bb><b><i>convallis</i></b></color> ut
            erat vel iaculis. Praesent faucibus
            ultrices odio, a ornare tellus faucibus sit amet. Integer
            ut nunc nunc. Nunc in mauris in eros bibendum dapibus.
            Quisque pulvinar, arcu feugiat lacinia scelerisque, nisi
            nunc sollicitudin mi, ultricies vulputate ante tortor
            eleifend purus.
            
            
            Nam gravida libero lorem, vel fringilla velit cursus consequat.
            Vivamus interdum vulputate lectus, vel malesuada odio blandit
            ut. Etiam posuere faucibus nunc, vitae volutpat elit gravida ut.
            Ut auctor sodales felis, ac ullamcorper ante placerat id. Orci
            varius natoque penatibus et magnis dis parturient montes, nascetur
            ridiculus mus. Nam consectetur ante ut tellus egestas condimentum.
            Duis at lacus nec nisl efficitur hendrerit.
            """;
        if (this.minecraft == null) return;
        for (MDComponent component : this.parser.parse(testText)) {
            pose.pushPose();
            component.render(guiGraphics, this.minecraft, maxX, maxY);
            pose.popPose();
            int offsetY = component.getHeight(this.minecraft, maxX, maxY) + 5;
            maxY -= offsetY;
            pose.translate(0, offsetY, 0);
        }
        pose.popPose();
    }
}
