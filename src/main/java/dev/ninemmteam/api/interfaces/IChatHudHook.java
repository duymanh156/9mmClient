package dev.ninemmteam.api.interfaces;

import net.minecraft.text.Text;

public interface IChatHudHook {
   void fentany1Client$addMessage(Text var1, int var2);

   void fentany1Client$addMessage(Text var1);

   void fentany1Client$addMessageOutSync(Text var1, int var2);
}
