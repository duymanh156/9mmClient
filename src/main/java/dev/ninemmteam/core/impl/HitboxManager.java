package dev.ninemmteam.core.impl;

import dev.ninemmteam.fentanyl;
import dev.ninemmteam.api.events.eventbus.EventListener;
import dev.ninemmteam.api.events.impl.PacketEvent;
import dev.ninemmteam.api.utils.Wrapper;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityPose;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.network.packet.s2c.play.EntityTrackerUpdateS2CPacket;
import net.minecraft.util.math.Box;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

public class HitboxManager implements Wrapper {
    public static HitboxManager INSTANCE = new HitboxManager();

    private final List<Entity> serverCrawling = new CopyOnWriteArrayList<>();

    public HitboxManager() {
        fentanyl.EVENT_BUS.subscribe(this);
    }

    @EventListener
    public void onPacketReceive(PacketEvent.Receive event) {
        if (event.isCancelled()) return;
        
        if (event.getPacket() instanceof EntityTrackerUpdateS2CPacket packet) {
            Entity entity = mc.world.getEntityById(packet.id());
            if (!(entity instanceof PlayerEntity)) {
                return;
            }

            packet.trackedValues().forEach(serializedEntry -> {
                try {
                    int id = serializedEntry.id();
                    if (id == 6) {
                        Object value = serializedEntry.value();
                        if (value instanceof EntityPose pose) {
                            if (pose == EntityPose.SWIMMING) {
                                if (!serverCrawling.contains(entity)) {
                                    serverCrawling.add(entity);
                                }
                            } else {
                                serverCrawling.remove(entity);
                            }
                        }
                    }
                } catch (Exception ignored) {}
            });
        }
    }

    public boolean isServerCrawling(Entity entity) {
        return serverCrawling.contains(entity);
    }

    public Box getCrawlingBoundingBox(Entity entity) {
        return entity.getDimensions(EntityPose.SWIMMING).getBoxAt(entity.getPos());
    }
}
