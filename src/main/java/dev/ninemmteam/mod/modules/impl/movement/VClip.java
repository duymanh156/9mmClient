package dev.ninemmteam.mod.modules.impl.movement;

import dev.ninemmteam.fentanyl;
import dev.ninemmteam.api.events.eventbus.EventListener;
import dev.ninemmteam.api.events.impl.UpdateEvent;
import dev.ninemmteam.api.utils.math.Timer;
import dev.ninemmteam.api.utils.world.BlockUtil;
import dev.ninemmteam.mod.modules.Module;
import dev.ninemmteam.mod.modules.impl.combat.Burrow;
import dev.ninemmteam.mod.modules.settings.impl.BooleanSetting;
import dev.ninemmteam.mod.modules.settings.impl.EnumSetting;
import dev.ninemmteam.mod.modules.settings.impl.SliderSetting;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;

import java.util.ArrayList;
import java.util.List;

public class VClip extends Module {
    public static VClip INSTANCE;
    
    private final EnumSetting<Mode> mode = this.add(new EnumSetting<>("Mode", Mode.Auto));
    
    private final SliderSetting delay = this.add(new SliderSetting("Delay", 0.3, 0.0, 1.0, 0.1, () -> this.mode.is(Mode.Auto)));
    private final Timer webTimer = new Timer();
    private final SliderSetting webTime = this.add(new SliderSetting("WebTime", 0, 0, 500, () -> this.mode.is(Mode.Auto)));
    private final BooleanSetting checkBurrow = this.add(new BooleanSetting("CheckBurrow", true, () -> this.mode.is(Mode.Auto)));
    private final BooleanSetting fullCheck = this.add(new BooleanSetting("FullCheck", true, () -> this.mode.is(Mode.Auto)).setParent());
    private final SliderSetting fullDegree = this.add(new SliderSetting("FullDegree", 75.0, 0.0, 100.0, 25.0, () -> this.mode.is(Mode.Auto) && this.fullCheck.isOpen()).setSuffix("%"));
    private final SliderSetting clipHeight = this.add(new SliderSetting("ClipHeight", 2.0, 1.0, 3.0, 1.0, () -> this.mode.is(Mode.Auto)).setSuffix(" blocks"));
    private final Timer timer = new Timer();

    public VClip() {
        super("VClip", Category.Movement);
        this.setChinese("防带头穿墙");
        INSTANCE = this;
    }

    @Override
    public String getInfo() {
        return ((Mode)this.mode.getValue()).name();
    }

    public static boolean hasCollision(BlockPos pos) {
        return !mc.world.getBlockState(pos).getCollisionShape(mc.world, pos).isEmpty();
    }

    public static boolean isFullCollisionBlock(BlockPos pos) {
        return !mc.world.getBlockState(pos).getCollisionShape(mc.world, pos).isEmpty() 
            && mc.world.getBlockState(pos).isFullCube(mc.world, pos);
    }

    @EventListener
    public void onUpdate(UpdateEvent event) {
        if (nullCheck()) return;
        
        if (this.mode.is(Mode.Auto)) {
            this.updateAuto();
        } else if (this.mode.is(Mode.Glitch)) {
            this.updateGlitch();
        } else if (this.mode.is(Mode.Teleport)) {
            this.updateTeleport();
        } else if (this.mode.is(Mode.Jump)) {
            this.updateJump();
        } else if (this.mode.is(Mode.Tne)) {
            this.updateTne();
        } else if (this.mode.is(Mode.NCP)) {
            this.updateNCP();
        }
    }

    private void updateAuto() {
        if (fentanyl.PLAYER.isInWeb(mc.player)) {
            this.webTimer.reset();
            return;
        }
        
        if (mc.player.isUsingItem()) {
            return;
        }
        
        if (this.checkBurrow.getValue() && !Burrow.INSTANCE.isOn()) {
            return;
        }
        
        if (!this.timer.passedS(this.delay.getValue())) {
            return;
        }
        
        if (!this.webTimer.passedMs(this.webTime.getValue())) {
            return;
        }
        
        if (!mc.player.isOnGround()) {
            return;
        }
        
        int height = this.clipHeight.getValueInt();
        
        boolean sb = false;
        for (BlockPos pos : getBurrows(0.3)) {
            for (int i = 1; i <= height; i++) {
                if (isFullCollisionBlock(pos.up(1 + i))) {
                    sb = true;
                }
            }
            if (hasCollision(pos.up(2 + height)) || hasCollision(pos.up(3 + height))) {
                return;
            }
        }
        
        if (sb) {
            if (this.fullCheck.getValue()) {
                if (getPlayerTarps() < this.fullDegree.getValue()) {
                    return;
                }
                doClip(height + 1);
                this.timer.reset();
            } else {
                doClip(height + 1);
                this.timer.reset();
            }
        }
    }

    private void updateNCP() {
        if (!mc.player.isOnGround()) {
            return;
        }
        
        int height = this.clipHeight.getValueInt();
        
        boolean canClip = true;
        for (BlockPos pos : getBurrows(0.3)) {
            for (int i = 1; i <= height; i++) {
                if (!isFullCollisionBlock(pos.up(1 + i))) {
                    canClip = false;
                }
            }
            if (hasCollision(pos.up(2 + height))) {
                canClip = false;
            }
        }
        
        if (canClip) {
            doNCPClip(height + 1);
            this.disable();
        }
    }

    private void doClip(int blocks) {
        double y = mc.player.getY() + blocks;
        mc.player.setPosition(mc.player.getX(), y, mc.player.getZ());
        mc.getNetworkHandler().sendPacket(new PlayerMoveC2SPacket.PositionAndOnGround(mc.player.getX(), y, mc.player.getZ(), true));
    }

