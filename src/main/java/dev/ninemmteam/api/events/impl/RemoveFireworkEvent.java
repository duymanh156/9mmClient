package dev.ninemmteam.api.events.impl;

import dev.ninemmteam.api.events.Event;
import net.minecraft.entity.projectile.FireworkRocketEntity;

public class RemoveFireworkEvent extends Event {
   public static final RemoveFireworkEvent instance = new RemoveFireworkEvent();
   private FireworkRocketEntity entity;

   private RemoveFireworkEvent() {
   }

   public static RemoveFireworkEvent get(FireworkRocketEntity entity) {
      instance.entity = entity;
      instance.setCancelled(false);
      return instance;
   }

   public FireworkRocketEntity getRocketEntity() {
      return this.entity;
   }
}
