package dev.ninemmteam.asm.mixins;

import dev.ninemmteam.api.interfaces.IChatHudLineHook;
import dev.ninemmteam.api.utils.math.FadeUtils;
import net.minecraft.client.gui.hud.ChatHudLine.Visible;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

@Mixin(Visible.class)
public class MixinChatHudLineVisible implements IChatHudLineHook {
   @Unique
   private int id = 0;
   @Unique
   private boolean sync = false;
   @Unique
   private FadeUtils fade;

   @Override
   public int fentany1Client$getMessageId() {
      return this.id;
   }

   @Override
   public void fentany1Client$setMessageId(int id) {
      this.id = id;
   }

   @Override
   public boolean fentany1Client$getSync() {
      return this.sync;
   }

   @Override
   public void fentany1Client$setSync(boolean sync) {
      this.sync = sync;
   }

   @Override
   public FadeUtils fentany1Client$getFade() {
      return this.fade;
   }

   @Override
   public void fentany1Client$setFade(FadeUtils fade) {
      this.fade = fade;
   }
}
