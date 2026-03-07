package dev.ninemmteam.mod.modules.impl.movement;

import dev.ninemmteam.fentanyl;
import dev.ninemmteam.api.events.eventbus.EventListener;
import dev.ninemmteam.api.events.impl.MoveEvent;
import dev.ninemmteam.api.events.impl.MovedEvent;
import dev.ninemmteam.api.events.impl.PacketEvent;
import dev.ninemmteam.api.utils.math.FadeUtils;
import dev.ninemmteam.api.utils.math.Timer;
import dev.ninemmteam.api.utils.path.BaritoneUtil;
import dev.ninemmteam.api.utils.player.EntityUtil;
import dev.ninemmteam.api.utils.player.MovementUtil;
import dev.ninemmteam.api.utils.world.BlockUtil;
import dev.ninemmteam.mod.modules.Module;
import dev.ninemmteam.mod.modules.settings.impl.BooleanSetting;
import dev.ninemmteam.mod.modules.settings.impl.EnumSetting;
import dev.ninemmteam.mod.modules.settings.impl.SliderSetting;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.network.packet.s2c.play.EntityVelocityUpdateS2CPacket;
import net.minecraft.network.packet.s2c.play.ExplosionS2CPacket;
import net.minecraft.network.packet.s2c.play.PlayerPositionLookS2CPacket;
import net.minecraft.util.math.Vec2f;
import net.minecraft.util.math.Vec3d;

public class Speed extends Module {
   public static Speed INSTANCE;
   public final EnumSetting mode = this.add(new EnumSetting("Mode", Mode.General));
   private final BooleanSetting slow = this.add(new BooleanSetting("Slowness", false, () -> this.mode.is(Mode.General)));
   private final SliderSetting lagTime = this.add(new SliderSetting("LagTime", 500.0, 0.0, 1000.0, 1.0, () -> this.mode.is(Mode.General)));
   public final SliderSetting timerBoost1 = this.add(new SliderSetting("Timer Boost", 1.09, 0.80, 1.20, 0.01, () -> this.mode.is(Mode.General)));
   private final SliderSetting strafeSpeed = this.add(new SliderSetting("BaseSpeed", 0.2873, 0.0, 1.0, 1.0E-4, () -> this.mode.is(Mode.General)));
   private final SliderSetting jumpMotion = this.add(new SliderSetting("JumpMotion", 0.42, 0, 1, 0.01, () -> this.mode.is(Mode.General)));

   private final SliderSetting horizon = this.add(new SliderSetting("H-Factor", 1.0, 0.0, 5.0, 0.01, () -> this.mode.is(Mode.DamageBoost)));
   private final SliderSetting vertical = this.add(new SliderSetting("V-Factor", 1.0, 0.0, 5.0, 0.01, () -> this.mode.is(Mode.DamageBoost)));
   private final SliderSetting coolDown = this.add(new SliderSetting("CoolDown", 1000.0, 0.0, 5000.0, 1.0, () -> this.mode.is(Mode.DamageBoost)));

   private final BooleanSetting inWater = this.add(new BooleanSetting("InWater", false, () -> this.mode.is(Mode.Misc)));
   private final BooleanSetting inBlock = this.add(new BooleanSetting("InBlock", false, () -> this.mode.is(Mode.Misc)));
   private final BooleanSetting airStop = this.add(new BooleanSetting("AirStop", false, () -> this.mode.is(Mode.Misc)));
   private final BooleanSetting jump = this.add(new BooleanSetting("AutoJump", true, () -> this.mode.is(Mode.Misc)));
   private final Timer expTimer = new Timer();
   private final Timer lagTimer = new Timer();
   private final Timer timer = new Timer();
   private final Timer timer2 = new Timer();
   private final FadeUtils end = new FadeUtils(500L);
   long lastMs = 0L;
   boolean moving = false;
   private boolean stop;
   private double speed;
   private double distance;
   private int stage;
   private double lastExp;
   private boolean boost;

