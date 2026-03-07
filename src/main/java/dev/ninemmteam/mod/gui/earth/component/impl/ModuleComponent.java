package dev.ninemmteam.mod.gui.earth.component.impl;

import dev.ninemmteam.mod.gui.earth.EarthClickGui;
import dev.ninemmteam.mod.gui.earth.Render2D;
import dev.ninemmteam.mod.gui.earth.TextRenderer;
import dev.ninemmteam.mod.gui.earth.component.Component;
import dev.ninemmteam.mod.gui.earth.component.SettingComponent;
import dev.ninemmteam.mod.modules.Module;
import dev.ninemmteam.mod.modules.impl.client.UISettings;
import dev.ninemmteam.mod.modules.settings.Setting;
import dev.ninemmteam.mod.modules.settings.impl.*;
import net.minecraft.client.gui.DrawContext;
import org.lwjgl.glfw.GLFW;

import java.awt.*;
import java.util.ArrayList;

public class ModuleComponent extends Component {
    private final Module module;
    private final ArrayList<Component> components = new ArrayList<>();
    private boolean listening = false;

    public ModuleComponent(Module module, float posX, float posY, float offsetX, float offsetY, float width, float height) {
        super(module.getDisplayName(), posX, posY, offsetX, offsetY, width, height);
        this.module = module;
    }

    @Override
    @SuppressWarnings("unchecked")
    public void init() {
        getComponents().clear();
        float offY = getHeight();
        String desc = module.getDescription();
        if (desc != null && !desc.isEmpty()) {
            this.setDescription(() -> desc);
        }

        if (!module.getSettings().isEmpty()) {
            for (Setting setting : module.getSettings()) {
                if (setting.getName().equalsIgnoreCase("enabled")) {
                    continue;
                }
                
                if (setting instanceof BindSetting) {
                    getComponents().add(new BindComponent((BindSetting) setting, getFinishedX(), getFinishedY(), 0, offY, getWidth(), 12));
                    offY += 12;
                }
                if (setting instanceof BooleanSetting) {
                    getComponents().add(new BooleanComponent((BooleanSetting) setting, getFinishedX(), getFinishedY(), 0, offY, getWidth(), 12));
                    offY += 12;
                }
                if (setting instanceof SliderSetting) {
                    getComponents().add(new NumberComponent((SliderSetting) setting, getFinishedX(), getFinishedY(), 0, offY, getWidth(), 12));
                    offY += 12;
                }
                if (setting instanceof EnumSetting) {
                    getComponents().add(new EnumComponent<>((EnumSetting<?>) setting, getFinishedX(), getFinishedY(), 0, offY, getWidth(), 12));
                    offY += 12;
                }
                if (setting instanceof ColorSetting) {
                    getComponents().add(new ColorComponent((ColorSetting) setting, getFinishedX(), getFinishedY(), 0, offY, getWidth(), 12));
                    offY += 12;
                }
                if (setting instanceof StringSetting) {
                    getComponents().add(new StringComponent((StringSetting) setting, getFinishedX(), getFinishedY(), 0, offY, getWidth(), 12));
                    offY += 12;
                }
            }
        }
        getComponents().forEach(Component::init);
    }

    @Override
    public void moved(float posX, float posY) {
        super.moved(posX, posY);
        getComponents().forEach(component -> component.moved(getFinishedX(), getFinishedY()));
    }

