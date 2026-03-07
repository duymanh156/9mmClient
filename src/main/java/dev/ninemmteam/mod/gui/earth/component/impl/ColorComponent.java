package dev.ninemmteam.mod.gui.earth.component.impl;

import dev.ninemmteam.mod.gui.earth.EarthClickGui;
import dev.ninemmteam.mod.gui.earth.Render2D;
import dev.ninemmteam.mod.gui.earth.TextRenderer;
import dev.ninemmteam.mod.gui.earth.component.SettingComponent;
import dev.ninemmteam.mod.modules.impl.client.UISettings;
import dev.ninemmteam.mod.modules.settings.impl.ColorSetting;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.util.math.MathHelper;

import java.awt.*;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.StringSelection;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.io.IOException;

public class ColorComponent extends SettingComponent<ColorSetting> {
    private final ColorSetting colorSetting;
    private boolean colorExtended, colorSelectorDragging, alphaSelectorDragging, hueSelectorDragging;
    private float hue, saturation, brightness, alpha;

    public ColorComponent(ColorSetting colorSetting, float posX, float posY, float offsetX, float offsetY, float width, float height) {
        super(colorSetting.getName(), posX, posY, offsetX, offsetY, width, height, colorSetting);
        this.colorSetting = colorSetting;
        float[] hsb = Color.RGBtoHSB(getColorSetting().getValue().getRed(), getColorSetting().getValue().getGreen(), getColorSetting().getValue().getBlue(), null);
        hue = hsb[0];
        saturation = hsb[1];
        brightness = hsb[2];
        alpha = getColorSetting().getValue().getAlpha() / 255.f;
    }

    @Override
    public void moved(float posX, float posY) {
        super.moved(posX, posY);
    }

