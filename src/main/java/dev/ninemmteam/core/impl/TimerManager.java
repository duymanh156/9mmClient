package dev.ninemmteam.core.impl;

import dev.ninemmteam.api.utils.player.MovementUtil;
import dev.ninemmteam.mod.modules.impl.movement.Speed;
import dev.ninemmteam.mod.modules.impl.player.TimerModule;

public class TimerManager {
   public float timer = 1.0F;
   public float lastTimer;

   public void set(float factor) {
      if (factor < 0.1F) {
         factor = 0.1F;
      }

      this.timer = factor;
   }

   public void reset() {
      this.timer = this.getDefault();
      this.lastTimer = this.timer;
   }

   public void tryReset() {
      if (this.lastTimer != this.getDefault()) {
         this.reset();
      }
   }

   public float get() {
      return this.timer;
   }

   public float getDefault() {
      if (TimerModule.INSTANCE.isOn()) {
         return TimerModule.INSTANCE.boostKey.isPressed() ? TimerModule.INSTANCE.boost.getValueFloat() : TimerModule.INSTANCE.multiplier.getValueFloat();
      } else {
         return (Speed.INSTANCE.isOn() && Speed.INSTANCE.timerBoost1.getValue() != 1) && (MovementUtil.isMoving() || MovementUtil.isJumping())
                 ? Speed.INSTANCE.timerBoost1.getValueFloat()
                 : 1.0f;
      }
   }

}

