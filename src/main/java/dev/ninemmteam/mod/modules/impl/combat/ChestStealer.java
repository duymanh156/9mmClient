package dev.ninemmteam.mod.modules.impl.combat;

import dev.ninemmteam.api.events.eventbus.EventListener;
import dev.ninemmteam.api.events.impl.UpdateEvent;
import dev.ninemmteam.api.utils.math.Timer;
import dev.ninemmteam.api.utils.player.InventoryUtil;
import dev.ninemmteam.api.utils.world.BlockUtil;
import dev.ninemmteam.mod.modules.Module;
import dev.ninemmteam.mod.modules.impl.player.SpeedMine;
import dev.ninemmteam.mod.modules.settings.impl.BindSetting;
import dev.ninemmteam.mod.modules.settings.impl.BooleanSetting;
import dev.ninemmteam.mod.modules.settings.impl.EnumSetting;
import dev.ninemmteam.mod.modules.settings.impl.SliderSetting;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.block.BedBlock;
import net.minecraft.block.PistonBlock;
import net.minecraft.block.ShulkerBoxBlock;
import net.minecraft.client.gui.screen.ingame.ShulkerBoxScreen;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.PotionContentsComponent;
import net.minecraft.entity.effect.StatusEffect;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.item.BlockItem;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.screen.ShulkerBoxScreenHandler;
import net.minecraft.screen.slot.Slot;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.MathHelper;

public class ChestStealer extends Module {
    public static ChestStealer INSTANCE;
    public final BooleanSetting rotate = this.add(new BooleanSetting("Rotate", true));
    public final Timer timeoutTimer = new Timer();
    final int[] stealCountList = new int[]{0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0};
    private final BooleanSetting autoDisable = this.add(new BooleanSetting("AutoDisable", true));
    private final SliderSetting disableTime = this.add(new SliderSetting("DisableTime", 500, 0, 1000));
    private final BooleanSetting place = this.add(new BooleanSetting("Place", true));
    private final BooleanSetting inventory = this.add(new BooleanSetting("InventorySwap", true));
    private final BooleanSetting preferOpen = this.add(new BooleanSetting("PerferOpen", true));
    private final BooleanSetting open = this.add(new BooleanSetting("Open", true));
    private final SliderSetting range = this.add(new SliderSetting("MaxRange", 4.0, 0.0, 6.0, 0.1));
    private final SliderSetting minRange = this.add(new SliderSetting("MinRange", 1.0, 0.0, 3.0, 0.1));
    private final BooleanSetting mine = this.add(new BooleanSetting("Mine", true));
    private final BooleanSetting keyMode = this.add(new BooleanSetting("KeyMode", false));
    private final BindSetting placeKey = this.add(new BindSetting("PlaceKey", -1, () -> this.keyMode.getValue()));
    private final BooleanSetting preferNearby = this.add(new BooleanSetting("PreferNearOpen", true));
    private final EnumSetting<Mode> mode = this.add(new EnumSetting<Mode>("Mode", Mode.SMART));
    private final BooleanSetting take = this.add(new BooleanSetting("Take", true, () -> this.mode.getValue() == Mode.SMART));
    private final BooleanSetting smart = this.add(new BooleanSetting("Smart", true, () -> this.mode.getValue() == Mode.SMART && this.take.getValue()).setParent());
    private final BooleanSetting forceMove = this.add(new BooleanSetting("ForceQuickMove", true, () -> this.mode.getValue() == Mode.SMART && this.take.getValue() && this.smart.isOpen()));
    private final SliderSetting crystal = this.add(new SliderSetting("Crystal", 256, 0, 512, () -> this.mode.getValue() == Mode.SMART && this.take.getValue() && this.smart.isOpen()));
    private final SliderSetting exp = this.add(new SliderSetting("Exp", 256, 0, 512, () -> this.mode.getValue() == Mode.SMART && this.take.getValue() && this.smart.isOpen()));
    private final SliderSetting totem = this.add(new SliderSetting("Totem", 6, 0, 36, () -> this.mode.getValue() == Mode.SMART && this.take.getValue() && this.smart.isOpen()));
    private final SliderSetting gapple = this.add(new SliderSetting("Gapple", 128, 0, 512, () -> this.mode.getValue() == Mode.SMART && this.take.getValue() && this.smart.isOpen()));
    private final SliderSetting obsidian = this.add(new SliderSetting("Obsidian", 64, 0, 512, () -> this.mode.getValue() == Mode.SMART && this.take.getValue() && this.smart.isOpen()));
    private final SliderSetting web = this.add(new SliderSetting("Web", 64, 0, 512, () -> this.mode.getValue() == Mode.SMART && this.take.getValue() && this.smart.isOpen()));
    private final SliderSetting glowstone = this.add(new SliderSetting("Glowstone", 128, 0, 512, () -> this.mode.getValue() == Mode.SMART && this.take.getValue() && this.smart.isOpen()));
    private final SliderSetting anchor = this.add(new SliderSetting("Anchor", 128, 0, 512, () -> this.mode.getValue() == Mode.SMART && this.take.getValue() && this.smart.isOpen()));
    private final SliderSetting pearl = this.add(new SliderSetting("Pearl", 16, 0, 64, () -> this.mode.getValue() == Mode.SMART && this.take.getValue() && this.smart.isOpen()));
    private final SliderSetting piston = this.add(new SliderSetting("Piston", 64, 0, 512, () -> this.mode.getValue() == Mode.SMART && this.take.getValue() && this.smart.isOpen()));
    private final SliderSetting redstone = this.add(new SliderSetting("RedStone", 64, 0, 512, () -> this.mode.getValue() == Mode.SMART && this.take.getValue() && this.smart.isOpen()));
    private final SliderSetting bed = this.add(new SliderSetting("Bed", 256, 0, 512, () -> this.mode.getValue() == Mode.SMART && this.take.getValue() && this.smart.isOpen()));
    private final SliderSetting speed = this.add(new SliderSetting("Speed", 1, 0, 8, () -> this.mode.getValue() == Mode.SMART && this.take.getValue() && this.smart.isOpen()));
    private final SliderSetting resistance = this.add(new SliderSetting("Resistance", 1, 0, 8, () -> this.mode.getValue() == Mode.SMART && this.take.getValue() && this.smart.isOpen()));
    private final SliderSetting strength = this.add(new SliderSetting("Strength", 1, 0, 8, () -> this.mode.getValue() == Mode.SMART && this.take.getValue() && this.smart.isOpen()));
    private final BooleanSetting closeAfterRegear = this.add(new BooleanSetting("CloseAfter", false, () -> this.mode.getValue() == Mode.REGEAR));
    private final SliderSetting regearSpeed = this.add(new SliderSetting("RegearSpeed", 1.0, 1.0, 1000.0, 1.0, () -> this.mode.getValue() == Mode.REGEAR));
    private final SliderSetting regearClicks = this.add(new SliderSetting("ClicksPerTick", 1.0, 1.0, 36.0, 1.0, () -> this.mode.getValue() == Mode.REGEAR));
    private final Timer timer = new Timer();
    private final Timer regearTimer = new Timer();
    private final List<BlockPos> openList = new ArrayList<BlockPos>();
    private final Map<Integer, String> regearExpectedInv = new HashMap<Integer, String>();
    private boolean regearSetupDone = false;
    private static final File KITS_FILE = new File(System.getProperty("user.dir") + File.separator + "nina" + File.separator + "kits.cfg");
    private final Map<Integer, String> shulkerSlots = new HashMap<Integer, String>();
    public BlockPos placePos = null;
    private BlockPos openPos;
    private boolean opend = false;
    private boolean keyPressed = false;
    private boolean keyModePlacementDone = false;

