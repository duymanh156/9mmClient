package dev.ninemmteam.api.events.impl;

import dev.ninemmteam.api.events.Event;
import net.minecraft.text.Text;

public class ReceiveMessageEvent extends Event {
   private static final ReceiveMessageEvent INSTANCE = new ReceiveMessageEvent();
   public Text message;

   private ReceiveMessageEvent() {
   }

   public static ReceiveMessageEvent get(Text message) {
      INSTANCE.message = message;
      INSTANCE.setCancelled(false);
      return INSTANCE;
   }
}
