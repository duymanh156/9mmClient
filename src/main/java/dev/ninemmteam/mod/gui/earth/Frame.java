package dev.ninemmteam.mod.gui.earth;

import dev.ninemmteam.mod.gui.earth.component.Component;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;

import java.util.ArrayList;

public class Frame {
    private final String label;
    private float posX;
    private float posY;
    private float dragOffsetX;
    private float dragOffsetY;
    private float width;
    private final float height;
    private boolean extended;
    private boolean dragging;
    private final ArrayList<Component> components = new ArrayList<>();
    private int scrollY;

    public Frame(String label, float posX, float posY, float width, float height) {
        this.label = label;
        this.posX = posX;
        this.posY = posY;
        this.width = width;
        this.height = height;
    }

    public void init() {
        components.forEach(Component::init);
    }

    public void moved(float posX, float posY) {
        components.forEach(component -> component.moved(posX, posY));
    }

    public void drawScreen(DrawContext context, int mouseX, int mouseY, float partialTicks) {
        int windowWidth = MinecraftClient.getInstance().getWindow().getScaledWidth();
        int windowHeight = MinecraftClient.getInstance().getWindow().getScaledHeight();
        
        if (isDragging()) {
            setPosX(dragOffsetX + mouseX);
            setPosY(dragOffsetY + mouseY);
            getComponents().forEach(component -> component.moved(getPosX(), getPosY() + getScrollY()));
        }
        
        if (getPosX() < 0) {
            setPosX(0);
            getComponents().forEach(component -> component.moved(getPosX(), getPosY() + getScrollY()));
        }
        if (getPosX() + getWidth() > windowWidth) {
            setPosX(windowWidth - getWidth());
            getComponents().forEach(component -> component.moved(getPosX(), getPosY() + getScrollY()));
        }
        if (getPosY() < 0) {
            setPosY(0);
            getComponents().forEach(component -> component.moved(getPosX(), getPosY() + getScrollY()));
        }
        if (getPosY() + getHeight() > windowHeight) {
            setPosY(windowHeight - getHeight());
            getComponents().forEach(component -> component.moved(getPosX(), getPosY() + getScrollY()));
        }
    }

    public void keyTyped(char character, int keyCode) {
        if (isExtended()) {
            for (Component component : new ArrayList<>(getComponents())) {
                component.keyTyped(character, keyCode);
            }
        }
    }

    public void charTyped(char chr, int modifiers) {
        if (isExtended()) {
            for (Component component : new ArrayList<>(getComponents())) {
                component.charTyped(chr, modifiers);
            }
        }
    }

    public boolean processMouseClick(int mouseX, int mouseY, int mouseButton) {
        final boolean hovered = mouseWithinBounds(mouseX, mouseY, getPosX(), getPosY(), getWidth(), getHeight());
        if (mouseButton == 0 && hovered) {
            dragOffsetX = getPosX() - mouseX;
            dragOffsetY = getPosY() - mouseY;
            return true;
        }
        return false;
    }

    public void mouseClicked(int mouseX, int mouseY, int mouseButton) {
        final boolean hovered = mouseWithinBounds(mouseX, mouseY, getPosX(), getPosY(), getWidth(), getHeight());
        if (mouseButton == 1 && hovered) {
            setExtended(!isExtended());
        }
    }

    public void mouseReleased(int mouseX, int mouseY, int mouseButton) {
        if (mouseButton == 0) {
            setDragging(false);
        }
        if (isExtended()) {
            for (Component component : new ArrayList<>(getComponents())) {
                component.mouseReleased(mouseX, mouseY, mouseButton);
            }
        }
    }

    public static boolean mouseWithinBounds(int mouseX, int mouseY, float x, float y, float width, float height) {
        return mouseX >= x && mouseX <= x + width && mouseY >= y && mouseY <= y + height;
    }

    public ArrayList<Component> getComponents() {
        return components;
    }

    public String getLabel() {
        return label;
    }

    public float getWidth() {
        return width;
    }

    public float getHeight() {
        return height;
    }

    public float getPosX() {
        return posX;
    }

    public void setPosX(float posX) {
        this.posX = posX;
    }

    public float getPosY() {
        return posY;
    }

    public void setPosY(float posY) {
        this.posY = posY;
    }

    public boolean isExtended() {
        return extended;
    }

    public void setExtended(boolean extended) {
        this.extended = extended;
    }

    public boolean isDragging() {
        return dragging;
    }

    public void setDragging(boolean dragging) {
        this.dragging = dragging;
    }

    public int getScrollY() {
        return scrollY;
    }

    public void setScrollY(int scrollY) {
        this.scrollY = scrollY;
    }

    public void setWidth(float width) {
        this.width = width;
    }
}
