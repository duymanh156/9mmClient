package dev.ninemmteam.mod.modules.impl.render;

import dev.ninemmteam.fentanyl;
import dev.ninemmteam.api.events.eventbus.EventListener;
import dev.ninemmteam.api.events.impl.Render3DEvent;
import dev.ninemmteam.mod.modules.Module;
import dev.ninemmteam.mod.modules.settings.impl.BooleanSetting;
import dev.ninemmteam.mod.modules.settings.impl.SliderSetting;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.Vec3d;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class FakePPS extends Module {
    public static FakePPS INSTANCE;
    
    private final SliderSetting updateRate = add(new SliderSetting("UpdateRate", 5, 1, 20, 1));
    private final BooleanSetting self = add(new BooleanSetting("Self", false));
    private final BooleanSetting friends = add(new BooleanSetting("Friends", true));
    private final SliderSetting range = add(new SliderSetting("Range", 50, 1, 100, 1));
    
    private final Map<UUID, PlayerSnapshot> snapshots = new HashMap<>();
    private long lastUpdateTime = 0;
    
    public FakePPS() {
        super("FakePPS", Category.Render);
        setChinese("假卡顿");
        INSTANCE = this;
    }
    
    @Override
    public void onEnable() {
        super.onEnable();
        snapshots.clear();
        lastUpdateTime = System.currentTimeMillis();
    }
    
    @Override
    public void onDisable() {
        super.onDisable();
        snapshots.clear();
    }
    
    @EventListener
    public void onRender3D(Render3DEvent event) {
        if (nullCheck()) return;
        
        long currentTime = System.currentTimeMillis();
        long interval = (long) (1000.0 / updateRate.getValue());
        
        if (currentTime - lastUpdateTime >= interval) {
            for (PlayerEntity player : mc.world.getPlayers()) {
                if (!self.getValue() && player == mc.player) continue;
                if (!friends.getValue() && fentanyl.FRIEND.isFriend(player)) continue;
                
                double dist = mc.player.squaredDistanceTo(player);
                if (dist > range.getValue() * range.getValue()) {
                    snapshots.remove(player.getUuid());
                    continue;
                }
                
                snapshots.put(player.getUuid(), new PlayerSnapshot(
                    player.getX(),
                    player.getY(),
                    player.getZ(),
                    player.getYaw(),
                    player.getPitch(),
                    player.bodyYaw,
                    player.headYaw
                ));
            }
            lastUpdateTime = currentTime;
        }
        
        for (PlayerEntity player : mc.world.getPlayers()) {
            if (!self.getValue() && player == mc.player) continue;
            if (!friends.getValue() && fentanyl.FRIEND.isFriend(player)) continue;
            
            PlayerSnapshot snapshot = snapshots.get(player.getUuid());
            if (snapshot != null) {
                applySnapshot(player, snapshot);
            }
        }
    }
    
    private void applySnapshot(PlayerEntity player, PlayerSnapshot snapshot) {
        player.prevX = snapshot.x;
        player.prevY = snapshot.y;
        player.prevZ = snapshot.z;
        player.prevYaw = snapshot.yaw;
        player.prevPitch = snapshot.pitch;
        player.prevBodyYaw = snapshot.bodyYaw;
        player.prevHeadYaw = snapshot.headYaw;
    }
    
    public static boolean shouldApplyFakePPS(PlayerEntity player) {
        if (!INSTANCE.isOn()) return false;
        if (!INSTANCE.self.getValue() && player == INSTANCE.mc.player) return false;
        if (!INSTANCE.friends.getValue() && fentanyl.FRIEND.isFriend(player)) return false;
        
        double dist = INSTANCE.mc.player.squaredDistanceTo(player);
        return dist <= INSTANCE.range.getValue() * INSTANCE.range.getValue();
    }
    
    private static class PlayerSnapshot {
        final double x, y, z;
        final float yaw, pitch, bodyYaw, headYaw;
        
        PlayerSnapshot(double x, double y, double z, float yaw, float pitch, float bodyYaw, float headYaw) {
            this.x = x;
            this.y = y;
            this.z = z;
            this.yaw = yaw;
            this.pitch = pitch;
            this.bodyYaw = bodyYaw;
            this.headYaw = headYaw;
        }
    }
}
