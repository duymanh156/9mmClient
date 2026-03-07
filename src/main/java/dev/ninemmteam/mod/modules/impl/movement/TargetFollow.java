package dev.ninemmteam.mod.modules.impl.movement;

import dev.ninemmteam.fentanyl;
import dev.ninemmteam.api.events.eventbus.EventListener;
import dev.ninemmteam.api.events.impl.UpdateEvent;
import dev.ninemmteam.api.events.impl.UpdateRotateEvent;
import dev.ninemmteam.api.utils.math.MathUtil;
import dev.ninemmteam.mod.modules.Module;
import dev.ninemmteam.mod.modules.impl.client.AntiCheat;
import dev.ninemmteam.mod.modules.settings.impl.SliderSetting;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.Vec3d;

import dev.ninemmteam.core.impl.RotationManager;

public class TargetFollow extends Module {
    private final SliderSetting range = this.add(new SliderSetting("Range", 50.0, 1.0, 100.0, 1.0));

    public TargetFollow() {
        super("TargetFollow", Module.Category.Movement);
        this.setChinese("跟随玩家");
    }

    @Override
    public void onEnable() {
        if (nullCheck()) return;
        if (ElytraFly.INSTANCE.isOn() && ElytraFly.INSTANCE.mode.is(ElytraFly.Mode.Control)) {
            this.disable();
            sendMessage("§4TargetFollow disabled due to because ElytraFly mode is Control.");
        }
    }

    @Override
    public void onDisable() {
        if (nullCheck()) return;
    }

    @EventListener(priority = -9999)
    public void onRotation(UpdateRotateEvent event) {
        if (!nullCheck()) {
            PlayerEntity target = getClosestPlayer(this.range.getValue());
            if (target != null && ElytraFly.INSTANCE.isFallFlying()) {
                Vec3d pos = MathUtil.getClosestPointToBox(mc.player.getEyePos(), target.getBoundingBox());
                float[] rotations = RotationManager.getRotation(pos);
                event.setYaw(rotations[0]);
                event.setPitch(rotations[1]);
                mc.options.forwardKey.setPressed(true);
            }
        }
    }

    private PlayerEntity getClosestPlayer(double range) {
        PlayerEntity target = null;
        double distance = range;

        for (Entity entity : fentanyl.THREAD.getEntities()) {
            if (entity instanceof PlayerEntity player && entity != mc.player && !fentanyl.FRIEND.isFriend(player.getName().getString())) {
                if (mc.player.distanceTo(entity) <= range) {
                    if (target == null) {
                        target = player;
                        distance = mc.player.distanceTo(entity);
                    } else if (mc.player.distanceTo(entity) < distance) {
                        target = player;
                        distance = mc.player.distanceTo(entity);
                    }
                }
            }
        }
        return target;
    }

}