package dev.ninemmteam.mod.modules.impl.movement;

import dev.ninemmteam.fentanyl;
import dev.ninemmteam.api.events.eventbus.EventListener;
import dev.ninemmteam.api.events.impl.MoveEvent;
import dev.ninemmteam.api.events.impl.MovedEvent;
import dev.ninemmteam.api.events.impl.PacketEvent;
import dev.ninemmteam.api.utils.math.Timer;
import dev.ninemmteam.api.utils.player.MovementUtil;
import dev.ninemmteam.api.utils.world.BlockUtil;
import dev.ninemmteam.mod.modules.Module;
import dev.ninemmteam.mod.modules.settings.impl.BooleanSetting;
import dev.ninemmteam.mod.modules.settings.impl.EnumSetting;
import dev.ninemmteam.mod.modules.settings.impl.SliderSetting;
import net.minecraft.entity.EntityType;
import net.minecraft.item.Items;
import net.minecraft.network.packet.s2c.play.EntitySpawnS2CPacket;
import net.minecraft.network.packet.s2c.play.PlayerPositionLookS2CPacket;

public class LongJump extends Module {
    public static LongJump INSTANCE;
    
    private final EnumSetting<Mode> mode = this.add(new EnumSetting<>("Mode", Mode.Strafe));
    
    private final SliderSetting strafeSpeed = this.add(new SliderSetting("Speed", 0.2873, 0.0, 1.0, 1.0E-4, () -> this.mode.is(Mode.Strafe)));
    private final BooleanSetting jump = this.add(new BooleanSetting("Jump", true, () -> this.mode.is(Mode.Strafe)));
    private final SliderSetting lagTime = this.add(new SliderSetting("LagTime", 500.0, 0.0, 1000.0, 1.0, () -> this.mode.is(Mode.Strafe)));
    private final BooleanSetting airStop = this.add(new BooleanSetting("AirStop", false, () -> this.mode.is(Mode.Strafe)));
    
    private final BooleanSetting autoJump = this.add(new BooleanSetting("AutoJump", false, () -> this.mode.is(Mode.NoCheatPlusBoost)));
    private final BooleanSetting autoDisable = this.add(new BooleanSetting("DisableAfterFinished", false, () -> this.mode.is(Mode.NoCheatPlusBoost) || this.mode.is(Mode.NoCheatPlusBow)));
    private final SliderSetting ncpBoost = this.add(new SliderSetting("NCPBoost", 4.25, 1.0, 10.0, () -> this.mode.is(Mode.NoCheatPlusBoost)));
    
    private final SliderSetting charged = this.add(new SliderSetting("Charged", 4, 3, 20, () -> this.mode.is(Mode.NoCheatPlusBow)));
    private final SliderSetting speed = this.add(new SliderSetting("Speed", 2.5, 0.0, 20.0, () -> this.mode.is(Mode.NoCheatPlusBow)));
    private final SliderSetting arrowsToShoot = this.add(new SliderSetting("ArrowsToShoot", 8, 0, 20, () -> this.mode.is(Mode.NoCheatPlusBow)));
    private final SliderSetting fallDistance = this.add(new SliderSetting("FallDistanceToJump", 0.42, 0.0, 2.0, 0.01, () -> this.mode.is(Mode.NoCheatPlusBow)));
    
    private final Timer lagTimer = new Timer();
    private boolean stop;
    private double speedValue;
    private double distance;
    private int stage;
    private boolean boost;
    
    private boolean jumped = false;
    private boolean canBoost = false;
    private boolean boosted = false;
    private float arrowBoost = 0.0f;
    private float shotArrows = 0.0f;
    private boolean stopMovement = false;
    private int tickCounter = 0;
    private int bowState = 0;

    public LongJump() {
        super("LongJump", Category.Movement);
        this.setChinese("远跳");
        INSTANCE = this;
    }

    @Override
    public String getInfo() {
        return ((Mode)this.mode.getValue()).name();
    }