    public ChestStealer() {
        super("ChestStealer", Module.Category.Combat);
        this.setChinese("自动补给");
        INSTANCE = this;
    }

    private int getTargetSlotForShulker(int playerSlot, ShulkerBoxScreenHandler handler) {
        boolean is63SlotContainer = handler.slots.size() == 63;
        return playerSlot < 9 ? playerSlot + (is63SlotContainer ? 54 : 81) : playerSlot + (is63SlotContainer ? 18 : 45);
    }

    private int calculateContainerSlotForPlayerSlot(int playerSlot) {
        if (playerSlot >= 9 && playerSlot < 18) {
            return playerSlot + 18;
        }
        if (playerSlot >= 18 && playerSlot < 27) {
            return playerSlot + 18;
        }
        if (playerSlot >= 27) {
            return playerSlot + 18;
        }
        return playerSlot + 54;
    }

    private boolean hasShulkerInInventory() {
        for (int i = 0; i < 36; ++i) {
            ItemStack stack = mc.player.getInventory().getStack(i);
            if (!stack.isEmpty() && stack.getItem() instanceof BlockItem blockItem && blockItem.getBlock() instanceof ShulkerBoxBlock) {
                return true;
            }
        }
        return false;
    }

    public void loadCurrentKit() {
        try {
            Map<String, Object> kits = this.loadKits();
            String currentKitName = (String)kits.getOrDefault("pointer", "none");
            if (currentKitName.equals("none")) {
                this.sendMessage("§cNo kit selected! Use .regear load <name>");
                return;
            }
            if (!kits.containsKey(currentKitName)) {
                this.sendMessage("§cKit '" + currentKitName + "' not found!");
                return;
            }
            String kitItems = (String)kits.get(currentKitName);
            this.setupRegear(kitItems);
            this.sendMessage("§aLoaded kit: " + currentKitName);
        }
        catch (Exception e) {
            this.sendMessage("§cError loading kit: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public String getCurrentKitName() {
        try {
            Map<String, Object> kits = this.loadKits();
            return (String)kits.getOrDefault("pointer", "none");
        }
        catch (Exception e) {
            return "none";
        }
    }

    private Map<String, Object> loadKits() throws IOException {
        HashMap<String, Object> kits = new HashMap<String, Object>();
        if (!KITS_FILE.exists()) {
            KITS_FILE.getParentFile().mkdirs();
            KITS_FILE.createNewFile();
            kits.put("pointer", "none");
            this.saveKits(kits);
            return kits;
        }
        try (BufferedReader reader = new BufferedReader(new FileReader(KITS_FILE));) {
            String line;
            while ((line = reader.readLine()) != null) {
                String[] parts;
                if ((line = line.trim()).isEmpty() || line.startsWith("#") || !line.contains(":") || (parts = line.split(":", 2)).length != 2) continue;
                kits.put(parts[0].trim(), parts[1].trim());
            }
        }
        if (!kits.containsKey("pointer")) {
            kits.put("pointer", "none");
        }
        return kits;
    }

    private void saveKits(Map<String, Object> kits) throws IOException {
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(KITS_FILE));) {
            writer.write("# aether Regear Kits");
            writer.newLine();
            writer.newLine();
            for (Map.Entry<String, Object> entry : kits.entrySet()) {
                writer.write(entry.getKey() + ": " + String.valueOf(entry.getValue()));
                writer.newLine();
            }
        }
    }

