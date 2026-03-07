package dev.ninemmteam.mod.modules.impl.misc;

import dev.ninemmteam.api.events.eventbus.EventListener;
import dev.ninemmteam.api.events.impl.UpdateEvent;
import dev.ninemmteam.api.utils.math.Timer;
import dev.ninemmteam.api.utils.world.BlockUtil;
import dev.ninemmteam.mod.modules.Module;
import dev.ninemmteam.mod.modules.impl.player.SpeedMine;
import dev.ninemmteam.mod.modules.settings.impl.BooleanSetting;
import dev.ninemmteam.mod.modules.settings.impl.SliderSetting;
import net.minecraft.block.ShulkerBoxBlock;
import net.minecraft.client.gui.screen.ingame.ShulkerBoxScreen;
import net.minecraft.screen.ShulkerBoxScreenHandler;
import net.minecraft.screen.slot.Slot;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.util.math.BlockPos;

public class DropAntiRegear extends Module {
   private final SliderSetting delay = this.add(new SliderSetting("Delay", 5.0, 0.0, 10.0, 0.1).setSuffix("s"));
   private final BooleanSetting rotate = this.add(new BooleanSetting("Rotate", true));
   private final BooleanSetting inventory = this.add(new BooleanSetting("InventorySwap", true));
   private final BooleanSetting autoDisable = this.add(new BooleanSetting("AutoDisable", true));
   private final BooleanSetting mine = this.add(new BooleanSetting("Mine", true));
   private final BooleanSetting detectShulkers = this.add(new BooleanSetting("DetectShulkers", true));
   private final SliderSetting range = this.add(new SliderSetting("Range", 4.0, 0.0, 6.0, 0.1));
   private final SliderSetting disableTime = this.add(new SliderSetting("DisableTime", 500, 0, 1000));
   private final SliderSetting detectRange = this.add(new SliderSetting("DetectRange", 5.0, 1.0, 8.0, 0.1));
   private BlockPos detectedShulkerPos = null;
   private final Timer timer = new Timer();
   private final Timer timeoutTimer = new Timer();
   private final Timer delayTimer = new Timer();
   private BlockPos placePos = null;
   private BlockPos openPos = null;
   private BlockPos safePos = null;
   private boolean opend = false;

   public DropAntiRegear() {
      super("DropAntiRegear", Category.Misc);
      this.setChinese("丢弃反补给");
   }

   @Override
   public void onEnable() {
      this.opend = false;
      this.placePos = null;
      this.openPos = null;
      this.safePos = null;
      this.timeoutTimer.reset();
   }

   @Override
   public void onDisable() {
      this.opend = false;
      if (this.mine.getValue() && this.placePos != null) {
         SpeedMine.INSTANCE.mine(this.placePos);
      }
      this.placePos = null;
      this.openPos = null;
      this.safePos = null;
   }


   @EventListener
   public void onUpdate(UpdateEvent event) {
      if (mc.player == null || mc.world == null) {
         return;
      }
      if (this.delayTimer.passedMs(this.delay.getValue() * 1000.0)) {

         if (this.detectShulkers.getValue() && !this.opend) {
            if (this.detectedShulkerPos == null ||
                    !(mc.world.getBlockState(this.detectedShulkerPos).getBlock() instanceof ShulkerBoxBlock)) {
               this.detectedShulkerPos = findNearbyShulker();
            }

            if (this.detectedShulkerPos != null &&
                    !this.detectedShulkerPos.equals(this.openPos) &&
                    !this.detectedShulkerPos.equals(this.placePos)) {
               this.openPos = this.detectedShulkerPos;
               BlockUtil.clickBlock(this.detectedShulkerPos, BlockUtil.getClickSide(this.detectedShulkerPos), this.rotate.getValue());
            }
         }

         if (this.safePos != null && mc.player.squaredDistanceTo(this.safePos.toCenterPos()) > 100.0) {
            this.safePos = null;
         }

         if (!(mc.currentScreen instanceof ShulkerBoxScreen)) {
            if (this.opend) {
               this.opend = false;
               if (this.autoDisable.getValue()) {
                  this.timeoutToDisable();
               }

               if (this.openPos != null) {
                  if (mc.world.getBlockState(this.openPos).getBlock() instanceof ShulkerBoxBlock) {
                     BlockUtil.clickBlock(this.openPos, BlockUtil.getClickSide(this.openPos), this.rotate.getValue());
                  } else {
                     this.openPos = null;
                  }
               }
            } else if (this.safePos != null && mc.world.getBlockState(this.safePos).getBlock() instanceof ShulkerBoxBlock) {
               this.openPos = this.safePos;
               BlockUtil.clickBlock(this.safePos, BlockUtil.getClickSide(this.safePos), this.rotate.getValue());
            }
         } else {
            this.opend = true;
            if (mc.player.currentScreenHandler instanceof ShulkerBoxScreenHandler shulker) {
               boolean dropped = false;
               for (Slot slot : shulker.slots) {
                  if (slot.id < 27 && !slot.getStack().isEmpty()) {
                     mc.interactionManager.clickSlot(shulker.syncId, slot.id, 1, SlotActionType.THROW, mc.player);
                     dropped = true;
                  }
               }

               if (!dropped) {
                  mc.player.closeHandledScreen();
               }
            }

            if (this.autoDisable.getValue()) {
               this.timeoutToDisable();
            }
            this.delayTimer.reset();
         }
      }
   }

   private void timeoutToDisable() {
      if (this.timeoutTimer.passed(this.disableTime.getValueInt())) {
         this.disable();
      }
   }

   private BlockPos findNearbyShulker() {
      for (BlockPos pos : BlockUtil.getSphere((float)this.detectRange.getValue())) {
         if (mc.world.getBlockState(pos).getBlock() instanceof ShulkerBoxBlock) {
            if (!pos.equals(this.placePos) && !pos.equals(this.safePos)) {
               return pos;
            }
         }
      }
      return null;
   }
}

