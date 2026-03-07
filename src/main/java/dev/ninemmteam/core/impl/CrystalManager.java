package dev.ninemmteam.core.impl;

import dev.ninemmteam.fentanyl;
import dev.ninemmteam.api.events.eventbus.EventListener;
import dev.ninemmteam.api.events.impl.ClientTickEvent;
import dev.ninemmteam.api.utils.Wrapper;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.entity.Entity;
import net.minecraft.entity.decoration.EndCrystalEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;

public class CrystalManager implements Wrapper {
    public static CrystalManager INSTANCE;

    public static Map<BlockPos, BoxPair> crystalBoxes = new ConcurrentHashMap<>();

    public CrystalManager() {
        INSTANCE = this;
        fentanyl.EVENT_BUS.subscribe(this);
    }

    @EventListener
    public void onTick(ClientTickEvent event) {
        if (mc.world == null || mc.player == null) {
            crystalBoxes.clear();
            return;
        }

        for (Entity entity : mc.world.getEntities()) {
            if (entity instanceof EndCrystalEntity crystal) {
                if (!entity.isAlive()) continue;

                if (mc.player.distanceTo(entity) > 16) continue;

                if (!crystalBoxes.containsKey(crystal.getBlockPos())) {
                    crystalBoxes.put(entity.getBlockPos(), new BoxPair(entity.getBoundingBox()));
                }
            }
        }

        long currentTime = System.currentTimeMillis();
        for (Map.Entry<BlockPos, BoxPair> entry : crystalBoxes.entrySet()) {
            BlockPos pos = entry.getKey();
            BoxPair pair = entry.getValue();

            if (mc.player.getPos().squaredDistanceTo(pos.toCenterPos()) > 16.0 * 16.0) {
                crystalBoxes.remove(pos);
                return;
            }

            EndCrystalEntity crystal = null;
            for (EndCrystalEntity entity : mc.world.getNonSpectatingEntities(EndCrystalEntity.class, pair.box())) {
                if (entity.getBlockPos().equals(pos)) {
                    crystal = entity;
                    break;
                }
            }
            if (crystal == null && currentTime - pair.time() > 600L) {
                crystalBoxes.remove(pos);
            } else if (crystal != null) {
                crystalBoxes.put(pos, new BoxPair(crystal.getBoundingBox()));
            }
        }
    }

    public boolean isRecentlyBlocked(BlockPos pos) {
        if (mc.world == null) return false;

        Box blockBox = new Box(pos);

        for (Entity entity : mc.world.getNonSpectatingEntities(Entity.class, blockBox)) {
            if (entity instanceof EndCrystalEntity) {
                return true;
            }
        }
        for (Map.Entry<BlockPos, BoxPair> entry : crystalBoxes.entrySet()) {
            BoxPair pair = entry.getValue();
            Box box = pair.box();
            if (box.intersects(blockBox)) {
                return true;
            }
        }
        return false;
    }

    public record BoxPair(Box box, long time) {
        public BoxPair(Box box) {
            this(box, System.currentTimeMillis());
        }
    }
}