    public void setupRegear(String kitItems) {
        if (kitItems.isEmpty() || kitItems.split(" ").length != 36) {
            this.sendMessage("§cInvalid kit! create it again");
            return;
        }
        String[] items = kitItems.split(" ");
        this.regearExpectedInv.clear();
        for (int i = 0; i < 36; ++i) {
            if (items[i].equals("minecraft:air")) continue;
            this.regearExpectedInv.put(i, items[i]);
        }
        this.regearSetupDone = true;
    }

    private boolean isSlotCorrect(int slot, String expectedItemKey, ItemStack currentStack) {
        if (currentStack.isEmpty()) {
            return expectedItemKey.equals("minecraft:air");
        }
        String currentItemKey = currentStack.getItem().toString();
        return currentItemKey.equals(expectedItemKey);
    }

    private boolean needToTakeItem(ItemStack currentStack, String expectedItem) {
        if (currentStack.isEmpty()) {
            return true;
        }
        return !this.isSlotCorrect(0, expectedItem, currentStack);
    }

    private int findItemInShulker(String itemName, ShulkerBoxScreenHandler handler, int excludeSlot) {
        int maxSearch = handler.slots.size() == 63 ? 27 : 54;
        int bestSlot = -1;
        int bestCount = 0;
        for (int i = 0; i < maxSearch; ++i) {
            ItemStack stack;
            if (i == excludeSlot || (stack = handler.slots.get(i).getStack()).isEmpty()) continue;
            String stackKey = stack.getItem().toString();
            if (!stackKey.equals(itemName) || stack.getCount() <= bestCount) continue;
            bestCount = stack.getCount();
            bestSlot = i;
            if (bestCount >= 64) break;
        }
        return bestSlot;
    }

    private int findEmptySlotInShulker(ShulkerBoxScreenHandler handler) {
        int maxSearch = handler.slots.size() == 63 ? 27 : 54;
        for (int i = 0; i < maxSearch; ++i) {
            ItemStack stack = handler.slots.get(i).getStack();
            if (!stack.isEmpty()) continue;
            return i;
        }
        return -1;
    }

    private int findMergeableStack(String itemName, ShulkerBoxScreenHandler handler, int targetSlot) {
        int maxSearch = handler.slots.size() == 63 ? 27 : 54;
        ItemStack targetStack = handler.slots.get(targetSlot).getStack();
        if (targetStack.isEmpty() || targetStack.getCount() >= targetStack.getMaxCount()) {
            return -1;
        }
        for (int i = 0; i < maxSearch; ++i) {
            ItemStack stack;
            if (i == targetSlot || (stack = handler.slots.get(i).getStack()).isEmpty() || stack.getItem() != targetStack.getItem()) continue;
            if (!stack.getItem().toString().equals(targetStack.getItem().toString())) continue;
            return i;
        }
        return -1;
    }

    private void performPickupAction(ShulkerBoxScreenHandler handler, int sourceSlot, int targetSlot) {
        mc.interactionManager.clickSlot(handler.syncId, sourceSlot, 0, SlotActionType.PICKUP, mc.player);
        mc.interactionManager.clickSlot(handler.syncId, targetSlot, 0, SlotActionType.PICKUP, mc.player);
        ItemStack cursorStack = handler.getCursorStack();
        if (!cursorStack.isEmpty()) {
            mc.interactionManager.clickSlot(handler.syncId, sourceSlot, 0, SlotActionType.PICKUP, mc.player);
        }
    }

    private void performMergeAction(ShulkerBoxScreenHandler handler, int sourceSlot, int targetSlot) {
        ItemStack sourceStack = handler.slots.get(sourceSlot).getStack();
        ItemStack targetStack = handler.slots.get(targetSlot).getStack();
        if (sourceStack.isEmpty() || targetStack.isEmpty()) {
            return;
        }
        int spaceLeft = targetStack.getMaxCount() - targetStack.getCount();
        int available = Math.min(spaceLeft, sourceStack.getCount());
        if (available > 0) {
            if (sourceStack.getCount() == available) {
                mc.interactionManager.clickSlot(handler.syncId, sourceSlot, 0, SlotActionType.QUICK_MOVE, mc.player);
            } else {
                mc.interactionManager.clickSlot(handler.syncId, sourceSlot, 0, SlotActionType.PICKUP, mc.player);
                mc.interactionManager.clickSlot(handler.syncId, targetSlot, 0, SlotActionType.PICKUP, mc.player);
                ItemStack cursorStack = handler.getCursorStack();
                if (!cursorStack.isEmpty()) {
                    mc.interactionManager.clickSlot(handler.syncId, sourceSlot, 0, SlotActionType.PICKUP, mc.player);
                }
            }
        }
    }

