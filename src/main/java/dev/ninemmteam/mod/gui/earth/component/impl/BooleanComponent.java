package dev.ninemmteam.mod.gui.earth.component.impl;

import dev.ninemmteam.mod.gui.earth.EarthClickGui;
import dev.ninemmteam.mod.gui.earth.Render2D;
import dev.ninemmteam.mod.gui.earth.TextRenderer;
import dev.ninemmteam.mod.gui.earth.component.SettingComponent;
import dev.ninemmteam.mod.modules.impl.client.UISettings;
import dev.ninemmteam.mod.modules.settings.impl.BooleanSetting;
import net.minecraft.client.gui.DrawContext;

import java.awt.*;

public class BooleanComponent extends SettingComponent<BooleanSetting> {
    private final BooleanSetting booleanSetting;

    public BooleanComponent(BooleanSetting booleanSetting, float posX, float posY, float offsetX, float offsetY, float width, float height) {
        super(booleanSetting.getName(), posX, posY, offsetX, offsetY, width, height, booleanSetting);
        this.booleanSetting = booleanSetting;
    }

    @Override
    public void moved(float posX, float posY) {
        super.moved(posX, posY);
    }

    @Override
    public void drawScreen(EarthClickGui gui, int mouseX, int mouseY, float partialTicks) {
        super.drawScreen(gui, mouseX, mouseY, partialTicks);
        DrawContext context = gui.getContext();
        final boolean hovered = mouseWithinBounds(mouseX, mouseY, getFinishedX(), getFinishedY(), getWidth(), getHeight());
        
        String valueStr = booleanSetting.getValue() ? "On" : "Off";
        TextRenderer.drawWithShadow(context, getLabel() + ": §7" + valueStr, getFinishedX() + 5, getFinishedY() + getHeight() / 2 - (TextRenderer.getHeight() >> 1), 0xFFFFFFFF);
        
        if (hovered) {
            int hoverColor = 0x30FFFFFF;
            Render2D.drawRect(context, getFinishedX(), getFinishedY(), getFinishedX() + getWidth(), getFinishedY() + getHeight(), hoverColor);
        }
    }

    @Override
    public void keyTyped(char character, int keyCode) {
        super.keyTyped(character, keyCode);
    }

    @Override
    public void mouseClicked(int mouseX, int mouseY, int mouseButton) {
        super.mouseClicked(mouseX, mouseY, mouseButton);
        final boolean hovered = mouseWithinBounds(mouseX, mouseY, getFinishedX(), getFinishedY(), getWidth(), getHeight());
        if (hovered && mouseButton == 0) {
            booleanSetting.setValue(!booleanSetting.getValue());
        }
    }

    @Override
    public void mouseReleased(int mouseX, int mouseY, int mouseButton) {
        super.mouseReleased(mouseX, mouseY, mouseButton);
    }

    public BooleanSetting getBooleanSetting() {
        return booleanSetting;
    }
}
