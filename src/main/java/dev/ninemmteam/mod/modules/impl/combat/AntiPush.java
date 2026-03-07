package dev.ninemmteam.mod.modules.impl.combat;

import dev.ninemmteam.fentanyl;
import dev.ninemmteam.api.events.eventbus.EventListener;
import dev.ninemmteam.api.events.impl.UpdateEvent;
import dev.ninemmteam.api.utils.player.EntityUtil;
import dev.ninemmteam.api.utils.player.InventoryUtil;
import dev.ninemmteam.api.utils.world.BlockUtil;
import dev.ninemmteam.core.impl.CommandManager;
import dev.ninemmteam.mod.modules.Module;
import dev.ninemmteam.mod.modules.impl.client.AntiCheat;
import dev.ninemmteam.mod.modules.settings.impl.BooleanSetting;
import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.minecraft.block.PistonBlock;
import net.minecraft.block.SlabBlock;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket;
import net.minecraft.util.Hand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;

public class AntiPush extends Module {
    public static AntiPush INSTANCE;
    public final BooleanSetting rotate = this.add(new BooleanSetting("Rotate", true));
    public final BooleanSetting packet = this.add(new BooleanSetting("Packet", true));
    public final BooleanSetting helper = this.add(new BooleanSetting("Helper", true));
    public final BooleanSetting trap = this.add(new BooleanSetting("Trap", true).setParent());
    private final BooleanSetting usingPause = this.add(new BooleanSetting("UsingPause", true));
    private final BooleanSetting onlyBurrow = this.add(new BooleanSetting("OnlyBurrow", true, () -> this.trap.isOpen()).setParent());
    private final BooleanSetting whenDouble = this.add(new BooleanSetting("WhenDouble", true, () -> this.onlyBurrow.isOpen()));
    private final BooleanSetting inventory = this.add(new BooleanSetting("InventorySwap", true));
    private final BooleanSetting moveUp = this.add(new BooleanSetting("MoveUp", true));
    private final BooleanSetting placeBlock = this.add(new BooleanSetting("PlaceBlock", true));
    private final BooleanSetting lag = this.add(new BooleanSetting("Lag", true));
    private final BooleanSetting debug = this.add(new BooleanSetting("Debug", true));
    private boolean canLag = true;
    private boolean canMove = true;

    public AntiPush() {
        super("AntiPush", Category.Combat);
        this.setChinese("防活塞推");
        INSTANCE = this;
    }

    public static boolean canBlockFacing(BlockPos pos) {
        boolean airCheck = false;
        for (Direction side : Direction.values()) {
            if (!canClick(pos.offset(side))) continue;
            airCheck = true;
        }
        return airCheck;
    }

    public static boolean canClick(BlockPos pos) {
        if (AntiCheat.INSTANCE.multiPlace.getValue() && BlockUtil.placedPos.contains(pos)) {
            return true;
        }
        if (mc.world.getBlockState(pos).getBlock() == Blocks.COBWEB && AutoWeb.ignore) {
            return AutoCrystal.INSTANCE.airPlace.getValue();
        }
        return mc.world.getBlockState(pos).isAir() && (!BlockUtil.isClickable(getBlock(pos)) && !(getBlock(pos) instanceof SlabBlock) || mc.player.isSneaking());
    }

    public static boolean canReplace(BlockPos pos) {
        if (pos.getY() >= 320) {
            return false;
        }
        if (AntiCheat.INSTANCE.multiPlace.getValue() && BlockUtil.placedPos.contains(pos)) {
            return true;
        }
        if (mc.world.getBlockState(pos).getBlock() == Blocks.COBWEB && AutoWeb.ignore && AutoCrystal.INSTANCE.replace.getValue()) {
            return true;
        }
        return mc.world.getBlockState(pos).isReplaceable();
    }

    public static boolean canPlace(BlockPos pos) {
        if (!canBlockFacing(pos)) {
            return false;
        }
        if (!canReplace(pos)) {
            return false;
        }
        return !BlockUtil.hasCrystal(pos);
    }

    private static Block getBlock(BlockPos block) {
        return mc.world.getBlockState(block).getBlock();
    }

    public static Direction getPlaceSide(BlockPos pos) {
        return BlockUtil.getPlaceSide(pos);
    }

    public static void placeBlock(BlockPos pos, boolean rotate, boolean packet) {
        if (BlockUtil.allowAirPlace()) {
            BlockUtil.placedPos.add(pos);
            BlockUtil.clickBlock(pos, Direction.DOWN, rotate, Hand.MAIN_HAND, packet);
            return;
        }
        Direction side = getPlaceSide(pos);
        if (side == null) {
            return;
        }
        BlockUtil.placedPos.add(pos);
        BlockUtil.clickBlock(pos.offset(side), side.getOpposite(), rotate, Hand.MAIN_HAND, packet);
    }

    @EventListener
    public void onUpdate(UpdateEvent event) {
        if (this.usingPause.getValue() && mc.player.isUsingItem()) {
            return;
        }
        
        BlockPos pos = EntityUtil.getPlayerPos(true);
        boolean hasActivePiston = false;
        
        for (Direction i : Direction.values()) {
            if (i == Direction.DOWN || i == Direction.UP) continue;
            if (getBlock(pos.offset(i).down()) instanceof PistonBlock && 
                mc.world.getBlockState(pos.offset(i).down()).get(PistonBlock.FACING) == i) {
                hasActivePiston = true;
                break;
            }
        }
        
        if (!hasActivePiston && !fentanyl.PLAYER.insideBlock) {
            return;
        }
        
        this.block();
    }