    @Override
    public void onEnable() {
        if (mc.player != null) {
            this.speedValue = MovementUtil.getSpeed(false);
            this.distance = MovementUtil.getDistance2D();
        }
        this.stage = 4;
        
        this.jumped = false;
        this.canBoost = false;
        this.boosted = false;
        this.arrowBoost = 0.0f;
        this.shotArrows = 0.0f;
        this.stopMovement = false;
        this.bowState = 0;
        this.tickCounter = 0;
    }

    @Override
    public void onDisable() {
        this.stage = 0;
        this.speedValue = 0.0;
        this.distance = 0.0;
        this.shotArrows = 0.0f;
        this.arrowBoost = 0.0f;
    }

    @EventListener(priority = 100)
    public void onPacketReceive(PacketEvent.Receive event) {
        if (event.getPacket() instanceof PlayerPositionLookS2CPacket) {
            this.lagTimer.reset();
            this.resetStrafe();
        }
        
        if (this.mode.is(Mode.NoCheatPlusBow) && event.getPacket() instanceof EntitySpawnS2CPacket packet) {
            if (packet.getEntityType() == EntityType.ARROW && packet.getUuid() != null) {
                if (this.shotArrows > 0.0) {
                    this.shotArrows -= 1.0f;
                    this.arrowBoost += 1.0f;
                }
            }
        }
    }

    @EventListener
    public void onMoved(MovedEvent event) {
        if (!nullCheck()) {
            double dx = mc.player.getX() - mc.player.prevX;
            double dz = mc.player.getZ() - mc.player.prevZ;
            this.distance = Math.sqrt(dx * dx + dz * dz);
        }
    }

    @EventListener
    public void onMove(MoveEvent event) {
        if (this.mode.is(Mode.Strafe)) {
            this.handleStrafe(event);
        } else if (this.mode.is(Mode.NoCheatPlusBoost)) {
            this.handleNCPBoost(event);
        }
    }

    private void handleStrafe(MoveEvent event) {
        if (!MovementUtil.isMoving() && this.airStop.getValue()) {
            MovementUtil.setMotionX(0.0);
            MovementUtil.setMotionZ(0.0);
        }

        if (!mc.player.isRiding()
            && !mc.player.isHoldingOntoLadder()
            && !mc.player.getAbilities().flying
            && !mc.player.isFallFlying()
            && MovementUtil.isMoving()) {
            
            if (this.stop) {
                this.stop = false;
            } else if (this.lagTimer.passedMs(this.lagTime.getValueInt())) {
                if (this.stage == 1) {
                    this.speedValue = 1.35 * MovementUtil.getSpeed(false, this.strafeSpeed.getValue()) - 0.01;
                } else if (this.stage != 2 || !mc.player.isOnGround() || !mc.options.jumpKey.isPressed() && !this.jump.getValue()) {
                    if (this.stage == 3) {
                        this.speedValue = this.distance - 0.66 * (this.distance - MovementUtil.getSpeed(false, this.strafeSpeed.getValue()));
                        this.boost = !this.boost;
                    } else {
                        if ((BlockUtil.canCollide(mc.player.getBoundingBox().offset(0.0, MovementUtil.getMotionY(), 0.0)) || mc.player.collidedSoftly)
                            && this.stage > 0) {
                            this.stage = 1;
                        }
                        this.speedValue = this.distance - this.distance / 159.0;
                    }
                } else {
                    double yMotion = 0.3999 + MovementUtil.getJumpSpeed();
                    MovementUtil.setMotionY(yMotion);
                    event.setY(yMotion);
                    this.speedValue = this.speedValue * (this.boost ? 1.6835 : 1.395);
                }

                this.speedValue = Math.min(this.speedValue, 10.0);
                this.speedValue = Math.max(this.speedValue, MovementUtil.getSpeed(false, this.strafeSpeed.getValue()));
                
                double n = mc.player.input.movementForward;
                double n2 = mc.player.input.movementSideways;
                double n3 = mc.player.getYaw();
                
                if (n == 0.0 && n2 == 0.0) {
                    event.setX(0.0);
                    event.setZ(0.0);
                } else if (n != 0.0 && n2 != 0.0) {
                    n *= Math.sin(Math.PI / 4);
                    n2 *= Math.cos(Math.PI / 4);
                }

                event.setX((n * this.speedValue * -Math.sin(Math.toRadians(n3)) + n2 * this.speedValue * Math.cos(Math.toRadians(n3))) * 0.99);
                event.setZ((n * this.speedValue * Math.cos(Math.toRadians(n3)) - n2 * this.speedValue * -Math.sin(Math.toRadians(n3))) * 0.99);
                this.stage++;
            }
        } else {
            this.resetStrafe();
            this.stop = true;
        }
    }

