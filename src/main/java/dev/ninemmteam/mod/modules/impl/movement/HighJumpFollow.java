package dev.ninemmteam.mod.modules.impl.movement;

import dev.ninemmteam.fentanyl;
import dev.ninemmteam.api.events.eventbus.EventListener;
import dev.ninemmteam.api.events.impl.UpdateEvent;
import dev.ninemmteam.api.utils.math.Timer;
import dev.ninemmteam.mod.modules.Module;
import dev.ninemmteam.mod.modules.settings.impl.SliderSetting;
import net.minecraft.entity.player.PlayerEntity;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;

public class HighJumpFollow extends Module {
    private final SliderSetting height = this.add(new SliderSetting("Height", 5.0, 1.0, 10.0));
    private final SliderSetting time = this.add(new SliderSetting("Time", 0.3, 0.1, 2.0));
    private final SliderSetting range = this.add(new SliderSetting("Range", 10.0, 5.0, 50.0));

    private final Map<Integer, LinkedList<Record>> playerHistory = new HashMap<>();
    private final Timer jumpTimer = new Timer();

    private boolean shouldDisableHighJump = false;
    private final Timer highJumpDisableTimer = new Timer();
    private boolean highJumpWasOff = false;

    public HighJumpFollow() {
        super("HighJumpFollow", "Jumps when nearby players rise quickly", Category.Movement);
        this.setChinese("高跳跟随");
    }

    @EventListener
    public void onUpdate(UpdateEvent event) {
        if (nullCheck()) return;

        if (shouldDisableHighJump && highJumpDisableTimer.passed(100)) { // 100ms = 0.1s
            HighJump highJump = fentanyl.MODULE.getModule(HighJump.class);
            if (highJumpWasOff && highJump.isOn()) {
                highJump.disable();
            }
            shouldDisableHighJump = false;
        }

        playerHistory.keySet().removeIf(id -> mc.world.getEntityById(id) == null);

        for (PlayerEntity player : mc.world.getPlayers()) {
            if (player == mc.player || fentanyl.FRIEND.isFriend(player)) {
                playerHistory.remove(player.getId());
                continue;
            }

            if (mc.player.distanceTo(player) > range.getValue()) {
                playerHistory.remove(player.getId());
                continue;
            }

            int id = player.getId();
            double currentY = player.getY();
            long now = System.currentTimeMillis();
            long timeWindow = (long) (time.getValue() * 1000);

            LinkedList<Record> history = playerHistory.computeIfAbsent(id, k -> new LinkedList<>());

            history.addLast(new Record(now, currentY));

            while (!history.isEmpty() && now - history.getFirst().time > timeWindow) {
                history.removeFirst();
            }

            if (!history.isEmpty()) {
                double minY = Double.MAX_VALUE;
                for (Record r : history) {
                    if (r.y < minY) {
                        minY = r.y;
                    }
                }

                if (currentY - minY >= height.getValue()) {
                    if (mc.player.isOnGround() && jumpTimer.passed(500)) { // 500ms cooldown
                        HighJump highJump = fentanyl.MODULE.getModule(HighJump.class);
                        highJumpWasOff = !highJump.isOn();
                        
                        if (highJumpWasOff) {
                            highJump.enable();
                        }
                        
                        mc.player.jump();
                        
                        if (highJumpWasOff) {
                            shouldDisableHighJump = true;
                            highJumpDisableTimer.reset();
                        }
                        
                        jumpTimer.reset();
                        history.clear(); 
                    }
                }
            }
        }
    }

    private static class Record {
        long time;
        double y;

        Record(long time, double y) {
            this.time = time;
            this.y = y;
        }
    }
}
