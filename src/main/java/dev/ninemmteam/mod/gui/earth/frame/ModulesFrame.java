package dev.ninemmteam.mod.gui.earth.frame;

import dev.ninemmteam.mod.gui.earth.EarthClickGui;
import dev.ninemmteam.mod.gui.earth.Frame;
import dev.ninemmteam.mod.gui.earth.Render2D;
import dev.ninemmteam.mod.gui.earth.TextRenderer;
import dev.ninemmteam.mod.gui.earth.component.Component;
import dev.ninemmteam.mod.gui.earth.component.SettingComponent;
import dev.ninemmteam.mod.gui.earth.component.impl.ModuleComponent;
import dev.ninemmteam.mod.modules.impl.client.UISettings;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;

import java.awt.*;
import java.util.ArrayList;

public class ModulesFrame extends Frame {

    public ModulesFrame(String name, float posX, float posY, float width, float height) {
        super(name, posX, posY, width, height);
        this.setExtended(true);
    }

    @Override
    public void moved(float posX, float posY) {
        super.moved(posX, posY);
    }

    @Override
    public void drawScreen(DrawContext context, int mouseX, int mouseY, float partialTicks) {
        super.drawScreen(context, mouseX, mouseY, partialTicks);
        int windowHeight = MinecraftClient.getInstance().getWindow().getScaledHeight();
        float scrollMaxHeight = windowHeight;
        
        Color topColor = UISettings.getTopColor();
        Color topBgColor = UISettings.getTopBgColor();
        
        Render2D.drawRect(context, getPosX(), getPosY(), getPosX() + getWidth(), getPosY() + getHeight(), topBgColor.getRGB());
        Render2D.drawBorderedRect(context, getPosX(), getPosY(), getPosX() + getWidth(), getPosY() + getHeight(), 0.5f, 0, topColor.getRGB());
        
        TextRenderer.drawWithShadow(context, getLabel(), getPosX() + 3, getPosY() + getHeight() / 2 - (TextRenderer.getHeight() >> 1), 0xFFFFFFFF);
        
        if (UISettings.getInstance().categorySize.getValue()) {
            String disString = "[" + getComponents().size() + "]";
            TextRenderer.drawWithShadow(context, disString, (getPosX() + getWidth() - 3 - TextRenderer.getWidth(disString)), (getPosY() + getHeight() / 2 - (TextRenderer.getHeight() >> 1)), 0xFFFFFFFF);
        }
        
        if (isExtended()) {
            if (getScrollY() > 0) setScrollY(0);
            if (getScrollCurrentHeight() > scrollMaxHeight) {
                if (getScrollY() - 6 < -(getScrollCurrentHeight() - scrollMaxHeight))
                    setScrollY((int) -(getScrollCurrentHeight() - scrollMaxHeight));
            } else if (getScrollY() < 0) setScrollY(0);
            
            Render2D.drawRect(context, getPosX(), getPosY() + getHeight(), getPosX() + getWidth(), getPosY() + getHeight() + 1 + (getCurrentHeight()), 0x92000000);
            
            context.enableScissor((int) getPosX(), (int) (getPosY() + getHeight() + 1), (int) (getPosX() + getWidth()), (int) (getPosY() + getHeight() + scrollMaxHeight + 1));
            getComponents().forEach(component -> component.drawScreen(EarthClickGui.getInstance(), mouseX, mouseY, partialTicks));
            context.disableScissor();
        }
        updatePositions();
    }

    @Override
    public void keyTyped(char character, int keyCode) {
        super.keyTyped(character, keyCode);
    }

    @Override
    public void mouseClicked(int mouseX, int mouseY, int mouseButton) {
        super.mouseClicked(mouseX, mouseY, mouseButton);
        
        if (isExtended()) {
            for (Component component : new ArrayList<>(getComponents())) {
                component.mouseClicked(mouseX, mouseY, mouseButton);
            }
        }
    }

    @Override
    public void mouseReleased(int mouseX, int mouseY, int mouseButton) {
        super.mouseReleased(mouseX, mouseY, mouseButton);
        if (isExtended()) {
            for (Component component : new ArrayList<>(getComponents())) {
                component.mouseReleased(mouseX, mouseY, mouseButton);
            }
        }
    }

    public void handleScroll(double delta) {
        if (isExtended()) {
            final float scrollSpeed = (UISettings.getInstance().scrollSpeed.getValueInt() >> 2);
            int windowHeight = MinecraftClient.getInstance().getWindow().getScaledHeight();
            if (delta < 0) {
                if (getScrollY() - scrollSpeed < -(getScrollCurrentHeight() - Math.min(getScrollCurrentHeight(), windowHeight)))
                    setScrollY((int) -(getScrollCurrentHeight() - Math.min(getScrollCurrentHeight(), windowHeight)));
                else setScrollY((int) (getScrollY() - scrollSpeed));
            } else if (delta > 0) {
                setScrollY((int) (getScrollY() + scrollSpeed));
            }
        }
    }

    private void updatePositions() {
        float offsetY = getHeight() + 1;
        for (Component component : new ArrayList<>(getComponents())) {
            component.setOffsetY(offsetY);
            component.moved(getPosX(), getPosY() + getScrollY());
            if (component instanceof ModuleComponent) {
                if (component.isExtended()) {
                    for (Component component1 : new ArrayList<>(((ModuleComponent) component).getComponents())) {
                        if (component1 instanceof SettingComponent) {
                            offsetY += component1.getHeight();
                        }
                    }
                    offsetY += 3.f;
                }
            }
            offsetY += component.getHeight();
        }
    }

    private float getScrollCurrentHeight() {
        return getCurrentHeight() + getHeight() + 3.f;
    }

    private float getCurrentHeight() {
        float cHeight = 1;
        for (Component component : new ArrayList<>(getComponents())) {
            if (component instanceof ModuleComponent) {
                if (component.isExtended()) {
                    for (Component component1 : new ArrayList<>(((ModuleComponent) component).getComponents())) {
                        if (component1 instanceof SettingComponent) {
                            cHeight += component1.getHeight();
                        }
                    }
                    cHeight += 3.f;
                }
            }
            cHeight += component.getHeight();
        }
        return cHeight;
    }
}
