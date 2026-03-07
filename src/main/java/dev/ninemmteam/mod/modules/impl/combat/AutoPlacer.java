package dev.ninemmteam.mod.modules.impl.combat;

import dev.ninemmteam.fentanyl;
import dev.ninemmteam.api.events.eventbus.EventListener;
import dev.ninemmteam.api.events.impl.ClientTickEvent;
import dev.ninemmteam.api.events.impl.CollisionBoxEvent;
import dev.ninemmteam.api.events.impl.PacketEvent;
import dev.ninemmteam.api.events.impl.TickEvent;
import dev.ninemmteam.api.utils.combat.CombatUtil;
import dev.ninemmteam.api.utils.math.Timer;
import dev.ninemmteam.api.utils.player.InventoryUtil;
import dev.ninemmteam.api.utils.render.Render3DUtil;
import dev.ninemmteam.api.utils.world.BlockUtil;
import dev.ninemmteam.api.utils.world.CrystalUtil;
import dev.ninemmteam.core.impl.CrystalManager;
import dev.ninemmteam.core.impl.PacketManager;
import dev.ninemmteam.core.impl.PriorityManager;
import dev.ninemmteam.mod.modules.Module;
import dev.ninemmteam.mod.modules.impl.client.AntiCheat;
import dev.ninemmteam.mod.modules.impl.misc.AutoBreak;
import dev.ninemmteam.mod.modules.settings.impl.BooleanSetting;
import dev.ninemmteam.mod.modules.settings.impl.ColorSetting;
import dev.ninemmteam.mod.modules.settings.impl.SliderSetting;
import java.awt.Color;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import net.minecraft.block.Blocks;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Items;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket;
import net.minecraft.network.packet.s2c.play.BlockUpdateS2CPacket;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.shape.VoxelShapes;

public class AutoPlacer extends Module {
    public static AutoPlacer INSTANCE;

    Timer timer = new Timer();
    private final SliderSetting delay = this.add(new SliderSetting("Delay", 0, 0, 1000, 1).setSuffix("ms"));
    public final SliderSetting minDmg = this.add(new SliderSetting("MinDamage", 7, 0, 36, 1).setSuffix("dmg"));
    private final SliderSetting targetRange = this.add(new SliderSetting("TargetRange", 5.0, 1.0, 10.0, 0.1).setSuffix("m"));
    public final SliderSetting range = this.add(new SliderSetting("Range", 6.0, 3.0, 6.0, 0.1).setSuffix("m"));
    private final SliderSetting blocksPerTick = this.add(new SliderSetting("Blocks", 10, 1, 10, 1));
    private final BooleanSetting rotate = this.add(new BooleanSetting("Rotate", false));
    private final BooleanSetting strictDirection = this.add(new BooleanSetting("Strict", false));
    private final BooleanSetting eatingPause = this.add(new BooleanSetting("EatingPause", true));

    public final BooleanSetting render = this.add(new BooleanSetting("Render", true).setParent());
    public final ColorSetting fill = this.add(new ColorSetting("Fill", new Color(255, 0, 0, 25), this.render::isOpen).injectBoolean(true));
    public final ColorSetting line = this.add(new ColorSetting("Line", new Color(255, 0, 0, 255), this.render::isOpen).injectBoolean(true));
    public final SliderSetting fadeTime = this.add(new SliderSetting("FadeTime", 200, 0, 1000, 1, this.render::isOpen).setSuffix("ms"));

    public Map<BlockPos, Long> renderPositions = new HashMap<>();

    Entity target;
    List<BlockPos> toPlace = new ArrayList<>();

    public Map<BlockPos, Long> placed = new HashMap<>();

    double startY = 0;

    boolean rotateFlag = false;

    public AutoPlacer() {
        super("AutoPlacer", Module.Category.Combat);
        this.setChinese("自动铺垫");
        INSTANCE = this;
    }

