package dev.ninemmteam.mod.modules.impl.combat;

import dev.ninemmteam.fentanyl;
import dev.ninemmteam.api.events.eventbus.EventListener;
import dev.ninemmteam.api.events.impl.UpdateEvent;
import dev.ninemmteam.api.utils.combat.CombatUtil;
import dev.ninemmteam.api.utils.math.Timer;
import dev.ninemmteam.api.utils.player.EntityUtil;
import dev.ninemmteam.api.utils.player.InventoryUtil;
import dev.ninemmteam.api.utils.world.BlockUtil;
import dev.ninemmteam.mod.modules.Module;
import dev.ninemmteam.mod.modules.impl.client.AntiCheat;
import dev.ninemmteam.mod.modules.impl.render.PlaceRender;
import dev.ninemmteam.mod.modules.settings.enums.SwingSide;
import dev.ninemmteam.mod.modules.settings.impl.BooleanSetting;
import dev.ninemmteam.mod.modules.settings.impl.SliderSetting;
import net.minecraft.block.BlockState;
import net.minecraft.block.TrapdoorBlock;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.network.packet.c2s.play.PlayerInteractBlockC2SPacket;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;

import net.minecraft.entity.EntityPose;
import java.util.ArrayList;
import java.util.List;

public class AutoDoor2 extends Module {
    private final SliderSetting range = this.add(new SliderSetting("Range", 5.0, 1.0, 6.0));
    private final SliderSetting delay = this.add(new SliderSetting("Delay", 50, 0, 500).setSuffix("ms"));
    private final SliderSetting predict = this.add(new SliderSetting("Predict", 2.0, 0.0, 8.0).setSuffix("tick"));
    private final BooleanSetting rotate = this.add(new BooleanSetting("Rotate", true));
    private final BooleanSetting packet = this.add(new BooleanSetting("Packet", true));
    private final BooleanSetting inventory = this.add(new BooleanSetting("InventorySwap", true));
    private final BooleanSetting onlySurrounded = this.add(new BooleanSetting("OnlySurrounded", true));
    private final BooleanSetting strictSurround = this.add(new BooleanSetting("StrictSurround", false, this.onlySurrounded::getValue));
    private final BooleanSetting spam = this.add(new BooleanSetting("Spam", true));
    private final BooleanSetting replaceObs = this.add(new BooleanSetting("ReplaceObs", false));
    
    private final Timer timer = new Timer();
    private BlockPos currentTrapdoorPos = null;
    private final Timer obsidianPlaceTimer = new Timer();
    private BlockPos lastObsidianPos = null;

    public AutoDoor2() {
        super("AutoDoor2", Category.Combat);
        this.setChinese("自动活板门");
    }

    @EventListener
    public void onUpdate(UpdateEvent event) {
        PlayerEntity target = CombatUtil.getClosestEnemy(this.range.getValue());
        if (target == null) return;

        if (!this.timer.passedMs(this.delay.getValue())) return;

        if (this.onlySurrounded.getValue()) {
            if (this.strictSurround.getValue()) {
                if (!isStrictlySurrounded(target)) {
                    return;
                }
            } else {
                if (!isSurrounded(target)) {
                    return;
                }
            }
        }

        BlockPos feetPos = EntityUtil.getEntityPos(target, true);
        BlockState feetState = mc.world.getBlockState(feetPos);
        boolean hasFeetTrapdoor = feetState.getBlock() instanceof TrapdoorBlock;
        BlockPos currentHeadPos = EntityUtil.getEntityPos(target, true).up();
        
        handleFeetTrapdoor(feetPos, target);
        
        if (!hasFeetTrapdoor) {
            List<BlockPos> headPositions = new ArrayList<>();
            headPositions.add(currentHeadPos);

            if (this.predict.getValue() > 0) {
                Vec3d velocity = target.getVelocity();
                if (velocity.lengthSquared() > 0.0001) {
                    Vec3d predictedVec = target.getPos().add(velocity.multiply(this.predict.getValue()));
                    BlockPos predictedHeadPos = BlockPos.ofFloored(predictedVec).up();
                    if (!predictedHeadPos.equals(currentHeadPos)) {
                        headPositions.add(predictedHeadPos);
                    }
                }
            }

            for (BlockPos headPos : headPositions) {
                boolean isRealTarget = target.getBoundingBox().intersects(new Box(headPos));
                Direction closestWall = getClosestWall(target, headPos);
                
                if (closestWall != null) {
                    if (!isRealTarget || getDistanceToWall(target, closestWall) <= 0.4875) {
                         handleHeadTrapdoor(target, headPos, closestWall, isRealTarget);
                    }
                }
            }
        }
        
        if (this.replaceObs.getValue()) {
            handleReplaceObsidian(target, currentHeadPos);
            
            checkObsidianInstantMining();
        }
    }
    