    @Override
    public void drawScreen(EarthClickGui gui, int mouseX, int mouseY, float partialTicks) {
        super.drawScreen(gui, mouseX, mouseY, partialTicks);
        DrawContext context = gui.getContext();
        final boolean hovered = mouseWithinBounds(mouseX, mouseY, getFinishedX(), getFinishedY(), getWidth(), getHeight());

        Color modulesColor = UISettings.getModulesColor();
        Color onModule = UISettings.getOnModule();
        Color offModule = UISettings.getOffModule();
        
        int hoverColor = 0x40FFFFFF;
        if (hovered && !module.isOn())
            Render2D.drawRect(context, getFinishedX() + 1, getFinishedY() + 0.5f, getFinishedX() + getWidth() - 1, getFinishedY() + getHeight() - 0.5f, hoverColor);
        if (module.isOn()) {
            Render2D.drawRect(context, getFinishedX() + 1, getFinishedY() + 0.5f, getFinishedX() + getWidth() - 1, getFinishedY() + getHeight() - 0.5f, modulesColor.getRGB());
            if (hovered) {
                Render2D.drawRect(context, getFinishedX() + 1, getFinishedY() + 0.5f, getFinishedX() + getWidth() - 1, getFinishedY() + getHeight() - 0.5f, hoverColor);
            }
        }

        String label = getLabel();
        if (isListening()) {
            label = "Listening...";
        }
        TextRenderer.drawWithShadow(context, label, getFinishedX() + 4, getFinishedY() + getHeight() / 2 - (TextRenderer.getHeight() >> 1), module.isOn() ? Render2D.brighter(onModule.getRGB()) : Render2D.brighter(offModule.getRGB()));
        
        float rightOffset = 4;
        
        if (!getComponents().isEmpty()) {
            String openClose = isExtended() ? UISettings.getInstance().close.getValue() : UISettings.getInstance().open.getValue();
            TextRenderer.drawWithShadow(context, openClose, getFinishedX() + getWidth() - 4 - TextRenderer.getWidth(openClose), getFinishedY() + getHeight() / 2 - (TextRenderer.getHeight() >> 1), module.isOn() ? Render2D.brighter(onModule.getRGB()) : Render2D.brighter(offModule.getRGB()));
            rightOffset += TextRenderer.getWidth(openClose) + 2;
        }

        if (UISettings.getInstance().showBind.getValue() && module.getBindSetting().getValue() != 0) {
            int bind = module.getBindSetting().getValue();
            String bindName = getKeyName(bind);
            if (bindName != null && !bindName.isEmpty() && !bindName.equals("-1")) {
                String moduleBinding = "[" + bindName + "]";
                float bindX = getFinishedX() + getWidth() - rightOffset - TextRenderer.getWidth(moduleBinding);
                int bindColor = module.isOn() ? 0xFFAAAAAA : 0xFF888888;
                TextRenderer.drawWithShadow(context, moduleBinding, bindX, getFinishedY() + getHeight() / 2 - (TextRenderer.getHeight() >> 1) + 1, bindColor);
            }
        }
        
        if (isExtended()) {
            for (Component component : new ArrayList<>(getComponents())) {
                if (component instanceof SettingComponent) {
                    component.drawScreen(gui, mouseX, mouseY, partialTicks);
                }
            }
            if (module.isOn()) {
                Render2D.drawRect(context, getFinishedX() + 1.0f, getFinishedY() + getHeight() - 0.5f, getFinishedX() + 3, getFinishedY() + getHeight() + getComponentsSize(), hovered ? Render2D.brighter(modulesColor.getRGB()) : modulesColor.getRGB());
                Render2D.drawRect(context, getFinishedX() + 1.0f, getFinishedY() + getHeight() + getComponentsSize(), getFinishedX() + getWidth() - 1.f, getFinishedY() + getHeight() + getComponentsSize() + 2, hovered ? Render2D.brighter(modulesColor.getRGB()) : modulesColor.getRGB());
                Render2D.drawRect(context, getFinishedX() + getWidth() - 3.f, getFinishedY() + getHeight() - 0.5f, getFinishedX() + getWidth() - 1.f, getFinishedY() + getHeight() + getComponentsSize(), hovered ? Render2D.brighter(modulesColor.getRGB()) : modulesColor.getRGB());
            }
            Render2D.drawBorderedRect(context, getFinishedX() + 3.0f, getFinishedY() + getHeight() - 0.5f, getFinishedX() + getWidth() - 3.f, getFinishedY() + getHeight() + getComponentsSize() + 0.5f, 0.5f, 0, UISettings.getInstance().whiteSettings.getValue() ? 0xffffffff : 0xff000000);
        }
        Render2D.drawBorderedRect(context, getFinishedX() + 1, getFinishedY() + 0.5f, getFinishedX() + 1 + getWidth() - 2, getFinishedY() - 0.5f + getHeight() + (isExtended() ? (getComponentsSize() + 3.0f) : 0), 0.5f, 0, 0xff000000);
        updatePositions();
    }

