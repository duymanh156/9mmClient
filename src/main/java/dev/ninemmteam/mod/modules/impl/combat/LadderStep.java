package dev.ninemmteam.mod.modules.impl.combat;

import dev.ninemmteam.api.events.eventbus.EventListener;
import dev.ninemmteam.api.events.impl.ClientTickEvent;
import dev.ninemmteam.api.utils.math.Timer;
import dev.ninemmteam.api.utils.player.EntityUtil;
import dev.ninemmteam.api.utils.player.InventoryUtil;
import dev.ninemmteam.api.utils.world.BlockUtil;
import dev.ninemmteam.mod.modules.Module;
import dev.ninemmteam.mod.modules.settings.impl.BooleanSetting;
import dev.ninemmteam.mod.modules.settings.impl.SliderSetting;
import net.minecraft.block.Blocks;
import net.minecraft.network.packet.c2s.play.PlayerInteractBlockC2SPacket;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;

public class LadderStep extends Module {
   public static LadderStep INSTANCE;
   
   private final SliderSetting placeDelay = this.add(new SliderSetting("PlaceDelay", 50, 0, 500));
   private final SliderSetting maxDepth = this.add(new SliderSetting("MaxDepth", 4, 2, 10));
   private final BooleanSetting inventorySwap = this.add(new BooleanSetting("InventorySwap", true));
   private final BooleanSetting autoDisable = this.add(new BooleanSetting("AutoDisable", true));
   
   private final Timer timer = new Timer();
   private boolean placedObsidian = false;
   private BlockPos holePos = null;
   private int holeDepth = 0;
   private int state = 0;
   
   public LadderStep() {
      super("LadderStep", Module.Category.Combat);
      this.setChinese("梯子台阶");
      INSTANCE = this;
   }
   
   @Override
   public void onEnable() {
      super.onEnable();
      this.placedObsidian = false;
      this.holePos = null;
      this.holeDepth = 0;
      this.state = 0;
      this.timer.reset();
   }
   
   @EventListener
   public void onTick(ClientTickEvent event) {
      if (nullCheck() || event.isPost()) return;
      
      if (!this.inventorySwap.getValue() || EntityUtil.inInventory()) {
         if (this.timer.passedMs(this.placeDelay.getValueInt())) {
            this.update();
         }
      }
   }
   
   private void update() {
      if (this.holePos == null) {
         if (!this.detectHole()) {
            this.sendMessage("§4No hole detected.");
            if (this.autoDisable.getValue()) {
               this.disable();
            }
            return;
         }
         this.sendMessage("§aHole detected! Depth: " + this.holeDepth);
      }
      
      if (this.holePos == null) return;
      
      int obsidianSlot = this.findObsidianSlot();
      int ladderSlot = this.findLadderSlot();
      
      if (this.holeDepth > 2 && !this.placedObsidian) {
         if (obsidianSlot == -1) {
            this.sendMessage("§4No obsidian found.");
            this.disable();
            return;
         }
         
         BlockPos fillPos = this.holePos.down(this.holeDepth - 2);
         if (mc.world.isAir(fillPos) || BlockUtil.canReplace(fillPos)) {
            if (this.placeBlock(fillPos, obsidianSlot)) {
               this.timer.reset();
               return;
            }
         }
         
         this.placedObsidian = true;
         this.holeDepth = 2;
         this.timer.reset();
         return;
      }
      
      if (ladderSlot == -1) {
         this.sendMessage("§4No ladder found.");
         this.disable();
         return;
      }
      
      boolean allPlaced = true;
      for (int i = 0; i < this.holeDepth; i++) {
         BlockPos ladderPos = this.holePos.down(i);
         if (mc.world.isAir(ladderPos) || BlockUtil.canReplace(ladderPos)) {
            Direction ladderFacing = this.findLadderFacing(ladderPos);
            if (ladderFacing != null) {
               allPlaced = false;
               if (this.placeLadder(ladderPos, ladderSlot, ladderFacing)) {
                  this.timer.reset();
                  return;
               }
            }
         }
      }
      
      if (allPlaced) {
         this.sendMessage("§aLadderStep completed!");
         if (this.autoDisable.getValue()) {
            this.disable();
         }
      }
   }
   
   private boolean detectHole() {
      BlockPos playerPos = mc.player.getBlockPos();
      
      int depth = 0;
      for (int y = 1; y <= this.maxDepth.getValueInt(); y++) {
         BlockPos checkPos = playerPos.down(y);
         if (mc.world.isAir(checkPos) || BlockUtil.canReplace(checkPos)) {
            depth++;
         } else {
            break;
         }
      }
      
      if (depth >= 1) {
         BlockPos bottom = playerPos.down(depth);
         BlockPos belowBottom = bottom.down();
         
         if (!mc.world.isAir(belowBottom) && !BlockUtil.canReplace(belowBottom)) {
            this.holePos = playerPos.down(1);
            this.holeDepth = depth;
            return true;
         }
      }
      
      return false;
   }
   