    private boolean isSurrounded(PlayerEntity target) {
        BlockPos pos = EntityUtil.getEntityPos(target, true);
        for (Direction dir : Direction.values()) {
            if (dir == Direction.UP || dir == Direction.DOWN) continue;
            BlockPos offset = pos.offset(dir);
            if (mc.world.isAir(offset) && BlockUtil.canReplace(offset)) {
                return false;
            }
        }
        return true;
    }

    private boolean isStrictlySurrounded(PlayerEntity target) {
        BlockPos pos = EntityUtil.getEntityPos(target, true);
        BlockPos headPos = pos.up();

        for (Direction dir : Direction.values()) {
            if (dir == Direction.UP || dir == Direction.DOWN) continue;
            BlockPos offset = pos.offset(dir);
            if (mc.world.isAir(offset) && BlockUtil.canReplace(offset)) {
                return false;
            }
        }

        for (Direction dir : Direction.values()) {
            if (dir == Direction.UP || dir == Direction.DOWN) continue;
            BlockPos offset = headPos.offset(dir);
            if (mc.world.isAir(offset) && BlockUtil.canReplace(offset)) {
                return false;
            }
        }

        return true;
    }
    
    private Direction getClosestWall(PlayerEntity target, BlockPos headPos) {
        Direction bestDir = null;
        double minDesc = Double.MAX_VALUE;
        
        for (Direction dir : Direction.values()) {
            if (dir == Direction.UP || dir == Direction.DOWN) continue;
            
            // Check if there is a block to attach to at head level
            BlockPos wallPos = headPos.offset(dir);
            if (!mc.world.isAir(wallPos) && !BlockUtil.canReplace(wallPos)) {
                double dist = getDistanceToWall(target, dir);
                if (dist < minDesc) {
                    minDesc = dist;
                    bestDir = dir;
                }
            }
        }
        return bestDir;
    }
    
    private double getDistanceToWall(PlayerEntity target, Direction dir) {

        double pX = target.getX();
        double pZ = target.getZ();
        
        double relX = pX - Math.floor(pX);
        double relZ = pZ - Math.floor(pZ);
        
        switch (dir) {
            case NORTH: 
                return relZ; 
            case SOUTH: 
                return 1.0 - relZ;
            case WEST: 
                return relX;
            case EAST: 
                return 1.0 - relX;
        }
        return 1.0;
    }

    private boolean handleHeadTrapdoor(PlayerEntity target, BlockPos pos, Direction wallDir, boolean isRealTarget) {
        if (target.isCrawling() || target.isSwimming()) {
            return false;
        }
        
        BlockState state = mc.world.getBlockState(pos);
        
        if (state.getBlock() instanceof TrapdoorBlock) {
            
            if (isRealTarget) {
                if (!target.isSwimming() && !target.isCrawling()) {
                     if (this.spam.getValue()) {
                         BlockUtil.clickBlock(pos, Direction.UP, this.rotate.getValue(), Hand.MAIN_HAND, this.packet.getValue());
                         this.timer.reset();
                         return true;
                     } else {
                         if (!state.get(TrapdoorBlock.OPEN)) {
                            BlockUtil.clickBlock(pos, Direction.UP, this.rotate.getValue(), Hand.MAIN_HAND, this.packet.getValue());
                            this.timer.reset();
                            return true;
                        }
                     }
                }
            } else {
                if (!state.get(TrapdoorBlock.OPEN)) {
                    BlockUtil.clickBlock(pos, Direction.UP, this.rotate.getValue(), Hand.MAIN_HAND, this.packet.getValue());
                    this.timer.reset();
                    return true;
                }
            }
        } else {
            if (BlockUtil.canPlace(pos) || state.isAir() || state.isReplaceable()) {
                if (placeTrapdoorAgainstWall(pos, wallDir)) {
                    this.timer.reset();
                    return true;
                }
            }
        }
        return false;
    }

    private void handleFeetTrapdoor(BlockPos pos, PlayerEntity target) {
        if (target == null) return;
        
        BlockState state = mc.world.getBlockState(pos);
        
        if (state.getBlock() instanceof TrapdoorBlock) {
            if (!target.isCrawling() && !target.isSwimming()) {
                BlockUtil.clickBlock(pos, Direction.UP, this.rotate.getValue(), Hand.MAIN_HAND, this.packet.getValue());
                this.timer.reset();
            } else if (state.get(TrapdoorBlock.OPEN)) {
                BlockUtil.clickBlock(pos, Direction.UP, this.rotate.getValue(), Hand.MAIN_HAND, this.packet.getValue());
                this.timer.reset();
            }
        } else {
             if (BlockUtil.canPlace(pos) || state.isAir() || state.isReplaceable()) {
                if (placeTrapdoor(pos, target)) {
                    this.timer.reset();
                }
            }
        }
    }
    
