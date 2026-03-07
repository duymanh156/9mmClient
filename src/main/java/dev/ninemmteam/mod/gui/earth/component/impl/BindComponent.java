package dev.ninemmteam.mod.gui.earth.component.impl;

import dev.ninemmteam.mod.gui.earth.EarthClickGui;
import dev.ninemmteam.mod.gui.earth.Render2D;
import dev.ninemmteam.mod.gui.earth.TextRenderer;
import dev.ninemmteam.mod.gui.earth.component.SettingComponent;
import dev.ninemmteam.mod.modules.impl.client.UISettings;
import dev.ninemmteam.mod.modules.settings.impl.BindSetting;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.util.InputUtil;
import org.lwjgl.glfw.GLFW;

import java.awt.*;

public class BindComponent extends SettingComponent<BindSetting> {
    private static final MinecraftClient mc = MinecraftClient.getInstance();
    private final BindSetting bindSetting;
    private boolean listening = false;

    public BindComponent(BindSetting bindSetting, float posX, float posY, float offsetX, float offsetY, float width, float height) {
        super(bindSetting.getName(), posX, posY, offsetX, offsetY, width, height, bindSetting);
        this.bindSetting = bindSetting;
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
        
        String valueStr;
        if (isListening()) {
            valueStr = "§eListening...";
        } else if (hovered && InputUtil.isKeyPressed(mc.getWindow().getHandle(), GLFW.GLFW_KEY_LEFT_SHIFT) && bindSetting.getName().equals("Key")) {
            if (bindSetting.isHoldEnable()) {
                valueStr = "§7Toggle/§fHold";
            } else {
                valueStr = "§fToggle§7/Hold";
            }
        } else {
            valueStr = bindSetting.getKeyString();
        }
        
        TextRenderer.drawWithShadow(context, getLabel() + ": §7" + valueStr, getFinishedX() + 5, getFinishedY() + getHeight() / 2 - (TextRenderer.getHeight() >> 1), 0xFFFFFFFF);
        
        if (hovered) {
            int hoverColor = 0x30FFFFFF;
            Render2D.drawRect(context, getFinishedX(), getFinishedY(), getFinishedX() + getWidth(), getFinishedY() + getHeight(), hoverColor);
        }
    }

    @Override
    public void keyTyped(char character, int keyCode) {
        super.keyTyped(character, keyCode);
        
        if (isListening()) {
            if (keyCode == GLFW.GLFW_KEY_ESCAPE) {
                bindSetting.setValue(0);
                setListening(false);
                return;
            }
            if (keyCode == GLFW.GLFW_KEY_BACKSPACE) {
                bindSetting.setValue(0);
                setListening(false);
                return;
            }
            bindSetting.setValue(keyCode);
            setListening(false);
        }
    }

    @Override
    public void mouseClicked(int mouseX, int mouseY, int mouseButton) {
        super.mouseClicked(mouseX, mouseY, mouseButton);
        final boolean hovered = mouseWithinBounds(mouseX, mouseY, getFinishedX(), getFinishedY(), getWidth(), getHeight());
        if (hovered && mouseButton == 0) {
            if (InputUtil.isKeyPressed(mc.getWindow().getHandle(), GLFW.GLFW_KEY_LEFT_SHIFT) && bindSetting.getName().equals("Key")) {
                bindSetting.setHoldEnable(!bindSetting.isHoldEnable());
            } else {
                setListening(!isListening());
            }
        } else if (isListening() && hovered) {
            int key = -(mouseButton + 2);
            bindSetting.setValue(key);
            setListening(false);
        }
    }

    @Override
    public void mouseReleased(int mouseX, int mouseY, int mouseButton) {
        super.mouseReleased(mouseX, mouseY, mouseButton);
    }

    public BindSetting getBindSetting() {
        return bindSetting;
    }
    
    public boolean isListening() {
        return listening;
    }
    
    public void setListening(boolean listening) {
        this.listening = listening;
    }
}