   public Speed() {
      super("Speed", Category.Movement);
      this.setChinese("加速");
      INSTANCE = this;
   }

   @Override
   public void onDisable() {
      fentanyl.TIMER.reset();
   }

   @Override
   public void onEnable() {
      if (mc.player != null) {
         this.speed = MovementUtil.getSpeed(false);
         this.distance = MovementUtil.getDistance2D();
         fentanyl.TIMER.reset();
      }

      this.stage = 4;
   }

   @Override
   public void onRender2D(DrawContext drawContext, float tickDelta) {
      if (MovementUtil.isMoving() && !EntityUtil.isInsideBlock()) {
         if (!this.moving) {
            {
               this.lastMs = 0L;
            }

            this.moving = true;
         }

         this.timer.reset();
         if (this.timer2.passed(this.lastMs)) {
            fentanyl.TIMER.reset();
         }
      } else {
         if (this.moving) {
            fentanyl.TIMER.reset();{
               this.timer.setMs(Math.max(this.lastMs - this.timer2.getMs(), 0L));
            }

            this.moving = false;
         }

         this.end.setLength(this.timer.getMs());
         this.end.reset();
      }
   }

   @EventListener(priority = 100)
   public void invoke(PacketEvent.Receive event) {
      if (!BaritoneUtil.isActive()) {
         if (event.getPacket() instanceof EntityVelocityUpdateS2CPacket packet) {
            if (mc.player != null && packet.getEntityId() == mc.player.getId() && vertical.getValue() != 0) {
               double speed = Math.sqrt(packet.getVelocityX() * packet.getVelocityX() + packet.getVelocityZ() * packet.getVelocityZ());
               this.lastExp = this.expTimer.passed(this.coolDown.getValueInt()) ? speed : speed - this.lastExp;
               if (this.lastExp > 0.0) {
                  this.expTimer.reset();
                  this.speed = this.speed + this.lastExp * this.horizon.getValue();
                  this.distance = this.distance + this.lastExp * this.horizon.getValue();
                  if (MovementUtil.getMotionY() > 0.0 && this.vertical.getValue() != 0.0) {
                     MovementUtil.setMotionY(jumpMotion.getValue() * this.vertical.getValue());
                  }
               }
            }
         } else if (mc.player != null && event.getPacket() instanceof ExplosionS2CPacket packetx
                 && horizon.getValue() != 0
                 && mc.player.getPos().distanceTo(new Vec3d(packetx.getX(), packetx.getY(), packetx.getZ())) < 15.0) {
             double speed = Math.sqrt(packetx.getPlayerVelocityX() * packetx.getPlayerVelocityX() + packetx.getPlayerVelocityZ() * packetx.getPlayerVelocityZ());
             this.lastExp = this.expTimer.passed(this.coolDown.getValueInt()) ? speed : speed - this.lastExp;
             if (this.lastExp > 0.0) {
                 this.expTimer.reset();
                 this.speed = this.speed + this.lastExp * this.horizon.getValue();
                 this.distance = this.distance + this.lastExp * this.horizon.getValue();
                 if (MovementUtil.getMotionY() > 0.0) {
                     MovementUtil.setMotionY(MovementUtil.getMotionY() * this.vertical.getValue());
                 }
             }
         }

          if (event.getPacket() instanceof PlayerPositionLookS2CPacket) {
            this.lagTimer.reset();
            this.resetStrafe();
         }
      }
   }

   @EventListener
   public void onMove(MovedEvent event) {
      if (!nullCheck()) {
          double dx = 0;
          if (mc.player != null) {
              dx = mc.player.getX() - mc.player.prevX;
          }
          double dz = 0;
          if (mc.player != null) {
              dz = mc.player.getZ() - mc.player.prevZ;
          }
          this.distance = Math.sqrt(dx * dx + dz * dz);
      }
   }

