package dev.ninemmteam.mod.modules.impl.movement;

import dev.ninemmteam.api.events.eventbus.EventListener;
import dev.ninemmteam.api.events.impl.UpdateEvent;
import dev.ninemmteam.asm.accessors.ILivingEntity;
import dev.ninemmteam.mod.modules.Module;

public class NoJumpDelay extends Module {
   public static NoJumpDelay INSTANCE;

   public NoJumpDelay() {
      super("NoJumpDelay", Module.Category.Movement);
      this.setChinese("无跳跃冷却");
      INSTANCE = this;
   }

   @EventListener
   public void onUpdate(UpdateEvent event) {
      ((ILivingEntity)mc.player).setLastJumpCooldown(0);
   }
}
