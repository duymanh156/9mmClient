package dev.ninemmteam.mod.gui.earth;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.util.Window;

import java.awt.*;

public class Render2D {
    public static void drawRect(DrawContext context, float left, float top, float right, float bottom, int color) {
        context.fill((int) left, (int) top, (int) right, (int) bottom, color);
    }

    public static void drawBorderedRect(DrawContext context, float left, float top, float right, float bottom, float borderWidth, int insideColor, int borderColor) {
        drawRect(context, left, top, right, bottom, insideColor);
        drawRect(context, left, top, right, top + borderWidth, borderColor);
        drawRect(context, left, bottom - borderWidth, right, bottom, borderColor);
        drawRect(context, left, top + borderWidth, left + borderWidth, bottom - borderWidth, borderColor);
        drawRect(context, right - borderWidth, top + borderWidth, right, bottom - borderWidth, borderColor);
    }

    public static void drawGradientRect(DrawContext context, float left, float top, float right, float bottom, boolean horizontal, int startColor, int endColor) {
        if (horizontal) {
            context.fillGradient((int) left, (int) top, (int) right, (int) bottom, startColor, endColor);
        } else {
            context.fillGradient((int) left, (int) top, (int) right, (int) bottom, startColor, endColor);
        }
    }

    public static void drawCheckMark(DrawContext context, float x, float y, float size, int color) {
        MinecraftClient mc = MinecraftClient.getInstance();
        context.getMatrices().push();
        context.getMatrices().translate(x, y, 0);
        context.getMatrices().scale(size / 10f, size / 10f, 1);
        
        int[][] checkPoints = {{2, 5}, {4, 8}, {8, 2}};
        for (int i = 0; i < checkPoints.length - 1; i++) {
            int[] p1 = checkPoints[i];
            int[] p2 = checkPoints[i + 1];
            drawLine(context, p1[0], p1[1], p2[0], p2[1], color);
        }
        context.getMatrices().pop();
    }

    private static void drawLine(DrawContext context, int x1, int y1, int x2, int y2, int color) {
        context.fill(x1, y1, x1 + 1, y1 + 1, color);
        context.fill(x2, y2, x2 + 1, y2 + 1, color);
    }

    public static void drawCheckeredBackground(DrawContext context, float left, float top, float right, float bottom) {
        boolean white = true;
        int size = 4;
        for (float x = left; x < right; x += size) {
            for (float y = top; y < bottom; y += size) {
                int color = white ? 0xFFFFFFFF : 0xFFCCCCCC;
                drawRect(context, x, y, Math.min(x + size, right), Math.min(y + size, bottom), color);
                white = !white;
            }
            white = !white;
        }
    }

    public static int brighter(int color) {
        Color c = new Color(color, true);
        return new Color(
            Math.min(255, (int) (c.getRed() * 1.2f)),
            Math.min(255, (int) (c.getGreen() * 1.2f)),
            Math.min(255, (int) (c.getBlue() * 1.2f)),
            c.getAlpha()
        ).getRGB();
    }

    public static int darker(int color) {
        Color c = new Color(color, true);
        return new Color(
            (int) (c.getRed() * 0.8f),
            (int) (c.getGreen() * 0.8f),
            (int) (c.getBlue() * 0.8f),
            c.getAlpha()
        ).getRGB();
    }
}
