package dev.ninemmteam.core.impl;

import dev.ninemmteam.fentanyl;
import dev.ninemmteam.api.events.eventbus.EventListener;
import dev.ninemmteam.api.events.impl.BlockBreakingProgressEvent;
import dev.ninemmteam.api.events.impl.ClientTickEvent;
import dev.ninemmteam.api.events.impl.PacketEvent;
import dev.ninemmteam.api.events.impl.ServerConnectBeginEvent;
import dev.ninemmteam.api.utils.Wrapper;
import dev.ninemmteam.api.utils.math.FadeUtils;
import dev.ninemmteam.api.utils.math.Timer;
import dev.ninemmteam.mod.modules.Module;
import dev.ninemmteam.mod.modules.impl.client.AntiCheat;
import dev.ninemmteam.mod.modules.impl.player.SpeedMine;
import dev.ninemmteam.mod.modules.impl.render.BreakESP;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.network.packet.s2c.play.BlockBreakingProgressS2CPacket;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;

public class BreakManager implements Wrapper {
   public final ConcurrentHashMap<Integer, BreakManager.BreakData> breakMap = new ConcurrentHashMap();
   public final ConcurrentHashMap<Integer, BreakManager.BreakData> doubleMap = new ConcurrentHashMap();

   public BreakManager() {
      fentanyl.EVENT_BUS.subscribe(this);
   }

   @EventListener
   public void onServerConnectBegin(ServerConnectBeginEvent event) {
      this.breakMap.clear();
      this.doubleMap.clear();
   }

   @EventListener
   public void onTick(ClientTickEvent event) {
      if (!Module.nullCheck()) {
         if (AntiCheat.INSTANCE.detectDouble.getValue()) {
            for (int i : fentanyl.BREAK.doubleMap.keySet()) {
               BreakManager.BreakData breakData = (BreakManager.BreakData) fentanyl.BREAK.doubleMap.get(i);
               if (breakData == null
                  || breakData.getEntity() == null
                  || mc.world.isAir(breakData.pos)
                  || breakData.timer
                     .passedMs(
                        Math.max(AntiCheat.INSTANCE.minTimeout.getValue() * 1000.0, breakData.breakTime * AntiCheat.INSTANCE.doubleMineTimeout.getValue())
                     )) {
                  fentanyl.BREAK.doubleMap.remove(i);
               }
            }
         }

         for (BreakManager.BreakData breakData : this.breakMap.values()) {
            breakData.breakTime = Math.max(BreakESP.getBreakTime(breakData.pos, false), 50.0);
            if (SpeedMine.unbreakable(breakData.pos)) {
               breakData.fade.setLength(0L);
               breakData.complete = false;
               breakData.failed = true;
            } else if (mc.world.isAir(breakData.pos)) {
               breakData.fade.setLength(0L);
               breakData.complete = true;
               breakData.failed = false;
            } else if (!breakData.complete && breakData.timer.passedMs(breakData.breakTime * AntiCheat.INSTANCE.breakTimeout.getValue())) {
               breakData.fade.setLength(0L);
               breakData.failed = true;
            } else {
               breakData.fade.setLength((long)breakData.breakTime);
            }
         }
      }
   }

   @EventListener
   public void onPacket(PacketEvent.Receive event) {
      if (!Module.nullCheck()) {
         if (event.getPacket() instanceof BlockBreakingProgressS2CPacket packet) {
            if (packet.getPos() == null) {
               return;
            }

            BreakManager.BreakData breakData = new BreakManager.BreakData(packet.getPos(), packet.getEntityId(), false);
            if (breakData.getEntity() == null) {
               return;
            }

            if (MathHelper.sqrt((float)breakData.getEntity().getEyePos().squaredDistanceTo(packet.getPos().toCenterPos())) > 8.0F) {
               return;
            }

            if (AntiCheat.INSTANCE.detectDouble.getValue() && packet.getProgress() != 255) {
               if (packet.getProgress() != 0) {
                  BreakManager.BreakData doublePos = (BreakManager.BreakData)this.doubleMap.get(packet.getEntityId());
                  if (doublePos != null) {
                     doublePos.pos = packet.getPos();
                     doublePos.timer.reset();
                  } else if (!SpeedMine.unbreakable(packet.getPos())) {
                     this.doubleMap.put(packet.getEntityId(), new BreakManager.BreakData(packet.getPos(), packet.getEntityId(), true));
                  }

                  return;
               }

               BreakManager.BreakData doublePos = (BreakManager.BreakData)this.doubleMap.get(packet.getEntityId());
               if (doublePos != null && doublePos.pos.equals(packet.getPos()) && !doublePos.timer.passedS(150.0)) {
                  return;
               }
            }

            BreakManager.BreakData current = (BreakManager.BreakData)this.breakMap.get(packet.getEntityId());
            if (current != null && !current.failed && current.pos.equals(packet.getPos())) {
               return;
            }

            this.breakMap.put(packet.getEntityId(), breakData);
            fentanyl.EVENT_BUS.post(BlockBreakingProgressEvent.get(packet.getPos(), packet.getEntityId(), packet.getProgress()));
            if (AntiCheat.INSTANCE.detectDouble.getValue() && !this.doubleMap.containsKey(packet.getEntityId()) && !SpeedMine.unbreakable(packet.getPos())) {
               this.doubleMap.put(packet.getEntityId(), new BreakManager.BreakData(packet.getPos(), packet.getEntityId(), true));
            }
         }
      }
   }

   public boolean isMining(BlockPos pos) {
      return this.isMining(pos, true);
   }

   public boolean isMining(BlockPos pos, boolean self) {
      if (self && SpeedMine.getBreakPos() != null && SpeedMine.getBreakPos().equals(pos)) {
         return true;
      } else {
         for (BreakManager.BreakData breakData : this.breakMap.values()) {
            if (breakData.getEntity() != null
               && !(breakData.getEntity().getEyePos().distanceTo(pos.toCenterPos()) > 7.0)
               && !breakData.failed
               && breakData.pos.equals(pos)) {
               return true;
            }
         }

         return false;
      }
   }

   public static class BreakData {
      public BlockPos pos;
      private final int entityId;
      public final FadeUtils fade;
      public final Timer timer;
      public double breakTime;
      public boolean failed = false;
      public boolean complete = false;

      public BreakData(BlockPos pos, int entityId, boolean extraBreak) {
         this.pos = pos;
         this.entityId = entityId;
         this.breakTime = Math.max(BreakESP.getBreakTime(pos, extraBreak), 50.0);
         this.fade = new FadeUtils((long)this.breakTime);
         this.timer = new Timer();
      }

      public Entity getEntity() {
         if (Wrapper.mc.world == null) {
            return null;
         } else {
            Entity entity = Wrapper.mc.world.getEntityById(this.entityId);
            return entity instanceof PlayerEntity ? entity : null;
         }
      }
   }
}
