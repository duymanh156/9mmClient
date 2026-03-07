package dev.ninemmteam.mod.modules.impl.misc;

import dev.ninemmteam.mod.modules.Module;

public class AutoBreak extends Module {
    public static AutoBreak INSTANCE;

    public boolean didAction = false;

    public AutoBreak() {
        super("AutoBreak", Module.Category.Misc);
        this.setChinese("自动破坏");
        INSTANCE = this;
    }

    @Override
    public void onDisable() {
        didAction = false;
        super.onDisable();
    }

    @Override
    public String getInfo() {
        return null;
    }
}
