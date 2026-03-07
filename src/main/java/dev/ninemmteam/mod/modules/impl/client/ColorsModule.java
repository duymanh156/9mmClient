package dev.ninemmteam.mod.modules.impl.client;

import dev.ninemmteam.mod.modules.Module;
import dev.ninemmteam.mod.modules.settings.impl.ColorSetting;
import java.awt.Color;

public class ColorsModule extends Module {
   public static ColorsModule INSTANCE;
   public final ColorSetting clientColor = this.add(new ColorSetting("Color", new Color(-6710785)).allowClientColor(false));

   public ColorsModule() {
      super("Colors", Module.Category.Client);
      this.setChinese("颜色");
      INSTANCE = this;
   }

   @Override
   public void enable() {
      this.state = true;
   }

   @Override
   public void disable() {
      this.state = true;
   }

   @Override
   public boolean isOn() {
      return true;
   }
}