    @Override
    public void drawScreen(EarthClickGui gui, int mouseX, int mouseY, float partialTicks) {
        super.drawScreen(gui, mouseX, mouseY, partialTicks);
        DrawContext context = gui.getContext();
        TextRenderer.drawWithShadow(context, getLabel(), getFinishedX() + 5, getFinishedY() + 7 - (TextRenderer.getHeight() >> 1), 0xFFFFFFFF);
        Render2D.drawRect(context, getFinishedX() + getWidth() - 20, getFinishedY() + 4, getFinishedX() + getWidth() - 5, getFinishedY() + 11, getColorSetting().getValue().getRGB());

        setHeight(isColorExtended() ? 154 : 14);
        if (isColorExtended()) {
            final float expandedX = getFinishedX() + 1;
            final float expandedY = getFinishedY() + 14;

            final float colorPickerLeft = expandedX + 6;
            final float colorPickerTop = expandedY + 1;
            final float colorPickerRight = colorPickerLeft + (getWidth() - 20);
            final float colorPickerBottom = colorPickerTop + (getHeight() - 68);

            final int selectorWhiteOverlayColor = new Color(0xFF, 0xFF, 0xFF, 180).getRGB();

            int colorMouseX = (int) MathHelper.clamp(mouseX, colorPickerLeft, colorPickerRight);
            int colorMouseY = (int) MathHelper.clamp(mouseY, colorPickerTop, colorPickerBottom);

            Render2D.drawRect(context, colorPickerLeft - 0.5F, colorPickerTop - 0.5F, colorPickerRight + 0.5F, colorPickerBottom + 0.5F, 0xFF000000);

            drawColorPickerRect(context, colorPickerLeft, colorPickerTop, colorPickerRight, colorPickerBottom);

            float colorSelectorX = saturation * (colorPickerRight - colorPickerLeft);
            float colorSelectorY = (1 - brightness) * (colorPickerBottom - colorPickerTop);

            if (colorSelectorDragging) {
                float wWidth = colorPickerRight - colorPickerLeft;
                float xDif = colorMouseX - colorPickerLeft;
                this.saturation = xDif / wWidth;
                colorSelectorX = xDif;

                float hHeight = colorPickerBottom - colorPickerTop;
                float yDif = colorMouseY - colorPickerTop;
                this.brightness = 1 - (yDif / hHeight);
                colorSelectorY = yDif;

                updateColor(Color.HSBtoRGB(hue, saturation, brightness));
            }

            final float csLeft = colorPickerLeft + colorSelectorX - 0.5f;
            final float csTop = colorPickerTop + colorSelectorY - 0.5f;
            final float csRight = colorPickerLeft + colorSelectorX + 0.5f;
            final float csBottom = colorPickerTop + colorSelectorY + 0.5f;

            Render2D.drawRect(context, csLeft, csTop, csRight, csBottom, selectorWhiteOverlayColor);

            final float hueSliderLeft = colorPickerRight + 2;
            final float hueSliderRight = hueSliderLeft + 4;

            int hueMouseY = (int) MathHelper.clamp(mouseY, colorPickerTop, colorPickerBottom);

            final float hueSliderYDif = colorPickerBottom - colorPickerTop;

            float hueSelectorY = (1 - this.hue) * hueSliderYDif;

            if (hueSelectorDragging) {
                float yDif = hueMouseY - colorPickerTop;
                this.hue = 1 - (yDif / hueSliderYDif);
                hueSelectorY = yDif;

                updateColor(Color.HSBtoRGB(hue, saturation, brightness));
            }

            Render2D.drawRect(context, hueSliderLeft - 0.5F, colorPickerTop - 0.5F, hueSliderRight + 0.5F, colorPickerBottom + 0.5F, 0xFF000000);

            final float inc = 0.2F;
            final float times = 1 / inc;
            final float sHeight = colorPickerBottom - colorPickerTop;
            final float size = sHeight / times;
            float sY = colorPickerTop;

            for (int i = 0; i < times; i++) {
                boolean last = i == times - 1;
                Render2D.drawGradientRect(context, hueSliderLeft, sY, hueSliderRight, sY + size, false, Color.HSBtoRGB(1 - inc * i, 1.0F, 1.0F), Color.HSBtoRGB(1 - inc * (i + 1), 1.0F, 1.0F));
                if (!last) sY += size;
            }

            final float hsTop = colorPickerTop + hueSelectorY - 0.5f;
            final float hsBottom = colorPickerTop + hueSelectorY + 0.5f;

            Render2D.drawRect(context, hueSliderLeft, hsTop, hueSliderRight, hsBottom, selectorWhiteOverlayColor);

            final float alphaSliderTop = colorPickerBottom + 2;
            final float alphaSliderBottom = alphaSliderTop + 4;

            int color = Color.HSBtoRGB(hue, saturation, brightness);

            int r = color >> 16 & 0xFF;
            int g = color >> 8 & 0xFF;
            int b = color & 0xFF;

            final float hsHeight = colorPickerRight - colorPickerLeft;

            float alphaSelectorX = alpha * hsHeight;

            if (alphaSelectorDragging) {
                float xDif = colorMouseX - colorPickerLeft;
                this.alpha = xDif / hsHeight;
                alphaSelectorX = xDif;

                updateColor(new Color(r, g, b, (int) (alpha * 255)).getRGB());
            }

            Render2D.drawRect(context, colorPickerLeft - 0.5F, alphaSliderTop - 0.5F, colorPickerRight + 0.5F, alphaSliderBottom + 0.5F, 0xFF000000);

            Render2D.drawCheckeredBackground(context, colorPickerLeft, alphaSliderTop, colorPickerRight, alphaSliderBottom);

            Render2D.drawGradientRect(context, colorPickerLeft, alphaSliderTop, colorPickerRight, alphaSliderBottom, true, new Color(r, g, b, 0).getRGB(), new Color(r, g, b, 255).getRGB());

            final float asLeft = colorPickerLeft + alphaSelectorX - 0.5f;
            final float asRight = colorPickerLeft + alphaSelectorX + 0.5f;

            Render2D.drawRect(context, asLeft, alphaSliderTop, asRight, alphaSliderBottom, selectorWhiteOverlayColor);

            int modulesColor = UISettings.getModulesColor().getRGB();
            Render2D.drawGradientRect(context, colorPickerLeft, alphaSliderBottom + 2, colorPickerLeft + ((getWidth() - 16) / 2), alphaSliderBottom + 14, false, modulesColor, Render2D.darker(Render2D.darker(modulesColor)));
            Render2D.drawBorderedRect(context, colorPickerLeft, alphaSliderBottom + 2, colorPickerLeft + ((getWidth() - 16) / 2), alphaSliderBottom + 14, 0.5f, 0, 0xff000000);
            TextRenderer.drawWithShadow(context, "Copy", colorPickerLeft + ((getWidth() - 16) / 2) / 2 - (TextRenderer.getWidth("Copy") >> 1), alphaSliderBottom + 8 - (TextRenderer.getHeight() >> 1), 0xFFFFFFFF);

            Render2D.drawGradientRect(context, hueSliderRight - ((getWidth() - 16) / 2), alphaSliderBottom + 2, hueSliderRight, alphaSliderBottom + 14, false, modulesColor, Render2D.darker(Render2D.darker(modulesColor)));
            Render2D.drawBorderedRect(context, hueSliderRight - ((getWidth() - 16) / 2), alphaSliderBottom + 2, hueSliderRight, alphaSliderBottom + 14, 0.5f, 0, 0xff000000);
            TextRenderer.drawWithShadow(context, "Paste", hueSliderRight - ((getWidth() - 16) / 4) - (TextRenderer.getWidth("Paste") >> 1), alphaSliderBottom + 8 - (TextRenderer.getHeight() >> 1), 0xFFFFFFFF);
        }
    }