    private boolean checkAllItemsCorrect(ShulkerBoxScreenHandler handler) {
        for (Map.Entry<Integer, String> entry : this.regearExpectedInv.entrySet()) {
            int targetSlot = this.getTargetSlotForShulker(entry.getKey(), handler);
            if (targetSlot < 0 || targetSlot >= handler.slots.size()) continue;
            ItemStack itemInSlot = handler.slots.get(targetSlot).getStack();
            String expected = entry.getValue();
            if (expected.contains("shulker_box")) {
                if (itemInSlot.isEmpty()) continue;
                return false;
            }
            if (itemInSlot.isEmpty()) {
                return false;
            }
            if (this.isSlotCorrect(entry.getKey(), expected, itemInSlot)) continue;
            return false;
        }
        return true;
    }

    private void doRegear(ShulkerBoxScreenHandler handler) {
        if (!this.regearTimer.passed((long)(1000.0 / this.regearSpeed.getValue()))) {
            return;
        }
        boolean allItemsCorrect = true;
        int actionsTaken = 0;
        ArrayList<Map.Entry<Integer, String>> itemsToProcess = new ArrayList<Map.Entry<Integer, String>>(this.regearExpectedInv.entrySet());
        itemsToProcess.sort((a, b) -> {
            int slotA = a.getKey();
            int slotB = b.getKey();
            String expectedA = a.getValue();
            String expectedB = b.getValue();
            if (expectedA.contains("shulker_box") && !expectedB.contains("shulker_box")) {
                return 1;
            }
            if (!expectedA.contains("shulker_box") && expectedB.contains("shulker_box")) {
                return -1;
            }
            ItemStack stackA = handler.slots.get(this.getTargetSlotForShulker(slotA, handler)).getStack();
            ItemStack stackB = handler.slots.get(this.getTargetSlotForShulker(slotB, handler)).getStack();
            boolean isEmptyA = stackA.isEmpty();
            boolean isEmptyB = stackB.isEmpty();
            boolean isCorrectA = this.isSlotCorrect(slotA, expectedA, stackA);
            boolean isCorrectB = this.isSlotCorrect(slotB, expectedB, stackB);
            if (isEmptyA && !isEmptyB) {
                return -1;
            }
            if (!isEmptyA && isEmptyB) {
                return 1;
            }
            if (!isCorrectA && isCorrectB) {
                return -1;
            }
            if (isCorrectA && !isCorrectB) {
                return 1;
            }
            return 0;
        });
        for (Map.Entry<Integer, String> entry : itemsToProcess) {
            int mergeSlot;
            int targetSlot;
            if (actionsTaken >= this.regearClicks.getValueInt()) break;
            int slotIndex = entry.getKey();
            String expectedItem = entry.getValue();
            ItemStack playerStack = mc.player.getInventory().getStack(slotIndex);
            if (!playerStack.isEmpty() && playerStack.getItem() instanceof BlockItem blockItem && blockItem.getBlock() instanceof ShulkerBoxBlock || (targetSlot = this.getTargetSlotForShulker(slotIndex, handler)) < 0 || targetSlot >= handler.slots.size()) continue;
            ItemStack targetStack = handler.slots.get(targetSlot).getStack();
            if (expectedItem.contains("shulker_box")) {
                int emptyShulkerSlot;
                if (targetStack.isEmpty() || (emptyShulkerSlot = this.findEmptySlotInShulker(handler)) == -1) continue;
                mc.interactionManager.clickSlot(handler.syncId, targetSlot, 0, SlotActionType.PICKUP, mc.player);
                mc.interactionManager.clickSlot(handler.syncId, emptyShulkerSlot, 0, SlotActionType.PICKUP, mc.player);
                if (!handler.getCursorStack().isEmpty()) {
                    mc.interactionManager.clickSlot(handler.syncId, targetSlot, 0, SlotActionType.PICKUP, mc.player);
                }
                ++actionsTaken;
                allItemsCorrect = false;
                continue;
            }
            if (this.needToTakeItem(targetStack, expectedItem)) {
                int emptySlot;
                int sourceSlot = this.findItemInShulker(expectedItem, handler, targetSlot);
                if (sourceSlot != -1) {
                    this.performPickupAction(handler, sourceSlot, targetSlot);
                    ++actionsTaken;
                    allItemsCorrect = false;
                    continue;
                }
                if (targetStack.isEmpty() || this.isSlotCorrect(slotIndex, expectedItem, targetStack) || (emptySlot = this.findEmptySlotInShulker(handler)) == -1) continue;
                mc.interactionManager.clickSlot(handler.syncId, targetSlot, 0, SlotActionType.PICKUP, mc.player);
                mc.interactionManager.clickSlot(handler.syncId, emptySlot, 0, SlotActionType.PICKUP, mc.player);
                ++actionsTaken;
                allItemsCorrect = false;
                continue;
            }
            if (targetStack.getCount() >= targetStack.getMaxCount() || (mergeSlot = this.findMergeableStack(expectedItem, handler, targetSlot)) == -1) continue;
            this.performMergeAction(handler, mergeSlot, targetSlot);
            ++actionsTaken;
        }
        this.regearTimer.reset();
        if ((allItemsCorrect || this.checkAllItemsCorrect(handler)) && this.closeAfterRegear.getValue()) {
            mc.currentScreen.close();
        }
    }

