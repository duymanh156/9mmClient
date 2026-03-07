package dev.ninemmteam.mod.gui.earth.component;

import dev.ninemmteam.mod.modules.settings.Setting;

public class SettingComponent<T extends Setting> extends Component {
    protected final T setting;

    public SettingComponent(String label, float posX, float posY, float offsetX,
                            float offsetY, float width, float height, T setting) {
        super(label, posX, posY, offsetX, offsetY, width, height);
        this.setting = setting;
    }

    public T getSetting() {
        return setting;
    }
}
