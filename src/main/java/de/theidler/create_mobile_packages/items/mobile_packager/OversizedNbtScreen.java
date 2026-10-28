package de.theidler.create_mobile_packages.items.mobile_packager;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

public class OversizedNbtScreen extends Screen {
    private static final int WARNING_RED = 0xFFFF5555;

    public OversizedNbtScreen() {
        super(Component.translatable("item.create_mobile_packages.mobile_packager.too_large_title"));
    }

    @Override
    protected void init() {
        int buttonWidth = 100;
        addRenderableWidget(Button.builder(
                Component.translatable("gui.ok"),
                button -> onClose()
        ).bounds(width / 2 - buttonWidth / 2, height / 2 + 42, buttonWidth, 20).build());
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(graphics);
        int centerX = width / 2;
        int centerY = height / 2;
        graphics.fill(centerX - 155, centerY - 65, centerX + 155, centerY + 72, 0xF0101010);
        graphics.renderOutline(centerX - 155, centerY - 65, 310, 137, 0xFFAA3333);
        graphics.drawCenteredString(font, title, centerX, centerY - 48, WARNING_RED);
        graphics.drawCenteredString(font,
                Component.translatable("item.create_mobile_packages.mobile_packager.too_large_line1"),
                centerX, centerY - 20, WARNING_RED);
        graphics.drawCenteredString(font,
                Component.translatable("item.create_mobile_packages.mobile_packager.too_large_line2"),
                centerX, centerY - 5, WARNING_RED);
        graphics.drawCenteredString(font,
                Component.translatable("item.create_mobile_packages.mobile_packager.too_large_line3"),
                centerX, centerY + 10, WARNING_RED);
        super.render(graphics, mouseX, mouseY, partialTick);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