    private void handleReplaceObsidian(PlayerEntity target, BlockPos headPos) {
        if (!target.isCrawling() && !target.isSwimming()) {
            return;
        }
        
        BlockState state = mc.world.getBlockState(headPos);
        
        if (state.getBlock() instanceof TrapdoorBlock) {
            this.currentTrapdoorPos = headPos;
            
            if (BlockUtil.canPlace(headPos)) {
                dev.ninemmteam.mod.modules.impl.player.SpeedMine.INSTANCE.mine(headPos);
                
                clearSpeedMinePosition();
                
                this.timer.reset();
                return;
            }
        } else if (state.isAir() || state.isReplaceable()) {
            if (this.currentTrapdoorPos != null && this.currentTrapdoorPos.equals(headPos)) {
                if (placeObsidian(headPos)) {
                    this.currentTrapdoorPos = null;
                    this.lastObsidianPos = headPos;
                    this.obsidianPlaceTimer.reset();
                    this.timer.reset();
                }
            }
        } else {
            this.currentTrapdoorPos = null;
        }
    }
    
    private boolean placeObsidian(BlockPos pos) {
        int slot = this.findItem(net.minecraft.item.Items.OBSIDIAN);
        int oldSlot = mc.player.getInventory().selectedSlot;

        if (slot != -1) {
            this.doSwap(slot);

            BlockUtil.placeBlock(pos, this.rotate.getValue(), this.packet.getValue());

            if (this.inventory.getValue()) {
                this.doSwap(slot);
                EntityUtil.syncInventory();
            } else {
                InventoryUtil.switchToSlot(oldSlot);
            }
            PlaceRender.INSTANCE.create(pos);
            return true;
        }
        return false;
    }
    
    private void checkObsidianInstantMining() {
        if (this.lastObsidianPos == null || !this.obsidianPlaceTimer.passedMs(100)) {
            return;
        }
        
        BlockState state = mc.world.getBlockState(this.lastObsidianPos);
        if (state.isAir() || state.isReplaceable()) {
            clearSpeedMinePosition();
            this.lastObsidianPos = null;
        }
    }
    
    private void clearSpeedMinePosition() {
        try {
            dev.ninemmteam.mod.modules.impl.player.SpeedMine speedMine = dev.ninemmteam.mod.modules.impl.player.SpeedMine.INSTANCE;
            
            java.lang.reflect.Field breakPosField = speedMine.getClass().getDeclaredField("breakPos");
            breakPosField.setAccessible(true);
            breakPosField.set(speedMine, null);
        } catch (Exception ignored) {
        }
        
        dev.ninemmteam.mod.modules.impl.player.SpeedMine.secondPos = null;
    }
    
    private void doSwap(int slot) {
        if (this.inventory.getValue()) {
            if (mc.player != null) {
                InventoryUtil.inventorySwap(slot, mc.player.getInventory().selectedSlot);
            }
        } else {
            InventoryUtil.switchToSlot(slot);
        }
    }
    
    private int findItem(net.minecraft.item.Item item) {
        if (this.inventory.getValue()) {
            return InventoryUtil.findItemInventorySlot(item);
        }
        return InventoryUtil.findItem(item);
    }
    
    private int findClass(Class clazz) {
        if (this.inventory.getValue()) {
            return InventoryUtil.findClassInventorySlot(clazz);
        }
        return InventoryUtil.findClass(clazz);
    }
    
    private boolean placeTrapdoorAgainstWall(BlockPos pos, Direction wallDir) {
         int slot = this.findClass(TrapdoorBlock.class);
         int oldSlot = mc.player.getInventory().selectedSlot;

         if (slot != -1) {
             this.doSwap(slot);

             BlockPos neighbor = pos.offset(wallDir);
             Direction side = wallDir.getOpposite(); 
             
             Vec3d hitVec = neighbor.toCenterPos().add(
                 side.getVector().getX() * 0.5, 
                 0.9, 
                 side.getVector().getZ() * 0.5
             );
             
             clickBlock(neighbor, side, this.rotate.getValue(), this.packet.getValue(), hitVec);

             if (this.inventory.getValue()) {
                 this.doSwap(slot);
                 EntityUtil.syncInventory();
             } else {
                 InventoryUtil.switchToSlot(oldSlot);
             }
             PlaceRender.INSTANCE.create(pos);
             return true;
         }
         return false;
    }

