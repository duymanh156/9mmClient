package dev.ninemmteam.mod.modules.impl.misc;

import dev.ninemmteam.api.events.eventbus.EventListener;
import dev.ninemmteam.api.events.impl.UpdateEvent;
import dev.ninemmteam.api.utils.math.Timer;
import dev.ninemmteam.asm.accessors.IContainerComponent;
import dev.ninemmteam.mod.modules.Module;
import dev.ninemmteam.mod.modules.settings.impl.BooleanSetting;
import dev.ninemmteam.mod.modules.settings.impl.SliderSetting;
import net.minecraft.component.ComponentMap;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.NbtComponent;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtList;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.util.collection.DefaultedList;

public class DropShulkerBox extends Module {
    private final Timer timer = new Timer();
    private final SliderSetting delay = this.add(new SliderSetting("Delay", 100, 0, 1000, 10).setSuffix("ms"));
    private final BooleanSetting checkInventory = this.add(new BooleanSetting("CheckInventory", true));
    private final BooleanSetting checkHotbar = this.add(new BooleanSetting("CheckHotbar", true));
    private final BooleanSetting checkOffhand = this.add(new BooleanSetting("CheckOffhand", true));
    private final BooleanSetting dropFullShulkers = this.add(new BooleanSetting("DropFullShulkers", false));
    private final BooleanSetting onlyWhenEmpty = this.add(new BooleanSetting("OnlyWhenEmpty", false));
    
    public DropShulkerBox() {
        super("DropShulkerBox", Category.Misc);
        this.setChinese("丢弃潜影盒");
    }

    @EventListener
    public void onUpdate(UpdateEvent event) {
        if (mc.player == null || mc.world == null) {
            return;
        }
        
        if (!timer.passedMs(delay.getValueInt())) {
            return;
        }
        
        if (checkInventory.getValue()) {
            for (int i = 9; i < 36; i++) {
                ItemStack stack = mc.player.getInventory().getStack(i);
                if (shouldDropShulker(stack)) {
                    dropItem(i);
                    timer.reset();
                    return;
                }
            }
        }
        
        if (checkHotbar.getValue()) {
            for (int i = 0; i < 9; i++) {
                ItemStack stack = mc.player.getInventory().getStack(i);
                if (shouldDropShulker(stack)) {
                    dropItem(i < 9 ? i + 36 : i);
                    timer.reset();
                    return;
                }
            }
        }
        
        if (checkOffhand.getValue()) {
            ItemStack stack = mc.player.getOffHandStack();
            if (shouldDropShulker(stack)) {
                dropItem(45);
                timer.reset();
                return;
            }
        }
    }
    
    private boolean shouldDropShulker(ItemStack stack) {
        if (stack.isEmpty() || stack.getItem() != Items.SHULKER_BOX) {
            return false;
        }
        
        boolean isFull = isShulkerFull(stack);
        
        if (dropFullShulkers.getValue()) {
            return isFull;
        } else {
            return !isFull;
        }
    }
    
    private boolean isShulkerFull(ItemStack shulkerStack) {
        if (shulkerStack.isEmpty() || shulkerStack.getItem() != Items.SHULKER_BOX) {
            return false;
        }
        
        ComponentMap components = shulkerStack.getComponents();
        
        if (components.contains(DataComponentTypes.CONTAINER)) {
            IContainerComponent container = (IContainerComponent)(Object)components.get(DataComponentTypes.CONTAINER);
            if (container != null) {
                DefaultedList<ItemStack> stacks = container.getStacks();
                if (stacks.isEmpty()) {
                    return false;
                }
                
                for (ItemStack stack : stacks) {
                    if (stack.isEmpty()) {
                        return false;
                    }
                }
                return true;
            }
        }
        
        if (components.contains(DataComponentTypes.BLOCK_ENTITY_DATA)) {
            NbtComponent nbtComponent = (NbtComponent) components.get(DataComponentTypes.BLOCK_ENTITY_DATA);
            if (nbtComponent != null && nbtComponent.contains("Items")) {
                NbtList items = (NbtList) nbtComponent.getNbt().get("Items");
                if (items == null || items.isEmpty()) {
                    return false;
                }
                
                boolean[] filledSlots = new boolean[27];
                for (int i = 0; i < items.size(); i++) {
                    NbtCompound itemTag = items.getCompound(i);
                    int slot = itemTag.getByte("Slot") & 255;
                    if (slot >= 0 && slot < 27) {
                        filledSlots[slot] = true;
                    }
                }
                
                for (boolean filled : filledSlots) {
                    if (!filled) {
                        return false;
                    }
                }
                return true;
            }
        }
        
        return false;
    }
    
    private void dropItem(int slot) {
        int syncId = mc.player.currentScreenHandler.syncId;
        mc.interactionManager.clickSlot(syncId, slot, 1, SlotActionType.THROW, mc.player);
    }
}