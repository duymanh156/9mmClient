package dev.ninemmteam.mod.modules.impl.client;

import dev.ninemmteam.api.events.eventbus.EventListener;
import dev.ninemmteam.api.events.impl.UpdateEvent;
import dev.ninemmteam.mod.gui.earth.EarthClickGui;
import dev.ninemmteam.mod.modules.Module;
import dev.ninemmteam.mod.modules.settings.impl.*;

import java.awt.*;

public class UISettings extends Module {
    private static UISettings INSTANCE;
    
    public final SliderSetting scrollSpeed = this.add(new SliderSetting("ScrollSpeed", 80, 1, 200));
    public final BooleanSetting description = this.add(new BooleanSetting("Description", true));
    public final SliderSetting descriptionWidth = this.add(new SliderSetting("DescriptionWidth", 240, 100, 1000));
    public final BooleanSetting showBind = this.add(new BooleanSetting("ShowBind", true));
    public final BooleanSetting categorySize = this.add(new BooleanSetting("CategorySize", true));
    public final BooleanSetting whiteSettings = this.add(new BooleanSetting("WhiteSettings", true));
    public final StringSetting open = this.add(new StringSetting("Open", "+"));
    public final StringSetting close = this.add(new StringSetting("Close", "-"));
    public final BooleanSetting catEars = this.add(new BooleanSetting("CatEars", false));
    
    public final BooleanSetting colors = this.add(new BooleanSetting("Colors", false).setParent());
    public final ColorSetting topColor = this.add(new ColorSetting("TopColor", new Color(0, 80, 255, 255), this.colors::isOpen));
    public final ColorSetting topBgColor = this.add(new ColorSetting("TopBgColor", new Color(30, 30, 30, 255), this.colors::isOpen));
    public final ColorSetting modulesColor = this.add(new ColorSetting("ModulesColor", new Color(0, 80, 255, 255), this.colors::isOpen));
    public final ColorSetting onModule = this.add(new ColorSetting("OnModule", new Color(255, 255, 255, 255), this.colors::isOpen));
    public final ColorSetting offModule = this.add(new ColorSetting("OffModule", new Color(170, 170, 170, 255), this.colors::isOpen));
    public final ColorSetting settingColor = this.add(new ColorSetting("SettingColor", new Color(0, 80, 255, 255), this.colors::isOpen));
    public final ColorSetting textColorDesc = this.add(new ColorSetting("TextColorDesc", new Color(255, 255, 255, 255), this.colors::isOpen));

    public UISettings() {
        super("UISettings", Category.Client);
        this.setChinese("界面设置");
        INSTANCE = this;
    }

    public static UISettings getInstance() {
        return INSTANCE;
    }

    public static Color getTopColor() {
        return INSTANCE.topColor.getValue();
    }

    public static Color getTopBgColor() {
        return INSTANCE.topBgColor.getValue();
    }

    public static Color getModulesColor() {
        return INSTANCE.modulesColor.getValue();
    }

    public static Color getOnModule() {
        return INSTANCE.onModule.getValue();
    }

    public static Color getOffModule() {
        return INSTANCE.offModule.getValue();
    }

    public static Color getSettingColor() {
        return INSTANCE.settingColor.getValue();
    }

    public static Color getTextColorDesc() {
        return INSTANCE.textColorDesc.getValue();
    }

    @Override
    public void onEnable() {
        if (nullCheck()) {
            disable();
            return;
        }
        EarthClickGui.getInstance().init();
        mc.setScreen(EarthClickGui.getInstance());
    }

    @Override
    public void onDisable() {
        if (mc.currentScreen instanceof EarthClickGui) {
            mc.currentScreen.close();
        }
    }

    @EventListener
    public void onUpdate(UpdateEvent event) {
        if (!(mc.currentScreen instanceof EarthClickGui)) {
            disable();
        }
    }
}