    private String getKeyName(int key) {
        if (key <= 0) return null;
        String name = GLFW.glfwGetKeyName(key, 0);
        if (name != null) {
            return name.toUpperCase();
        }
        if (key >= GLFW.GLFW_KEY_F1 && key <= GLFW.GLFW_KEY_F25) {
            return "F" + (key - GLFW.GLFW_KEY_F1 + 1);
        }
        switch (key) {
            case GLFW.GLFW_KEY_SPACE: return "SPACE";
            case GLFW.GLFW_KEY_BACKSPACE: return "BACK";
            case GLFW.GLFW_KEY_TAB: return "TAB";
            case GLFW.GLFW_KEY_ENTER: return "ENTER";
            case GLFW.GLFW_KEY_LEFT_SHIFT: return "LSHIFT";
            case GLFW.GLFW_KEY_RIGHT_SHIFT: return "RSHIFT";
            case GLFW.GLFW_KEY_LEFT_CONTROL: return "LCTRL";
            case GLFW.GLFW_KEY_RIGHT_CONTROL: return "RCTRL";
            case GLFW.GLFW_KEY_LEFT_ALT: return "LALT";
            case GLFW.GLFW_KEY_RIGHT_ALT: return "RALT";
            case GLFW.GLFW_KEY_INSERT: return "INS";
            case GLFW.GLFW_KEY_DELETE: return "DEL";
            case GLFW.GLFW_KEY_HOME: return "HOME";
            case GLFW.GLFW_KEY_END: return "END";
            case GLFW.GLFW_KEY_PAGE_UP: return "PGUP";
            case GLFW.GLFW_KEY_PAGE_DOWN: return "PGDN";
            case GLFW.GLFW_KEY_UP: return "UP";
            case GLFW.GLFW_KEY_DOWN: return "DOWN";
            case GLFW.GLFW_KEY_LEFT: return "LEFT";
            case GLFW.GLFW_KEY_RIGHT: return "RIGHT";
            case GLFW.GLFW_KEY_CAPS_LOCK: return "CAPS";
            case GLFW.GLFW_KEY_NUM_LOCK: return "NUM";
            case GLFW.GLFW_KEY_SCROLL_LOCK: return "SCROLL";
            default: return null;
        }
    }

    @Override
    public void keyTyped(char character, int keyCode) {
        super.keyTyped(character, keyCode);
        
        if (isListening()) {
            if (keyCode == GLFW.GLFW_KEY_ESCAPE) {
                module.getBindSetting().setValue(0);
                setListening(false);
                return;
            }
            if (keyCode == GLFW.GLFW_KEY_BACKSPACE) {
                module.getBindSetting().setValue(0);
                setListening(false);
                return;
            }
            module.getBindSetting().setValue(keyCode);
            setListening(false);
            return;
        }
        
        if (isExtended()) {
            for (Component component : new ArrayList<>(getComponents())) {
                if (component instanceof SettingComponent) {
                    component.keyTyped(character, keyCode);
                }
            }
        }
    }

    @Override
    public void mouseClicked(int mouseX, int mouseY, int mouseButton) {
        super.mouseClicked(mouseX, mouseY, mouseButton);
        final boolean hovered = mouseWithinBounds(mouseX, mouseY, getFinishedX(), getFinishedY(), getWidth(), getHeight());
        if (hovered) {
            switch (mouseButton) {
                case 0:
                    if (!isListening()) {
                        module.toggle();
                    }
                    break;
                case 1:
                    if (!isListening() && !getComponents().isEmpty())
                        setExtended(!isExtended());
                    break;
                case 2:
                    setListening(!isListening());
                    break;
                default:
                    break;
            }
        }
        if (isExtended()) {
            for (Component component : new ArrayList<>(getComponents())) {
                if (component instanceof SettingComponent) {
                    component.mouseClicked(mouseX, mouseY, mouseButton);
                }
            }
        }
    }

    @Override
    public void mouseReleased(int mouseX, int mouseY, int mouseButton) {
        super.mouseReleased(mouseX, mouseY, mouseButton);
        if (isExtended()) {
            for (Component component : new ArrayList<>(getComponents())) {
                if (component instanceof SettingComponent) {
                    component.mouseReleased(mouseX, mouseY, mouseButton);
                }
            }
        }
    }

    private float getComponentsSize() {
        float size = 0;
        for (Component component : new ArrayList<>(getComponents())) {
            if (component instanceof SettingComponent) {
                size += component.getHeight();
            }
        }
        return size;
    }

    private void updatePositions() {
        float offsetY = getHeight();
        for (Component component : new ArrayList<>(getComponents())) {
            if (component instanceof SettingComponent) {
                component.setOffsetY(offsetY);
                component.moved(getPosX(), getPosY());
                offsetY += component.getHeight();
            }
        }
    }

    public Module getModule() {
        return module;
    }

    public ArrayList<Component> getComponents() {
        return components;
    }
    
    public boolean isListening() {
        return listening;
    }
    
    public void setListening(boolean listening) {
        this.listening = listening;
    }
}
