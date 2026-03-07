package dev.ninemmteam.mod.gui.earth.component.impl;

import dev.ninemmteam.mod.gui.earth.EarthClickGui;
import dev.ninemmteam.mod.gui.earth.Render2D;
import dev.ninemmteam.mod.gui.earth.TextRenderer;
import dev.ninemmteam.mod.gui.earth.component.SettingComponent;
import dev.ninemmteam.mod.modules.impl.client.UISettings;
import dev.ninemmteam.mod.modules.settings.impl.SliderSetting;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.util.math.MathHelper;
import org.lwjgl.glfw.GLFW;

public class NumberComponent extends SettingComponent<SliderSetting> {
    private final SliderSetting numberSetting;
    private boolean sliding;
    private boolean listening = false;
    private String inputValue = "";

    public NumberComponent(SliderSetting numberSetting, float posX, float posY, float offsetX, float offsetY, float width, float height) {
        super(numberSetting.getName(), posX, posY, offsetX, offsetY, width, height, numberSetting);
        this.numberSetting = numberSetting;
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
        if (listening) {
            valueStr = "§e" + inputValue + (System.currentTimeMillis() % 1000 < 500 ? "_" : "");
        } else {
            valueStr = String.format("%.2f", numberSetting.getValue());
        }
        TextRenderer.drawWithShadow(context, getLabel() + ": §7" + valueStr, getFinishedX() + 5, getFinishedY() + getHeight() / 2 - (TextRenderer.getHeight() >> 1), 0xFFFFFFFF);
        
        float length = (float) MathHelper.floor(((numberSetting.getValue()) - numberSetting.getMin()) / (numberSetting.getMax() - numberSetting.getMin()) * (getWidth() - 10));
        int settingColor = UISettings.getSettingColor().getRGB();
        Render2D.drawBorderedRect(context, getFinishedX() + 5, getFinishedY() + getHeight() - 2.5f, getFinishedX() + 5 + length, getFinishedY() + getHeight() - 0.5f, 0.5f, settingColor, 0xff000000);
        
        if (hovered) {
            int hoverColor = 0x30FFFFFF;
            Render2D.drawRect(context, getFinishedX(), getFinishedY(), getFinishedX() + getWidth(), getFinishedY() + getHeight(), hoverColor);
        }
        
        if (sliding) {
            double val = ((mouseX - (getFinishedX() + 5)) * (numberSetting.getMax() - numberSetting.getMin()) / (getWidth() - 10) + numberSetting.getMin());
            numberSetting.setValue(MathHelper.clamp(val, numberSetting.getMin(), numberSetting.getMax()));
        }
    }

    @Override
    public void keyTyped(char character, int keyCode) {
        super.keyTyped(character, keyCode);
        
        if (listening) {
            if (keyCode == GLFW.GLFW_KEY_ESCAPE) {
                listening = false;
                inputValue = "";
                return;
            }
            if (keyCode == GLFW.GLFW_KEY_BACKSPACE) {
                if (!inputValue.isEmpty()) {
                    inputValue = inputValue.substring(0, inputValue.length() - 1);
                }
                return;
            }
            if (keyCode == GLFW.GLFW_KEY_ENTER) {
                applyInput();
                return;
            }
            if (keyCode == GLFW.GLFW_KEY_MINUS || keyCode == GLFW.GLFW_KEY_KP_SUBTRACT) {
                if (inputValue.isEmpty()) {
                    inputValue = "-";
                }
                return;
            }
            if (keyCode == GLFW.GLFW_KEY_PERIOD || keyCode == GLFW.GLFW_KEY_KP_DECIMAL) {
                if (!inputValue.contains(".")) {
                    inputValue += ".";
                }
                return;
            }
            if (keyCode >= GLFW.GLFW_KEY_0 && keyCode <= GLFW.GLFW_KEY_9) {
                inputValue += (char)('0' + (keyCode - GLFW.GLFW_KEY_0));
                return;
            }
            if (keyCode >= GLFW.GLFW_KEY_KP_0 && keyCode <= GLFW.GLFW_KEY_KP_9) {
                inputValue += (char)('0' + (keyCode - GLFW.GLFW_KEY_KP_0));
                return;
            }
        }
    }
    
    @Override
    public void charTyped(char chr, int modifiers) {
        if (listening) {
            if ((chr >= '0' && chr <= '9') || chr == '.' || chr == '-') {
                if (inputValue.isEmpty() && chr == '-') {
                    inputValue = "-";
                } else if (chr == '.' && inputValue.contains(".")) {
                    return;
                } else {
                    inputValue += chr;
                }
            }
        }
        super.charTyped(chr, modifiers);
    }
    
    private void applyInput() {
        if (!inputValue.isEmpty() && !inputValue.equals("-")) {
            try {
                double value = Double.parseDouble(inputValue);
                numberSetting.setValue(MathHelper.clamp(value, numberSetting.getMin(), numberSetting.getMax()));
            } catch (NumberFormatException e) {
            }
        }
        listening = false;
        inputValue = "";
    }

    @Override
    public void mouseClicked(int mouseX, int mouseY, int mouseButton) {
        super.mouseClicked(mouseX, mouseY, mouseButton);
        final boolean hovered = mouseWithinBounds(mouseX, mouseY, getFinishedX(), getFinishedY(), getWidth(), getHeight());
        
        if (hovered) {
            if (mouseButton == 0) {
                if (listening) {
                    applyInput();
                } else {
                    setSliding(true);
                }
            } else if (mouseButton == 1) {
                listening = !listening;
                if (listening) {
                    inputValue = String.format("%.2f", numberSetting.getValue());
                } else {
                    applyInput();
                }
            }
        } else if (listening) {
            applyInput();
        }
    }

    @Override
    public void mouseReleased(int mouseX, int mouseY, int mouseButton) {
        super.mouseReleased(mouseX, mouseY, mouseButton);
        if (isSliding())
            setSliding(false);
    }

    public SliderSetting getNumberSetting() {
        return numberSetting;
    }

    public boolean isSliding() {
        return sliding;
    }

    public void setSliding(boolean sliding) {
        this.sliding = sliding;
    }
}
