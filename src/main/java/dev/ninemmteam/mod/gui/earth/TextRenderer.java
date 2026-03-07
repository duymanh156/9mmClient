package dev.ninemmteam.mod.gui.earth;

import dev.ninemmteam.core.impl.FontManager;
import dev.ninemmteam.mod.modules.impl.client.ClickGui;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.text.Text;

public class TextRenderer {
    public static void drawWithShadow(DrawContext context, String text, float x, float y, int color) {
        if (useCustomFont()) {
            FontManager.ui.drawString(context.getMatrices(), text, (int) x, (int) y, color, ClickGui.getInstance().shadow.getValue());
        } else {
            context.drawTextWithShadow(MinecraftClient.getInstance().textRenderer, Text.literal(text), (int) x, (int) y, color);
        }
    }
    
    public static int getWidth(String text) {
        if (useCustomFont()) {
            return (int) FontManager.ui.getWidth(text);
        }
        return MinecraftClient.getInstance().textRenderer.getWidth(text);
    }
    
    public static int getHeight() {
        if (useCustomFont()) {
            return (int) FontManager.ui.getFontHeight();
        }
        return MinecraftClient.getInstance().textRenderer.fontHeight;
    }
    
    private static boolean useCustomFont() {
        return ClickGui.getInstance() != null 
            && ClickGui.getInstance().font.getValue() 
            && FontManager.ui != null;
    }
}
