package dev.ninemmteam.mod.modules.impl.combat;

import dev.ninemmteam.fentanyl;
import dev.ninemmteam.api.events.eventbus.EventListener;
import dev.ninemmteam.api.events.impl.ClientTickEvent;
import dev.ninemmteam.api.utils.math.Timer;
import dev.ninemmteam.api.utils.player.EntityUtil;
import dev.ninemmteam.api.utils.player.InventoryUtil;
import dev.ninemmteam.api.utils.world.BlockUtil;
import dev.ninemmteam.mod.modules.Module;
import dev.ninemmteam.mod.modules.impl.exploit.Blink;
import dev.ninemmteam.mod.modules.impl.misc.FakePlayer;
import dev.ninemmteam.mod.modules.settings.impl.BooleanSetting;
import dev.ninemmteam.mod.modules.settings.impl.SliderSetting;
import net.minecraft.block.*;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.math.*;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class AutoSquare extends Module {
    public static AutoSquare INSTANCE;

    private final BooleanSetting autoDisable = add(new BooleanSetting("AutoDisable", true));
    private final BooleanSetting rotate = add(new BooleanSetting("Rotation", true));
    private final BooleanSetting yawDeceive = add(new BooleanSetting("YawDeceive", true));
    private final BooleanSetting inventory = add(new BooleanSetting("InventorySwap", true));
    private final SliderSetting range = add(new SliderSetting("Range", 5.0, 0.0, 6.0));
    private final SliderSetting placeRange = add(new SliderSetting("PlaceRange", 5.0, 0.0, 6.0));
    private final SliderSetting delay = add(new SliderSetting("Delay", 50, 0, 1000));

    private final Timer timer = new Timer();
    private PlayerEntity target = null;
    private int step = 0;
    private BlockPos pistonPos = null;
    private BlockPos powerPos = null;
    private BlockPos debrisPos = null;

    public AutoSquare() {
        super("AutoSquare", Category.Combat);
        setChinese("压方块");
        INSTANCE = this;
    }

    @Override
    public void onEnable() {
        target = null;
        step = 0;
        pistonPos = null;
        powerPos = null;
        debrisPos = null;
        timer.reset();
        
        checkItems();
    }

    private void checkItems() {
        if (mc.player == null) return;
        
        List<String> missing = new ArrayList<>();
        
        int piston = inventory.getValue() 
            ? InventoryUtil.findClassInventorySlot(PistonBlock.class) 
            : InventoryUtil.findClass(PistonBlock.class);
        if (piston == -1) {
            missing.add("§c活塞");
        }
        
        int power = inventory.getValue() 
            ? InventoryUtil.findBlockInventorySlot(Blocks.REDSTONE_BLOCK) 
            : InventoryUtil.findBlock(Blocks.REDSTONE_BLOCK);
        if (power == -1) {
            missing.add("§c红石块");
        }
        
        int debris = inventory.getValue() 
            ? InventoryUtil.findBlockInventorySlot(Blocks.ANCIENT_DEBRIS) 
            : InventoryUtil.findBlock(Blocks.ANCIENT_DEBRIS);
        if (debris == -1) {
            missing.add("§c远古残骸");
        }
        
        mc.player.sendMessage(Text.of("§e[AutoSquare] §f物品检查:"), false);
        
        if (missing.isEmpty()) {
            mc.player.sendMessage(Text.of("§a  ✓ 所有物品齐全"), false);
        } else {
            mc.player.sendMessage(Text.of("§e  缺少物品: " + String.join("§f, ", missing)), false);
        }
    }

    @EventListener
    public void onTick(ClientTickEvent event) {
        if (mc.player == null || mc.world == null) return;
        if (!event.isPre()) return;
        if (!timer.passedMs(delay.getValue())) return;
        if (Blink.INSTANCE.isOn()) return;

        int pistonSlot = findPiston();
        int powerSlot = findPower();
        int debrisSlot = findDebris();
        
        if (pistonSlot == -1 || powerSlot == -1 || debrisSlot == -1) {
            return;
        }

        if (step == 0) {
            target = findTarget();
            if (target == null) return;

            BlockPos targetPos = BlockPos.ofFloored(target.getPos());
            
            for (Direction dir : Direction.Type.HORIZONTAL) {
                BlockPos pPos = targetPos.offset(dir);
                BlockPos dPos = targetPos.offset(dir.getOpposite());
                
                if (BlockUtil.canPlace(pPos, placeRange.getValue()) && BlockUtil.canPlace(dPos, placeRange.getValue())) {
                    BlockPos powPos = findPowerPos(pPos, dPos);
                    if (powPos != null && BlockUtil.canPlace(powPos, placeRange.getValue())) {
                        pistonPos = pPos;
                        debrisPos = dPos;
                        powerPos = powPos;
                        step = 1;
                        timer.reset();
                        return;
                    }
                }
            }
            
            for (Direction dir : Direction.Type.HORIZONTAL) {
                BlockPos dPos = targetPos.offset(dir);
                BlockPos pPos = targetPos.offset(dir, 2);
                
                if (BlockUtil.canPlace(pPos, placeRange.getValue()) && BlockUtil.canPlace(dPos, placeRange.getValue())) {
                    BlockPos powPos = findPowerPos(pPos, dPos);
                    if (powPos != null && BlockUtil.canPlace(powPos, placeRange.getValue())) {
                        pistonPos = pPos;
                        debrisPos = dPos;
                        powerPos = powPos;
                        step = 1;
                        timer.reset();
                        return;
                    }
                }
            }
            return;
        }

        if (step == 1) {
            placeBlock(debrisPos, debrisSlot);
            step = 2;
            timer.reset();
            return;
        }

        if (step == 2) {
            if (yawDeceive.getValue() && pistonPos != null && debrisPos != null) {
                Direction pushDir = getDirection(pistonPos, debrisPos);
                if (pushDir != null) {
                    setPistonYaw(pushDir);
                }
            }
            placeBlock(pistonPos, pistonSlot);
            step = 3;
            timer.reset();
            return;
        }

        if (step == 3) {
            placeBlock(powerPos, powerSlot);
            step = 4;
            timer.reset();
            return;
        }

        if (step == 4) {
            if (autoDisable.getValue()) {
                disable();
            }
        }
    }

    private Direction getDirection(BlockPos from, BlockPos to) {
        int dx = to.getX() - from.getX();
        int dz = to.getZ() - from.getZ();
        
        if (Math.abs(dx) > Math.abs(dz)) {
            return dx > 0 ? Direction.EAST : Direction.WEST;
        } else {
            return dz > 0 ? Direction.SOUTH : Direction.NORTH;
        }
    }

    private BlockPos findPowerPos(BlockPos piston, BlockPos exclude) {
        for (Direction d : Direction.values()) {
            BlockPos pos = piston.offset(d);
            if (!pos.equals(exclude) && BlockUtil.canPlace(pos, placeRange.getValue())) {
                return pos;
            }
        }
        return null;
    }

    private void placeBlock(BlockPos pos, int slot) {
        if (pos == null || slot == -1) return;
        if (!mc.world.getBlockState(pos).isReplaceable()) return;
        
        int oldSlot = mc.player.getInventory().selectedSlot;
        
        if (inventory.getValue()) {
            InventoryUtil.inventorySwap(slot, mc.player.getInventory().selectedSlot);
        } else {
            InventoryUtil.switchToSlot(slot);
        }
        
        if (rotate.getValue()) {
            Direction side = BlockUtil.getPlaceSide(pos);
            if (side != null) {
                fentanyl.ROTATION.lookAt(pos.offset(side), side.getOpposite());
            }
        }
        
        BlockUtil.placeBlock(pos, rotate.getValue(), false);
        
        if (inventory.getValue()) {
            InventoryUtil.inventorySwap(slot, mc.player.getInventory().selectedSlot);
            EntityUtil.syncInventory();
        } else {
            InventoryUtil.switchToSlot(oldSlot);
        }
    }

    private void setPistonYaw(Direction dir) {
        float yaw = switch (dir) {
            case EAST -> -90.0f;
            case WEST -> 90.0f;
            case NORTH -> 180.0f;
            case SOUTH -> 0.0f;
            default -> mc.player.getYaw();
        };
        fentanyl.ROTATION.snapAt(yaw, 5.0f);
    }

    private PlayerEntity findTarget() {
        List<PlayerEntity> players = new ArrayList<>();
        
        for (PlayerEntity player : mc.world.getPlayers()) {
            if (player == mc.player) continue;
            if (player.isDead() || player.getHealth() <= 0) continue;
            
            double dist = mc.player.squaredDistanceTo(player);
            if (dist <= range.getValue() * range.getValue()) {
                players.add(player);
            }
        }
        
        if (FakePlayer.INSTANCE.isOn() && FakePlayer.fakePlayer != null) {
            PlayerEntity fp = FakePlayer.fakePlayer;
            if (!fp.isDead() && fp.getHealth() > 0) {
                double dist = mc.player.squaredDistanceTo(fp);
                if (dist <= range.getValue() * range.getValue()) {
                    players.add(fp);
                }
            }
        }
        
        if (players.isEmpty()) return null;
        
        players.sort(Comparator.comparingDouble(p -> mc.player.squaredDistanceTo(p)));
        return players.get(0);
    }

    private int findPiston() {
        return inventory.getValue() 
            ? InventoryUtil.findClassInventorySlot(PistonBlock.class) 
            : InventoryUtil.findClass(PistonBlock.class);
    }

    private int findPower() {
        return inventory.getValue() 
            ? InventoryUtil.findBlockInventorySlot(Blocks.REDSTONE_BLOCK) 
            : InventoryUtil.findBlock(Blocks.REDSTONE_BLOCK);
    }

    private int findDebris() {
        return inventory.getValue() 
            ? InventoryUtil.findBlockInventorySlot(Blocks.ANCIENT_DEBRIS) 
            : InventoryUtil.findBlock(Blocks.ANCIENT_DEBRIS);
    }

    @Override
    public String getInfo() {
        if (target != null) {
            return target.getName().getString() + " [" + step + "/4]";
        }
        return null;
    }
}
