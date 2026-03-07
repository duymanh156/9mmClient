package dev.ninemmteam.mod.modules.impl.render;

import dev.ninemmteam.fentanyl;
import dev.ninemmteam.api.events.eventbus.EventListener;
import dev.ninemmteam.api.events.impl.Render3DEvent;
import dev.ninemmteam.mod.modules.Module;
import dev.ninemmteam.mod.modules.settings.impl.BooleanSetting;
import dev.ninemmteam.mod.modules.settings.impl.SliderSetting;
import net.minecraft.entity.EntityPose;
import net.minecraft.entity.player.PlayerEntity;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class FakeSquat extends Module {
    public static FakeSquat INSTANCE;
    

    private final BooleanSetting friends = add(new BooleanSetting("Friends", true));
    private final BooleanSetting onlyMoving = add(new BooleanSetting("OnlyMoving", false));
    private final BooleanSetting noLimb = add(new BooleanSetting("NoLimb", false));
    private final SliderSetting range = add(new SliderSetting("Range", 50, 1, 100, 1));
    
    private final Map<UUID, EntityPose> originalPoses = new HashMap<>();
    
    public FakeSquat() {
        super("FakeSquat", Category.Render);
        setChinese("假蹲");
        INSTANCE = this;
    }
    
    @Override
    public void onEnable() {
        super.onEnable();
        originalPoses.clear();
    }
    
    @Override
    public void onDisable() {
        super.onDisable();
        restoreAll();
    }
    
    @EventListener
    public void onRender3D(Render3DEvent event) {
        if (nullCheck()) return;
        
        for (PlayerEntity player : mc.world.getPlayers()) {
            if (player == mc.player) continue;
            
            if (!friends.getValue() && fentanyl.FRIEND.isFriend(player)) continue;
            
            double dist = mc.player.squaredDistanceTo(player);
            if (dist > range.getValue() * range.getValue()) {
                restorePlayer(player);
                continue;
            }
            
            if (onlyMoving.getValue() && player.getVelocity().horizontalLength() < 0.01) {
                restorePlayer(player);
                continue;
            }
            
            forceSneak(player);
        }
    }
    
    private void forceSneak(PlayerEntity player) {
        UUID uuid = player.getUuid();
        
        if (!originalPoses.containsKey(uuid)) {
            originalPoses.put(uuid, player.getPose());
        }
        
        player.setPose(EntityPose.CROUCHING);
    }
    
    private void restorePlayer(PlayerEntity player) {
        UUID uuid = player.getUuid();
        
        if (originalPoses.containsKey(uuid)) {
            EntityPose originalPose = originalPoses.get(uuid);
            if (player.getPose() == EntityPose.CROUCHING) {
                player.setPose(originalPose);
            }
            originalPoses.remove(uuid);
        }
    }
    
    private void restoreAll() {
        if (mc.world == null) return;
        
        for (PlayerEntity player : mc.world.getPlayers()) {
            UUID uuid = player.getUuid();
            
            if (originalPoses.containsKey(uuid)) {
                EntityPose originalPose = originalPoses.get(uuid);
                player.setPose(originalPose);
            }
        }
        
        originalPoses.clear();
    }
    
    public static boolean shouldForceSneak(PlayerEntity player) {
        if (!INSTANCE.isOn()) return false;
        if (player == INSTANCE.mc.player) return false;
        if (!INSTANCE.friends.getValue() && fentanyl.FRIEND.isFriend(player)) return false;
        
        double dist = INSTANCE.mc.player.squaredDistanceTo(player);
        if (dist > INSTANCE.range.getValue() * INSTANCE.range.getValue()) return false;
        
        if (INSTANCE.onlyMoving.getValue()) {
            return player.getVelocity().horizontalLength() >= 0.01;
        }
        
        return true;
    }
    
    public static boolean shouldNoLimb(PlayerEntity player) {
        if (!INSTANCE.isOn()) return false;
        if (!INSTANCE.noLimb.getValue()) return false;
        if (player == INSTANCE.mc.player) return false;
        if (!INSTANCE.friends.getValue() && fentanyl.FRIEND.isFriend(player)) return false;
        
        double dist = INSTANCE.mc.player.squaredDistanceTo(player);
        return dist <= INSTANCE.range.getValue() * INSTANCE.range.getValue();
    }
}
