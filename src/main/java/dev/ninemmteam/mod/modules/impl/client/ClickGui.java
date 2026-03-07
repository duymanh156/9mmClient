package dev.ninemmteam.mod.modules.impl.client;

import dev.ninemmteam.fentanyl;
import dev.ninemmteam.api.events.eventbus.EventListener;
import dev.ninemmteam.api.events.impl.UpdateEvent;
import dev.ninemmteam.mod.gui.earth.EarthClickGui;
import dev.ninemmteam.mod.gui.earth.frame.CategoryFrame;
import dev.ninemmteam.mod.modules.Module;
import dev.ninemmteam.mod.modules.settings.impl.BooleanSetting;
import dev.ninemmteam.mod.modules.settings.impl.ColorSetting;
import dev.ninemmteam.mod.modules.settings.impl.SliderSetting;
import java.awt.Color;

import net.minecraft.client.sound.PositionedSoundInstance;
import net.minecraft.sound.SoundEvents;

public class ClickGui extends Module {
   private static ClickGui INSTANCE;
   public final BooleanSetting abc = this.add(new BooleanSetting("ABC", true));
   public final BooleanSetting autoSave = this.add(new BooleanSetting("AutoSave", true));
   public final BooleanSetting font = this.add(new BooleanSetting("Font", false));
   public final BooleanSetting shadow = this.add(new BooleanSetting("Shadow", true));
   public final BooleanSetting sound = this.add(new BooleanSetting("Sound", true).setParent());
   public final BooleanSetting guiSound = this.add(new BooleanSetting("GuiSound", true));
   public final SliderSetting soundPitch = this.add(new SliderSetting("SoundPitch", 1.0, 0.0, 2.0, 0.1, this.sound::isOpen));
   public final SliderSetting buttonheight = this.add(new SliderSetting("ButtonHeight", 1, 0, 7));
   public final SliderSetting textOffset = this.add(new SliderSetting("TextOffset", -1.0, -5.0, 5.0, 1.0));
   public final SliderSetting titleOffset = this.add(new SliderSetting("TitleOffset", -1.0, -5.0, 5.0, 1.0));
   public final BooleanSetting blur = this.add(new BooleanSetting("Blur", false).setParent());
   public final SliderSetting radius = this.add(new SliderSetting("Radius", 58.4, 0.0, 100.0, this.blur::isOpen));
   public final BooleanSetting elements = this.add(new BooleanSetting("Elements", false).setParent());
   public final BooleanSetting line = this.add(new BooleanSetting("Line", true, this.elements::isOpen));
   public final BooleanSetting gear = this.add(new BooleanSetting("Gear", true, this.elements::isOpen));
   public final BooleanSetting colors = this.add(new BooleanSetting("Colors", false).setParent());
   public final ColorSetting color = this.add(new ColorSetting("Color", new Color(0, 80, 255, 255), this.colors::isOpen));
   public final ColorSetting bgEnable = this.add(new ColorSetting("BgEnable", new Color(0, 80, 255, 200), this.colors::isOpen));
   public final ColorSetting bgColor = this.add(new ColorSetting("BgColor", new Color(30, 30, 30, 200), this.colors::isOpen));
   public final ColorSetting bgButton = this.add(new ColorSetting("BgButton", new Color(45, 45, 45, 200), this.colors::isOpen));
   public final ColorSetting backGround = this.add(new ColorSetting("BackGround", new Color(20, 20, 20, 230), this.colors::isOpen));
   public final ColorSetting textColor = this.add(new ColorSetting("TextColor", new Color(-1, true), this.colors::isOpen));
   public final ColorSetting enableTextColor = this.add(new ColorSetting("EnableColor", new Color(0, 120, 255, 255), this.colors::isOpen));
   public final ColorSetting tint = this.add(new ColorSetting("Tint", new Color(-1, true)).injectBoolean(false));
   public final ColorSetting endColor = this.add(new ColorSetting("End", new Color(1342903851, true), () -> this.tint.booleanValue));
   public double alphaValue = 1.0;

   public ClickGui() {
      super("ClickGui", Module.Category.Client);
      this.setChinese("点击界面");
      INSTANCE = this;
   }

   public static ClickGui getInstance() {
      return INSTANCE;
   }

   public void refreshFrames() {
      if (mc != null && mc.currentScreen instanceof EarthClickGui) {
         EarthClickGui gui = EarthClickGui.getInstance();
         for (var frame : gui.getFrames()) {
            if (frame instanceof CategoryFrame) {
               frame.init();
            }
         }
      }
   }

   @Override
   public void onEnable() {
      if (nullCheck()) {
         this.disable();
      } else {
         if (this.guiSound.getValue() && mc.getSoundManager() != null) {
            mc.getSoundManager().play(PositionedSoundInstance.master(SoundEvents.UI_BUTTON_CLICK, this.soundPitch.getValueFloat()));
         }

         EarthClickGui.getInstance().init();
         mc.setScreen(EarthClickGui.getInstance());
      }
   }

   @Override
   public void onDisable() {
      if (mc.currentScreen instanceof EarthClickGui) {
         mc.currentScreen.close();
      }

      if (this.guiSound.getValue() && mc.getSoundManager() != null) {
         mc.getSoundManager().play(PositionedSoundInstance.master(SoundEvents.UI_BUTTON_CLICK, this.soundPitch.getValueFloat()));
      }

      if (this.autoSave.getValue()) {
         fentanyl.save();
      }
   }

   @EventListener
   public void onUpdate(UpdateEvent event) {
      if (!(mc.currentScreen instanceof EarthClickGui)) {
         this.disable();
      }
   }
}
