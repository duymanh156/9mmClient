package dev.ninemmteam.mod.modules.impl.misc;

import dev.ninemmteam.api.events.eventbus.EventListener;
import dev.ninemmteam.api.events.impl.UpdateEvent;
import dev.ninemmteam.mod.modules.Module;
import dev.ninemmteam.mod.modules.settings.impl.SliderSetting;
import net.minecraft.entity.player.PlayerEntity;

public class Auto250 extends Module {
    public static Auto250 INSTANCE;
    private final SliderSetting speed = this.add(new SliderSetting("Speed", 10.0, 1.0, 50.0, 1.0));
    private float currentYaw = 0.0F;

    public Auto250() {
        super("Auto250", Category.Misc);
        this.setChinese("自动转圈");
        INSTANCE = this;
    }

    @Override
    public void onEnable() {
        super.onEnable();
        if (mc.player != null) {
            this.currentYaw = mc.player.getYaw();
        }
    }

    @EventListener
    public void onUpdate(UpdateEvent event) {
        if (mc.player != null) {
            float speedValue = this.speed.getValueFloat();
            this.currentYaw += speedValue;
            if (this.currentYaw >= 360.0F) {
                this.currentYaw -= 360.0F;
            }
            mc.player.setYaw(this.currentYaw);
            mc.player.setHeadYaw(this.currentYaw);
            mc.player.bodyYaw = this.currentYaw;
        }
    }

    @Override
    public void onDisable() {
        super.onDisable();
        if (mc.player != null) {
            mc.player.setHeadYaw(mc.player.getYaw());
            mc.player.bodyYaw = mc.player.getYaw();
        }
    }

    @Override
    public String getInfo() {
        return String.valueOf((int) this.speed.getValue());
    }
}