    @EventListener
    public void onPlayerUpdatePre(TickEvent event) {
        if (nullCheck()) return;
        if (!event.isPre()) return;

        if (PriorityManager.INSTANCE.isUsageLocked() && !Objects.equals(PriorityManager.INSTANCE.usageLockCause, "AutoPlacer")) {
            return;
        }

        if (AutoBreak.INSTANCE != null && AutoBreak.INSTANCE.didAction) return;

        if (eatingPause.getValue() && mc.player.isUsingItem()) return;

        if (getSlot() == -1) {
            return;
        }

        Iterator<Map.Entry<BlockPos, Long>> iterator = placed.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<BlockPos, Long> entry = iterator.next();
            BlockPos pos = entry.getKey();
            long time = entry.getValue();

            if (System.currentTimeMillis() - time > 200) {
                iterator.remove();
            }
        }

        toPlace.clear();
        int blocksInTick = 0;

        target = CombatUtil.getClosestEnemy(targetRange.getValueFloat());

        if (target != null) {
            BlockPos targetPos = getBestCrystalPlacePos((PlayerEntity) target);

            if (targetPos != null && mc.world.getBlockState(targetPos).isReplaceable()) {
                placeLabel:
                if (timer.passed(delay.getValueInt())) {
                    if (blocksInTick >= blocksPerTick.getValueInt()) break placeLabel;

                    if (toPlace.contains(targetPos)) break placeLabel;

                    if (placed.containsKey(targetPos)) {
                        if (CrystalManager.INSTANCE != null && CrystalManager.INSTANCE.isRecentlyBlocked(targetPos)) {
                            placed.remove(targetPos);
                        } else if (System.currentTimeMillis() - placed.get(targetPos) < 60) {
                            break placeLabel;
                        }
                    }

                    if (blocksInTick == 0 && rotate.getValue() && !AntiCheat.INSTANCE.protocol.getValue()) {
                        doRotate(targetPos, strictDirection.getValue());
                    }

                    toPlace.add(targetPos);
                    PriorityManager.INSTANCE.lockUsageLock("AutoPlacer");
                    blocksInTick++;
                    timer.resetDelay();
                }
            }
        }

        if (blocksInTick == 0) {
            PriorityManager.INSTANCE.unlockUsageLock();
        }

