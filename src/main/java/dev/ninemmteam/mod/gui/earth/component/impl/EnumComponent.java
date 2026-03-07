package dev.ninemmteam.mod.gui.earth.component.impl;

import dev.ninemmteam.mod.gui.earth.EarthClickGui;
import dev.ninemmteam.mod.gui.earth.Render2D;
import dev.ninemmteam.mod.gui.earth.TextRenderer;
import dev.ninemmteam.mod.gui.earth.component.SettingComponent;
import dev.ninemmteam.mod.modules.settings.impl.EnumSetting;
import net.minecraft.client.gui.DrawContext;

public class EnumComponent<E extends Enum<E>> extends SettingComponent<EnumSetting<E>> {
    private final EnumSetting<E> enumSetting;

    public EnumComponent(EnumSetting<E> enumSetting, float posX, float posY, float offsetX, float offsetY, float width, float height) {
        super(enumSetting.getName(), posX, posY, offsetX, offsetY, width, height, enumSetting);
        this.enumSetting = enumSetting;
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
        
        TextRenderer.drawWithShadow(context, getLabel() + ": §7" + enumSetting.getValue().name(), getFinishedX() + 5, getFinishedY() + getHeight() / 2 - (TextRenderer.getHeight() >> 1), 0xFFFFFFFF);
        
        if (hovered) {
            int hoverColor = 0x30FFFFFF;
            Render2D.drawRect(context, getFinishedX(), getFinishedY(), getFinishedX() + getWidth(), getFinishedY() + getHeight(), hoverColor);
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public void mouseClicked(int mouseX, int mouseY, int mouseButton) {
        super.mouseClicked(mouseX, mouseY, mouseButton);
        final boolean hovered = mouseWithinBounds(mouseX, mouseY, getFinishedX(), getFinishedY(), getWidth(), getHeight());
        if (hovered) {
            if (mouseButton == 0) {
                enumSetting.next();
            } else if (mouseButton == 1) {
                enumSetting.previous();
            }
        }
    }

    @Override
    public void mouseReleased(int mouseX, int mouseY, int mouseButton) {
        super.mouseReleased(mouseX, mouseY, mouseButton);
    }

    public EnumSetting<E> getEnumSetting() {
        return enumSetting;
    }
}
