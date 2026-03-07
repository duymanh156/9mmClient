package dev.ninemmteam.mod.modules.impl.combat;

import dev.ninemmteam.api.events.eventbus.EventListener;
import dev.ninemmteam.api.events.impl.UpdateEvent;
import dev.ninemmteam.api.utils.combat.CombatUtil;
import dev.ninemmteam.api.utils.player.EntityUtil;
import dev.ninemmteam.api.utils.world.BlockPosX;
import dev.ninemmteam.api.utils.world.BlockUtil;
import dev.ninemmteam.core.impl.FriendMineTracker;
import dev.ninemmteam.mod.modules.Module;
import dev.ninemmteam.mod.modules.impl.exploit.Blink;
import dev.ninemmteam.mod.modules.impl.player.SpeedMine;
import dev.ninemmteam.mod.modules.settings.impl.BooleanSetting;
import dev.ninemmteam.mod.modules.settings.impl.SliderSetting;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.TrapdoorBlock;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.PickaxeItem;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;

public class AutoMine extends Module {
   public static AutoMine INSTANCE;
   public final SliderSetting targetRange = this.add(new SliderSetting("TargetRange", 6.0, 0.0, 8.0, 0.1).setSuffix("m"));
   public final SliderSetting range = this.add(new SliderSetting("Range", 6.0, 0.0, 8.0, 0.1).setSuffix("m"));
   private final BooleanSetting burrow = this.add(new BooleanSetting("Burrow", true));
   private final BooleanSetting head = this.add(new BooleanSetting("Head", true));
   private final BooleanSetting face = this.add(new BooleanSetting("Face", true));
   private final BooleanSetting down = this.add(new BooleanSetting("Down", false));
   private final BooleanSetting surround = this.add(new BooleanSetting("Surround", true));
   private final BooleanSetting cevPause = this.add(new BooleanSetting("CevPause", true));
   private final BooleanSetting forceDouble = this.add(new BooleanSetting("ForceDouble", false));
   private final BooleanSetting avoidFriend = this.add(new BooleanSetting("AvoidFriend", true));
   public static final List<Block> hard = Arrays.asList(
      Blocks.OBSIDIAN, Blocks.ENDER_CHEST, Blocks.NETHERITE_BLOCK, Blocks.CRYING_OBSIDIAN, Blocks.RESPAWN_ANCHOR, Blocks.ANCIENT_DEBRIS, Blocks.ANVIL
   );

   public AutoMine() {
      super("AutoMine", Module.Category.Combat);
      this.setChinese("自动挖掘");
      INSTANCE = this;
   }

   @EventListener
   public void onUpdate(UpdateEvent event) {
      if (!Blink.INSTANCE.isOn() || !Blink.INSTANCE.pauseModule.getValue()) {
         PlayerEntity player = CombatUtil.getClosestEnemy(this.targetRange.getValue());
         if (player != null) {
            this.doBreak(player);
         }
      }
   }

