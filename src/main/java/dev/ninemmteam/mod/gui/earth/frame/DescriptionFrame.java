package dev.ninemmteam.mod.gui.earth.frame;

import dev.ninemmteam.mod.gui.earth.EarthClickGui;
import dev.ninemmteam.mod.gui.earth.Frame;
import dev.ninemmteam.mod.gui.earth.Render2D;
import dev.ninemmteam.mod.gui.earth.TextRenderer;
import dev.ninemmteam.mod.modules.impl.client.UISettings;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.text.Text;

import java.awt.*;
import java.util.List;

public class DescriptionFrame extends Frame {
    private String description;

    public DescriptionFrame(float posX, float posY, float width, float height) {
        this("Description", posX, posY, width, height);
    }

    public DescriptionFrame(String label, float posX, float posY, float width, float height) {
        super(label, posX, posY, width, height);
    }

    @Override
    public void drawScreen(DrawContext context, int mouseX, int mouseY, float partialTicks) {
        if (description == null || !UISettings.getInstance().description.getValue()) {
            return;
        }

        super.drawScreen(context, mouseX, mouseY, partialTicks);
        Color topColor = UISettings.getTopColor();
        Color topBgColor = UISettings.getTopBgColor();
        Color textColor = new Color(255, 255, 255); // 默认白色，可根据UISettings实际API调整
        
        Render2D.drawRect(context, getPosX(), getPosY(), getPosX() + getWidth(), getPosY() + getHeight(), topBgColor.getRGB());
        Render2D.drawBorderedRect(context, getPosX(), getPosY(), getPosX() + getWidth(), getPosY() + getHeight(), 0.5f, 0, topColor.getRGB());
        TextRenderer.drawWithShadow(context, getLabel(), getPosX() + 3, getPosY() + getHeight() / 2 - (TextRenderer.getHeight() >> 1), 0xFFFFFFFF);

        float y = this.getPosY() + 2 + (getHeight() / 2) + TextRenderer.getHeight();
        var lines = MinecraftClient.getInstance().textRenderer.wrapLines(Text.literal(description), (int) this.getWidth() - 1);

        Render2D.drawRect(context, getPosX(), getPosY() + getHeight(), getPosX() + getWidth(), getPosY() + getHeight() + 3 + (TextRenderer.getHeight() + 1) * lines.size(), 0x92000000);

        for (var line : lines) {
            String string = line.toString();
            TextRenderer.drawWithShadow(context, string, (int) this.getPosX() + 3, (int) y, textColor.getRGB());
            y += TextRenderer.getHeight() + 1;
        }
    }

    @Override
    public void mouseReleased(int mouseX, int mouseY, int mouseButton) {
        super.mouseReleased(mouseX, mouseY, mouseButton);
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }
}
