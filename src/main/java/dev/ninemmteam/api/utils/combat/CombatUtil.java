package dev.ninemmteam.api.utils.combat;

import dev.ninemmteam.fentanyl;
import dev.ninemmteam.api.utils.Wrapper;
import dev.ninemmteam.api.utils.math.MathUtil;
import dev.ninemmteam.api.utils.math.Timer;
import dev.ninemmteam.api.utils.player.EntityUtil;
import dev.ninemmteam.api.utils.world.BlockUtil;
import dev.ninemmteam.mod.modules.impl.client.AntiCheat;
import dev.ninemmteam.mod.modules.settings.enums.SwingSide;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.entity.Entity;
import net.minecraft.entity.decoration.EndCrystalEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.network.packet.c2s.play.PlayerInteractEntityC2SPacket;
import net.minecraft.util.Hand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;

public class CombatUtil implements Wrapper {
   public static final Timer breakTimer = new Timer();
   public static boolean terrainIgnore = false;
   public static BlockPos modifyPos;
   public static BlockState modifyBlockState = Blocks.AIR.getDefaultState();

   public static List<PlayerEntity> getEnemies(double range) {
      List<PlayerEntity> list = new ArrayList();

      for (AbstractClientPlayerEntity player : fentanyl.THREAD.getPlayers()) {
         if (isValid(player, range)) {
            list.add(player);
         }
      }

      return list;
   }

   public static void attackCrystal(BlockPos pos, boolean rotate, boolean eatingPause) {
      attackCrystal(new Box(pos), rotate, eatingPause);
   }

   public static void attackCrystal(Box box, boolean rotate, boolean eatingPause) {
      for (EndCrystalEntity entity : BlockUtil.getEndCrystals(box)) {
         attackWithDelay(entity, rotate, eatingPause);
      }
   }

   public static void attackWithDelay(Entity entity, boolean rotate, boolean usingPause) {
      if (breakTimer.passed((long)(AntiCheat.INSTANCE.attackDelay.getValue() * 1000.0))) {
         if (!usingPause || !mc.player.isUsingItem()) {
            attack(entity, rotate);
         }
      }
   }

   public static void attack(Entity entity, boolean rotate) {
      if (entity != null) {
         Vec3d attackVec = MathUtil.getClosestPointToBox(mc.player.getEyePos(), entity.getBoundingBox());
         if (mc.player.getEyePos().distanceTo(attackVec) > AntiCheat.INSTANCE.ieRange.getValue()) {
            return;
         }

         breakTimer.reset();
         if (rotate && AntiCheat.INSTANCE.attackRotate.getValue()) {
            fentanyl.ROTATION.lookAt(attackVec);
         }

         mc.getNetworkHandler().sendPacket(PlayerInteractEntityC2SPacket.attack(entity, mc.player.isSneaking()));
         mc.player.resetLastAttackedTicks();
         EntityUtil.swingHand(Hand.MAIN_HAND, (SwingSide)AntiCheat.INSTANCE.attackSwing.getValue());
         if (rotate && AntiCheat.INSTANCE.attackRotate.getValue()) {
            fentanyl.ROTATION.snapBack();
         }
      }
   }

   public static boolean isntValid(Entity entity, double range) {
      return !isValid(entity, range);
   }

   public static boolean isValid(Entity entity, double range) {
      boolean invalid = entity == null
         || !entity.isAlive()
         || entity.equals(mc.player)
         || entity instanceof PlayerEntity player && fentanyl.FRIEND.isFriend(player)
         || mc.player.getPos().distanceTo(entity.getPos()) > range;
      return !invalid;
   }

   public static boolean isValid(Entity entity) {
      boolean invalid = entity == null
         || !entity.isAlive()
         || entity.equals(mc.player)
         || entity instanceof PlayerEntity player && fentanyl.FRIEND.isFriend(player);
      return !invalid;
   }

   public static PlayerEntity getClosestEnemy(double distance) {
      PlayerEntity closest = null;

      for (PlayerEntity player : getEnemies(distance)) {
         if (closest == null) {
            closest = player;
         } else if (mc.player.squaredDistanceTo(player.getPos()) < mc.player.squaredDistanceTo(closest)) {
            closest = player;
         }
      }

      return closest;
   }
}