   @EventListener
   public void invoke(MoveEvent event) {
      if (!MovementUtil.isMoving() && this.airStop.getValue()) {
         MovementUtil.setMotionX(0.0);
         MovementUtil.setMotionZ(0.0);
      }

       if (mc.player != null && (this.inWater.getValue() || !mc.player.isSubmergedInWater() && !mc.player.isTouchingWater() && !mc.player.isInLava())
               && !mc.player.isRiding()
               && !mc.player.isHoldingOntoLadder()
               && (this.inBlock.getValue() || !EntityUtil.isInsideBlock())
               && !mc.player.getAbilities().flying
               && !mc.player.isFallFlying()
               && MovementUtil.isMoving()) {
           if (this.stop) {
               this.stop = false;
           } else if (this.lagTimer.passed(this.lagTime.getValueInt())) {
               if (this.stage == 1) {
                   this.speed = 1.35 * MovementUtil.getSpeed(this.slow.getValue(), this.strafeSpeed.getValue()) - 0.01;
               } else if (this.stage != 2 || !mc.player.isOnGround() || !mc.options.jumpKey.isPressed() && !this.jump.getValue()) {
                   if (this.stage == 3) {
                       this.speed = this.distance - 0.66 * (this.distance - MovementUtil.getSpeed(this.slow.getValue(), this.strafeSpeed.getValue()));
                       this.boost = !this.boost;
                   } else {
                       if ((BlockUtil.canCollide(null, mc.player.getBoundingBox().offset(0.0, MovementUtil.getMotionY(), 0.0)) || mc.player.collidedSoftly)
                               && this.stage > 0) {
                           this.stage = 1;
                       }

                       this.speed = this.distance - this.distance / 159.0;
                   }
               } else {
                   double yMotion = jumpMotion.getValue();
                   MovementUtil.setMotionY(yMotion);
                   event.setY(yMotion);
                   this.speed = this.speed * (this.boost ? 1.6835 : 1.395);
               }

               this.speed = Math.min(this.speed, 10.0);
               this.speed = Math.max(this.speed, MovementUtil.getSpeed(this.slow.getValue(), this.strafeSpeed.getValue()));
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

               event.setX((n * this.speed * -Math.sin(Math.toRadians(n3)) + n2 * this.speed * Math.cos(Math.toRadians(n3))) * 0.99);
               event.setZ((n * this.speed * Math.cos(Math.toRadians(n3)) - n2 * this.speed * -Math.sin(Math.toRadians(n3))) * 0.99);
               this.stage++;
           }
       }
   }

   public Vec2f handleStrafeMotion(float speed) {
       float forward = 0;
       if (mc.player != null) {
           forward = mc.player.input.movementForward;
       }
       float strafe = 0;
       if (mc.player != null) {
           strafe = mc.player.input.movementSideways;
       }
       float yaw = 0;
       if (mc.player != null) {
           yaw = mc.player.prevYaw + (mc.player.getYaw() - mc.player.prevYaw) * mc.getRenderTickCounter().getTickDelta(true);
       }
       if (forward == 0.0F && strafe == 0.0F) {
         return Vec2f.ZERO;
      } else {
         if (forward != 0.0F) {
            if (strafe >= 1.0F) {
               yaw += forward > 0.0F ? -45.0F : 45.0F;
               strafe = 0.0F;
            } else if (strafe <= -1.0F) {
               yaw += forward > 0.0F ? 45.0F : -45.0F;
               strafe = 0.0F;
            }

            if (forward > 0.0F) {
               forward = 1.0F;
            } else if (forward < 0.0F) {
               forward = -1.0F;
            }
         }

         float rx = (float)Math.cos(Math.toRadians(yaw));
         float rz = (float)(-Math.sin(Math.toRadians(yaw)));
         return new Vec2f(forward * speed * rz + strafe * speed * rx, forward * speed * rx - strafe * speed * rz);
      }
   }

   public void resetStrafe() {
      this.stage = 4;
      this.speed = 0.0;
      this.distance = 0.0;
   }
   public static enum Mode {
      General,
      DamageBoost,
      Misc,
   }
}
