package dev.ninemmteam.mod.gui.earth.component.impl;

import dev.ninemmteam.mod.gui.earth.EarthClickGui;
import dev.ninemmteam.mod.gui.earth.Render2D;
import dev.ninemmteam.mod.gui.earth.TextRenderer;
import dev.ninemmteam.mod.gui.earth.component.SettingComponent;
import dev.ninemmteam.mod.modules.settings.impl.StringSetting;
import net.minecraft.client.gui.DrawContext;
import org.lwjgl.glfw.GLFW;

public class StringComponent extends SettingComponent<StringSetting> {
    private final StringSetting stringSetting;
    private boolean listening;

    public StringComponent(StringSetting stringSetting, float posX, float posY, float offsetX, float offsetY, float width, float height) {
        super(stringSetting.getName(), posX, posY, offsetX, offsetY, width, height, stringSetting);
        this.stringSetting = stringSetting;
    }

    @Override
    public void drawScreen(EarthClickGui gui, int mouseX, int mouseY, float partialTicks) {
        super.drawScreen(gui, mouseX, mouseY, partialTicks);
        DrawContext context = gui.getContext();
        final boolean hovered = mouseWithinBounds(mouseX, mouseY, getFinishedX() + 5, getFinishedY() + 1, getWidth() - 10, getHeight() - 2);
        
        Render2D.drawBorderedRect(context, getFinishedX() + 4.5f, getFinishedY() + 1.0f, getFinishedX() + getWidth() - 4.5f, getFinishedY() + getHeight() - 0.5f, 0.5f, hovered ? 0x66333333 : 0, 0xff000000);
        
        String displayText = isListening() ? stringSetting.getValue() + (System.currentTimeMillis() % 1000 < 500 ? "_" : "") : stringSetting.getName() + ": §7" + stringSetting.getValue();
        TextRenderer.drawWithShadow(context, displayText, getFinishedX() + 6.5f, getFinishedY() + getHeight() - TextRenderer.getHeight() - 1f, 0xFFFFFFFF);
    }

    @Override
    public void keyTyped(char character, int keyCode) {
        super.keyTyped(character, keyCode);
        if (isListening()) {
            if (keyCode == GLFW.GLFW_KEY_ESCAPE) {
                setListening(false);
                return;
            }
            if (keyCode == GLFW.GLFW_KEY_ENTER) {
                setListening(false);
                return;
            }
            if (keyCode == GLFW.GLFW_KEY_BACKSPACE) {
                if (!stringSetting.getValue().isEmpty()) {
                    stringSetting.setValue(stringSetting.getValue().substring(0, stringSetting.getValue().length() - 1));
                }
                return;
            }
            if (character >= 32 && character <= 126) {
                stringSetting.setValue(stringSetting.getValue() + character);
            }
        }
    }

    @Override
    public void mouseClicked(int mouseX, int mouseY, int mouseButton) {
        super.mouseClicked(mouseX, mouseY, mouseButton);
        final boolean hovered = mouseWithinBounds(mouseX, mouseY, getFinishedX() + 5, getFinishedY() + 1, getWidth() - 10, getHeight() - 2);
        if (hovered && mouseButton == 0)
            setListening(!isListening());
    }

    @Override
    public void mouseReleased(int mouseX, int mouseY, int mouseButton) {
        super.mouseReleased(mouseX, mouseY, mouseButton);
    }

    public StringSetting getStringSetting() {
        return stringSetting;
    }

    public boolean isListening() {
        return listening;
    }

    public void setListening(boolean listening) {
        this.listening = listening;
    }
}
