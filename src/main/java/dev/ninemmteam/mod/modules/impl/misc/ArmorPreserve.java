package dev.ninemmteam.mod.modules.impl.misc;

import dev.ninemmteam.api.events.eventbus.EventListener;
import dev.ninemmteam.api.events.impl.UpdateEvent;
import dev.ninemmteam.api.utils.player.EntityUtil;
import dev.ninemmteam.mod.modules.Module;
import dev.ninemmteam.mod.modules.impl.combat.AutoXP;
import dev.ninemmteam.mod.modules.settings.impl.SliderSetting;
import net.minecraft.item.ArmorItem;
import net.minecraft.item.ElytraItem;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.slot.SlotActionType;

public class ArmorPreserve extends Module {
   private final SliderSetting threshold = this.add(new SliderSetting("Threshold", 10, 1, 100));

   public ArmorPreserve() {
      super("ArmorPreserve", Category.Misc);
      this.setChinese("脱甲修甲");
   }

   @EventListener
   public void onUpdate(UpdateEvent event) {
      if (mc.player == null || mc.world == null) {
         return;
      }

      if (AutoXP.INSTANCE.isOn()) {
         this.equipFromCrafting();
      } else {
         this.checkAndUnequip();
      }
   }

   private void checkAndUnequip() {
      for (int i = 5; i <= 8; i++) {
         ItemStack stack = mc.player.playerScreenHandler.slots.get(i).getStack();
         if (!stack.isEmpty()) {
            boolean shouldProtect = false;
            if (stack.getItem() instanceof ArmorItem) {
               int durability = EntityUtil.getDamagePercent(stack);
               if (durability <= this.threshold.getValueInt()) {
                  shouldProtect = true;
               }
            }

            if (shouldProtect) {
               this.moveToCrafting(i);
            }
         }
      }
   }

   private void moveToCrafting(int armorSlot) {
      boolean movedToCrafting = false;
      for (int i = 1; i <= 4; i++) {
         if (mc.player.playerScreenHandler.slots.get(i).getStack().isEmpty()) {
            mc.interactionManager.clickSlot(mc.player.playerScreenHandler.syncId, armorSlot, 0, SlotActionType.PICKUP, mc.player);
            mc.interactionManager.clickSlot(mc.player.playerScreenHandler.syncId, i, 0, SlotActionType.PICKUP, mc.player);
            movedToCrafting = true;
            return;
         }
      }

      if (!movedToCrafting) {
         for (int i = 9; i <= 40; i++) {
            if (mc.player.playerScreenHandler.slots.get(i).getStack().isEmpty()) {
               mc.interactionManager.clickSlot(mc.player.playerScreenHandler.syncId, armorSlot, 0, SlotActionType.PICKUP, mc.player);
               mc.interactionManager.clickSlot(mc.player.playerScreenHandler.syncId, i, 0, SlotActionType.PICKUP, mc.player);
               return;
            }
         }
      }
   }

   private void equipFromCrafting() {
      for (int i = 1; i <= 4; i++) {
         ItemStack stack = mc.player.playerScreenHandler.slots.get(i).getStack();
         if (!stack.isEmpty()) {
            int targetSlot = -1;
            if (stack.getItem() instanceof ArmorItem) {
               ArmorItem armor = (ArmorItem)stack.getItem();
               switch (armor.getSlotType()) {
                  case HEAD:
                     targetSlot = 5;
                     break;
                  case CHEST:
                     targetSlot = 6;
                     break;
                  case LEGS:
                     targetSlot = 7;
                     break;
                  case FEET:
                     targetSlot = 8;
                     break;
                  default:
                     break;
               }
            }

            if (targetSlot != -1) {
               ItemStack targetStack = mc.player.playerScreenHandler.slots.get(targetSlot).getStack();
               if (targetStack.isEmpty() || (targetSlot == 6 && targetStack.getItem() instanceof ElytraItem)) {
                  if (!targetStack.isEmpty()) {
                     this.moveElytraToInventory();
                  }
                  mc.interactionManager.clickSlot(mc.player.playerScreenHandler.syncId, i, 0, SlotActionType.PICKUP, mc.player);
                  mc.interactionManager.clickSlot(mc.player.playerScreenHandler.syncId, targetSlot, 0, SlotActionType.PICKUP, mc.player);
                  return;
               }
            }
         }
      }
   }

   private void moveElytraToInventory() {
      for (int i = 9; i <= 40; i++) {
         if (mc.player.playerScreenHandler.slots.get(i).getStack().isEmpty()) {
            mc.interactionManager.clickSlot(mc.player.playerScreenHandler.syncId, 6, 0, SlotActionType.PICKUP, mc.player);
            mc.interactionManager.clickSlot(mc.player.playerScreenHandler.syncId, i, 0, SlotActionType.PICKUP, mc.player);
            return;
         }
      }
   }
}
