package dev.ninemmteam.mod.modules.impl.combat;

import dev.ninemmteam.api.events.eventbus.EventListener;
import dev.ninemmteam.api.events.impl.ClientTickEvent;
import dev.ninemmteam.api.utils.math.Timer;
import dev.ninemmteam.api.utils.player.InventoryUtil;
import dev.ninemmteam.api.utils.world.BlockUtil;
import dev.ninemmteam.mod.modules.Module;
import dev.ninemmteam.mod.modules.settings.impl.BooleanSetting;
import dev.ninemmteam.mod.modules.settings.impl.SliderSetting;
import java.util.HashSet;
import java.util.Set;
import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.minecraft.block.ShulkerBoxBlock;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.ShulkerBoxBlockEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.network.packet.c2s.play.PlayerInteractBlockC2SPacket;
import net.minecraft.network.packet.c2s.play.UpdateSelectedSlotC2SPacket;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;

public class AntiShulker extends Module {
    public static AntiShulker INSTANCE;
    
    private final SliderSetting range = this.add(new SliderSetting("Range", 5.0, 1.0, 8.0, 0.1));
    private final SliderSetting placeDelay = this.add(new SliderSetting("PlaceDelay", 50, 0, 500, 10));
    private final BooleanSetting inventorySwap = this.add(new BooleanSetting("InventorySwap", true));
    private final BooleanSetting friends = this.add(new BooleanSetting("Friends", false));
    private final BooleanSetting self = this.add(new BooleanSetting("Self", false));
    private final BooleanSetting notify = this.add(new BooleanSetting("Notify", true));
    
    private final Timer placeTimer = new Timer();
    private final Set<BlockPos> blockedShulkers = new HashSet<>();
    
    public AntiShulker() {
        super("AntiShulker", Module.Category.Combat);
        this.setChinese("反潜影盒");
        INSTANCE = this;
    }
    
    @Override
    public void onEnable() {
        super.onEnable();
        blockedShulkers.clear();
    }
    
    @Override
    public void onDisable() {
        super.onDisable();
        blockedShulkers.clear();
    }
    
    @EventListener
    public void onTick(ClientTickEvent event) {
        if (nullCheck() || event.isPost()) return;
        
        blockedShulkers.removeIf(pos -> {
            Block block = mc.world.getBlockState(pos).getBlock();
            return !(block instanceof ShulkerBoxBlock);
        });
        
        for (BlockEntity blockEntity : BlockUtil.getTileEntities()) {
            if (!(blockEntity instanceof ShulkerBoxBlockEntity shulkerBox)) continue;
            
            BlockPos shulkerPos = shulkerBox.getPos();
            
            if (blockedShulkers.contains(shulkerPos)) continue;
            
            Block block = mc.world.getBlockState(shulkerPos).getBlock();
            if (!(block instanceof ShulkerBoxBlock)) continue;
            
            double distance = mc.player.squaredDistanceTo(shulkerPos.toCenterPos());
            double maxRange = range.getValue();
            if (distance > maxRange * maxRange) continue;
            
            if (!placeTimer.passedMs(placeDelay.getValueInt())) continue;
            
            Direction facing = getShulkerFacing(shulkerBox);
            BlockPos targetPos = shulkerPos.offset(facing);
            
            if (isAirOrReplaceable(targetPos)) {
                if (placeObsidian(targetPos)) {
                    blockedShulkers.add(shulkerPos);
                    placeTimer.reset();
                    if (notify.getValue()) {
                        this.sendMessage("§aBlocked shulker at " + shulkerPos.toShortString());
                    }
                    return;
                }
            }
        }
    }
    
    private Direction getShulkerFacing(ShulkerBoxBlockEntity shulkerBox) {
        try {
            var blockState = shulkerBox.getCachedState();
            if (blockState.contains(ShulkerBoxBlock.FACING)) {
                Direction facing = blockState.get(ShulkerBoxBlock.FACING);
                if (facing != null) return facing;
            }
        } catch (Exception ignored) {}
        return Direction.UP;
    }
    
    private boolean isAirOrReplaceable(BlockPos pos) {
        if (pos == null) return false;
        return mc.world.isAir(pos) || mc.world.getBlockState(pos).isReplaceable();
    }
    
