package dev.ninemmteam.mod.modules.impl.combat;

import dev.ninemmteam.fentanyl;
import dev.ninemmteam.api.events.eventbus.EventListener;
import dev.ninemmteam.api.events.impl.ClientTickEvent;
import dev.ninemmteam.api.events.impl.MoveEvent;
import dev.ninemmteam.api.events.impl.PacketEvent;
import dev.ninemmteam.api.utils.math.MathUtil;
import dev.ninemmteam.mod.modules.Module;
import dev.ninemmteam.mod.modules.settings.impl.BooleanSetting;
import dev.ninemmteam.mod.modules.settings.impl.SliderSetting;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Items;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket;
import net.minecraft.network.packet.s2c.play.PlayerPositionLookS2CPacket;
import net.minecraft.util.Hand;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;

public class AutoMace extends Module {
    public static AutoMace INSTANCE;
    public final SliderSetting targetRange = this.add(new SliderSetting("Target Range", 10, 10, 30, 1));
    public final SliderSetting attackRange = this.add(new SliderSetting("Attack Range", 6, 3, 6, 0.1));
    public final BooleanSetting rotate = this.add(new BooleanSetting("Rotate", false));
    public final BooleanSetting track = this.add(new BooleanSetting("Track", true));
    public final BooleanSetting rubberband = this.add(new BooleanSetting("Lagback", true));

    private PlayerEntity target = null;
    private long lastAttackTime = 0;
    private boolean reset = false;

    public AutoMace() {
        super("AutoMace", "AutoMace: maces people after falling", Category.Combat);
        INSTANCE = this;
    }

    @Override
    public void onEnable() {
        super.onEnable();
        if (nullCheck()) return;
        
        if (isElytraEquipped())
            if (mc.player.isFallFlying()) mc.player.stopFallFlying();
    }

    @Override
    public void onDisable() {
        super.onDisable();
        target = null;
        lastAttackTime = 0;
        reset = false;
    }

    @EventListener
    public void onTick(ClientTickEvent event) {
        if (nullCheck()) return;
        
        target = getTarget();

        if (target == null || mc.player.distanceTo(target) > attackRange.getValue()) return;

        int slot = getHotbarItemSlot(Items.MACE);
        if (slot == -1) return;

        if (rotate.getValue()) {
            Vec3d hitVec = MathUtil.getClosestPointToBox(mc.player.getEyePos(), target.getBoundingBox());
            fentanyl.ROTATION.lookAt(hitVec);
        }

        if (System.currentTimeMillis() - lastAttackTime > 1000) {
            attackTarget(slot);
            lastAttackTime = System.currentTimeMillis();
        }
    }

    @EventListener
    public void onPacketReceive(PacketEvent.Receive event) {
        if (event.getPacket() instanceof PlayerPositionLookS2CPacket && reset) {
            mc.player.networkHandler.sendPacket(new PlayerMoveC2SPacket.OnGroundOnly(mc.player.isOnGround()));
            reset = false;
        }
    }

    @EventListener
    public void onMove(MoveEvent event) {
        if (!track.getValue() || nullCheck() || mc.player.isOnGround() || target == null) return;
        if (mc.player.distanceTo(target) <= attackRange.getValue()) return;
        if (System.currentTimeMillis() - lastAttackTime < 200) return;

        if (mc.player.squaredDistanceTo(target.getX(), mc.player.getY(), target.getZ()) > 36.0) {
            if (!mc.player.isFallFlying()) {
                equipElytra();
            }
        } else {
            if (mc.player.isFallFlying()) {
                disEquipElytra();
            }
        }
        
        Vec3d playerPos = mc.player.getPos();
        Vec3d targetPos = target.getPos();
        double dX = targetPos.x - playerPos.x;
        double dZ = targetPos.z - playerPos.z;
        double dist = Math.sqrt(dX * dX + dZ * dZ);
        
        if (dist > 0) {
            dX /= dist;
            dZ /= dist;
            
            double speed = mc.player.isFallFlying() ? 0.15 : 0.2873;
            event.setX(dX * speed);
            event.setZ(dZ * speed);
        }
    }

    private void attackTarget(int slot) {
        int previousSlot = mc.player.getInventory().selectedSlot;
        Vec3d previous = mc.player.getPos();
        
        if (slot != previousSlot)
            mc.player.getInventory().selectedSlot = slot;
        
        mc.interactionManager.attackEntity(mc.player, target);
        mc.player.swingHand(Hand.MAIN_HAND);

        if (slot != previousSlot)
            mc.player.getInventory().selectedSlot = previousSlot;

        if (rubberband.getValue()) {
            mc.player.networkHandler.sendPacket(new PlayerMoveC2SPacket.OnGroundOnly(false));
            mc.player.networkHandler.sendPacket(new PlayerMoveC2SPacket.PositionAndOnGround(mc.player.getX(), mc.player.getY() + 1, mc.player.getZ(), false));
            mc.player.networkHandler.sendPacket(new PlayerMoveC2SPacket.PositionAndOnGround(previous.x, previous.y, previous.z, false));
            reset = true;
        }

        if (rotate.getValue()) {
            fentanyl.ROTATION.snapBack();
        }
    }

    private PlayerEntity getTarget() {
        PlayerEntity optimalTarget = null;
        double closestDistance = Double.MAX_VALUE;
        
        for (PlayerEntity player : mc.world.getPlayers()) {
            if (player == mc.player) continue;
            if (!player.isAlive() || player.getHealth() <= 0.0f) continue;
            if (mc.player.squaredDistanceTo(player) > MathHelper.square(targetRange.getValue())) continue;
            
            double distance = mc.player.distanceTo(player);
            if (distance < closestDistance) {
                closestDistance = distance;
                optimalTarget = player;
            }
        }
        
        return optimalTarget;
    }

    private int getHotbarItemSlot(net.minecraft.item.Item item) {
        for (int i = 0; i < 9; i++) {
            if (mc.player.getInventory().getStack(i).getItem() == item) {
                return i;
            }
        }
        return -1;
    }

    private boolean isElytraEquipped() {
        return mc.player.getInventory().getArmorStack(2).getItem() == Items.ELYTRA;
    }

    private void equipElytra() {
        for (int i = 0; i < 4; i++) {
            if (mc.player.getInventory().getArmorStack(i).getItem() == Items.ELYTRA) {
                mc.player.getInventory().armor.set(2, mc.player.getInventory().getArmorStack(i));
                mc.player.getInventory().armor.set(i, net.minecraft.item.ItemStack.EMPTY);
                break;
            }
        }
    }

    private void disEquipElytra() {
        for (int i = 0; i < 4; i++) {
            if (i != 2 && mc.player.getInventory().getArmorStack(i).isEmpty()) {
                mc.player.getInventory().armor.set(i, mc.player.getInventory().getArmorStack(2));
                mc.player.getInventory().armor.set(2, net.minecraft.item.ItemStack.EMPTY);
                break;
            }
        }
    }
}