    public int findShulker() {
        if (this.inventory.getValue()) {
            for (int i = 0; i < 36; ++i) {
                ItemStack stack = mc.player.getInventory().getStack(i);
                if (!stack.isEmpty() && stack.getItem() instanceof BlockItem blockItem && blockItem.getBlock() instanceof ShulkerBoxBlock) {
                    return i < 9 ? i + 36 : i;
                }
            }
            return -1;
        }
        return InventoryUtil.findClass(ShulkerBoxBlock.class);
    }

    @Override
    public void onEnable() {
        this.opend = false;
        this.openPos = null;
        this.timeoutTimer.reset();
        this.placePos = null;
        this.regearSetupDone = false;
        this.keyPressed = false;
        this.keyModePlacementDone = false;
        if (!nullCheck() && this.place.getValue() && !this.keyMode.getValue()) {
            this.doPlace();
        }
    }

    private void doKeyModePlace() {
        int oldSlot = mc.player.getInventory().selectedSlot;
        double distance = 100.0;
        BlockPos bestPos = null;
        for (BlockPos pos : BlockUtil.getSphere((float)this.range.getValue())) {
            if (!mc.world.isAir(pos.up()) || !mc.world.isAir(pos)) continue;
            if (this.preferOpen.getValue() && mc.world.getBlockState(pos).getBlock() instanceof ShulkerBoxBlock) {
                return;
            }
            if (MathHelper.sqrt((float)mc.player.squaredDistanceTo(pos.toCenterPos())) < this.minRange.getValue() || !BlockUtil.clientCanPlace(pos, false) || !BlockUtil.isStrictDirection(pos.offset(Direction.DOWN), Direction.UP) || !BlockUtil.canClick(pos.offset(Direction.DOWN)) || bestPos != null && !(MathHelper.sqrt((float)mc.player.squaredDistanceTo(pos.toCenterPos())) < distance)) continue;
            distance = MathHelper.sqrt((float)mc.player.squaredDistanceTo(pos.toCenterPos()));
            bestPos = pos;
        }
        if (bestPos != null) {
            if (this.findShulker() == -1) {
                this.sendMessage("§4No shulkerbox found.");
                return;
            }
            if (this.inventory.getValue()) {
                int slot = this.findShulker();
                InventoryUtil.inventorySwap(slot, oldSlot);
                this.placeBlock(bestPos);
                this.placePos = bestPos;
                InventoryUtil.inventorySwap(slot, oldSlot);
            } else {
                InventoryUtil.switchToSlot(this.findShulker());
                this.placeBlock(bestPos);
                this.placePos = bestPos;
                InventoryUtil.switchToSlot(oldSlot);
            }
            this.timer.reset();
            this.keyModePlacementDone = true;
        } else {
            this.sendMessage("§4No place position found.");
        }
    }

    private boolean tryOpenNearbyShulker() {
        for (BlockPos pos : BlockUtil.getSphere((float)this.range.getValue())) {
            if (this.openList.contains(pos) || !mc.world.isAir(pos.up()) && !BlockUtil.canReplace(pos.up()) || !(mc.world.getBlockState(pos).getBlock() instanceof ShulkerBoxBlock)) continue;
            this.openPos = pos;
            BlockUtil.clickBlock(pos, BlockUtil.getClickSide(pos), this.rotate.getValue());
            return true;
        }
        return false;
    }

    private void doPlace() {
        int oldSlot = mc.player.getInventory().selectedSlot;
        double distance = 100.0;
        BlockPos bestPos = null;
        for (BlockPos pos : BlockUtil.getSphere((float)this.range.getValue())) {
            if (!mc.world.isAir(pos.up()) || !mc.world.isAir(pos)) continue;
            if (this.preferOpen.getValue() && mc.world.getBlockState(pos).getBlock() instanceof ShulkerBoxBlock) {
                return;
            }
            if (MathHelper.sqrt((float)mc.player.squaredDistanceTo(pos.toCenterPos())) < this.minRange.getValue() || !BlockUtil.clientCanPlace(pos, false) || !BlockUtil.isStrictDirection(pos.offset(Direction.DOWN), Direction.UP) || !BlockUtil.canClick(pos.offset(Direction.DOWN)) || bestPos != null && !(MathHelper.sqrt((float)mc.player.squaredDistanceTo(pos.toCenterPos())) < distance)) continue;
            distance = MathHelper.sqrt((float)mc.player.squaredDistanceTo(pos.toCenterPos()));
            bestPos = pos;
        }
        if (bestPos != null) {
            if (this.findShulker() == -1) {
                this.sendMessage("§4No shulkerbox found.");
                return;
            }
            if (this.inventory.getValue()) {
                int slot = this.findShulker();
                InventoryUtil.inventorySwap(slot, oldSlot);
                this.placeBlock(bestPos);
                this.placePos = bestPos;
                InventoryUtil.inventorySwap(slot, oldSlot);
            } else {
                InventoryUtil.switchToSlot(this.findShulker());
                this.placeBlock(bestPos);
                this.placePos = bestPos;
                InventoryUtil.switchToSlot(oldSlot);
            }
            this.timer.reset();
        } else {
            this.sendMessage("§4No place position found.");
        }
    }

