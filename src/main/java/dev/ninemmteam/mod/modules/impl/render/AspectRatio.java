package dev.ninemmteam.mod.modules.impl.render;

import dev.ninemmteam.mod.modules.Module;
import dev.ninemmteam.mod.modules.settings.impl.SliderSetting;

public class AspectRatio extends Module {
   public static AspectRatio INSTANCE;
   public final SliderSetting ratio = this.add(new SliderSetting("Ratio", 1.78, 0.0, 5.0, 0.01));

   public AspectRatio() {
      super("AspectRatio", Module.Category.Render);
      this.setChinese("分辨率");
      INSTANCE = this;
   }
}
