package dev.ninemmteam.mod.modules.impl.misc;

import dev.ninemmteam.api.events.eventbus.EventListener;
import dev.ninemmteam.api.events.impl.UpdateEvent;
import dev.ninemmteam.mod.modules.Module;
import dev.ninemmteam.mod.modules.settings.impl.StringSetting;

public class AutoCommand extends Module {
    private final StringSetting command = this.add(new StringSetting("Command", "/kit 1"));
    private boolean sent = false;

    public AutoCommand() {
        super("AutoCommand", Module.Category.Misc);
        this.setChinese("自动指令");
    }

    @Override
    public void onEnable() {
        this.sent = false;
    }

    @EventListener
    public void onUpdate(UpdateEvent event) {
        if (!this.sent && mc.player != null && mc.world != null && mc.getNetworkHandler() != null) {
            String cmd = this.command.getValue();
            if (cmd.startsWith("/")) {
                cmd = cmd.substring(1);
            }
            mc.player.networkHandler.sendCommand(cmd);
            this.sent = true;
        }
    }

    @Override
    public void onLogin() {
        this.sent = false;
    }

    @Override
    public void onLogout() {
        this.sent = false;
    }
}
