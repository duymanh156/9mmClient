package dev.ninemmteam.mod.modules.impl.client;

import dev.ninemmteam.fentanyl;
import dev.ninemmteam.mod.modules.Module;
import dev.ninemmteam.mod.modules.settings.impl.StringSetting;

public class WindowTitle extends Module {
    public static WindowTitle INSTANCE;
    public final StringSetting title = this.add(new StringSetting("Title", fentanyl.NAME + " " + fentanyl.VERSION));

    public WindowTitle() {
        super("WindowTitle", Module.Category.Client);
        this.setChinese("窗口标题");
        INSTANCE = this;
        this.enable();
    }

    public String getDynamicTitle() {
        return this.title.getValue();
    }
}
