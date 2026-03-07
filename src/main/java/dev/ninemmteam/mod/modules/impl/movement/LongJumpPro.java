package dev.ninemmteam.mod.modules.impl.movement;

import dev.ninemmteam.fentanyl;
import dev.ninemmteam.api.events.eventbus.EventListener;
import dev.ninemmteam.api.events.impl.MoveEvent;
import dev.ninemmteam.api.events.impl.PacketEvent;
import dev.ninemmteam.api.events.impl.TickEvent;
import dev.ninemmteam.api.events.impl.SendMovementPacketsEvent;
import dev.ninemmteam.api.utils.player.MovementUtil;
import dev.ninemmteam.mod.modules.Module;
import dev.ninemmteam.mod.modules.settings.impl.BooleanSetting;
import dev.ninemmteam.mod.modules.settings.impl.EnumSetting;
import dev.ninemmteam.mod.modules.settings.impl.SliderSetting;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket;
import net.minecraft.network.packet.s2c.play.PlayerPositionLookS2CPacket;
import net.minecraft.client.gui.screen.ingame.CraftingScreen;

public class LongJumpPro extends Module {
    private final EnumSetting<JumpMode> mode = this.add(new EnumSetting<>("Mode", JumpMode.NORMAL));
    private final SliderSetting boost = this.add(new SliderSetting("Boost", 4.5, 0.1, 10.0, 0.1, () -> this.mode.getValue() == JumpMode.NORMAL));
    private final BooleanSetting autoDisable = this.add(new BooleanSetting("AutoDisable", true));
    private int stage;
    private double distance;
    private double speed;
    private int airTicks;
    private int groundTicks;

    public LongJumpPro() {
        super("LongJumpPro", Category.Movement);
        this.setChinese("远跳Pro");
    }

    @Override
    public String getInfo() {
        return this.mode.getValue().name();
    }

    @Override
    public void onEnable() {
        this.groundTicks = 0;
    }

    @Override
    public void onDisable() {
        this.stage = 0;
        this.distance = 0.0;
        fentanyl.TIMER.reset();
    }

    @EventListener
    public void onTick(TickEvent event) {
        if (event.isPost() || nullCheck()) {
            return;
        }
        double dx = mc.player.getX() - mc.player.prevX;
        double dz = mc.player.getZ() - mc.player.prevZ;
        this.distance = Math.sqrt(dx * dx + dz * dz);
    }

    @EventListener
    public void onPlayerMove(MoveEvent event) {
        if (this.mode.getValue() == JumpMode.NORMAL) {
            int amplifier;
            if (nullCheck() || !MovementUtil.isMoving()) {
                return;
            }
            double speedEffect = 1.0;
            double slowEffect = 1.0;
            if (mc.player.hasStatusEffect(StatusEffects.SPEED)) {
                StatusEffectInstance effect = mc.player.getActiveStatusEffects().get(StatusEffects.SPEED);
                amplifier = effect.getAmplifier();
                speedEffect = 1.0 + 0.2 * (amplifier + 1);
            }
            if (mc.player.hasStatusEffect(StatusEffects.SLOWNESS)) {
                StatusEffectInstance effect = mc.player.getActiveStatusEffects().get(StatusEffects.SLOWNESS);
                amplifier = effect.getAmplifier();
                slowEffect = 1.0 + 0.2 * (amplifier + 1);
            }
            double base = 0.2873 * speedEffect / slowEffect;
            if (this.stage == 0) {
                this.stage = 1;
                this.speed = this.boost.getValue() * base - 0.01;
            } else if (this.stage == 1) {
                this.stage = 2;
                MovementUtil.setMotionY(0.42);
                event.setY(0.42);
                this.speed *= 2.149;
            } else if (this.stage == 2) {
                this.stage = 3;
                double moveSpeed = 0.66 * (this.distance - base);
                this.speed = this.distance - moveSpeed;
            } else {
                if (!mc.world.isSpaceEmpty(mc.player, mc.player.getBoundingBox().offset(0.0, mc.player.getVelocity().getY(), 0.0)) || mc.player.collidedSoftly) {
                    this.stage = 0;
                }
                this.speed = this.distance - this.distance / 159.0;
            }
            this.speed = Math.max(this.speed, base);
            double[] motion = MovementUtil.directionSpeed(this.speed);
            event.setX(motion[0]);
            event.setZ(motion[1]);
        }
    }

