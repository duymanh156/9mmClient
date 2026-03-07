package dev.ninemmteam.core.impl;

import dev.ninemmteam.fentanyl;
import dev.ninemmteam.api.events.eventbus.EventListener;
import dev.ninemmteam.api.events.impl.PacketEvent;
import dev.ninemmteam.api.events.impl.Render3DEvent;
import dev.ninemmteam.api.utils.Wrapper;
import dev.ninemmteam.api.utils.render.Render3DUtil;
import net.minecraft.block.Blocks;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.network.packet.s2c.play.BlockBreakingProgressS2CPacket;
import net.minecraft.util.math.BlockPos;

import java.awt.Color;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class FriendMineTracker implements Wrapper {
    public static FriendMineTracker INSTANCE;
    
    private final Map<Integer, BlockPos> friendMiningBlocks = new ConcurrentHashMap<>();
    private final Map<Integer, String> friendNames = new ConcurrentHashMap<>();
    private final Map<Integer, Long> miningStartTime = new ConcurrentHashMap<>();
    private static final long TIMEOUT_MS = 5000;
    
    public FriendMineTracker() {
        INSTANCE = this;
        fentanyl.EVENT_BUS.subscribe(this);
    }
    
    @EventListener
    public void onPacketReceive(PacketEvent.Receive event) {
        if (mc.world == null || mc.player == null) return;
        
        if (event.getPacket() instanceof BlockBreakingProgressS2CPacket packet) {
            BlockPos pos = packet.getPos();
            int entityId = packet.getEntityId();
            
            Entity entity = mc.world.getEntityById(entityId);
            if (entity == null || !(entity instanceof PlayerEntity player) || player.equals(mc.player)) return;
            
            String playerName = player.getGameProfile().getName();
            
            if (fentanyl.FRIEND.isFriend(playerName)) {
                if (mc.world.getBlockState(pos).getBlock() != Blocks.AIR) {
                    friendMiningBlocks.put(entityId, pos);
                    friendNames.put(entityId, playerName);
                    miningStartTime.put(entityId, System.currentTimeMillis());
                }
            }
        }
    }
    
    @EventListener
    public void onRender3D(Render3DEvent event) {
        if (friendMiningBlocks.isEmpty()) return;
        
        long currentTime = System.currentTimeMillis();
        friendMiningBlocks.entrySet().removeIf(entry -> {
            int entityId = entry.getKey();
            Long startTime = miningStartTime.get(entityId);
            if (startTime == null || currentTime - startTime > TIMEOUT_MS) {
                miningStartTime.remove(entityId);
                friendNames.remove(entityId);
                return true;
            }
            
            BlockPos pos = entry.getValue();
            if (mc.world.getBlockState(pos).getBlock() == Blocks.AIR) {
                miningStartTime.remove(entityId);
                friendNames.remove(entityId);
                return true;
            }
            return false;
        });
        
        for (Map.Entry<Integer, BlockPos> entry : friendMiningBlocks.entrySet()) {
            int entityId = entry.getKey();
            BlockPos pos = entry.getValue();
            String name = friendNames.get(entityId);
            if (name == null) continue;
            
            Color friendColor = new Color(0, 255, 100, 150);
            Render3DUtil.drawFill(event.matrixStack, new net.minecraft.util.math.Box(pos), friendColor);
            Render3DUtil.drawBox(event.matrixStack, new net.minecraft.util.math.Box(pos), new Color(0, 255, 100, 255));
            Render3DUtil.drawText3D(name, pos.toCenterPos().add(0, 0.5, 0), new Color(0, 255, 100, 255));
        }
    }
    
    public boolean isFriendMining(BlockPos pos) {
        return friendMiningBlocks.containsValue(pos);
    }
    
    public String getFriendMiningAt(BlockPos pos) {
        for (Map.Entry<Integer, BlockPos> entry : friendMiningBlocks.entrySet()) {
            if (entry.getValue().equals(pos)) {
                return friendNames.get(entry.getKey());
            }
        }
        return null;
    }
    
    public Map<Integer, BlockPos> getFriendMiningBlocks() {
        return friendMiningBlocks;
    }
    
    public void clear() {
        friendMiningBlocks.clear();
        friendNames.clear();
        miningStartTime.clear();
    }
}
