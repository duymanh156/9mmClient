package dev.ninemmteam.mod.modules.impl.movement;

import dev.ninemmteam.mod.modules.Module;

public class EntityControl extends Module {
   public static EntityControl INSTANCE;

   public EntityControl() {
      super("EntityControl", Module.Category.Movement);
      this.setChinese("骑行控制");
      INSTANCE = this;
   }
}
