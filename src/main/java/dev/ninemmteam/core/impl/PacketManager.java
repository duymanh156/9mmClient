package dev.ninemmteam.core.impl;

import dev.ninemmteam.fentanyl;
import dev.ninemmteam.api.events.eventbus.EventListener;
import dev.ninemmteam.api.events.impl.GameLeftEvent;
import dev.ninemmteam.api.utils.Wrapper;
import net.minecraft.client.network.PlayerListEntry;
import net.minecraft.network.packet.Packet;

import java.util.HashSet;
import java.util.Set;

public class PacketManager implements Wrapper {
    public static PacketManager INSTANCE = new PacketManager();

    private static final Set<Packet<?>> PACKET_CACHE = new HashSet<>();
    public static final Set<Packet<?>> CANCEL_AT_HANDLE = new HashSet<>();

    public PacketManager() {
        fentanyl.EVENT_BUS.subscribe(this);
    }

    @EventListener
    public void onGameLeft(GameLeftEvent event) {
        PACKET_CACHE.clear();
    }

    public void sendPacket(final Packet<?> p) {
        if (mc.getNetworkHandler() != null) {
            PACKET_CACHE.add(p);
            mc.getNetworkHandler().sendPacket(p);
        }
    }

    public void specialCaseCancel(Packet<?> packet) {
        CANCEL_AT_HANDLE.add(packet);
    }

    public int getClientLatency() {
        if (mc.getNetworkHandler() != null) {
            PlayerListEntry playerEntry = mc.getNetworkHandler().getPlayerListEntry(mc.player.getUuid());
            if (playerEntry != null) {
                return playerEntry.getLatency();
            }
        }
        return 0;
    }

    public boolean isCached(Packet<?> p) {
        return PACKET_CACHE.contains(p);
    }

    public boolean uncache(Packet<?> p) {
        return PACKET_CACHE.remove(p);
    }
}