    private void doNCPClip(int blocks) {
        double x = mc.player.getX();
        double y = mc.player.getY();
        double z = mc.player.getZ();
        
        mc.getNetworkHandler().sendPacket(new PlayerMoveC2SPacket.PositionAndOnGround(x, y + 0.4199999868869781, z, false));
        mc.getNetworkHandler().sendPacket(new PlayerMoveC2SPacket.PositionAndOnGround(x, y + 0.7531999805212017, z, false));
        mc.getNetworkHandler().sendPacket(new PlayerMoveC2SPacket.PositionAndOnGround(x, y + 1.001335979112147, z, false));
        
        double targetY = y + blocks;
        mc.player.setPosition(x, targetY, z);
        mc.getNetworkHandler().sendPacket(new PlayerMoveC2SPacket.PositionAndOnGround(x, targetY, z, true));
    }

    private void updateGlitch() {
        double posX = mc.player.getX();
        double posY = Math.round(mc.player.getY());
        double posZ = mc.player.getZ();
        boolean onGround = mc.player.isOnGround();
        mc.getNetworkHandler().sendPacket(new PlayerMoveC2SPacket.PositionAndOnGround(posX, posY, posZ, onGround));
        double halfY = 0.005;
        posY -= halfY;
        mc.player.setPosition(posX, posY, posZ);
        mc.getNetworkHandler().sendPacket(new PlayerMoveC2SPacket.PositionAndOnGround(posX, posY, posZ, onGround));
        posY -= halfY * 300.0;
        mc.player.setPosition(posX, posY, posZ);
        mc.getNetworkHandler().sendPacket(new PlayerMoveC2SPacket.PositionAndOnGround(posX, posY, posZ, onGround));
        this.disable();
    }

    private void updateTeleport() {
        mc.player.setPosition(mc.player.getX(), mc.player.getY() + 3.0, mc.player.getZ());
        mc.getNetworkHandler().sendPacket(new PlayerMoveC2SPacket.PositionAndOnGround(mc.player.getX(), mc.player.getY(), mc.player.getZ(), true));
        this.disable();
    }

    private void updateJump() {
        mc.getNetworkHandler().sendPacket(new PlayerMoveC2SPacket.PositionAndOnGround(mc.player.getX(), mc.player.getY() + 0.4199999868869781, mc.player.getZ(), false));
        mc.getNetworkHandler().sendPacket(new PlayerMoveC2SPacket.PositionAndOnGround(mc.player.getX(), mc.player.getY() + 0.7531999805212017, mc.player.getZ(), false));
        mc.player.setPosition(mc.player.getX(), mc.player.getY() + 1.0, mc.player.getZ());
        mc.getNetworkHandler().sendPacket(new PlayerMoveC2SPacket.PositionAndOnGround(mc.player.getX(), mc.player.getY(), mc.player.getZ(), true));
        this.disable();
    }

    private void updateTne() {
        if (!fentanyl.nightly) return;
        
        BlockPos playerPos = mc.player.getBlockPos();
        BlockPos headPos = playerPos.up(2);
        List<BlockPos> candidatePositions = new ArrayList<>();

        for (int x = -1; x <= 1; x++) {
            for (int z = -1; z <= 1; z++) {
                if (x == 0 && z == 0) continue;
                BlockPos checkPos = headPos.add(x, 0, z);
                if (hasBlock(checkPos)) {
                    candidatePositions.add(checkPos.up());
                }
            }
        }
        
        if (hasBlock(headPos)) {
            candidatePositions.add(headPos.up());
        }
        
        for (BlockPos pos : candidatePositions) {
            if (!hasBlock(pos) && !hasBlock(pos.up()) && !hasBlock(pos.up(2))) {
                teleport(pos);
                disable();
                return;
            }
        }
        this.disable();
    }

    private boolean hasBlock(BlockPos pos) {
        return !mc.world.isAir(pos);
    }

    private void teleport(BlockPos pos) {
        if (hasBlock(pos) || hasBlock(pos.up()) || hasBlock(pos.up(2))) {
            return;
        }
        double x = pos.getX() + 0.5;
        double y = pos.getY();
        double z = pos.getZ() + 0.5;
        mc.player.setPosition(x, y, z);
        mc.getNetworkHandler().sendPacket(new PlayerMoveC2SPacket.PositionAndOnGround(x, y, z, true));
    }

    private List<BlockPos> getBurrows(double range) {
        List<BlockPos> list = new ArrayList<>();
        Vec3d playerPos = mc.player.getPos();
        for (double x = playerPos.x - range; x <= playerPos.x + range; x += range * 2) {
            for (double z = playerPos.z - range; z <= playerPos.z + range; z += range * 2) {
                list.add(new BlockPos((int)Math.floor(x), (int)Math.floor(playerPos.y), (int)Math.floor(z)));
            }
        }
        return list;
    }

    private double getPlayerTarps() {
        int traps = 0;
        for (float x : new float[]{0.29f, -0.29f}) {
            for (float z : new float[]{0.29f, -0.29f}) {
                BlockPos pos = new BlockPos((int)Math.floor(mc.player.getX() + x), (int)Math.floor(mc.player.getY() + 2), (int)Math.floor(mc.player.getZ() + z));
                if (hasCollision(pos)) {
                    traps += 25;
                }
            }
        }
        return traps;
    }

    public enum Mode {
        Auto,
        Glitch,
        Teleport,
        Tne,
        Jump,
        NCP
    }
}
