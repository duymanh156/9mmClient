package dev.ninemmteam.mod.modules.impl.combat;

import dev.ninemmteam.fentanyl;
import dev.ninemmteam.api.events.eventbus.EventListener;
import dev.ninemmteam.api.events.impl.ClientTickEvent;
import dev.ninemmteam.api.utils.combat.CombatUtil;
import dev.ninemmteam.api.utils.math.Timer;
import dev.ninemmteam.api.utils.player.EntityUtil;
import dev.ninemmteam.api.utils.player.InventoryUtil;
import dev.ninemmteam.api.utils.world.BlockUtil;
import dev.ninemmteam.mod.modules.Module;
import dev.ninemmteam.mod.modules.impl.client.AntiCheat;
import dev.ninemmteam.mod.modules.settings.enums.SwingSide;
import dev.ninemmteam.mod.modules.settings.impl.BooleanSetting;
import dev.ninemmteam.mod.modules.settings.impl.SliderSetting;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;
import net.minecraft.block.AbstractPressurePlateBlock;
import net.minecraft.block.Block;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.network.packet.c2s.play.PlayerInteractBlockC2SPacket;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;

public class AutoPlate extends Module {
   public static AutoPlate INSTANCE;
   public static boolean ignore = false;
   public final SliderSetting placeDelay = this.add(new SliderSetting("PlaceDelay", 50, 0, 500));
   public final SliderSetting blocksPer = this.add(new SliderSetting("BlocksPer", 4, 1, 10));
   public final SliderSetting placeRange = this.add(new SliderSetting("PlaceRange", 5.0, 0.0, 6.0, 0.1));
   public final SliderSetting targetRange = this.add(new SliderSetting("TargetRange", 8.0, 0.0, 12.0, 0.1));
   public final BooleanSetting rotate = this.add(new BooleanSetting("Rotate", false));
   public final BooleanSetting inventorySwap = this.add(new BooleanSetting("InventorySwap", true));
   public final BooleanSetting usingPause = this.add(new BooleanSetting("UsingPause", true));
   private final Timer timer = new Timer();
   private final Set<BlockPos> placedPositions = new HashSet<>();
   private int progress = 0;

   public AutoPlate() {
      super("AutoPlate", Module.Category.Combat);
      this.setChinese("自动压力板");
      INSTANCE = this;
   }

   @Override
   public String getInfo() {
      return this.placedPositions.isEmpty() ? null : "Working";
   }

   @EventListener
   public void onTick(ClientTickEvent event) {
      if (!nullCheck() && !event.isPost()) {
         ignore = true;
         this.update();
         ignore = false;
      }
   }

   @Override
   public void onDisable() {
      this.placedPositions.clear();
      this.progress = 0;
      ignore = false;
   }

   private void update() {
      if (this.timer.passed(this.placeDelay.getValueInt())) {
         if (!this.inventorySwap.getValue() || EntityUtil.inInventory()) {
            this.placedPositions.clear();
            this.progress = 0;
            
            int plateSlot = this.getPlateSlot();
            if (plateSlot != -1) {
               if (!this.usingPause.getValue() || !mc.player.isUsingItem()) {
                  for (PlayerEntity player : CombatUtil.getEnemies(this.targetRange.getValue())) {
                     if (this.progress >= this.blocksPer.getValueInt()) {
                        break;
                     }
                     
                     Set<BlockPos> feetPositions = this.getFeetPositions(player);
                     for (BlockPos pos : feetPositions) {
                        if (this.progress >= this.blocksPer.getValueInt()) {
                           break;
                        }
                        if (this.placePlate(pos, plateSlot)) {
                           this.progress++;
                        }
                     }
                  }
               }
            }
         }
      }
   }

   private Set<BlockPos> getFeetPositions(PlayerEntity player) {
      Set<BlockPos> positions = new HashSet<>();
      Box box = player.getBoundingBox();
      
      int minX = (int) Math.floor(box.minX);
      int maxX = (int) Math.floor(box.maxX);
      int minZ = (int) Math.floor(box.minZ);
      int maxZ = (int) Math.floor(box.maxZ);
      int y = player.getBlockPos().getY();
      
      for (int x = minX; x <= maxX; x++) {
         for (int z = minZ; z <= maxZ; z++) {
            positions.add(new BlockPos(x, y, z));
         }
      }
      
      return positions;
   }

