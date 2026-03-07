package dev.ninemmteam.mod.modules.impl.combat;

import dev.ninemmteam.fentanyl;
import dev.ninemmteam.api.events.eventbus.EventListener;
import dev.ninemmteam.api.events.impl.EntitySpawnEvent;
import dev.ninemmteam.api.events.impl.PacketEvent;
import dev.ninemmteam.api.events.impl.RemoveEntityEvent;
import dev.ninemmteam.api.events.impl.UpdateEvent;
import dev.ninemmteam.api.utils.math.Timer;
import dev.ninemmteam.api.utils.player.EntityUtil;
import dev.ninemmteam.api.utils.player.InventoryUtil;
import dev.ninemmteam.api.utils.world.BlockUtil;
import dev.ninemmteam.api.utils.world.CrystalUtil;
import dev.ninemmteam.mod.modules.Module;
import dev.ninemmteam.mod.modules.impl.render.PlaceRender;
import dev.ninemmteam.mod.modules.settings.impl.BooleanSetting;
import dev.ninemmteam.mod.modules.settings.impl.EnumSetting;
import dev.ninemmteam.mod.modules.settings.impl.SliderSetting;
import net.minecraft.block.Blocks;
import net.minecraft.entity.decoration.EndCrystalEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.network.packet.c2s.play.PlayerInteractEntityC2SPacket;
import net.minecraft.network.packet.s2c.play.EntitiesDestroyS2CPacket;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class AntiCrystalBomb extends Module {
    public static AntiCrystalBomb INSTANCE;
    
    public enum TargetMode {
        Enemy,
        Friend,
        All
    }
    
    private final SliderSetting minDamage = this.add(new SliderSetting("MinDamage", 4.0, 0.0, 20.0));
    private final SliderSetting range = this.add(new SliderSetting("Range", 6.0, 1.0, 10.0));
    private final SliderSetting delay = this.add(new SliderSetting("Delay", 0, 0, 500).setSuffix("ms"));
    private final BooleanSetting rotate = this.add(new BooleanSetting("Rotate", true));
    private final BooleanSetting packet = this.add(new BooleanSetting("Packet", true));
    private final BooleanSetting inventory = this.add(new BooleanSetting("InventorySwap", true));
    private final EnumSetting<TargetMode> targetMode = this.add(new EnumSetting<>("Target", TargetMode.Enemy));
    private final BooleanSetting self = this.add(new BooleanSetting("Self", true));
    private final BooleanSetting debug = this.add(new BooleanSetting("Debug", false));
    
    private final Timer timer = new Timer();
    private final Map<BlockPos, CrystalInfo> crystalMap = new ConcurrentHashMap<>();
    
    public AntiCrystalBomb() {
        super("AntiCrystalBomb", Category.Combat);
        this.setChinese("防水晶轰炸");
        INSTANCE = this;
    }
    
    private record CrystalInfo(BlockPos pos, Vec3d crystalPos, long spawnTime, String ownerName, boolean isSelf, boolean isFriend) {}
    
    @EventListener
    public void onEntitySpawn(EntitySpawnEvent event) {
        if (mc.world == null || mc.player == null) return;
        
        if (event.getEntity() instanceof EndCrystalEntity crystal) {
            BlockPos pos = crystal.getBlockPos();
            Vec3d crystalPos = crystal.getPos();
            
            PlayerEntity closestPlayer = null;
            double closestDist = Double.MAX_VALUE;
            
            for (PlayerEntity player : mc.world.getPlayers()) {
                double dist = player.squaredDistanceTo(crystalPos);
                if (dist < closestDist && dist < 36.0) {
                    closestDist = dist;
                    closestPlayer = player;
                }
            }
            
            String ownerName = closestPlayer != null ? closestPlayer.getName().getString() : "Unknown";
            boolean isSelf = closestPlayer == mc.player;
            boolean isFriend = closestPlayer != null && fentanyl.FRIEND.isFriend(closestPlayer);
            
            crystalMap.put(pos, new CrystalInfo(pos, crystalPos, System.currentTimeMillis(), ownerName, isSelf, isFriend));
            
            if (debug.getValue()) {
                dev.ninemmteam.core.impl.CommandManager.sendMessage("Crystal spawned at " + pos + " by " + ownerName + 
                    (isSelf ? " (Self)" : isFriend ? " (Friend)" : " (Enemy)"));
            }
        }
    }
    
    @EventListener
    public void onPacketReceive(PacketEvent.Receive event) {
        if (mc.world == null || mc.player == null) return;
        
        if (event.getPacket() instanceof EntitiesDestroyS2CPacket packet) {
            packet.getEntityIds().forEach(id -> {
                if (mc.world.getEntityById(id) instanceof EndCrystalEntity crystal) {
                    BlockPos pos = crystal.getBlockPos();
                    CrystalInfo info = crystalMap.remove(pos);
                    
                    if (info == null) {
                        Map.Entry<BlockPos, CrystalInfo> entry = crystalMap.entrySet().stream()
                            .filter(e -> e.getKey().getSquaredDistance(pos) < 4)
                            .findFirst()
                            .orElse(null);
                        if (entry != null) {
                            info = entry.getValue();
                            crystalMap.remove(entry.getKey());
                        }
                    }
                    
                    if (info != null) {
                        handleCrystalExplosion(info);
                    }
                }
            });
        }
    }
    
    @EventListener
    public void onRemoveEntity(RemoveEntityEvent event) {
        if (mc.world == null || mc.player == null) return;
        
        if (event.getEntity() instanceof EndCrystalEntity crystal) {
            BlockPos pos = crystal.getBlockPos();
            CrystalInfo info = crystalMap.remove(pos);
            
            if (info == null) {
                Map.Entry<BlockPos, CrystalInfo> entry = crystalMap.entrySet().stream()
                    .filter(e -> e.getKey().getSquaredDistance(pos) < 4)
                    .findFirst()
                    .orElse(null);
                if (entry != null) {
                    info = entry.getValue();
                    crystalMap.remove(entry.getKey());
                }
            }
            
            if (info != null) {
                handleCrystalExplosion(info);
            }
        }
    }
    
    @EventListener
    public void onUpdate(UpdateEvent event) {
        crystalMap.entrySet().removeIf(entry -> 
            System.currentTimeMillis() - entry.getValue().spawnTime() > 30000
        );
    }
    
    private void handleCrystalExplosion(CrystalInfo info) {
        if (!timer.passedMs(delay.getValue())) return;
        
        if (!shouldProcess(info)) return;
        
        Vec3d crystalPos = info.crystalPos();
        double distanceToPlayer = mc.player.getPos().distanceTo(crystalPos);
        
        if (distanceToPlayer > range.getValue()) return;
        
        float damage = CrystalUtil.calculateDamage(mc.player, crystalPos, false, false);
        
        if (damage < minDamage.getValueFloat()) return;
        
        if (debug.getValue()) {
            String ownerType = info.isSelf() ? "Self" : info.isFriend() ? "Friend" : "Enemy";
            dev.ninemmteam.core.impl.CommandManager.sendMessage(
                ownerType + " crystal exploded! Damage: " + String.format("%.1f", damage) + 
                " Owner: " + info.ownerName()
            );
        }
        
        placeObsidianBetween(crystalPos);
        
        timer.reset();
    }
    
    private boolean shouldProcess(CrystalInfo info) {
        if (info.isSelf()) {
            return self.getValue();
        }
        
        if (info.isFriend()) {
            return targetMode.getValue() == TargetMode.Friend || targetMode.getValue() == TargetMode.All;
        }
        
        return targetMode.getValue() == TargetMode.Enemy || targetMode.getValue() == TargetMode.All;
    }
    
    private void placeObsidianBetween(Vec3d crystalPos) {
        Vec3d playerPos = mc.player.getEyePos();
        Vec3d direction = playerPos.subtract(crystalPos).normalize();
        
        BlockPos bestPos = null;
        double bestReduction = 0;
        
        for (int i = 1; i <= 3; i++) {
            Vec3d checkPos = crystalPos.add(direction.multiply(i));
            BlockPos blockPos = BlockPos.ofFloored(checkPos);
            
            if (canPlaceObsidian(blockPos)) {
                double reduction = calculateDamageReduction(crystalPos, blockPos);
                
                if (reduction > bestReduction) {
                    bestReduction = reduction;
                    bestPos = blockPos;
                }
            }
        }
        
        if (bestPos != null) {
            placeObsidian(bestPos);
        }
    }
    
    private double calculateDamageReduction(Vec3d crystalPos, BlockPos blockPos) {
        Vec3d blockCenter = blockPos.toCenterPos();
        Vec3d playerEye = mc.player.getEyePos();
        
        Vec3d toPlayer = playerEye.subtract(crystalPos).normalize();
        Vec3d toBlock = blockCenter.subtract(crystalPos).normalize();
        
        return toBlock.dotProduct(toPlayer);
    }
    
    private boolean canPlaceObsidian(BlockPos pos) {
        if (mc.world.isAir(pos) || mc.world.getBlockState(pos).isReplaceable()) {
            if (!BlockUtil.canPlace(pos)) return false;
            
            for (Direction dir : Direction.values()) {
                BlockPos neighbor = pos.offset(dir);
                if (!mc.world.isAir(neighbor) && !mc.world.getBlockState(neighbor).isReplaceable()) {
                    return true;
                }
            }
        }
        return false;
    }
    
    private void placeObsidian(BlockPos pos) {
        int slot = findObsidian();
        if (slot == -1) return;
        
        int oldSlot = mc.player.getInventory().selectedSlot;
        doSwap(slot);
        
        BlockUtil.placeBlock(pos, rotate.getValue(), packet.getValue());
        
        if (inventory.getValue()) {
            doSwap(slot);
            EntityUtil.syncInventory();
        } else {
            InventoryUtil.switchToSlot(oldSlot);
        }
        
        PlaceRender.INSTANCE.create(pos);
        
        if (debug.getValue()) {
            dev.ninemmteam.core.impl.CommandManager.sendMessage("Placed obsidian at " + pos);
        }
    }
    
    private int findObsidian() {
        if (inventory.getValue()) {
            return InventoryUtil.findBlockInventorySlot(Blocks.OBSIDIAN);
        }
        return InventoryUtil.findBlock(Blocks.OBSIDIAN);
    }
    
    private void doSwap(int slot) {
        if (inventory.getValue()) {
            InventoryUtil.inventorySwap(slot, mc.player.getInventory().selectedSlot);
        } else {
            InventoryUtil.switchToSlot(slot);
        }
    }
    
    @Override
    public void onDisable() {
        crystalMap.clear();
    }
}