    @Override
    public void mouseClicked(int mouseX, int mouseY, int mouseButton) {
        super.mouseClicked(mouseX, mouseY, mouseButton);
        if (mouseButton == 0) {
            final boolean hovered = mouseWithinBounds(mouseX, mouseY, getFinishedX() + getWidth() - 20, getFinishedY() + 4, 15, 7);
            if (isColorExtended()) {
                final float expandedX = getFinishedX() + 1;
                final float expandedY = getFinishedY() + 14;

                final float colorPickerLeft = expandedX + 6;
                final float colorPickerTop = expandedY + 1;
                final float colorPickerRight = colorPickerLeft + (getWidth() - 20);
                final float colorPickerBottom = colorPickerTop + (getHeight() - 68);

                final float alphaSliderTop = colorPickerBottom + 2;
                final float alphaSliderBottom = alphaSliderTop + 4;

                final float hueSliderLeft = colorPickerRight + 2;
                final float hueSliderRight = hueSliderLeft + 4;

                final boolean hoveredCopy = mouseWithinBounds(mouseX, mouseY, colorPickerLeft, alphaSliderBottom + 2, ((getWidth() - 16) / 2), 12);
                final boolean hoveredPaste = mouseWithinBounds(mouseX, mouseY, hueSliderRight - ((getWidth() - 16) / 2), alphaSliderBottom + 2, ((getWidth() - 16) / 2), 12);

                if (hoveredCopy) {
                    final StringSelection selection = new StringSelection(String.format("#%08X", getColorSetting().getValue().getRGB()));
                    Toolkit.getDefaultToolkit().getSystemClipboard().setContents(selection, selection);
                }

                if (hoveredPaste) {
                    if (getClipBoard() != null) {
                        try {
                            int colorValue = (int) Long.parseLong(getClipBoard().replace("#", "").replace("0x", ""), 16);
                            getColorSetting().setValue(new Color(colorValue, true));
                            float[] hsb = Color.RGBtoHSB(getColorSetting().getValue().getRed(), getColorSetting().getValue().getGreen(), getColorSetting().getValue().getBlue(), null);
                            hue = hsb[0];
                            saturation = hsb[1];
                            brightness = hsb[2];
                            alpha = getColorSetting().getValue().getAlpha() / 255.f;
                        } catch (Exception ignored) {
                        }
                    }
                }

                if (!hoveredCopy && !hoveredPaste) {
                    if (mouseWithinBounds(mouseX, mouseY, colorPickerLeft, colorPickerTop, (getWidth() - 20), (getHeight() - 36)))
                        colorSelectorDragging = true;

                    if (mouseWithinBounds(mouseX, mouseY, hueSliderLeft, colorPickerTop, 4, (getHeight() - 36)))
                        hueSelectorDragging = true;
                }

                if (!hoveredCopy && !hoveredPaste && mouseWithinBounds(mouseX, mouseY, colorPickerLeft, alphaSliderTop, (getWidth() - 20), 4))
                    alphaSelectorDragging = true;
            }

            if (hovered)
                setColorExtended(!isColorExtended());
        }
    }

    @Override
    public void mouseReleased(int mouseX, int mouseY, int mouseButton) {
        super.mouseReleased(mouseX, mouseY, mouseButton);
        if (mouseButton == 0) {
            if (colorSelectorDragging) colorSelectorDragging = false;
            if (alphaSelectorDragging) alphaSelectorDragging = false;
            if (hueSelectorDragging) hueSelectorDragging = false;
        }
    }

    private String getClipBoard() {
        try {
            return (String) Toolkit.getDefaultToolkit().getSystemClipboard().getData(DataFlavor.stringFlavor);
        } catch (HeadlessException | IOException | UnsupportedFlavorException e) {
            e.printStackTrace();
        }
        return null;
    }

    private void updateColor(int hex) {
        getColorSetting().setValue(new Color(
                hex >> 16 & 0xFF,
                hex >> 8 & 0xFF,
                hex & 0xFF,
                (int) (alpha * 255)));
    }

    private void drawColorPickerRect(DrawContext context, float left, float top, float right, float bottom) {
        final int hueBasedColor = Color.HSBtoRGB(hue, 1.0F, 1.0F);

        Render2D.drawGradientRect(context, left, top, right, bottom, true, 0xFFFFFFFF, hueBasedColor);
        Render2D.drawGradientRect(context, left, top, right, bottom, false, 0, 0xFF000000);
    }

    public ColorSetting getColorSetting() {
        return colorSetting;
    }

    public void setHue(float hue) {
        this.hue = hue;
    }

    public void setSaturation(float saturation) {
        this.saturation = saturation;
    }

    public void setBrightness(float brightness) {
        this.brightness = brightness;
    }

    public float getAlpha() {
        return alpha;
    }

    public void setAlpha(float alpha) {
        this.alpha = alpha;
    }

    public boolean isColorExtended() {
        return colorExtended;
    }

    public void setColorExtended(boolean colorExtended) {
        this.colorExtended = colorExtended;
    }
}