    private void update() {
        this.stealCountList[0] = (int)(this.crystal.getValue() - InventoryUtil.getItemCount(Items.END_CRYSTAL));
        this.stealCountList[1] = (int)(this.exp.getValue() - InventoryUtil.getItemCount(Items.EXPERIENCE_BOTTLE));
        this.stealCountList[2] = (int)(this.totem.getValue() - InventoryUtil.getItemCount(Items.TOTEM_OF_UNDYING));
        this.stealCountList[3] = (int)(this.gapple.getValue() - InventoryUtil.getItemCount(Items.ENCHANTED_GOLDEN_APPLE));
        this.stealCountList[4] = (int)(this.obsidian.getValue() - InventoryUtil.getItemCount(net.minecraft.block.Blocks.OBSIDIAN.asItem()));
        this.stealCountList[5] = (int)(this.web.getValue() - InventoryUtil.getItemCount(net.minecraft.block.Blocks.COBWEB.asItem()));
        this.stealCountList[6] = (int)(this.glowstone.getValue() - InventoryUtil.getItemCount(net.minecraft.block.Blocks.GLOWSTONE.asItem()));
        this.stealCountList[7] = (int)(this.anchor.getValue() - InventoryUtil.getItemCount(net.minecraft.block.Blocks.RESPAWN_ANCHOR.asItem()));
        this.stealCountList[8] = (int)(this.pearl.getValue() - InventoryUtil.getItemCount(Items.ENDER_PEARL));
        this.stealCountList[9] = (int)(this.piston.getValue() - InventoryUtil.getItemCount(net.minecraft.block.Blocks.PISTON.asItem()) - InventoryUtil.getItemCount(net.minecraft.block.Blocks.STICKY_PISTON.asItem()));
        this.stealCountList[10] = (int)(this.redstone.getValue() - InventoryUtil.getItemCount(net.minecraft.block.Blocks.REDSTONE_BLOCK.asItem()));
        this.stealCountList[11] = (int)(this.bed.getValue() - InventoryUtil.getItemCount(BedBlock.class));
        this.stealCountList[12] = (int)(this.speed.getValue() - InventoryUtil.getPotionCount((StatusEffect)StatusEffects.SPEED.value()));
        this.stealCountList[13] = (int)(this.resistance.getValue() - InventoryUtil.getPotionCount((StatusEffect)StatusEffects.RESISTANCE.value()));
        this.stealCountList[14] = (int)(this.strength.getValue() - InventoryUtil.getPotionCount((StatusEffect)StatusEffects.STRENGTH.value()));
    }

    @Override
    public void onDisable() {
        this.opend = false;
        this.regearSetupDone = false;
        this.keyPressed = false;
        this.keyModePlacementDone = false;
        if (this.mine.getValue() && this.placePos != null) {
            SpeedMine.INSTANCE.mine(this.placePos);
        }
    }