   private void doBreak(PlayerEntity player) {
      BlockPos pos = EntityUtil.getEntityPos(player, true);
      if (SpeedMine.getBreakPos() == null
         || SpeedMine.getBreakPos().equals(SpeedMine.secondPos)
         || SpeedMine.secondPos == null
         || mc.world.isAir(SpeedMine.secondPos)
         || !this.forceDouble.getValue()) {
         double[] yOffset = new double[]{-0.8, 0.3, 1.1};
         double[] xzOffset = new double[]{0.3, -0.3};

         for (PlayerEntity entity : CombatUtil.getEnemies(this.targetRange.getValue())) {
            for (double y : yOffset) {
               for (double x : xzOffset) {
                  for (double z : xzOffset) {
                     BlockPos offsetPos = new BlockPosX(entity.getX() + x, entity.getY() + y, entity.getZ() + z);
                     if (this.canBreak(offsetPos) && offsetPos.equals(SpeedMine.getBreakPos())) {
                        return;
                     }
                  }
               }
            }
         }

         List<Float> yList = new ArrayList();
         if (this.down.getValue()) {
            yList.add(-0.8F);
         }

         if (this.head.getValue()) {
            yList.add(2.3F);
         }

         if (this.burrow.getValue()) {
            yList.add(0.3F);
         }

         if (this.face.getValue()) {
            yList.add(1.1F);
         }

         Iterator var32 = yList.iterator();

         while (var32.hasNext()) {
            double y = ((Float)var32.next()).floatValue();

            for (double offset : xzOffset) {
               BlockPos offsetPos = new BlockPosX(player.getX() + offset, player.getY() + y, player.getZ() + offset);
               if (this.canBreak(offsetPos)) {
                  SpeedMine.INSTANCE.mine(offsetPos);
                  return;
               }
            }
         }

         var32 = yList.iterator();

         while (var32.hasNext()) {
            double y = ((Float)var32.next()).floatValue();

            for (double offsetx : xzOffset) {
               for (double offset2 : xzOffset) {
                  BlockPos offsetPos = new BlockPosX(player.getX() + offset2, player.getY() + y, player.getZ() + offsetx);
                  if (this.canBreak(offsetPos)) {
                     SpeedMine.INSTANCE.mine(offsetPos);
                     return;
                  }
               }
            }
         }

         if (this.surround.getValue()) {
            for (Direction i : Direction.values()) {
               if (i != Direction.UP
                  && i != Direction.DOWN
                  && !(Math.sqrt(mc.player.getEyePos().squaredDistanceTo(pos.offset(i).toCenterPos())) > this.range.getValue())
                  && (mc.world.isAir(pos.offset(i)) || pos.offset(i).equals(SpeedMine.getBreakPos()))
                  && this.canPlaceCrystal(pos.offset(i), false)
                  && !pos.offset(i).equals(SpeedMine.secondPos)) {
                  return;
               }
            }

            ArrayList<BlockPos> list = new ArrayList();

            for (Direction ix : Direction.values()) {
               if (ix != Direction.UP
                  && ix != Direction.DOWN
                  && !(Math.sqrt(mc.player.getEyePos().squaredDistanceTo(pos.offset(ix).toCenterPos())) > this.range.getValue())
                  && this.canBreak(pos.offset(ix))
                  && this.canPlaceCrystal(pos.offset(ix), true)
                  && !this.isSurroundPos(pos.offset(ix))) {
                  list.add(pos.offset(ix));
               }
            }

            if (!list.isEmpty()) {
               SpeedMine.INSTANCE.mine((BlockPos)list.stream().min(Comparator.comparingDouble(E -> E.getSquaredDistance(mc.player.getEyePos()))).get());
            } else {
               for (Direction ixx : Direction.values()) {
                  if (ixx != Direction.UP
                     && ixx != Direction.DOWN
                     && !(Math.sqrt(mc.player.getEyePos().squaredDistanceTo(pos.offset(ixx).toCenterPos())) > this.range.getValue())
                     && this.canBreak(pos.offset(ixx))
                     && this.canPlaceCrystal(pos.offset(ixx), false)) {
                     list.add(pos.offset(ixx));
                  }
               }

               if (!list.isEmpty()) {
                  SpeedMine.INSTANCE.mine((BlockPos)list.stream().min(Comparator.comparingDouble(E -> E.getSquaredDistance(mc.player.getEyePos()))).get());
               }
            }
         }
      }
   }

   private boolean isSurroundPos(BlockPos pos) {
      for (Direction i : Direction.values()) {
         if (i != Direction.UP && i != Direction.DOWN) {
            BlockPos self = EntityUtil.getPlayerPos(true);
            if (self.offset(i).equals(pos)) {
               return true;
            }
         }
      }

      return false;
   }

   public boolean canPlaceCrystal(BlockPos pos, boolean block) {
      BlockPos obsPos = pos.down();
      BlockPos boost = obsPos.up();
      return (BlockUtil.getBlock(obsPos) == Blocks.BEDROCK || BlockUtil.getBlock(obsPos) == Blocks.OBSIDIAN || !block)
         && BlockUtil.noEntityBlockCrystal(boost, true, true)
         && BlockUtil.noEntityBlockCrystal(boost.up(), true, true);
   }

   private boolean isObsidian(BlockPos pos) {
      return mc.player.getEyePos().distanceTo(pos.toCenterPos()) <= SpeedMine.INSTANCE.range.getValue()
         && hard.contains(BlockUtil.getBlock(pos))
         && BlockUtil.getClickSideStrict(pos) != null;
   }

   private boolean canBreak(BlockPos pos) {
      if (this.avoidFriend.getValue() && FriendMineTracker.INSTANCE != null && FriendMineTracker.INSTANCE.isFriendMining(pos)) {
          return false;
      }
      
      BlockState state = mc.world.getBlockState(pos);
      if (state.getBlock() instanceof TrapdoorBlock && pos.equals(SpeedMine.getBreakPos())) {
          return false;
      }
      
      return this.isObsidian(pos)
         && (BlockUtil.getClickSideStrict(pos) != null || pos.equals(SpeedMine.getBreakPos()))
         && (
            !pos.equals(SpeedMine.secondPos)
               || !(mc.player.getMainHandStack().getItem() instanceof PickaxeItem)
                  && !SpeedMine.INSTANCE.autoSwitch.getValue()
                  && !SpeedMine.INSTANCE.noGhostHand.getValue()
         );
   }
}