    private void handleNCPBoost(MoveEvent event) {
        if (!MovementUtil.isMoving() && this.jumped) {
            MovementUtil.setMotionX(0.0);
            MovementUtil.setMotionZ(0.0);
            event.setX(0.0);
            event.setZ(0.0);
        }
    }

    @EventListener
    public void onUpdate(MovedEvent event) {
        if (this.mode.is(Mode.NoCheatPlusBoost)) {
            this.updateNCPBoost();
        } else if (this.mode.is(Mode.NoCheatPlusBow)) {
            this.updateNCPPBow();
        }
    }

    private void updateNCPBoost() {
        if (this.jumped && (mc.player.isOnGround() || mc.player.getAbilities().flying)) {
            if (this.autoDisable.getValue() && this.boosted) {
                this.disable();
            }
            this.jumped = false;
        }
        
        if (this.autoJump.getValue() && mc.player.isOnGround() && MovementUtil.isMoving()) {
            mc.player.jump();
            this.jumped = true;
            this.canBoost = true;
        }
        
        if (this.canBoost) {
            MovementUtil.setMotionX(mc.player.getVelocity().getX() * this.ncpBoost.getValue());
            MovementUtil.setMotionZ(mc.player.getVelocity().getZ() * this.ncpBoost.getValue());
            this.boosted = true;
        }
        
        this.canBoost = false;
    }

    private void updateNCPPBow() {
        if (this.arrowBoost <= this.arrowsToShoot.getValueInt()) {
            mc.options.useKey.setPressed(true);
            fentanyl.ROTATION.snapAt(mc.player.getYaw(), -90.0f);
            this.stopMovement = true;
            
            if (mc.player.getItemUseTime() >= this.charged.getValueInt()) {
                if (mc.player.getMainHandStack().getItem() == Items.BOW) {
                    mc.interactionManager.stopUsingItem(mc.player);
                    this.shotArrows += 1.0f;
                }
            }
            this.bowState = 0;
        } else {
            mc.options.useKey.setPressed(false);
            
            if (mc.player.isUsingItem()) {
                mc.interactionManager.stopUsingItem(mc.player);
            }
            
            this.shotArrows = 0.0f;
            
            if (this.bowState == 0) {
                this.bowState = 1;
                this.tickCounter = 5;
            } else if (this.bowState == 1) {
                --this.tickCounter;
                if (this.tickCounter <= 0) {
                    mc.player.jump();
                    double[] dir = MovementUtil.directionSpeed(this.speed.getValue());
                    MovementUtil.setMotionX(dir[0]);
                    MovementUtil.setMotionZ(dir[1]);
                    this.bowState = 2;
                    this.tickCounter = 5;
                }
            } else if (this.bowState == 2) {
                --this.tickCounter;
                if (this.tickCounter <= 0) {
                    this.arrowBoost = 0.0f;
                    this.bowState = 0;
                }
            }
        }
        
        if (this.jumped && (mc.player.isOnGround() || mc.player.getAbilities().flying)) {
            this.jumped = false;
        }
    }

    public void resetStrafe() {
        this.stage = 4;
        this.speedValue = 0.0;
        this.distance = 0.0;
    }

    public enum Mode {
        Strafe,
        NoCheatPlusBoost,
        NoCheatPlusBow
    }
}
