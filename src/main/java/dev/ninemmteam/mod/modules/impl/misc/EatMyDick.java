package dev.ninemmteam.mod.modules.impl.misc;

import dev.ninemmteam.fentanyl;
import dev.ninemmteam.api.events.eventbus.EventListener;
import dev.ninemmteam.api.events.impl.UpdateEvent;
import dev.ninemmteam.api.utils.math.Timer;
import dev.ninemmteam.api.utils.player.InventoryUtil;
import dev.ninemmteam.api.utils.world.BlockUtil;
import dev.ninemmteam.mod.modules.Module;
import dev.ninemmteam.mod.modules.settings.impl.BooleanSetting;
import dev.ninemmteam.mod.modules.settings.impl.SliderSetting;
import net.minecraft.block.Blocks;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;

public class EatMyDick extends Module {
    public static EatMyDick INSTANCE;
    private final boolean rotate = true;
    private final boolean breakCrystal = true;
    private final double placeRange = 4.0;
    private final Timer placeTimer = new Timer();
    private final Timer breakTimer = new Timer();
    private final SliderSetting placeDelay = this.add(new SliderSetting("PlaceDelay", 50, 0, 500).setSuffix("ms"));
    private final BooleanSetting autoDisable = this.add(new BooleanSetting("AutoDisable", true));
    private int state = 0;
    private BlockPos targetPos = null;
    private Direction facing = null;

    public EatMyDick() {
        super("EatMyDick", Category.Misc);
        this.setChinese("吃我的迪克");
        INSTANCE = this;
    }

    @Override
    public void onEnable() {
        super.onEnable();
        this.state = 0;
        this.targetPos = null;
        this.facing = null;
        this.placeTimer.reset();
    }

    @EventListener
    public void onUpdate(UpdateEvent event) {
        if (mc.player != null && mc.world != null) {
            // 初始化目标位置和朝向
            if (this.targetPos == null || this.facing == null) {
                this.facing = mc.player.getHorizontalFacing();
                this.targetPos = mc.player.getBlockPos().offset(this.facing, 2);
            }
            
            switch (this.state) {
                case 0:
                    if (this.placeTimer.passedMs((long) this.placeDelay.getValue())) {
                        this.state = 1;
                    }
                    break;
                    
                case 1:
                    if (BlockUtil.canPlace(this.targetPos, this.placeRange, this.breakCrystal)) {
                        this.placeBlock(this.targetPos);
                    }
                    this.placeTimer.reset();
                    this.state = 2;
                    break;
                    
                case 2:
                    if (this.placeTimer.passedMs((long) this.placeDelay.getValue())) {
                        BlockPos leftPos = getLeftPos(this.targetPos, this.facing);
                        if (BlockUtil.canPlace(leftPos, this.placeRange, this.breakCrystal)) {
                            this.placeBlock(leftPos);
                        }
                        this.placeTimer.reset();
                        this.state = 3;
                    }
                    break;
                    
                case 3:
                    if (this.placeTimer.passedMs((long) this.placeDelay.getValue())) {
                        BlockPos rightPos = getRightPos(this.targetPos, this.facing);
                        if (BlockUtil.canPlace(rightPos, this.placeRange, this.breakCrystal)) {
                            this.placeBlock(rightPos);
                        }
                        this.placeTimer.reset();
                        this.state = 4;
                    }
                    break;
                    
                case 4:
                    if (this.placeTimer.passedMs((long) this.placeDelay.getValue())) {
                        BlockPos upPos1 = this.targetPos.up(1);
                        if (BlockUtil.canPlace(upPos1, this.placeRange, this.breakCrystal)) {
                            this.placeBlock(upPos1);
                        }
                        this.placeTimer.reset();
                        this.state = 5;
                    }
                    break;
                    
                case 5:
                    if (this.placeTimer.passedMs((long) this.placeDelay.getValue())) {
                        BlockPos upPos2 = this.targetPos.up(2);
                        this.placeBlock(upPos2);
                        this.placeTimer.reset();
                        this.state = 6;
                    }
                    break;
                    
                case 6:
                    if (this.placeTimer.passedMs((long) this.placeDelay.getValue())) {
                        // 跳跃
                        mc.player.jump();
                        this.placeTimer.reset();
                        this.state = 8;
                    }
                    break;
                    
                case 8:
                    if (this.placeTimer.passedMs((long) this.placeDelay.getValue() / 2)) {
                        BlockPos upPos3 = this.targetPos.up(3);
                        this.placeBlock(upPos3);
                        if (mc.interactionManager != null && mc.player != null) {
                            int old = mc.player.getInventory().selectedSlot;
                            int block = this.getBlock();
                            if (block != -1) {
                                this.doSwap(block);
                                Direction side = Direction.DOWN;
                                BlockPos placeOn = upPos3.down();
                                Vec3d hitPos = placeOn.toCenterPos().add(0.5, 0.5, 0.5);
                                BlockHitResult result = new BlockHitResult(hitPos, side, placeOn, false);
                                mc.interactionManager.interactBlock(mc.player, Hand.MAIN_HAND, result);
                                this.doSwap(old);
                            }
                        }
                        this.placeTimer.reset();
                        this.state = 7;
                    }
                    break;
                    
                case 7:
                    if (mc.interactionManager != null) {
                        if (mc.world.getBlockState(this.targetPos).getBlock() == Blocks.OBSIDIAN || 
                            mc.world.getBlockState(this.targetPos).getBlock() == Blocks.CRYING_OBSIDIAN) {
                            Vec3d blockCenter = this.targetPos.toCenterPos();
                            if (this.rotate) {
                                fentanyl.ROTATION.lookAt(blockCenter);
                            }
                            Direction side = Direction.UP;
                            mc.interactionManager.updateBlockBreakingProgress(this.targetPos, side);

                            if (this.rotate) {
                                fentanyl.ROTATION.snapBack();
                            }

                            if (mc.world.isAir(this.targetPos) && this.autoDisable.getValue()) {
                                this.disable();
                            }
                        } else if (mc.world.isAir(this.targetPos) && this.autoDisable.getValue()) {
                            this.disable();
                        }
                    }
                    break;
            }
        }
    }

    private void placeBlock(BlockPos pos) {
        if (pos != null) {
            int old = mc.player.getInventory().selectedSlot;
            int block = this.getBlock();
            if (block != -1) {
                BlockUtil.placedPos.add(pos);
                this.doSwap(block);
                BlockUtil.placeBlock(pos, rotate);
                this.doSwap(old);
            }
        }
    }

    private void doSwap(int slot) {
        InventoryUtil.switchToSlot(slot);
    }

    private int getBlock() {
        int cryingObsidian = InventoryUtil.findBlock(Blocks.CRYING_OBSIDIAN);
        if (cryingObsidian != -1) {
            return cryingObsidian;
        }
        
        return InventoryUtil.findBlock(Blocks.OBSIDIAN);
    }

    private BlockPos getLeftPos(BlockPos pos, Direction facing) {
        switch (facing) {
            case NORTH:
                return pos.west();
            case SOUTH:
                return pos.east();
            case EAST:
                return pos.north();
            case WEST:
                return pos.south();
            default:
                return pos;
        }
    }

    private BlockPos getRightPos(BlockPos pos, Direction facing) {
        switch (facing) {
            case NORTH:
                return pos.east();
            case SOUTH:
                return pos.west();
            case EAST:
                return pos.south();
            case WEST:
                return pos.north();
            default:
                return pos;
        }
    }
}