    @EventListener
    public void onUpdate(UpdateEvent event) {
        if (this.keyMode.getValue() && this.placeKey.isPressed() && mc.currentScreen == null) {
            if (!this.keyPressed) {
                this.opend = false;
                this.openPos = null;
                this.timeoutTimer.reset();
                this.placePos = null;
                this.keyModePlacementDone = false;
                if (this.preferNearby.getValue() && this.tryOpenNearbyShulker()) {
                    this.keyPressed = true;
                    return;
                }
                this.doKeyModePlace();
                this.keyPressed = true;
            }
            if (this.keyModePlacementDone && this.placePos != null && mc.world.getBlockState(this.placePos).getBlock() instanceof ShulkerBoxBlock && !this.opend && !(mc.currentScreen instanceof ShulkerBoxScreen) && this.timer.passed(200L)) {
                this.openPos = this.placePos;
                BlockUtil.clickBlock(this.placePos, BlockUtil.getClickSide(this.placePos), this.rotate.getValue());
                this.timer.reset();
                this.keyModePlacementDone = false;
            }
        } else {
            this.keyPressed = false;
        }
        if (this.mode.getValue() == Mode.SMART && this.smart.getValue()) {
            this.update();
        }
        this.openList.removeIf(pos -> !(mc.world.getBlockState(pos).getBlock() instanceof ShulkerBoxBlock));
        if (!(mc.currentScreen instanceof ShulkerBoxScreen)) {
            if (this.opend) {
                this.opend = false;
                if (this.autoDisable.getValue() && !this.keyMode.getValue()) {
                    this.timeoutToDisable();
                }
                if (this.mine.getValue() && this.openPos != null) {
                    if (mc.world.getBlockState(this.openPos).getBlock() instanceof ShulkerBoxBlock) {
                        SpeedMine.INSTANCE.mine(this.openPos);
                    } else {
                        this.openPos = null;
                    }
                }
            } else if (!this.keyMode.getValue() && this.open.getValue()) {
                if (this.placePos == null || !(MathHelper.sqrt((float)mc.player.squaredDistanceTo(this.placePos.toCenterPos())) <= this.range.getValue()) || !mc.world.isAir(this.placePos.up()) || this.timer.passed(500L) && !(mc.world.getBlockState(this.placePos).getBlock() instanceof ShulkerBoxBlock)) {
                    boolean found = false;
                    for (BlockPos pos2 : BlockUtil.getSphere((float)this.range.getValue())) {
                        if (this.openList.contains(pos2) || !mc.world.isAir(pos2.up()) && !BlockUtil.canReplace(pos2.up()) || !(mc.world.getBlockState(pos2).getBlock() instanceof ShulkerBoxBlock)) continue;
                        this.openPos = pos2;
                        BlockUtil.clickBlock(pos2, BlockUtil.getClickSide(pos2), this.rotate.getValue());
                        found = true;
                        break;
                    }
                    if (!found && this.autoDisable.getValue() && !this.keyMode.getValue()) {
                        this.timeoutToDisable();
                    }
                } else if (mc.world.getBlockState(this.placePos).getBlock() instanceof ShulkerBoxBlock) {
                    this.openPos = this.placePos;
                    BlockUtil.clickBlock(this.placePos, BlockUtil.getClickSide(this.placePos), this.rotate.getValue());
                }
            } else if (this.mode.getValue() == Mode.SMART && !this.take.getValue() && this.autoDisable.getValue() && !this.keyMode.getValue()) {
                this.timeoutToDisable();
            } else if (this.keyMode.getValue() && this.keyModePlacementDone && this.placePos != null && mc.world.getBlockState(this.placePos).getBlock() instanceof ShulkerBoxBlock && this.timer.passed(200L)) {
                this.openPos = this.placePos;
                BlockUtil.clickBlock(this.placePos, BlockUtil.getClickSide(this.placePos), this.rotate.getValue());
                this.keyModePlacementDone = false;
            }
        } else {
            this.opend = true;
            if (this.openPos != null) {
                this.openList.add(this.openPos);
            }
            if (this.mode.getValue() == Mode.REGEAR) {
                if (mc.player.currentScreenHandler instanceof ShulkerBoxScreenHandler shulker) {
                    if (!this.regearSetupDone) {
                        this.loadCurrentKit();
                        this.regearSetupDone = true;
                    }
                    if (this.regearSetupDone && !this.regearExpectedInv.isEmpty()) {
                        this.doRegear(shulker);
                    } else {
                        String currentKit = this.getCurrentKitName();
                        if (!currentKit.equals("none")) {
                            this.sendMessage("§cFailed to load kit: " + currentKit);
                        } else {
                            this.sendMessage("§cNo kit selected! Use .regear load <name>");
                        }
                    }
                    if (this.autoDisable.getValue() && this.closeAfterRegear.getValue() && !this.keyMode.getValue()) {
                        this.timeoutToDisable();
                    }
                }
            } else if (this.mode.getValue() == Mode.SMART) {
                if (!this.take.getValue()) {
                    if (this.autoDisable.getValue() && !this.keyMode.getValue()) {
                        this.timeoutToDisable();
                    }
                } else {
                    boolean take = false;
                    if (mc.player.currentScreenHandler instanceof ShulkerBoxScreenHandler shulker) {
                        block1: for (Slot slot : shulker.slots) {
                            if (slot.id >= 27 || slot.getStack().isEmpty()) continue;
                            Type type = this.needSteal(slot.getStack());
                            if (this.smart.getValue() && type != Type.QuickMove && (type != Type.Stack || !this.forceMove.getValue())) {
                                if (type != Type.Stack) continue;
                                for (int slot1 = 0; slot1 < 36; ++slot1) {
                                    ItemStack stack = mc.player.getInventory().getStack(slot1);
                                    if (stack.isEmpty() || !stack.isStackable() || stack.getItem() != slot.getStack().getItem() || stack.getCount() >= stack.getMaxCount()) continue;
                                    int i = (slot1 < 9 ? slot1 + 36 : slot1) + 18;
                                    mc.interactionManager.clickSlot(shulker.syncId, slot.id, 0, SlotActionType.PICKUP, mc.player);
                                    mc.interactionManager.clickSlot(shulker.syncId, i, 0, SlotActionType.PICKUP, mc.player);
                                    mc.interactionManager.clickSlot(shulker.syncId, slot.id, 0, SlotActionType.PICKUP, mc.player);
                                    take = true;
                                    continue block1;
                                }
                                continue;
                            }
                            mc.interactionManager.clickSlot(shulker.syncId, slot.id, 0, SlotActionType.QUICK_MOVE, mc.player);
                            take = true;
                        }
                    }
                    if (this.autoDisable.getValue() && !take && !this.keyMode.getValue()) {
                        this.timeoutToDisable();
                    }
                }
            }
        }
    }

    private void timeoutToDisable() {
        if (this.timeoutTimer.passed(this.disableTime.getValueInt())) {
            this.disable();
        }
    }