    @EventListener
    public void onPlayerUpdate(SendMovementPacketsEvent event) {
        if (nullCheck()) {
            return;
        }
        if (this.mode.getValue() == JumpMode.GLIDE) {
            if (mc.player.isRiding() || mc.player.isHoldingOntoLadder() || mc.player.isFallFlying()) {
                return;
            }
            if (mc.player.isOnGround()) {
                this.distance = 0.0;
            }
            float direction = mc.player.getYaw() + (mc.player.input.movementForward < 0.0f ? 180 : 0) 
                + (mc.player.input.movementSideways > 0.0f ? -90.0f * (mc.player.input.movementForward < 0.0f ? -0.5f : (mc.player.input.movementForward > 0.0f ? 0.5f : 1.0f)) : 0.0f) 
                - (mc.player.input.movementSideways < 0.0f ? -90.0f * (mc.player.input.movementForward < 0.0f ? -0.5f : (mc.player.input.movementForward > 0.0f ? 0.5f : 1.0f)) : 0.0f);
            double dx = Math.cos(Math.toRadians(direction + 90.0f));
            double dz = Math.sin(Math.toRadians(direction + 90.0f));
            if (!mc.player.collidedSoftly && !mc.player.isOnGround()) {
                ++this.airTicks;
                if (mc.player.input.jumping) {
                    mc.player.networkHandler.sendPacket(new PlayerMoveC2SPacket.PositionAndOnGround(mc.player.getX(), 2.147483647E9, mc.player.getZ(), false));
                }
                this.groundTicks = 0;
                if (!mc.player.collidedSoftly) {
                    double yVel = mc.player.getVelocity().getY();
                    if (yVel == -0.07190068807140403) {
                        MovementUtil.setMotionY(yVel * 0.35f);
                    } else if (yVel == -0.10306193759436909) {
                        MovementUtil.setMotionY(yVel * 0.55f);
                    } else if (yVel == -0.13395038817442878) {
                        MovementUtil.setMotionY(yVel * 0.67f);
                    } else if (yVel == -0.16635183030382) {
                        MovementUtil.setMotionY(yVel * 0.69f);
                    } else if (yVel == -0.19088711097794803) {
                        MovementUtil.setMotionY(yVel * 0.71f);
                    } else if (yVel == -0.21121925191528862) {
                        MovementUtil.setMotionY(yVel * 0.2f);
                    } else if (yVel == -0.11979897632390576) {
                        MovementUtil.setMotionY(yVel * 0.93f);
                    } else if (yVel == -0.18758479151225355) {
                        MovementUtil.setMotionY(yVel * 0.72f);
                    } else if (yVel == -0.21075983825251726) {
                        MovementUtil.setMotionY(yVel * 0.76f);
                    }
                    if (this.getJumpCollisions(70.0) < 0.5) {
                        if (yVel == -0.23537393014173347) {
                            MovementUtil.setMotionY(yVel * 0.03f);
                        } else if (yVel == -0.08531999505205401) {
                            MovementUtil.setMotionY(yVel * -0.5);
                        } else if (yVel == -0.03659320313669756) {
                            MovementUtil.setMotionY(yVel * -0.1f);
                        } else if (yVel == -0.07481386749524899) {
                            MovementUtil.setMotionY(yVel * -0.07f);
                        } else if (yVel == -0.0732677700939672) {
                            MovementUtil.setMotionY(yVel * -0.05f);
                        } else if (yVel == -0.07480988066790395) {
                            MovementUtil.setMotionY(yVel * -0.04f);
                        } else if (yVel == -0.0784000015258789) {
                            MovementUtil.setMotionY(yVel * 0.1f);
                        } else if (yVel == -0.08608320193943977) {
                            MovementUtil.setMotionY(yVel * 0.1f);
                        } else if (yVel == -0.08683615560584318) {
                            MovementUtil.setMotionY(yVel * 0.05f);
                        } else if (yVel == -0.08265497329678266) {
                            MovementUtil.setMotionY(yVel * 0.05f);
                        } else if (yVel == -0.08245009535659828) {
                            MovementUtil.setMotionY(yVel * 0.05f);
                        } else if (yVel == -0.08244005633718426) {
                            MovementUtil.setMotionY(-0.08243956442521608);
                        } else if (yVel == -0.08243956442521608) {
                            MovementUtil.setMotionY(-0.08244005590677261);
                        }
                        if (yVel > -0.1 && yVel < -0.08 && !mc.player.isOnGround() && mc.player.input.sneaking) {
                            MovementUtil.setMotionY(-1.0E-4f);
                        }
                    } else if (yVel < -0.2 && yVel > -0.24) {
                        MovementUtil.setMotionY(yVel * 0.7);
                    } else if (yVel < -0.25 && yVel > -0.32) {
                        MovementUtil.setMotionY(yVel * 0.8);
                    } else if (yVel < -0.35 && yVel > -0.8) {
                        MovementUtil.setMotionY(yVel * 0.98);
                    } else if (yVel < -0.8 && yVel > -1.6) {
                        MovementUtil.setMotionY(yVel * 0.99);
                    }
                }
                fentanyl.TIMER.set(0.85f);
                double[] jumpFactor = new double[]{0.420606, 0.417924, 0.415258, 0.412609, 0.409977, 0.407361, 0.404761, 0.402178, 0.399611, 0.39706, 0.394525, 0.392, 0.3894, 0.38644, 0.383655, 0.381105, 0.37867, 0.37625, 0.37384, 0.37145, 0.369, 0.3666, 0.3642, 0.3618, 0.35945, 0.357, 0.354, 0.351, 0.348, 0.345, 0.342, 0.339, 0.336, 0.333, 0.33, 0.327, 0.324, 0.321, 0.318, 0.315, 0.312, 0.309, 0.307, 0.305, 0.303, 0.3, 0.297, 0.295, 0.293, 0.291, 0.289, 0.287, 0.285, 0.283, 0.281, 0.279, 0.277, 0.275, 0.273, 0.271, 0.269, 0.267, 0.265, 0.263, 0.261, 0.259, 0.257, 0.255, 0.253, 0.251, 0.249, 0.247, 0.245, 0.243, 0.241, 0.239, 0.237};
                if (mc.player.input.sneaking) {
                    try {
                        MovementUtil.setMotionXZ(dx * jumpFactor[this.airTicks - 1] * 3.0, dz * jumpFactor[this.airTicks - 1] * 3.0);
                    } catch (ArrayIndexOutOfBoundsException ignored) {
                    }
                    return;
                }
                MovementUtil.setMotionXZ(0.0, 0.0);
                return;
            }
            fentanyl.TIMER.reset();
            this.airTicks = 0;
            ++this.groundTicks;
            MovementUtil.setMotionXZ(mc.player.getVelocity().getX() / 13.0, mc.player.getVelocity().getZ() / 13.0);
            if (this.groundTicks == 1) {
                mc.player.networkHandler.sendPacket(new PlayerMoveC2SPacket.PositionAndOnGround(mc.player.getX(), mc.player.getY(), mc.player.getZ(), mc.player.isOnGround()));
                mc.player.networkHandler.sendPacket(new PlayerMoveC2SPacket.PositionAndOnGround(mc.player.getX() + 0.0624, mc.player.getY(), mc.player.getZ(), mc.player.isOnGround()));
                mc.player.networkHandler.sendPacket(new PlayerMoveC2SPacket.PositionAndOnGround(mc.player.getX(), mc.player.getY() + 0.419, mc.player.getZ(), mc.player.isOnGround()));
                mc.player.networkHandler.sendPacket(new PlayerMoveC2SPacket.PositionAndOnGround(mc.player.getX() + 0.0624, mc.player.getY(), mc.player.getZ(), mc.player.isOnGround()));
                mc.player.networkHandler.sendPacket(new PlayerMoveC2SPacket.PositionAndOnGround(mc.player.getX(), mc.player.getY() + 0.419, mc.player.getZ(), mc.player.isOnGround()));
            }
            if (this.groundTicks > 2) {
                this.groundTicks = 0;
                MovementUtil.setMotionXZ(dx * 0.3, dz * 0.3);
                MovementUtil.setMotionY(0.424f);
            }
        }
    }

    @EventListener
    public void onPacketReceive(PacketEvent.Receive event) {
        if (nullCheck() || mc.currentScreen instanceof CraftingScreen) {
            return;
        }
        if (event.getPacket() instanceof PlayerPositionLookS2CPacket && this.autoDisable.getValue()) {
            this.disable();
        }
    }

    private double getJumpCollisions(double d) {
        return 1.0;
    }

    public enum JumpMode {
        NORMAL,
        GLIDE
    }
}