    private void block() {
        BlockPos pos = EntityUtil.getPlayerPos(true);
        if (getBlock(pos.up(2)) == Blocks.OBSIDIAN || getBlock(pos.up(2)) == Blocks.CRYING_OBSIDIAN) {
            return;
        }
        if (this.lag.getValue()) {
            for (Direction i : Direction.values()) {
                if (i == Direction.DOWN || i == Direction.UP || !(getBlock(pos.offset(i).down()) instanceof PistonBlock) || mc.world.getBlockState(pos.offset(i).down()).get(PistonBlock.FACING) != i) {
                    this.canLag = true;
                    continue;
                }
                if (this.canLag) {
                    mc.getNetworkHandler().sendPacket(new PlayerMoveC2SPacket.PositionAndOnGround(mc.player.getX(), 1337.0, mc.player.getZ(), false));
                    this.canLag = false;
                }
                if (this.debug.getValue() && this.canLag) {
                    CommandManager.sendMessage("Checked pushed : can lag");
                    continue;
                }
                if (!this.debug.getValue() || this.canLag) continue;
                CommandManager.sendMessage("Checked pushed : can not lag");
            }
        }
        if (this.moveUp.getValue()) {
            for (Direction i : Direction.values()) {
                if (i == Direction.DOWN || i == Direction.UP || !(getBlock(pos.offset(i).down()) instanceof PistonBlock) || mc.world.getBlockState(pos.offset(i).down()).get(PistonBlock.FACING) != i) {
                    if (fentanyl.PLAYER.isInWeb(mc.player)) continue;
                    this.canMove = true;
                    continue;
                }
                if (this.canMove) {
                    mc.getNetworkHandler().sendPacket(new PlayerMoveC2SPacket.PositionAndOnGround(mc.player.getX(), mc.player.getY() + 0.4199999868869781, mc.player.getZ(), false));
                    mc.getNetworkHandler().sendPacket(new PlayerMoveC2SPacket.PositionAndOnGround(mc.player.getX(), mc.player.getY() + 0.7531999805212017, mc.player.getZ(), false));
                    mc.player.setPosition(mc.player.getX(), mc.player.getY() + 1.0, mc.player.getZ());
                    mc.getNetworkHandler().sendPacket(new PlayerMoveC2SPacket.PositionAndOnGround(mc.player.getX(), mc.player.getY(), mc.player.getZ(), true));
                    this.canMove = false;
                }
                if (this.debug.getValue() && this.canMove) {
                    CommandManager.sendMessage("Checked pushed : can move");
                    continue;
                }
                if (!this.debug.getValue() || this.canMove) continue;
                CommandManager.sendMessage("Checked pushed : can not move");
            }
        }
        if (!this.placeBlock.getValue()) {
            return;
        }
        int progress = 0;
        if (this.whenDouble.getValue()) {
            for (Direction i : Direction.values()) {
                if (i == Direction.DOWN || i == Direction.UP || !(getBlock(pos.offset(i).down()) instanceof PistonBlock) || mc.world.getBlockState(pos.offset(i).down()).get(PistonBlock.FACING) != i) continue;
                ++progress;
            }
        }
        for (Direction i : Direction.values()) {
            if (i == Direction.DOWN || i == Direction.UP || !(getBlock(pos.offset(i).down()) instanceof PistonBlock) || mc.world.getBlockState(pos.offset(i).down()).get(PistonBlock.FACING) != i) continue;
            this.placeBlock(pos.down().offset(i, -1));
            if (this.trap.getValue() && (getBlock(pos) != Blocks.OBSIDIAN || !this.onlyBurrow.getValue() || progress >= 2)) {
                this.placeBlock(pos.up(2));
                if (!canPlace(pos.up(2))) {
                    for (Direction i2 : Direction.values()) {
                        if (!canPlace(pos.offset(i2).up(2))) continue;
                        this.placeBlock(pos.offset(i2).up(2));
                        break;
                    }
                }
            }
            if (canPlace(pos.down().offset(i, -1)) || !this.helper.getValue()) continue;
            if (canPlace(pos.offset(i, -1))) {
                this.placeBlock(pos.offset(i, -1));
                continue;
            }
            this.placeBlock(pos.offset(i, -1).down());
        }
    }

    private void placeBlock(BlockPos pos) {
        if (!canPlace(pos)) {
            return;
        }
        int old = mc.player.getInventory().selectedSlot;
        int block = this.findBlock(Blocks.OBSIDIAN);
        if (block == -1) {
            return;
        }
        this.doSwap(block);
        placeBlock(pos, this.rotate.getValue(), this.packet.getValue());
        if (this.inventory.getValue()) {
            this.doSwap(block);
            EntityUtil.syncInventory();
        } else {
            this.doSwap(old);
        }
    }

    public int findBlock(Block blockIn) {
        if (this.inventory.getValue()) {
            return InventoryUtil.findBlockInventorySlot(blockIn);
        }
        return InventoryUtil.findBlock(blockIn);
    }

    private void doSwap(int slot) {
        if (this.inventory.getValue()) {
            InventoryUtil.inventorySwap(slot, mc.player.getInventory().selectedSlot);
        } else {
            InventoryUtil.switchToSlot(slot);
        }
    }
}