        if (AntiCheat.INSTANCE.protocol.getValue()) {
            doPlace();
        }
    }

    @EventListener
    public void onPlayerUpdatePost(TickEvent event) {
        if (nullCheck()) return;
        if (!event.isPost()) return;

        if (!AntiCheat.INSTANCE.protocol.getValue()) {
            doPlace();
        }
    }

    public BlockPos getBestCrystalPlacePos(PlayerEntity player) {
        BlockPos bestPos = null;
        double bestDMG = 0.5D;

        final List<BlockPos> sphere = BlockUtil.sphere(range.getValue() + 1.0f, mc.player.getBlockPos(), true, false);
        Set<BlockPos> keySet = new HashSet<>(Set.copyOf(placed.keySet()));
        keySet.addAll(toPlace);

        for (BlockPos pos : sphere) {
            BlockPos basePos = pos.down();

            if (!CrystalUtil.canPlaceCrystal(basePos, CatAura.INSTANCE.onePointTwelve.getValue()) && !BlockUtil.isReplaceable(basePos)) {
                continue;
            }

            if (basePos.getY() >= player.getBlockPos().getY()) {
                continue;
            }

            if (!CrystalUtil.canPlaceCrystalAir(basePos)) continue;

            if (BlockUtil.isBlockedOff(basePos) || BlockUtil.isBlockedOff(pos)) continue;

            if (BlockUtil.isReplaceable(basePos)) {
                if (!BlockUtil.canPlaceBlock(basePos, strictDirection.getValue(), keySet)) continue;
            }

            double distance = mc.player.getEyePos().squaredDistanceTo(new Vec3d(basePos.getX() + 0.5, basePos.getY() + 0.5, basePos.getZ() + 0.5));
            if (distance > range.getValue() * range.getValue()) {
                continue;
            }

            double dmg = CrystalUtil.calculateDamage(player, pos.toCenterPos(), CatAura.INSTANCE.terrain.getValue(), CatAura.INSTANCE.getMiningIgnore());
            if (dmg < minDmg.getValue()) {
                continue;
            }

            if (dmg > bestDMG) {
                bestPos = basePos;
                bestDMG = dmg;
            }
        }
        return bestPos;
    }

    private void doRotate(BlockPos pos, boolean strictDirection) {
        Direction side = BlockUtil.getPlaceableSide(pos, strictDirection, placed.keySet());
        if (side != null) {
            float[] rots = getBlockRotations(pos, side);
            if (rots != null) {
                fentanyl.ROTATION.lookAt(pos.toCenterPos().add(side.getVector().getX() * 0.5, side.getVector().getY() * 0.5, side.getVector().getZ() * 0.5));
            }
        }
    }

    private void doSilentRotate(BlockPos pos, boolean strictDirection) {
        Direction side = BlockUtil.getPlaceableSide(pos, strictDirection, placed.keySet());
        if (side != null) {
            float[] rots = getBlockRotations(pos, side);
            if (rots != null) {
                PacketManager.INSTANCE.sendPacket(new PlayerMoveC2SPacket.Full(
                    mc.player.getX(), mc.player.getY(), mc.player.getZ(), rots[0], rots[1], mc.player.isOnGround()));
            }
        }
    }

    private float[] getBlockRotations(BlockPos pos, Direction facing) {
        Vec3d hitVec = pos.toCenterPos().add(new Vec3d(facing.getVector().getX() * 0.5, facing.getVector().getY() * 0.5, facing.getVector().getZ() * 0.5));
        return getRotationsTo(mc.player.getEyePos(), hitVec);
    }

    private float[] getRotationsTo(Vec3d src, Vec3d dest) {
        double diffX = dest.x - src.x;
        double diffY = dest.y - src.y;
        double diffZ = dest.z - src.z;
        double dist = Math.sqrt(diffX * diffX + diffZ * diffZ);

        float yaw = (float) (Math.atan2(diffZ, diffX) * 180.0 / Math.PI) - 90.0f;
        float pitch = (float) (-(Math.atan2(diffY, dist) * 180.0 / Math.PI));

        yaw = fixYaw(yaw);

        return new float[]{MathHelper.wrapDegrees(yaw), MathHelper.wrapDegrees(pitch)};
    }

    private float fixYaw(float yaw) {
        float prevYaw = fentanyl.ROTATION.getLastYaw();
        float diff = yaw - prevYaw;

        if (diff < -180.0f || diff > 180.0f) {
            float round = Math.round(Math.abs(diff / 360.0f));
            diff = diff < 0.0f ? diff + 360.0f * round : diff - (360.0f * round);
        }
        return yaw;
    }

    private void silentSync() {
        float yaw = fentanyl.ROTATION.getLastYaw();
        float pitch = fentanyl.ROTATION.getLastPitch();
        PacketManager.INSTANCE.sendPacket(new PlayerMoveC2SPacket.Full(
            mc.player.getX(), mc.player.getY(), mc.player.getZ(), yaw, pitch, mc.player.isOnGround()));
    }

    public void doPlace() {
        if (nullCheck()) return;

        if (toPlace.isEmpty()) {
            PriorityManager.INSTANCE.unlockUsageLock();
            return;
        }

        int blockSlot = getSlot();
        if (blockSlot == -1) {
            PriorityManager.INSTANCE.unlockUsageLock();
            return;
        }

        int oldSlot = mc.player.getInventory().selectedSlot;
        boolean switched = false;

        for (BlockPos pos : toPlace) {
            if (blockSlot != mc.player.getInventory().selectedSlot) {
                InventoryUtil.switchToSlot(blockSlot);
                switched = true;
            }

            placed.put(pos, System.currentTimeMillis());

            if (rotate.getValue() && AntiCheat.INSTANCE.protocol.getValue()) {
                doSilentRotate(pos, strictDirection.getValue());
            }

            Direction side = BlockUtil.getPlaceableSide(pos, strictDirection.getValue(), placed.keySet());
            if (side != null) {
                BlockUtil.placeBlock(pos, side, !mc.player.getMainHandStack().getItem().equals(Items.ENDER_CHEST));
            }

            if (render.getValue()) {
                renderPositions.put(pos, System.currentTimeMillis());
            }
        }

        if (switched) {
            InventoryUtil.switchToSlot(oldSlot);
        }

        if ((!toPlace.isEmpty() && AntiCheat.INSTANCE.protocol.getValue() && rotate.getValue()) || rotateFlag) {
            silentSync();
        }

        toPlace.clear();
        PriorityManager.INSTANCE.unlockUsageLock();
    }

    int getSlot() {
        return InventoryUtil.findBlock(Blocks.OBSIDIAN);
    }

    @EventListener
    public void onPacket(PacketEvent.Receive event) {
        if (nullCheck()) return;

        if (event.getPacket() instanceof BlockUpdateS2CPacket packet) {
            final BlockPos targetPos = packet.getPos();
            if (placed.containsKey(targetPos)) {
                placed.remove(targetPos);
            }
        }
    }

    @EventListener
    public void onCollision(CollisionBoxEvent event) {
        if (nullCheck()) return;

        if (placed.containsKey(event.getPos())) {
            event.cancel();
            event.setVoxelShape(VoxelShapes.cuboid(new Box(0, 0, 0, 1.0, 1.0, 1.0)));
        }
    }

    @Override
    public void onEnable() {
        super.onEnable();
        if (nullCheck()) return;

        startY = mc.player.getY();
        placed.clear();
    }

    @Override
    public void onDisable() {
        super.onDisable();
        if (nullCheck()) return;

        if (PriorityManager.INSTANCE.isUsageLocked() && PriorityManager.INSTANCE.usageLockCause != null && PriorityManager.INSTANCE.usageLockCause.equals("AutoPlacer")) {
            PriorityManager.INSTANCE.unlockUsageLock();
        }

        placed.clear();
    }

    @Override
    public void onRender3D(MatrixStack matrixStack) {
        if (!render.getValue()) return;

        long currentTime = System.currentTimeMillis();
        renderPositions.entrySet().removeIf(entry -> currentTime - entry.getValue() > fadeTime.getValueInt());

        for (Map.Entry<BlockPos, Long> entry : renderPositions.entrySet()) {
            BlockPos pos = entry.getKey();
            long time = entry.getValue();
            long timePassed = currentTime - time;
            float alpha = 1.0f - (float) timePassed / (float) fadeTime.getValueInt();
            alpha = Math.max(0, Math.min(1, alpha));

            Box box = new Box(pos);

            if (fill.booleanValue) {
                Color fillColor = fill.getValue();
                Color adjustedFill = new Color(fillColor.getRed(), fillColor.getGreen(), fillColor.getBlue(), (int)(fillColor.getAlpha() * alpha));
                Render3DUtil.drawFill(matrixStack, box, adjustedFill);
            }

            if (line.booleanValue) {
                Color lineColor = line.getValue();
                Color adjustedLine = new Color(lineColor.getRed(), lineColor.getGreen(), lineColor.getBlue(), (int)(lineColor.getAlpha() * alpha));
                Render3DUtil.drawBox(matrixStack, box, adjustedLine);
            }
        }
    }

    @Override
    public String getInfo() {
        return target != null ? target.getName().getString() : null;
    }
}