    private boolean placeTrapdoor(BlockPos pos, PlayerEntity target) {
        int slot = this.findClass(TrapdoorBlock.class);
        int oldSlot = mc.player.getInventory().selectedSlot;

        if (slot != -1) {
            this.doSwap(slot);

            placeBlockTop(pos, this.rotate.getValue(), this.packet.getValue(), target);

            if (this.inventory.getValue()) {
                this.doSwap(slot);
                EntityUtil.syncInventory();
            } else {
                InventoryUtil.switchToSlot(oldSlot);
            }
            PlaceRender.INSTANCE.create(pos);
            return true;
        }
        return false;
    }

    private void placeBlockTop(BlockPos pos, boolean rotate, boolean packet, PlayerEntity target) {
        if (this.packet.getValue()) {
            Direction bestSide = null;
            double maxDist = -1.0;

            for (Direction side : Direction.values()) {
                if (side == Direction.UP || side == Direction.DOWN) continue;
                double dist = getDistanceToWall(target, side);
                if (dist > maxDist) {
                    maxDist = dist;
                    bestSide = side;
                }
            }

            Direction side = bestSide != null ? bestSide : BlockUtil.getClickSide(pos);
            if (side == null) return;

            if (this.rotate.getValue()) {
                 fentanyl.ROTATION.lookAt(pos.offset(side), side.getOpposite());
            }

            BlockPos neighbor = pos.offset(side);
            Direction opp = side.getOpposite();
            
            Vec3d hitVec = neighbor.toCenterPos().add(
                opp.getVector().getX() * 0.5,
                0.9, 
                opp.getVector().getZ() * 0.5
            );
            
            clickBlock(neighbor, opp, false, true, hitVec);
            
            if (this.rotate.getValue()) {
                fentanyl.ROTATION.snapBack();
            }
        } else {
               Direction bestSide = null;
               double maxDist = -1.0;
  
               for (Direction side : Direction.values()) {
                   if (side == Direction.UP || side == Direction.DOWN) continue;
                   double dist = getDistanceToWall(target, side);
                   if (dist > maxDist) {
                       maxDist = dist;
                       bestSide = side;
                   }
               }
               
               if (bestSide != null) {
                   BlockPos neighbor = pos.offset(bestSide);
                   if (BlockUtil.canClick(neighbor) && !BlockUtil.canReplace(neighbor)) {
                        Direction side = bestSide.getOpposite();
                        Vec3d hitVec = neighbor.toCenterPos().add(
                            side.getVector().getX() * 0.5, 
                            0.9, 
                            side.getVector().getZ() * 0.5
                        );
                        clickBlock(neighbor, side, rotate, packet, hitVec);
                        return;
                   }
               }
  
            bestSide = null;
            BlockPos bestNeighbor = null;

            if (BlockUtil.canClick(pos.up()) && !BlockUtil.canReplace(pos.up())) {
                 bestSide = Direction.DOWN;
                 bestNeighbor = pos.up();
            }
            
            if (bestSide == null) {
                for (Direction side : Direction.values()) {
                    if (side == Direction.UP || side == Direction.DOWN) continue;
                    BlockPos neighbor = pos.offset(side);
                    if (BlockUtil.canClick(neighbor) && !BlockUtil.canReplace(neighbor)) {
                        bestSide = side.getOpposite();
                        bestNeighbor = neighbor;
                        break;
                    }
                }
            }

            if (bestSide != null) {
                 Vec3d hitVec;
                 if (bestSide == Direction.DOWN) { 
                     hitVec = bestNeighbor.toCenterPos().add(0, -0.5, 0); 
                 } else {
                     hitVec = bestNeighbor.toCenterPos().add(
                         bestSide.getVector().getX() * 0.5, 
                         0.9, 
                         bestSide.getVector().getZ() * 0.5
                     );
                 }
                 
                 clickBlock(bestNeighbor, bestSide, rotate, packet, hitVec);
            }
        }
    }

    private void clickBlock(BlockPos pos, Direction side, boolean rotate, boolean packet, Vec3d hitVec) {
        if (rotate) {
            fentanyl.ROTATION.lookAt(hitVec);
        }

        EntityUtil.swingHand(Hand.MAIN_HAND, (SwingSide)AntiCheat.INSTANCE.interactSwing.getValue());
        BlockHitResult result = new BlockHitResult(hitVec, side, pos, false);
        
        if (packet) {
            Module.sendSequencedPacket(id -> new PlayerInteractBlockC2SPacket(Hand.MAIN_HAND, result, id));
        } else {
            mc.interactionManager.interactBlock(mc.player, Hand.MAIN_HAND, result);
        }

        mc.itemUseCooldown = 4;
        if (rotate) {
            fentanyl.ROTATION.snapBack();
        }
    }
}