    private Type needSteal(ItemStack i) {
        if (i.getItem().equals(Items.END_CRYSTAL) && this.stealCountList[0] > 0) {
            this.stealCountList[0] = this.stealCountList[0] - i.getCount();
            return this.stealCountList[0] < 0 ? Type.Stack : Type.QuickMove;
        }
        if (i.getItem().equals(Items.EXPERIENCE_BOTTLE) && this.stealCountList[1] > 0) {
            this.stealCountList[1] = this.stealCountList[1] - i.getCount();
            return this.stealCountList[1] < 0 ? Type.Stack : Type.QuickMove;
        }
        if (i.getItem().equals(Items.TOTEM_OF_UNDYING) && this.stealCountList[2] > 0) {
            this.stealCountList[2] = this.stealCountList[2] - i.getCount();
            return this.stealCountList[2] < 0 ? Type.Stack : Type.QuickMove;
        }
        if (i.getItem().equals(Items.ENCHANTED_GOLDEN_APPLE) && this.stealCountList[3] > 0) {
            this.stealCountList[3] = this.stealCountList[3] - i.getCount();
            return this.stealCountList[3] < 0 ? Type.Stack : Type.QuickMove;
        }
        if (i.getItem().equals(net.minecraft.block.Blocks.OBSIDIAN.asItem()) && this.stealCountList[4] > 0) {
            this.stealCountList[4] = this.stealCountList[4] - i.getCount();
            return this.stealCountList[4] < 0 ? Type.Stack : Type.QuickMove;
        }
        if (i.getItem().equals(net.minecraft.block.Blocks.COBWEB.asItem()) && this.stealCountList[5] > 0) {
            this.stealCountList[5] = this.stealCountList[5] - i.getCount();
            return this.stealCountList[5] < 0 ? Type.Stack : Type.QuickMove;
        }
        if (i.getItem().equals(net.minecraft.block.Blocks.GLOWSTONE.asItem()) && this.stealCountList[6] > 0) {
            this.stealCountList[6] = this.stealCountList[6] - i.getCount();
            return this.stealCountList[6] < 0 ? Type.Stack : Type.QuickMove;
        }
        if (i.getItem().equals(net.minecraft.block.Blocks.RESPAWN_ANCHOR.asItem()) && this.stealCountList[7] > 0) {
            this.stealCountList[7] = this.stealCountList[7] - i.getCount();
            return this.stealCountList[7] < 0 ? Type.Stack : Type.QuickMove;
        }
        if (i.getItem().equals(Items.ENDER_PEARL) && this.stealCountList[8] > 0) {
            this.stealCountList[8] = this.stealCountList[8] - i.getCount();
            return this.stealCountList[8] < 0 ? Type.Stack : Type.QuickMove;
        }
        if (i.getItem() instanceof BlockItem && ((BlockItem)i.getItem()).getBlock() instanceof PistonBlock && this.stealCountList[9] > 0) {
            this.stealCountList[9] = this.stealCountList[9] - i.getCount();
            return this.stealCountList[9] < 0 ? Type.Stack : Type.QuickMove;
        }
        if (i.getItem().equals(net.minecraft.block.Blocks.REDSTONE_BLOCK.asItem()) && this.stealCountList[10] > 0) {
            this.stealCountList[10] = this.stealCountList[10] - i.getCount();
            return this.stealCountList[10] < 0 ? Type.Stack : Type.QuickMove;
        }
        if (i.getItem() instanceof BlockItem && ((BlockItem)i.getItem()).getBlock() instanceof BedBlock && this.stealCountList[11] > 0) {
            this.stealCountList[11] = this.stealCountList[11] - i.getCount();
            return this.stealCountList[11] < 0 ? Type.Stack : Type.QuickMove;
        }
        if (i.getItem() == Items.SPLASH_POTION) {
            PotionContentsComponent potionContentsComponent = i.getOrDefault(DataComponentTypes.POTION_CONTENTS, PotionContentsComponent.DEFAULT);
            for (StatusEffectInstance effect : potionContentsComponent.getEffects()) {
                if (effect.getEffectType().value() == StatusEffects.SPEED.value()) {
                    if (this.stealCountList[12] <= 0) continue;
                    this.stealCountList[12] = this.stealCountList[12] - i.getCount();
                    if (this.stealCountList[12] < 0) {
                        return Type.Stack;
                    }
                    return Type.QuickMove;
                }
                if (effect.getEffectType().value() == StatusEffects.RESISTANCE.value()) {
                    if (this.stealCountList[13] <= 0) continue;
                    this.stealCountList[13] = this.stealCountList[13] - i.getCount();
                    if (this.stealCountList[13] < 0) {
                        return Type.Stack;
                    }
                    return Type.QuickMove;
                }
                if (effect.getEffectType().value() != StatusEffects.STRENGTH.value() || this.stealCountList[14] <= 0) continue;
                this.stealCountList[14] = this.stealCountList[14] - i.getCount();
                if (this.stealCountList[14] < 0) {
                    return Type.Stack;
                }
                return Type.QuickMove;
            }
        }
        return Type.None;
    }

    private void placeBlock(BlockPos pos) {
        AntiRegear.INSTANCE.safe.add(pos);
        BlockUtil.clickBlock(pos.offset(Direction.DOWN), Direction.UP, this.rotate.getValue());
    }

    public static enum Mode {
        SMART("Smart"),
        REGEAR("Regear");

        private final String name;

        private Mode(String name) {
            this.name = name;
        }

        public String toString() {
            return this.name;
        }
    }

    private static enum Type {
        None,
        Stack,
        QuickMove;
    }
}