   private Direction findLadderFacing(BlockPos pos) {
      Direction[] directions = new Direction[]{Direction.NORTH, Direction.SOUTH, Direction.EAST, Direction.WEST};
      
      for (Direction dir : directions) {
         BlockPos neighbor = pos.offset(dir);
         if (!mc.world.isAir(neighbor) && !BlockUtil.canReplace(neighbor)) {
            return dir.getOpposite();
         }
      }
      
      return null;
   }
   
   private boolean placeBlock(BlockPos pos, int slot) {
      if (slot == -1) return false;
      
      int oldSlot = mc.player.getInventory().selectedSlot;
      this.doSwap(slot);
      
      Direction side = BlockUtil.getPlaceSide(pos);
      if (side == null) {
         if (!BlockUtil.allowAirPlace()) {
            return false;
         }
         side = Direction.DOWN;
      }
      
      BlockPos clickPos = side == Direction.DOWN ? pos : pos.offset(side);
      Vec3d hitVec = new Vec3d(
         clickPos.getX() + 0.5 + side.getOpposite().getVector().getX() * 0.5,
         clickPos.getY() + 0.5 + side.getOpposite().getVector().getY() * 0.5,
         clickPos.getZ() + 0.5 + side.getOpposite().getVector().getZ() * 0.5
      );
      
      BlockHitResult result = new BlockHitResult(hitVec, side.getOpposite(), clickPos, false);
      Module.sendSequencedPacket(id -> new PlayerInteractBlockC2SPacket(Hand.MAIN_HAND, result, id));
      mc.player.swingHand(Hand.MAIN_HAND);
      
      if (this.inventorySwap.getValue()) {
         this.doSwap(slot);
         EntityUtil.syncInventory();
      } else {
         this.doSwap(oldSlot);
      }
      
      return true;
   }
   
   private boolean placeLadder(BlockPos pos, int slot, Direction facing) {
      if (slot == -1) return false;
      
      int oldSlot = mc.player.getInventory().selectedSlot;
      this.doSwap(slot);
      
      BlockPos placeAgainst = pos.offset(facing.getOpposite());
      Vec3d hitVec = new Vec3d(
         placeAgainst.getX() + 0.5 + facing.getVector().getX() * 0.5,
         placeAgainst.getY() + 0.5,
         placeAgainst.getZ() + 0.5 + facing.getVector().getZ() * 0.5
      );
      
      BlockHitResult result = new BlockHitResult(hitVec, facing, placeAgainst, false);
      Module.sendSequencedPacket(id -> new PlayerInteractBlockC2SPacket(Hand.MAIN_HAND, result, id));
      mc.player.swingHand(Hand.MAIN_HAND);
      
      if (this.inventorySwap.getValue()) {
         this.doSwap(slot);
         EntityUtil.syncInventory();
      } else {
         this.doSwap(oldSlot);
      }
      
      return true;
   }
   
   private void doSwap(int slot) {
      if (this.inventorySwap.getValue()) {
         InventoryUtil.inventorySwap(slot, mc.player.getInventory().selectedSlot);
      } else {
         InventoryUtil.switchToSlot(slot);
      }
   }
   
   private int findObsidianSlot() {
      if (this.inventorySwap.getValue()) {
         int cryingObsidianSlot = InventoryUtil.findBlockInventorySlot(Blocks.CRYING_OBSIDIAN);
         if (cryingObsidianSlot != -1) return cryingObsidianSlot;
         
         int obsidianSlot = InventoryUtil.findBlockInventorySlot(Blocks.OBSIDIAN);
         if (obsidianSlot != -1) return obsidianSlot;
         
         return -1;
      } else {
         int cryingObsidian = InventoryUtil.findBlock(Blocks.CRYING_OBSIDIAN);
         if (cryingObsidian != -1) return cryingObsidian;
         
         int obsidian = InventoryUtil.findBlock(Blocks.OBSIDIAN);
         if (obsidian != -1) return obsidian;
         
         return -1;
      }
   }
   
   private int findLadderSlot() {
      if (this.inventorySwap.getValue()) {
         return InventoryUtil.findBlockInventorySlot(Blocks.LADDER);
      } else {
         return InventoryUtil.findBlock(Blocks.LADDER);
      }
   }
}