   private boolean placePlate(BlockPos pos, int plateSlot) {
      if (this.placedPositions.contains(pos)) {
         return false;
      } else {
         this.placedPositions.add(pos);
         if (this.progress >= this.blocksPer.getValueInt()) {
            return false;
         } else if (plateSlot == -1) {
            return false;
         } else {
            Block blockAtPos = BlockUtil.getBlock(pos);
            if (blockAtPos instanceof AbstractPressurePlateBlock) {
               return false;
            } else if (BlockUtil.getPlaceSide(pos, this.placeRange.getValue()) != null
               && (mc.world.isAir(pos) || ignore && blockAtPos instanceof AbstractPressurePlateBlock)
               && pos.getY() < 320) {
               
               int oldSlot = mc.player.getInventory().selectedSlot;
               if (!this.placeBlock(pos, this.rotate.getValue(), plateSlot)) {
                  return false;
               } else {
                  BlockUtil.placedPos.add(pos);
                  this.progress++;
                  if (this.inventorySwap.getValue()) {
                     this.doSwap(plateSlot);
                     EntityUtil.syncInventory();
                  } else {
                     this.doSwap(oldSlot);
                  }
                  
                  this.timer.reset();
                  return true;
               }
            } else {
               return false;
            }
         }
      }
   }

   public boolean placeBlock(BlockPos pos, boolean rotate, int slot) {
      Direction side = BlockUtil.getPlaceSide(pos);
      if (side == null) {
         return BlockUtil.allowAirPlace() ? this.clickBlock(pos, Direction.DOWN, rotate, slot) : false;
      } else {
         return this.clickBlock(pos.offset(side), side.getOpposite(), rotate, slot);
      }
   }

   public boolean clickBlock(BlockPos pos, Direction side, boolean rotate, int slot) {
      Vec3d directionVec = new Vec3d(
         pos.getX() + 0.5 + side.getVector().getX() * 0.5,
         pos.getY() + 0.5 + side.getVector().getY() * 0.5,
         pos.getZ() + 0.5 + side.getVector().getZ() * 0.5
      );
      
      this.doSwap(slot);
      EntityUtil.swingHand(Hand.MAIN_HAND, (SwingSide)AntiCheat.INSTANCE.interactSwing.getValue());
      BlockHitResult result = new BlockHitResult(directionVec, side, pos, false);
      Module.sendSequencedPacket(id -> new PlayerInteractBlockC2SPacket(Hand.MAIN_HAND, result, id));
      
      return true;
   }

   private void doSwap(int slot) {
      if (this.inventorySwap.getValue()) {
         InventoryUtil.inventorySwap(slot, mc.player.getInventory().selectedSlot);
      } else {
         InventoryUtil.switchToSlot(slot);
      }
   }

   private int getPlateSlot() {
      if (this.inventorySwap.getValue()) {
         return this.findPressurePlateInInventory();
      }
      return this.findPressurePlateInHotbar();
   }

   private int findPressurePlateInHotbar() {
      for (int i = 0; i < 9; i++) {
         net.minecraft.item.ItemStack stack = mc.player.getInventory().getStack(i);
         if (stack.getItem() instanceof net.minecraft.item.BlockItem) {
            Block block = ((net.minecraft.item.BlockItem) stack.getItem()).getBlock();
            if (block instanceof AbstractPressurePlateBlock) {
               return i;
            }
         }
      }
      return -1;
   }

   private int findPressurePlateInInventory() {
      for (int i = 35; i >= 0; i--) {
         net.minecraft.item.ItemStack stack = mc.player.getInventory().getStack(i);
         if (stack.getItem() instanceof net.minecraft.item.BlockItem) {
            Block block = ((net.minecraft.item.BlockItem) stack.getItem()).getBlock();
            if (block instanceof AbstractPressurePlateBlock) {
               return i < 9 ? i + 36 : i;
            }
         }
      }
      return -1;
   }
}
