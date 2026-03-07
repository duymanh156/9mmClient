package dev.ninemmteam.api.events.impl;

import dev.ninemmteam.api.events.Event;

public class KeyboardInputEvent extends Event {
   private static final KeyboardInputEvent INSTANCE = new KeyboardInputEvent();

   private KeyboardInputEvent() {
   }

   public static KeyboardInputEvent get() {
      INSTANCE.setCancelled(false);
      return INSTANCE;
   }
}