    private boolean placeObsidian(BlockPos pos) {
        int obsidianSlot = findObsidianSlot();
        if (obsidianSlot == -1) {
            return false;
        }
        
        int oldSlot = mc.player.getInventory().selectedSlot;
        int hotbarSlot = obsidianSlot;
        
        if (inventorySwap.getValue()) {
            if (obsidianSlot >= 36) {
                mc.interactionManager.clickSlot(
                    mc.player.currentScreenHandler.syncId,
                    obsidianSlot,
                    oldSlot,
                    SlotActionType.SWAP,
                    mc.player
                );
                hotbarSlot = oldSlot;
            }
        } else {
            hotbarSlot = obsidianSlot;
            mc.player.getInventory().selectedSlot = hotbarSlot;
            mc.getNetworkHandler().sendPacket(new UpdateSelectedSlotC2SPacket(hotbarSlot));
        }
        
        if (!inventorySwap.getValue()) {
            mc.player.getInventory().selectedSlot = hotbarSlot;
            mc.getNetworkHandler().sendPacket(new UpdateSelectedSlotC2SPacket(hotbarSlot));
        }
        
        boolean success = false;
        
        Direction placeSide = BlockUtil.getPlaceSide(pos, range.getValue());
        if (placeSide != null) {
            BlockPos clickPos = pos.offset(placeSide);
            Vec3d hitVec = clickPos.toCenterPos().add(
                placeSide.getOpposite().getVector().getX() * 0.5,
                placeSide.getOpposite().getVector().getY() * 0.5,
                placeSide.getOpposite().getVector().getZ() * 0.5
            );
            
            BlockHitResult hitResult = new BlockHitResult(hitVec, placeSide.getOpposite(), clickPos, false);
            Module.sendSequencedPacket(id -> new PlayerInteractBlockC2SPacket(Hand.MAIN_HAND, hitResult, id));
            mc.player.swingHand(Hand.MAIN_HAND);
            success = true;
        } else {
            Direction clickSide = getClickSide(pos);
            Vec3d hitVec = pos.toCenterPos().add(
                clickSide.getVector().getX() * 0.5,
                clickSide.getVector().getY() * 0.5,
                clickSide.getVector().getZ() * 0.5
            );
            
            BlockHitResult hitResult = new BlockHitResult(hitVec, clickSide, pos, false);
            Module.sendSequencedPacket(id -> new PlayerInteractBlockC2SPacket(Hand.MAIN_HAND, hitResult, id));
            mc.player.swingHand(Hand.MAIN_HAND);
            success = true;
        }
        
        if (inventorySwap.getValue() && obsidianSlot >= 36) {
            mc.interactionManager.clickSlot(
                mc.player.currentScreenHandler.syncId,
                obsidianSlot,
                oldSlot,
                SlotActionType.SWAP,
                mc.player
            );
        } else {
            mc.player.getInventory().selectedSlot = oldSlot;
            mc.getNetworkHandler().sendPacket(new UpdateSelectedSlotC2SPacket(oldSlot));
        }
        
        return success;
    }
    
    private Direction getClickSide(BlockPos pos) {
        Vec3d eyePos = mc.player.getEyePos();
        Vec3d blockCenter = pos.toCenterPos();
        
        double dx = eyePos.x - blockCenter.x;
        double dy = eyePos.y - blockCenter.y;
        double dz = eyePos.z - blockCenter.z;
        
        double absDx = Math.abs(dx);
        double absDy = Math.abs(dy);
        double absDz = Math.abs(dz);
        
        if (absDy > absDx && absDy > absDz) {
            return dy > 0 ? Direction.UP : Direction.DOWN;
        } else if (absDx > absDz) {
            return dx > 0 ? Direction.EAST : Direction.WEST;
        } else {
            return dz > 0 ? Direction.SOUTH : Direction.NORTH;
        }
    }
    
    private int findObsidianSlot() {
        if (inventorySwap.getValue()) {
            for (int i = 0; i < 36; i++) {
                ItemStack stack = mc.player.getInventory().getStack(i);
                if (stack.getItem() == Items.OBSIDIAN) {
                    return i < 9 ? i + 36 : i;
                }
            }
            for (int i = 0; i < 36; i++) {
                ItemStack stack = mc.player.getInventory().getStack(i);
                if (stack.getItem() == Items.CRYING_OBSIDIAN) {
                    return i < 9 ? i + 36 : i;
                }
            }
            return -1;
        } else {
            for (int i = 0; i < 9; i++) {
                ItemStack stack = mc.player.getInventory().getStack(i);
                if (stack.getItem() == Items.OBSIDIAN) {
                    return i;
                }
            }
            for (int i = 0; i < 9; i++) {
                ItemStack stack = mc.player.getInventory().getStack(i);
                if (stack.getItem() == Items.CRYING_OBSIDIAN) {
                    return i;
                }
            }
            return -1;
        }
    }
    
    @Override
    public String getInfo() {
        return null;
    }
}
