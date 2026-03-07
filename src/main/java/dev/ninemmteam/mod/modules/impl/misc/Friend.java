package dev.ninemmteam.mod.modules.impl.misc;

import dev.ninemmteam.fentanyl;
import dev.ninemmteam.mod.modules.Module;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.hit.EntityHitResult;

public class Friend extends Module {
   public static Friend INSTANCE;

   public Friend() {
      super("Friend", Module.Category.Misc);
      this.setChinese("好友");
      INSTANCE = this;
   }

   @Override
   public void onEnable() {
      if (nullCheck()) {
         this.disable();
      } else {
         if (mc.crosshairTarget instanceof EntityHitResult entityHitResult && entityHitResult.getEntity() instanceof PlayerEntity player) {
            fentanyl.FRIEND.friend(player);
         }

         this.disable();
      }
   }
}
